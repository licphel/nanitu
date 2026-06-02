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

package net.nanitu.audio.io;

import net.nanitu.audio.Encoding;

import java.io.Serial;

/**
 * Thrown when an audio format is unrecognized, malformed, or not supported
 * by the active backend.
 *
 * <p>Common causes:
 * <ul>
 *   <li>The container header declares a codec ({@link Encoding}) that the
 *       decoder does not implement.</li>
 *   <li>A required header field is missing or contains an out-of-range value.</li>
 *   <li>The backend does not support the requested sample rate, bits depth,
 *       or channel count.</li>
 * </ul>
 *
 * @see AudioInputStream
 */
public class AudioFormatException extends RuntimeException {
  @Serial
  private static final long serialVersionUID = 2026053100L;

  /**
   * Creates a new {@code AudioFormatException} with the given detail message.
   *
   * @param message description of why the format was rejected
   */
  public AudioFormatException(String message) {
    super(message);
  }
}