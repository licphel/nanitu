/*
 * MIT License
 *
 * Copyright (c) 2026 Licphel
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

package net.fmhi.gfx.text;

import net.fmhi.gfx.Device;
import net.fmhi.gfx.text.raster.Glyph;
import net.fmhi.gfx.text.spi.FontProvider;
import net.fmhi.util.Service;
import org.jspecify.annotations.Nullable;

import java.io.FileInputStream;
import java.nio.ByteBuffer;

/**
 * A scalable font resource providing glyph metrics, glyph-index lookup, and on-demand rasterization.
 *
 * <p>Font instances are acquired via {@link #open(Device, String)}. Each font must be
 * {@link #close() closed} when no longer needed to release native resources.
 *
 * <p>All metrics are computed at the current {@link #resolution() resolution}.
 * Call {@link #setResolution(float)} to adjust render quality before rasterizing glyphs.
 */
public interface Font extends AutoCloseable {
  /** Default font size, in pixels. */
  float DEFAULT_SIZE = 16.0F;
  /** Plain weight with no italic — the default style. */
  int REGULAR = 0;
  /** Bold weight flag. */
  int BOLD = 1;
  /** Italic slant flag. */
  int ITALIC = 2;
  /** Underline decoration flag. */
  int UNDERLINE = 4;
  /** Strikethrough decoration flag. */
  int STRIKETHROUGH = 8;

  /**
   * Opens a font from the given file path.
   *
   * @param device the graphics device
   * @param path   the file system path to the font file
   * @return the opened font, or {@code null} if no font backend is available or font file not found
   */
  static @Nullable Font open(Device device, String path) {
    try {
      return Service.get(FontProvider.class).create(device, new FileInputStream(path));
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Returns the scaled metrics of this font at the current resolution.
   *
   * @return the font metrics at the current resolution
   */
  FontMetrics metrics();

  /**
   * Checks whether this font covers the given Unicode code point.
   *
   * @param codepoint the Unicode code point to look up
   * @return {@code true} if this font contains a glyph for the code point
   */
  boolean hasGlyph(int codepoint);

  /**
   * Returns the internal glyph index for the given Unicode code point.
   *
   * @param codepoint the Unicode code point to look up
   * @return the glyph index, or {@code 0} if the code point is not covered by this font
   */
  int getGlyphIndex(int codepoint);

  @Override
  void close();

  /**
   * Rasterizes a glyph to the texture atlas and returns its positioning data.
   *
   * @param glyphIndex the glyph index within the font
   * @param fontStyle  the style bitmask, combining flags defined on {@code Font}
   * @return the rasterized glyph, or {@code null} if the glyph has no visual representation
   */
  @Nullable Glyph rasterizeGlyph(int glyphIndex, int fontStyle);

  /**
   * Sets the resolution used for glyph rasterization.
   *
   * @param res the resolution, in pixels
   */
  void setResolution(float res);

  /**
   * Returns the current rasterization resolution.
   *
   * @return the resolution, in pixels
   */
  float resolution();

  /**
   * Returns the raw font file content as a read-only byte buffer.
   *
   * @return the font file data
   */
  ByteBuffer fileData();
}
