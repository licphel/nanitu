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

package net.fmhi.gfx.sprite;

import net.fmhi.gfx.pipe.Pipeline;
import net.fmhi.gfx.shader.ResourceSet;
import net.fmhi.gfx.texture.Texture;
import org.jspecify.annotations.Nullable;

/**
 * Internal render state tracking the active pipeline, primitive type, resource set, and texture for a
 * {@link Graphics2D}.
 */
final class Recorder {
  @Nullable Pipeline pipe;
  @Nullable Primitive primitive;
  @Nullable ResourceSet set;
  @Nullable Texture tex;

  Recorder() {
  }

  /**
   * Replaces all state fields at once.
   *
   * @param pipe      the pipeline, may be {@code null}
   * @param primitive the primitive type, may be {@code null}
   * @param set       the resource set, may be {@code null}
   * @param tex       the texture, may be {@code null}
   */
  void set(@Nullable Pipeline pipe, @Nullable Primitive primitive, @Nullable ResourceSet set, @Nullable Texture tex) {
    this.pipe = pipe;
    this.primitive = primitive;
    this.set = set;
    this.tex = tex;
  }
}
