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

/**
 * Baked layout declaring the shader resource bindings (textures, samplers,
 * uniform buffers) expected by a pipeline.
 *
 * <p>Call {@link #bake(Slot...)} to assign binding indices automatically in
 * declaration order:
 * <pre>{@code
 * ResourceSetLayout layout = ResourceSetLayout.bake(
 *     new ResourceSetLayout.Slot(1, "uTransform", ShaderType.VERTEX_BIT, ResourceType.UNIFORM_BUFFER),
 *     new ResourceSetLayout.Slot(1, "uTexture",  ShaderType.FRAGMENT_BIT, ResourceType.TEXTURE)
 * );
 * }</pre>
 *
 * <p>The resulting layout is immutable and is stored in
 * {@link RenderPipeDesc#resourceLayouts()}.
 *
 * @see RenderPipeDesc.Builder#resourceLayouts
 */
public final class ResourceSetLayout {
  /**
   * The baked slots in binding order.
   */
  public final Slot[] slots;

  private ResourceSetLayout(Slot[] slots) {
    this.slots = slots;
  }

  /**
   * Bakes a resource-set layout, assigning sequential binding indices
   * starting from 0.
   *
   * @param slots slot declarations in desired binding order
   * @return a baked layout with resolved binding indices
   */
  public static ResourceSetLayout bake(Slot... slots) {
    Slot[] baked = new Slot[slots.length];
    for (int i = 0; i < slots.length; i++) {
      Slot s = slots[i];
      baked[i] = new Slot(s.count, s.name, s.stages, s.type, i);
    }
    return new ResourceSetLayout(baked);
  }

  /**
   * Creates a new {@code Slot} declaring a single shader resource binding.
   *
   * @param count   the number of resources in this binding (e.g. array size, normally 1)
   * @param name    the shader-side variable name
   * @param stages  bitmask of {@link ShaderType} flags indicating which stages access this resource
   * @param type    the kind of resource ({@link ResourceType#TEXTURE} or
   *                {@link ResourceType#UNIFORM_BUFFER})
   * @param binding the resolved GPU binding index (assigned by {@link ResourceSetLayout#bake})
   */
  public record Slot(int count, String name, int stages, ResourceType type, int binding) {
    /**
     * Creates a new {@code Slot} without a resolved binding index.
     *
     * <p>The binding is set to 0 and will be assigned by
     * {@link ResourceSetLayout#bake}.
     *
     * @param count  the number of resources in this binding
     * @param name   the shader-side variable name
     * @param stages bitmask of {@link ShaderType} flags
     * @param type   the kind of resource
     */
    public Slot(int count, String name, int stages, ResourceType type) {
      this(count, name, stages, type, 0);
    }
  }
}
