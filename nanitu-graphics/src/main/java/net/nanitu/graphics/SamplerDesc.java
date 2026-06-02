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

package net.nanitu.graphics;

import net.nanitu.math.Color;

/**
 * Creates a new {@code SamplerDesc} describing texture filtering, wrapping,
 * and level-of-detail parameters.
 *
 * <p>The {@link Builder} has sensible defaults (linear, repeat, no anisotropy).
 *
 * @param magFilter       magnification filter
 * @param minFilter       minification filter
 * @param mipmapFilter    mipmap filter used between mip levels
 * @param wrapX           U-axis wrapping mode
 * @param wrapY           V-axis wrapping mode
 * @param wrapZ           W-axis wrapping mode
 * @param wrapBorderColor border colour for {@link TextureWrap#CLAMP_TO_BORDER}
 * @param lodBias         level-of-detail bias
 * @param minLod          minimum LOD clamp
 * @param maxLod          maximum LOD clamp
 * @param anisotropyLevel anisotropy (&gt; 1 to enable, 1 = off)
 * @see Sampler
 */
public record SamplerDesc(TextureFilter magFilter, TextureFilter minFilter, TextureFilter mipmapFilter,
                          TextureWrap wrapX, TextureWrap wrapY, TextureWrap wrapZ, Color wrapBorderColor, float lodBias,
                          float minLod, float maxLod, float anisotropyLevel) {
  /**
   * Linear filtering, repeat wrapping, no anisotropy.
   */
  public static final SamplerDesc DEFAULT = new Builder().build();

  /**
   * Nearest filtering, repeat wrapping, no anisotropy.
   * Suitable for pixel rendering.
   */
  public static final SamplerDesc PIXEL =
      new Builder().minFilter(TextureFilter.NEAREST).magFilter(TextureFilter.NEAREST).build();

  @SuppressWarnings("all")
  public static final class Builder {
    public TextureFilter magFilter = TextureFilter.LINEAR;
    public TextureFilter minFilter = TextureFilter.LINEAR;
    public TextureFilter mipmapFilter = TextureFilter.LINEAR;
    public TextureWrap wrapX = TextureWrap.REPEAT;
    public TextureWrap wrapY = TextureWrap.REPEAT;
    public TextureWrap wrapZ = TextureWrap.REPEAT;
    public Color wrapBorderColor = Color.EMPTY;
    public float lodBias = 0f;
    public float minLod = 0f;
    public float maxLod = 1000f;
    public float anisotropyLevel = 1f;

    public Builder magFilter(TextureFilter v) {
      magFilter = v;
      return this;
    }

    public Builder minFilter(TextureFilter v) {
      minFilter = v;
      return this;
    }

    public Builder mipmapFilter(TextureFilter v) {
      mipmapFilter = v;
      return this;
    }

    public Builder wrapX(TextureWrap v) {
      wrapX = v;
      return this;
    }

    public Builder wrapY(TextureWrap v) {
      wrapY = v;
      return this;
    }

    public Builder wrapZ(TextureWrap v) {
      wrapZ = v;
      return this;
    }

    public Builder wrapBorderColor(Color v) {
      wrapBorderColor = v;
      return this;
    }

    public Builder lodBias(float v) {
      lodBias = v;
      return this;
    }

    public Builder minLod(float v) {
      minLod = v;
      return this;
    }

    public Builder maxLod(float v) {
      maxLod = v;
      return this;
    }

    public Builder anisotropyLevel(float v) {
      anisotropyLevel = v;
      return this;
    }

    public SamplerDesc build() {
      return new SamplerDesc(magFilter, minFilter, mipmapFilter, wrapX, wrapY, wrapZ, wrapBorderColor, lodBias,
          minLod, maxLod, anisotropyLevel);
    }
  }
}
