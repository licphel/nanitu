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

import net.nanitu.math.Box2;

/**
 * Immutable shaped text — glyph IDs and positions produced by {@link TextShaper}.
 *
 * <p>Stores only integer glyph IDs and float (x,y) positions.
 * Rasterization into a texture atlas happens at render time.
 *
 * @param font      the font used for shaping
 * @param glyphs    the glyph indices, one per shaped glyph
 * @param positions the flattened (x, y) position array, stored as {@code [x0, y0, x1, y1, …]}
 * @param fontSize  the pixel size used for shaping
 * @param width     the total width of the shaped text, in pixels
 * @param height    the total height of the shaped text, in pixels
 */
public record TextBlob(Font font, int[] glyphs, float[] positions, float fontSize, float width, float height) {
  /**
   * Returns the number of glyphs in this blob.
   *
   * @return the glyph count
   */
  public int glyphCount() {
    return glyphs.length;
  }

  /**
   * Returns the bounding box of this shaped text, with origin at (0, 0).
   *
   * @return the bounding box with dimensions {@code (width, height)}
   */
  public Box2 bounds() {
    return Box2.create(0, 0, width, height);
  }

  /**
   * Returns the X position of the glyph at the given index.
   *
   * @param i the glyph index
   * @return the X coordinate, in pixels
   */
  public float glyphX(int i) {
    return positions[i * 2];
  }

  /**
   * Returns the Y position of the glyph at the given index.
   *
   * @param i the glyph index
   * @return the Y coordinate, in pixels
   */
  public float glyphY(int i) {
    return positions[i * 2 + 1];
  }
}
