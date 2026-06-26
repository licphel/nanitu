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

package net.nanitu.gfx.shader;

import net.nanitu.gfx.pipe.PipelineDesc;

/**
 * Baked vertex attribute layout describing how vertex data is interleaved in a buffer.
 *
 * <p>Call {@link #bake(Attr...)} to compute byte offsets and strides from a
 * list of attribute descriptors. The resulting layout is immutable and can be shared across pipelines with the same
 * vertex structure.
 *
 * @see PipelineDesc.Builder#vertexLayout
 */
public final class VertexLayout {
  /**
   * The ordered list of vertex attributes.
   */
  public final Attr[] attrs;
  /**
   * Total byte stride between consecutive vertices.
   */
  public final int stride;

  private VertexLayout(Attr[] attrs, int stride) {
    this.attrs = attrs;
    this.stride = stride;
  }

  /**
   * Bakes a vertex layout from a sequence of attribute descriptors.
   *
   * <p>Each attribute's {@code offset} is computed as the running sum of
   * previous attribute sizes. {@code location} is assigned sequentially starting from 0.
   *
   * @param attrs attribute descriptors in declaration order
   * @return a baked layout with computed offsets and stride
   */
  public static VertexLayout bake(Attr... attrs) {
    int offset = 0;
    int location = 0;
    Attr[] baked = new Attr[attrs.length];
    for (int i = 0; i < attrs.length; i++) {
      Attr a = attrs[i];
      int size = a.components * byteSize(a.type);
      baked[i] = new Attr(a.components, a.type, a.normalized, location, offset, size);
      offset += size;
      location++;
    }
    return new VertexLayout(baked, offset);
  }

  private static int byteSize(VertexAttributeType type) {
    return switch (type) {
      case UINT8, INT8 -> 1;
      case UINT16, INT16, FLOAT16 -> 2;
      case UINT32, INT32, FLOAT32 -> 4;
    };
  }

  /**
   * Creates a new {@code Attr} describing a single vertex attribute.
   *
   * @param components number of components (1–4)
   * @param type       the component data type
   * @param normalized {@code true} to map integer types to [0,1] or [-1,1] in the shader
   * @param location   the shader attribute location (assigned by {@link VertexLayout#bake})
   * @param offset     byte offset of this attribute within a vertex (assigned by {@code bake})
   * @param size       total byte size of this attribute ({@code components × sizeof(type)})
   */
  public record Attr(int components, VertexAttributeType type, boolean normalized, int location, int offset, int size) {
    /**
     * Creates a new {@code Attr} for an unresolved attribute — location, offset, and size are set to 0 and will be
     * computed by {@link VertexLayout#bake}.
     *
     * @param components number of components (1–4)
     * @param type       the component data type
     * @param normalized {@code true} to map integer types to [0,1] or [-1,1]
     */
    public Attr(int components, VertexAttributeType type, boolean normalized) {
      this(components, type, normalized, 0, 0, 0);
    }
  }
}
