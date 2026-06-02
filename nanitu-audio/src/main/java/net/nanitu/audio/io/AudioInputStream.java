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

import net.nanitu.audio.AudioFormat;
import net.nanitu.audio.Encoding;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Abstract base for decoded PCM audio streams.
 *
 * <p>Subclasses wrap a specific container format (e.g. WAVE) and expose its
 * decoded samples as a plain {@link InputStream} of raw PCM bytes.
 * The sample layout — rate, bits depth, channel count, and byte order — is
 * described by {@link #format()}, which is valid as soon as the decoder is
 * constructed and before any bytes are read.
 *
 * <p>Use the factory method {@link #open(InputStream)} to obtain an instance
 * without knowing the underlying format in advance.
 *
 * <p>Mark/reset is not supported; once bytes are consumed they cannot be
 * replayed unless the underlying stream itself supports seeking.
 *
 * @see WaveInputStream
 * @see AudioFormat
 */
public abstract class AudioInputStream extends InputStream {
  /**
   * Probes {@code in} for a known audio container and returns a decoder for it.
   *
   * <p>The method reads the first 12 bytes to identify the container, then
   * resets the stream so the decoder can re-read the full header.
   * Currently supported containers:
   * <ul>
   *   <li>RIFF/WAVE — PCM ({@link Encoding#PCM_SIGNED} / {@link Encoding#PCM_UNSIGNED}),
   *       IEEE float ({@link Encoding#PCM_FLOAT}), and WAVE extensible variants</li>
   * </ul>
   *
   * <p>The returned stream does <em>not</em> own the underlying {@code in};
   * callers are responsible for closing it.
   *
   * @param in source stream; must support {@link InputStream#mark(int) mark}/reset
   *           or be wrapped automatically by this method into a
   *           {@link BufferedInputStream}
   * @return a decoder whose {@link #format()} reflects the detected PCM layout
   * @throws IOException          if an I/O error occurs while reading the header
   * @throws AudioFormatException if the container is not recognized or the header is malformed
   */
  public static AudioInputStream open(InputStream in) throws IOException, AudioFormatException {
    BufferedInputStream buffered = in instanceof BufferedInputStream b ? b : new BufferedInputStream(in);
    buffered.mark(12);
    byte[] header = buffered.readNBytes(12);
    buffered.reset();

    if (header.length >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F' && header[8] == 'W' && header[9] == 'A' && header[10] == 'V' && header[11] == 'E') {
      return new WaveInputStream(buffered);
    }

    throw new AudioFormatException("Unsupported audio format");
  }

  /**
   * Returns the PCM format of the decoded audio data.
   *
   * <p>The format is determined at construction time and remains constant for
   * the lifetime of the stream. Read it before consuming any bytes so that
   * downstream code can allocate buffers of the right size and interpret
   * samples correctly.
   *
   * @return audio format describing sample rate, bit depth, channel count, and byte order
   */
  public abstract AudioFormat format();

  /**
   * Always throws {@link IOException} — audio decoders do not support reset.
   *
   * @throws IOException always
   */
  @Override
  public void reset() throws IOException {
    throw new IOException("Audio decoding cannot be reset");
  }

  /**
   * Returns {@code false} — mark/reset is not supported by audio decoders.
   *
   * @return {@code false} always
   */
  @Override
  public boolean markSupported() {
    return false;
  }
}