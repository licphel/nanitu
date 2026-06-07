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

package net.nanitu.gfx.text.sketch;

import net.nanitu.gfx.text.Font;
import net.nanitu.gfx.text.Text;
import net.nanitu.math.Box2;

/**
 * The final line-broken text layout, ready for rendering.
 *
 * <p>Positions and advances are in pixels. Each glyph occupies two consecutive
 * entries in the positions array: {@code [x0, y0, x1, y1, ...]}.
 *
 * @param font          the font used for shaping
 * @param glyphs        the array of glyph identifiers
 * @param positions     the X/Y position pairs for each glyph
 * @param advances      the advance width for each glyph, in pixels
 * @param fontSize      the font size used, in pixels
 * @param width         the total layout width, in pixels
 * @param height        the total layout height, in pixels
 * @param flipY         whether the Y axis is flipped
 * @param fontStyle     the style bitmask, combining flags from {@link net.nanitu.gfx.text.FontStyle}
 * @param lastLineWidth the width of the last line, in pixels
 */
public record TextSketch(Font font, int[] glyphs, float[] positions, float[] advances, float fontSize, float width,
                         float height, boolean flipY, int fontStyle, float lastLineWidth) implements Text {
  @Override
  public int glyphCount() {
    return glyphs.length;
  }

  @Override
  public int glyphId(int i) {
    return glyphs[i];
  }

  @Override
  public float glyphX(int i) {
    return positions[i * 2];
  }

  @Override
  public float glyphY(int i) {
    return positions[i * 2 + 1];
  }

  @Override
  public float glyphWidth(int i) {
    return advances[i];
  }

  @Override
  public float glyphHeight(int i) {
    return fontSize;
  }

  @Override
  public Box2 bounds() {
    return Box2.create(0, 0, width, height);
  }
}
