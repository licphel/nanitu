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

package net.nanitu.gfx.text;

import net.nanitu.gfx.Device;
import net.nanitu.gfx.text.spi.FontProvider;
import net.nanitu.util.Service;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;

/**
 * A scalable font resource providing glyph metrics, glyph-index lookup, and on-demand rasterization.
 *
 * <p>Font instances are acquired via {@link #open(String)} rather than direct construction.
 * Each font must be {@link #close() closed} when no longer needed to release native resources.
 *
 * <p>All metrics are computed at the current {@link #resolution() resolution}. Call
 * {@link #setResolution(float)} to adjust render quality before rasterizing glyphs.
 */
public interface Font extends AutoCloseable {
  /** The default line height, in pixels, used for initial metrics calculation. */
  float BASIC_LINE_HEIGHT = 16.0F;

  /**
   * Opens a font from the given file path.
   *
   * @param path the file system path to the font file
   * @return the opened font, or {@code null} if no {@link FontProvider} is available
   */
  static Font open(String path) {
    var p = Service.get(FontProvider.class);
    return p != null ? p.create(path) : null;
  }

  /**
   * Returns the scaled metrics of this font.
   *
   * @return the font metrics at the current resolution
   */
  FontMetrics metrics();

  /**
   * Checks whether this font contains a glyph for the given Unicode code point.
   *
   * @param codepoint the Unicode code point to look up
   * @return {@code true} if the font has a glyph for the code point
   */
  boolean hasGlyph(int codepoint);

  /**
   * Returns the internal glyph index for the given Unicode code point.
   *
   * @param codepoint the Unicode code point to look up
   * @return the glyph index, or {@code 0} if the code point is not covered
   */
  int getGlyphIndex(int codepoint);

  @Override
  void close();

  /**
   * Rasterizes a glyph to the texture atlas and returns its positioning data.
   *
   * @param device     the graphics device used for texture allocation
   * @param glyphIndex the glyph index within the font
   * @param fontStyle  the style bitmask, combining flags from {@link FontStyle}
   * @return the rasterized glyph, or {@code null} if the glyph has no visual representation
   */
  @Nullable Glyph rasterizeGlyph(Device device, int glyphIndex, int fontStyle);

  /**
   * Sets the resolution used for glyph rasterization.
   *
   * @param res the resolution in pixels
   */
  void setResolution(float res);

  /**
   * Returns the current rasterization resolution.
   *
   * @return the resolution in pixels
   */
  float resolution();

  /**
   * Returns the file system path from which this font was loaded.
   *
   * @return the font file path
   */
  String filePath();

  /**
   * Returns the raw content of the font file as a read-only byte buffer.
   *
   * @return the font file data
   */
  ByteBuffer fileData();
}
