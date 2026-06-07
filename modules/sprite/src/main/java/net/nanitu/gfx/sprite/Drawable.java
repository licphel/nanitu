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

package net.nanitu.gfx.sprite;

/**
 * A drawable object that renders itself using a {@link Brush}.
 *
 * <p>Implement this interface and pass instances to
 * {@link Brush#draw(Drawable, float, float, float, float, float, float, float, float)} to define custom drawing logic
 * driven by the Brush.
 *
 * @see Brush#draw(Drawable, float, float, float, float, float, float, float, float)
 */
@FunctionalInterface
public interface Drawable {
  /**
   * Draws this object at the given position and size.
   *
   * @param brush the brush to draw with
   * @param x     the X position
   * @param y     the Y position
   * @param w     the width
   * @param h     the height
   * @param u     the U texture coordinate offset
   * @param v     the V texture coordinate offset
   * @param uw    the U texture coordinate range
   * @param vh    the V texture coordinate range
   */
  void draw(Brush brush, float x, float y, float w, float h, float u, float v, float uw, float vh);
}
