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

package net.nanitu.gfx.pipe;

import net.nanitu.gfx.shader.ResourceSetLayout;
import net.nanitu.gfx.shader.VertexLayout;
import org.jspecify.annotations.Nullable;

/**
 * Creates a new {@code PipelineDesc} describing an immutable render pipeline.
 *
 * <p>Use {@link Builder} when you need non-default values:
 * <pre>{@code
 * PipelineDesc desc = new PipelineDesc.Builder()
 *     .blend(Blend.ALPHA_MIX)
 *     .depth(Depth.LEQ)
 *     .shaderProgram(myProgram)
 *     .vertexLayout(VertexLayout.bake(
 *         new VertexLayout.Attr(3, VertexAttributeType.FLOAT32, false),
 *         new VertexLayout.Attr(2, VertexAttributeType.FLOAT32, false)))
 *     .build();
 * }</pre>
 *
 * @param blend           blend state
 * @param depth           depth-test state
 * @param stencil         stencil-test state
 * @param rasterization   rasterization state
 * @param shaderProgram   the linked shader program (typed loosely to avoid circular dep)
 * @param vertexLayout    vertex attribute layout
 * @param resourceLayouts resource-set layouts
 * @param usage           pipeline category
 */
public record PipelineDesc(Blend blend, Depth depth, Stencil stencil, RasterizationDesc rasterization,
                           @Nullable Object shaderProgram, VertexLayout vertexLayout,
                           ResourceSetLayout[] resourceLayouts, PipelineUsage usage) {
  @SuppressWarnings("all")
  public static final class Builder {
    public Blend blend = Blend.ALPHA_MIX;
    public Depth depth = Depth.DISABLED;
    public Stencil stencil = Stencil.DISABLED;
    public RasterizationDesc rasterization = RasterizationDesc.DEFAULT;
    public @Nullable Object shaderProgram = null;
    public VertexLayout vertexLayout = VertexLayout.bake();
    public ResourceSetLayout[] resourceLayouts = new ResourceSetLayout[0];
    public PipelineUsage usage = PipelineUsage.RENDER;

    public Builder blend(Blend v) {
      blend = v;
      return this;
    }

    public Builder depth(Depth v) {
      depth = v;
      return this;
    }

    public Builder stencil(Stencil v) {
      stencil = v;
      return this;
    }

    public Builder rasterization(RasterizationDesc v) {
      rasterization = v;
      return this;
    }

    public Builder shaderProgram(Object v) {
      shaderProgram = v;
      return this;
    }

    public Builder vertexLayout(VertexLayout v) {
      vertexLayout = v;
      return this;
    }

    public Builder resourceLayouts(ResourceSetLayout... v) {
      resourceLayouts = v;
      return this;
    }

    public Builder usage(PipelineUsage v) {
      usage = v;
      return this;
    }

    public PipelineDesc build() {
      return new PipelineDesc(blend, depth, stencil, rasterization, shaderProgram, vertexLayout, resourceLayouts,
          usage);
    }
  }
}
