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

import net.nanitu.gfx.text.sketch.LineBreaker;
import net.nanitu.gfx.text.sketch.ShapedText;
import net.nanitu.gfx.text.spi.ShaperProvider;
import net.nanitu.math.Box2;
import net.nanitu.util.Service;

/**
 * A shaped and line-broken text layout ready for rendering.
 *
 * <p>Instances are created through {@link Builder} and provide access to
 * positioned glyphs, the overall bounding box, and formatting metadata.
 *
 * <p>All position and size values are in pixels.
 */
public interface Text {
  /**
   * Returns the font used to shape this text.
   *
   * @return the font
   */
  Font font();

  /**
   * Returns the total number of glyphs in this layout.
   *
   * @return the glyph count
   */
  int glyphCount();

  /**
   * Returns the font-specific glyph index at the given position.
   *
   * @param i the glyph index, from {@code 0} to {@link #glyphCount()} - 1
   * @return the glyph identifier within the font
   */
  int glyphId(int i);

  /**
   * Returns the horizontal position of the glyph at the given index.
   *
   * @param i the glyph index
   * @return the X coordinate in pixels
   */
  float glyphX(int i);

  /**
   * Returns the vertical position of the glyph at the given index.
   *
   * @param i the glyph index
   * @return the Y coordinate in pixels
   */
  float glyphY(int i);

  /**
   * Returns the advance width of the glyph at the given index.
   *
   * @param i the glyph index
   * @return the advance width in pixels
   */
  float glyphWidth(int i);

  /**
   * Returns the height of the glyph at the given index.
   *
   * @param i the glyph index
   * @return the height in pixels
   */
  float glyphHeight(int i);

  /**
   * Returns the axis-aligned bounding box enclosing all glyphs.
   *
   * @return the layout bounds
   */
  Box2 bounds();

  /**
   * Returns the width of the last line in the layout.
   *
   * @return the last line width in pixels
   */
  float lastLineWidth();

  /**
   * Returns the font size used for this text.
   *
   * @return the font size in pixels
   */
  float fontSize();

  /**
   * Returns whether the Y axis is flipped, so positive Y goes downward.
   *
   * @return {@code true} if Y is flipped
   */
  boolean flipY();

  /**
   * Returns the font style bitmask used for shaping.
   *
   * @return the style bitmask, combining flags from {@link FontStyle}
   */
  int fontStyle();

  /**
   * Returns the scale factor from design units to pixels.
   *
   * <p>This is the ratio of {@link #fontSize()} to the font's {@link Font#resolution() resolution}.
   *
   * @return the scale factor
   */
  default float scale() {
    return fontSize() / font().resolution();
  }

  /**
   * Returns the array of all glyph indices in this layout.
   *
   * @return the glyph index array
   */
  int[] glyphs();

  /**
   * Builder for creating {@link Text} instances from a font and a string.
   *
   * <p>Call {@link #build()} after configuring options to produce the final layout.
   */
  final class Builder {
    private final Font font;
    private final String text;
    float fontSize = 16;
    float widthLimit = Float.MAX_VALUE;
    boolean justify;
    boolean flipY = true;
    int fontStyle = FontStyle.REGULAR;

    /**
     * Creates a new builder for the given font and text content.
     *
     * @param font the font to shape with
     * @param text the text string to lay out
     */
    public Builder(Font font, String text) {
      this.font = font;
      this.text = text;
    }

    /**
     * Sets the font size for shaping.
     *
     * @param v the font size in pixels; defaults to {@code 16}
     * @return this builder
     */
    public Builder fontSize(float v) {
      fontSize = v;
      return this;
    }

    /**
     * Sets the maximum line width before wrapping.
     *
     * @param v the width limit in pixels; defaults to {@link Float#MAX_VALUE} (no wrapping)
     * @return this builder
     */
    public Builder widthLimit(float v) {
      widthLimit = v;
      return this;
    }

    /**
     * Sets whether lines should be justified to fill the width limit.
     *
     * @param v {@code true} to justify; defaults to {@code false}
     * @return this builder
     */
    public Builder justify(boolean v) {
      justify = v;
      return this;
    }

    /**
     * Sets whether the Y axis is flipped (positive Y downward).
     *
     * @param v {@code true} to flip Y; defaults to {@code true}
     * @return this builder
     */
    public Builder flipY(boolean v) {
      flipY = v;
      return this;
    }

    /**
     * Sets the font style for shaping.
     *
     * @param v the style bitmask, combining flags from {@link FontStyle}; defaults to {@link FontStyle#REGULAR}
     * @return this builder
     */
    public Builder fontStyle(int v) {
      fontStyle = v;
      return this;
    }

    /**
     * Shapes and line-breaks the text, producing the final layout.
     *
     * @return the shaped and laid-out text
     */
    public Text build() {
      ShapedText st = Service.get(ShaperProvider.class).shape(font, text, fontSize, flipY, fontStyle);
      return new LineBreaker(st, widthLimit, justify).breakText();
    }
  }
}
