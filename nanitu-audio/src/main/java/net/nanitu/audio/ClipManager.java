/*
 * MIT License
 *
 * Copyright (c) 2026 Mellowhue
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.nanitu.audio;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Background service that automatically closes {@link Clip}s once they finish playing.
 *
 * <p>Register a clip after starting playback; the manager's cleanup thread
 * polls all registered clips every 100 ms and closes any for which
 * {@link Clip#shouldClose()} returns {@code true}. This removes the need for
 * callers to track playback completion manually.
 *
 * <pre>{@code
 * ClipManager manager = new ClipManager();
 * Clip clip = mixer.getClip();
 * clip.open(format, data);
 * clip.play();
 * manager.register(clip);  // clip is closed automatically when done
 * }</pre>
 *
 * <p>Clips that are still playing when {@link #close()} is called are closed
 * immediately. The cleanup thread is a daemon thread and does not prevent JVM
 * shutdown.
 *
 * @see Clip
 */
public final class ClipManager implements AutoCloseable {
  private final Map<Integer, Clip> clips = new ConcurrentHashMap<>();
  private final Thread cleanupThread;
  private volatile boolean running = true;

  /**
   * Creates a new manager and starts the background cleanup thread.
   *
   * <p>The cleanup thread runs as a daemon; no explicit shutdown is required
   * unless clips need to be closed before JVM exit, in which case call
   * {@link #close()}.
   */
  public ClipManager() {
    cleanupThread = new Thread(this::cleanupLoop, "ClipManager-Cleanup");
    cleanupThread.setDaemon(true);
    cleanupThread.start();
  }

  /**
   * Registers a clip for automatic lifecycle management.
   *
   * <p>After registration, the background thread will call {@link Clip#close()}
   * on the clip the next time it finds {@link Clip#shouldClose()} returning
   * {@code true}. Registering the same clip instance more than once has no
   * additional effect (identity-based deduplication).
   *
   * @param clip the clip to manage
   */
  public void register(Clip clip) {
    clips.put(System.identityHashCode(clip), clip);
  }

  /**
   * Removes a clip from automatic management without closing it.
   *
   * <p>Use this when you want to take back manual control of a clip's
   * lifecycle before it finishes playing. Has no effect if the clip is
   * not currently registered.
   *
   * @param clip the clip to stop managing
   */
  public void unregister(Clip clip) {
    clips.remove(System.identityHashCode(clip));
  }

  /**
   * Returns the number of clips currently registered with this manager.
   *
   * <p>This count includes clips that have already finished playing but have
   * not yet been cleaned up by the background thread.
   *
   * @return number of registered clips
   */
  public int activeCount() {
    return clips.size();
  }

  @SuppressWarnings("BusyWait")
  private void cleanupLoop() {
    while (running) {
      try {
        Thread.sleep(100);

        for (Map.Entry<Integer, Clip> entry : clips.entrySet()) {
          Clip clip = entry.getValue();

          /*
           * Do not check Clip.isPlaying here.
           * We do not guarantee that method will be implemented properly
           * on every backend, but Clip.shouldClose does so.
           */
          if (clip.shouldClose()) {
            clip.close();
            clips.remove(entry.getKey());
          }
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }
  }

  /**
   * Stops the cleanup thread and closes all remaining registered clips.
   *
   * <p>After this call the manager must not be used again. Clips that were
   * still playing are stopped and released immediately.
   */
  @Override
  public void close() {
    running = false;
    cleanupThread.interrupt();

    for (Clip clip : clips.values()) {
      clip.close();
    }
  }
}