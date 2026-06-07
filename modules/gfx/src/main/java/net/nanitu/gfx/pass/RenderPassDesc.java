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

package net.nanitu.gfx.pass;

import net.nanitu.math.Color;

/**
 * Creates a new {@code RenderPassDesc} describing which attachments to clear and to what values.
 *
 * <p>The {@link #clearMask} bitmask selects which attachments are cleared.
 * Convenience factory {@link #of(Color)} clears all three.
 *
 * @param clearColor   the clear color for the color attachment
 * @param clearDepth   the clear depth value (0.0–1.0)
 * @param clearStencil the clear stencil value
 * @param clearMask    bitmask of {@link #CLEAR_COLOR}, {@link #CLEAR_DEPTH}, {@link #CLEAR_STENCIL}
 * @see RenderTarget#acquire(RenderPassDesc)
 */
public record RenderPassDesc(Color clearColor, double clearDepth, int clearStencil, int clearMask) {
  /** Clear the color attachment. */
  public static final int CLEAR_COLOR = 1;
  /** Clear the depth attachment. */
  public static final int CLEAR_DEPTH = 2;
  /** Clear the stencil attachment. */
  public static final int CLEAR_STENCIL = 4;

  /** Clear color + depth only (no stencil). Depth = 1.0, stencil = 0. */
  public static final RenderPassDesc DEFAULT = new Builder().build();

  /**
   * Clears all three attachments. Depth = 1.0, stencil = 0.
   *
   * @param color the color for the color attachment
   * @return a descriptor with all clear flags set
   */
  public static RenderPassDesc of(Color color) {
    Builder b = new Builder();
    b.clearColor = color;
    b.clearMask = CLEAR_COLOR | CLEAR_DEPTH | CLEAR_STENCIL;
    return b.build();
  }

  @SuppressWarnings("all")
  public static final class Builder {
    public Color clearColor = Color.BLACK;
    public double clearDepth = 1.0;
    public int clearStencil = 0;
    public int clearMask = CLEAR_COLOR | CLEAR_DEPTH;

    public Builder clearColor(Color v) {
      clearColor = v;
      return this;
    }

    public Builder clearDepth(double v) {
      clearDepth = v;
      return this;
    }

    public Builder clearStencil(int v) {
      clearStencil = v;
      return this;
    }

    public Builder clearMask(int v) {
      clearMask = v;
      return this;
    }

    public RenderPassDesc build() {
      return new RenderPassDesc(clearColor, clearDepth, clearStencil, clearMask);
    }
  }
}
