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

/**
 * Represents an audio output device (hardware or software).
 *
 * <p>A mixer is the entry point for all audio playback. It owns the connection
 * to the underlying audio backend and is the factory for {@link Clip} instances.
 *
 * <p>Typical usage:
 * <pre>{@code
 * Mixer mixer = mixerProvider.create();
 *
 * Clip clip = mixer.getClip();
 * clip.open(AudioFormat.stereo(44100, 16), pcmData);
 * clip.play();
 *
 * // In the main loop:
 * mixer.pollEvents();
 * }</pre>
 *
 * <p>Mixers are generally long-lived; close them only when shutting down
 * the audio subsystem entirely.
 *
 * @see MixerInfo
 * @see Clip
 */
public interface Mixer extends AutoCloseable {
  /**
   * Returns static information about this mixer implementation.
   *
   * @return name and hardware-acceleration flag for this mixer
   */
  MixerInfo info();

  /**
   * Processes pending mixer events.
   *
   * <p>Must be called regularly (e.g. once per frame) to update playback state
   * and trigger loop callbacks. Typically called from the application loop.
   */
  void pollEvents();

  /**
   * Creates a new, unopened {@link Clip} backed by this mixer.
   *
   * <p>Call {@link Clip#open} on the returned instance before starting playback.
   * The clip's resources are tied to this mixer; closing the mixer while clips
   * are still playing results in undefined behavior.
   *
   * @return a new, unopened clip
   */
  Clip getClip();

  /**
   * Enqueues a block of work to run on the audio thread.
   *
   * <p>Thread-safe: may be called from any thread.
   *
   * @param work the audio commands to execute
   */
  void submit(Runnable work);

  /**
   * Closes an audio mixer.
   * Generally, audio mixers are always alive.
   */
  @Override
  void close();
}