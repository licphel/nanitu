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

import net.nanitu.gfx.texture.Texture;
import net.nanitu.math.Box2;

/**
 * Identifies a sub-region of a {@link Texture}, defined by a source texture and a bounding rectangle in texel
 * coordinates.
 *
 * <p>Texture parts are immutable. Create one from a full texture, or offset from a parent
 * part to describe a relative sub-region.
 *
 * @param src    the source texture
 * @param region the region within the texture in texel coordinates
 */
public record TexturePart(Texture src, Box2 region) {

  /**
   * Creates a TexturePart covering the entire texture.
   *
   * @param tex the source texture
   */
  public TexturePart(Texture tex) {
    this(tex, Box2.create(0, 0, tex.width(), tex.height()));
  }

  /**
   * Creates a TexturePart offset from a parent part's region origin.
   *
   * @param parent         the parent texture part
   * @param relativeRegion the region relative to the parent's origin, in texels
   */
  public TexturePart(TexturePart parent, Box2 relativeRegion) {
    this(parent.src, relativeRegion.translate(parent.region.minX(), parent.region.minY()));
  }

  /**
   * Returns the U coordinate of the region origin in texels.
   *
   * @return the U coordinate
   */
  public float u() {
    return region.minX();
  }

  /**
   * Returns the V coordinate of the region origin in texels.
   *
   * @return the V coordinate
   */
  public float v() {
    return region.minY();
  }

  /**
   * Returns the width of the region in texels.
   *
   * @return the width of region
   */
  public float width() {
    return region.width();
  }

  /**
   * Returns the height of the region in texels.
   *
   * @return the height of region
   */
  public float height() {
    return region.height();
  }
}
