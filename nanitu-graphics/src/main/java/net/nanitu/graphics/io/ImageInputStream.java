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

package net.nanitu.graphics.io;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Abstract base for decoded image streams.
 *
 * <p>Subclasses wrap a specific image format (e.g. PNG) and expose decoded
 * pixel data as a plain {@link InputStream} of raw RGBA bytes. Image dimensions and channel count are described by
 * {@link #info()}.
 *
 * <p>Use {@link #open(InputStream)} to obtain an instance without knowing
 * the underlying format.
 *
 * @see PngInputStream
 * @see ImageInfo
 */
public abstract class ImageInputStream extends InputStream {
  /**
   * Probes {@code in} for a known image format and returns a decoder.
   *
   * <p>Currently supported: PNG ({@link PngInputStream}).
   *
   * @param in source stream; must support mark/reset or be wrapped
   * @return a decoder whose {@link #info()} reflects the image dimensions
   * @throws IOException if the format is not recognized
   */
  public static ImageInputStream open(InputStream in) throws IOException {
    BufferedInputStream buffered = in instanceof BufferedInputStream b ? b : new BufferedInputStream(in);
    buffered.mark(8);
    byte[] sig = buffered.readNBytes(8);
    buffered.reset();

    // PNG signature
    if (sig.length >= 8 && sig[0] == (byte) 0x89 && sig[1] == 'P' && sig[2] == 'N' && sig[3] == 'G' && sig[4] == 0x0D && sig[5] == 0x0A && sig[6] == 0x1A && sig[7] == 0x0A) {
      return new PngInputStream(buffered);
    }

    throw new ImageFormatException("Unsupported image format");
  }

  /**
   * Returns the image format describing dimensions and channels.
   *
   * @return image format
   */
  public abstract ImageInfo info();
}
