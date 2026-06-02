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

import org.jspecify.annotations.Nullable;

/**
 * A short audio clip loaded entirely into memory for low-latency playback.
 *
 * <p>Clips are optimized for sounds that must start instantly and may be
 * triggered many times — footsteps, gunshots, UI beeps, etc. The raw PCM
 * data is uploaded to the audio backend once via {@link #open} and can be
 * replayed any number of times without re-decoding.
 *
 * <p>Multiple simultaneous playbacks of the same clip are supported: each call
 * to {@link #play()} (or {@link #loop(int)}) starts an independent voice.
 *
 * <p>Lifecycle:
 * <ol>
 *   <li>Obtain a {@code Clip} from {@link Mixer#getClip()}.
 *   <li>Call {@link #open} once to supply format and PCM data.
 *   <li>Call {@link #play()} or {@link #loop(int)} as many times as needed.
 *   <li>Call {@link #close()} when the clip is no longer needed, or register
 *       it with a {@link ClipManager} for automatic cleanup.
 * </ol>
 *
 * @see ClipManager
 */
public interface Clip extends AutoCloseable {
  /**
   * Passed to {@link #loop(int)} to request infinite looping.
   *
   * <p>When used as the {@code count} argument, the clip repeats until
   * {@link #stop()} is explicitly called.
   */
  int LOOP_CONTINUOUSLY = Integer.MAX_VALUE;

  /**
   * Uploads PCM data to the audio backend and prepares the clip for playback.
   *
   * <p>Must be called exactly once before any playback method.
   * The contents of {@code data} are copied by the backend, so the array may
   * be discarded or reused after this call returns.
   *
   * @param format the PCM format of {@code data}
   * @param data   raw PCM bytes in the layout described by {@code format}
   * @throws IllegalStateException    if the clip has already been opened
   * @throws IllegalArgumentException if {@code format} is not supported by the backend
   */
  void open(AudioFormat format, byte[] data);

  /**
   * Returns whether {@link #open} has been called on this clip.
   *
   * <p>A clip that has been {@link #close() closed} may return {@code false}.
   *
   * @return {@code true} if the clip is open and ready for playback
   */
  boolean isOpen();

  /**
   * Pauses all active playback voices of this clip immediately.
   *
   * <p>Has no effect if the clip is already paused.
   * After calling {@code pause()}, {@link #isPlaying()} returns {@code false},
   * but this clip is not about to close.
   *
   * <p>It differs from {@link #stop()} since {@code pause()} does not reset
   * the playing progress.
   */
  void pause();

  /**
   * Stops all active playback voices of this clip immediately.
   *
   * <p>Has no effect if the clip is already stopped.
   * After calling {@code stop()}, {@link #isPlaying()} returns {@code false}.
   */
  void stop();

  /**
   * Starts playback, repeating {@code count} times in total.
   *
   * <p>Each call starts a new playback from the beginning.
   *
   * @param count total number of times to play ({@code 1} = once, {@code 2} = twice, etc.),
   *              or {@link #LOOP_CONTINUOUSLY} to loop until {@link #stop()} is called
   * @throws IllegalArgumentException if {@code count} is negative
   */
  void loop(int count);

  /**
   * Starts playback of the clip once.
   *
   * <p>Equivalent to {@link #loop(int) loop(1)}.
   * If the clip is already playing, this starts another instance
   * (the clip can be played multiple times simultaneously).
   */
  default void play() {
    loop(1);
  }

  /**
   * Returns whether this clip is currently playing.
   *
   * <p>Returns {@code false} if the clip has never been started, has been
   * {@link #stop() stopped}, or has finished all scheduled repetitions.
   *
   * @return {@code true} if at least one playback voice is active
   */
  boolean isPlaying();

  /**
   * Returns whether this clip should be closed by its {@link ClipManager}.
   *
   * <p>The default implementation returns {@code true} when the clip is open
   * but not playing. Implementations or callers that need to defer cleanup
   * (e.g. clips that have not yet started) may override this.
   *
   * @return {@code true} if the clip manager may safely close this clip
   */
  boolean shouldClose();

  /**
   * Returns the current value of the given controller.
   *
   * <p>Values are read from the local Java-side cache and reflect the last
   * value passed to {@link #set}; they do not round-trip through the audio
   * backend. {@link Controller#POSITION} is an exception: it is updated
   * asynchronously as playback progresses.
   *
   * @param ctr the controller to read
   * @return current value of {@code ctr}
   */
  float get(Controller ctr);

  /**
   * Updates the value of the given controller.
   *
   * <p>The change is applied asynchronously on the audio thread.
   * Negative values for {@link Controller#VOLUME} and {@link Controller#PITCH}
   * are clamped to {@code 0.0}.
   *
   * @param ctr   the controller to update
   * @param value the new value
   */
  void set(Controller ctr, float value);

  /**
   * Returns the PCM format this clip was opened with, or {@code null} if
   * {@link #open} has not yet been called.
   *
   * @return audio format, or {@code null} before {@link #open}
   */
  @Nullable AudioFormat format();

  /**
   * Releases the OpenAL resources held by this clip.
   *
   * <p>If this clip is registered with a {@link ClipManager}, it will be
   * closed automatically after playback ends — manual calls to this method
   * are generally not needed.
   */
  @Override
  void close();
}