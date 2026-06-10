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

import net.nanitu.gfx.Device;
import net.nanitu.gfx.pipe.*;
import net.nanitu.gfx.shader.*;
import org.jspecify.annotations.Nullable;

/**
 * Lazy-initialized shader programs, pipelines, resource set layouts, and vertex layouts shared across all {@link Brush}
 * instances.
 *
 * <p>Resources are allocated once on the first call to {@link #init(Device)}; subsequent calls are no-ops.
 */
final class InternalResources {
  private static final String VERT_SHADER_TEX = """
      #version 330 core
      
      layout(location = 0) in vec3 i_position;
      layout(location = 1) in vec4 i_color;
      layout(location = 2) in vec2 i_texCoord;
      
      out vec4 o_color;
      out vec2 o_texCoord;
      
      layout(std140) uniform u_transform {
          mat4 u_viewProjection;
      };
      
      void main(){
          o_color = i_color;
          o_texCoord = i_texCoord;
          gl_Position = u_viewProjection * vec4(i_position, 1.0);
      }
      """;

  private static final String FRAG_SHADER_TEX = """
      #version 330 core
      
      in vec4 o_color;
      in vec2 o_texCoord;
      
      uniform sampler2D u_texture;
      
      layout(location = 0) out vec4 fragColor;
      
      void main() {
          vec4 col = texture(u_texture, o_texCoord);
          fragColor = o_color * col;
      }
      """;

  private static final String VERT_SHADER_COL = """
      #version 330 core
      
      layout(location = 0) in vec3 i_position;
      layout(location = 1) in vec4 i_color;
      
      out vec4 o_color;
      
      layout(std140) uniform u_transform {
          mat4 u_viewProjection;
      };
      
      void main(){
          o_color = i_color;
          gl_Position = u_viewProjection * vec4(i_position, 1.0);
      }
      """;

  private static final String FRAG_SHADER_COL = """
      #version 330 core
      
      in vec4 o_color;
      
      layout(location = 0) out vec4 fragColor;
      
      void main() {
          fragColor = o_color;
      }
      """;

  @Nullable
  static ShaderProgram s4c;
  @Nullable
  static ShaderProgram s4t;
  @Nullable
  static ResourceSetLayout rl4c;
  @Nullable
  static ResourceSetLayout rl4t;
  @Nullable
  static VertexLayout vl4c;
  @Nullable
  static VertexLayout vl4t;
  @Nullable
  static Pipeline p4c;
  @Nullable
  static Pipeline p4t;
  private static boolean init;

  private InternalResources() {
  }

  /**
   * Initializes all shared GPU resources. Subsequent calls have no effect.
   *
   * @param device the graphics device used to create shader programs, pipelines, and layouts
   */
  static void init(Device device) {
    if (init) {
      return;
    }
    init = true;

    // Colored shader program
    ShaderModule vertCol = device.getShaderModule(new ShaderModuleDesc(ShaderType.VERTEX, VERT_SHADER_COL));
    ShaderModule fragCol = device.getShaderModule(new ShaderModuleDesc(ShaderType.FRAGMENT, FRAG_SHADER_COL));
    s4c = device.getShaderProgram(vertCol, fragCol);

    // Textured shader program
    ShaderModule vertTex = device.getShaderModule(new ShaderModuleDesc(ShaderType.VERTEX, VERT_SHADER_TEX));
    ShaderModule fragTex = device.getShaderModule(new ShaderModuleDesc(ShaderType.FRAGMENT, FRAG_SHADER_TEX));
    s4t = device.getShaderProgram(vertTex, fragTex);

    // Resource set layouts
    rl4c = ResourceSetLayout.bake(new Slot(1, "u_transform", ShaderType.VERTEX_BIT, ResourceType.UNIFORM_BUFFER));
    rl4t = ResourceSetLayout.bake(new Slot(1, "u_transform", ShaderType.VERTEX_BIT, ResourceType.UNIFORM_BUFFER),
        new Slot(1, "u_texture", ShaderType.FRAGMENT_BIT, ResourceType.TEXTURE));

    // Vertex layouts
    // Colored: vec3 position (FLOAT32) + vec4 color (FLOAT16)
    vl4c = VertexLayout.bake(new VertexLayout.Attr(3, VertexAttributeType.FLOAT32, false), new VertexLayout.Attr(4,
        VertexAttributeType.FLOAT16, false));

    // Textured: vec3 position (FLOAT32) + vec4 color (FLOAT16) + vec2 texCoord (FLOAT32)
    vl4t = VertexLayout.bake(new VertexLayout.Attr(3, VertexAttributeType.FLOAT32, false), new VertexLayout.Attr(4,
        VertexAttributeType.FLOAT16, false), new VertexLayout.Attr(2, VertexAttributeType.FLOAT32, false));

    // Pipelines
    p4c =
        device.getRenderPipeline(new PipelineDesc.Builder().blend(Blend.ALPHA_MIX).depth(Depth.DISABLED).rasterization(RasterizationDesc.DEFAULT).resourceLayouts(rl4c).shaderProgram(s4c).usage(PipelineUsage.RENDER).vertexLayout(vl4c).build());

    p4t =
        device.getRenderPipeline(new PipelineDesc.Builder().blend(Blend.ALPHA_MIX).depth(Depth.DISABLED).rasterization(RasterizationDesc.DEFAULT).resourceLayouts(rl4t).shaderProgram(s4t).usage(PipelineUsage.RENDER).vertexLayout(vl4t).build());
  }
}
