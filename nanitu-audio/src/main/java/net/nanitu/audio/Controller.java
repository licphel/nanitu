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
 * Identifies the real-time controls available on a {@link Clip}.
 *
 * <p>Pass a {@code Controller} constant to {@link Clip#get} or {@link Clip#set}
 * to read or change a specific parameter while the clip is playing.
 *
 * @see Clip
 */
public enum Controller {
  /**
   * Gain (amplitude) multiplier.
   *
   * <p>A value of {@code 1.0} means full volume as opened; {@code 0.0} is
   * silent. Values greater than {@code 1.0} amplify the signal. Negative
   * values are clamped to {@code 0.0}.
   */
  VOLUME,

  /**
   * Playback speed relative to the clip's original rate.
   *
   * <p>A value of {@code 1.0} means normal speed; {@code 2.0} doubles the
   * speed (and raises pitch by one octave); {@code 0.5} halves it.
   * Negative values are clamped to {@code 0.0}.
   */
  PITCH,

  /**
   * Playback position within the current repetition, in seconds.
   *
   * <p>Read-only in practice: the value is updated asynchronously by the
   * audio backend during playback. Writing a position offset is supported
   * by the backend but may not be perfectly accurate for looping clips.
   */
  POSITION
}
