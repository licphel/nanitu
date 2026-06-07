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

package net.nanitu;

import net.nanitu.audio.Clip;
import net.nanitu.audio.Controller;
import net.nanitu.audio.Mixer;
import net.nanitu.audio.io.AudioFormatException;
import net.nanitu.audio.io.AudioInputStream;
import net.nanitu.audio.spi.MixerProvider;
import net.nanitu.gfx.Device;
import net.nanitu.gfx.View;
import net.nanitu.gfx.ViewController;
import net.nanitu.gfx.buffer.BufferFrequency;
import net.nanitu.gfx.buffer.BufferObject;
import net.nanitu.gfx.buffer.BufferObjectDesc;
import net.nanitu.gfx.buffer.BufferType;
import net.nanitu.gfx.cmd.Encoder;
import net.nanitu.gfx.cmd.EncoderDesc;
import net.nanitu.gfx.io.ImageInputStream;
import net.nanitu.gfx.pass.RenderPassDesc;
import net.nanitu.gfx.pipe.CompareOp;
import net.nanitu.gfx.pipe.Depth;
import net.nanitu.gfx.pipe.Pipeline;
import net.nanitu.gfx.pipe.PipelineDesc;
import net.nanitu.gfx.shader.*;
import net.nanitu.gfx.spi.DeviceProvider;
import net.nanitu.gfx.spi.ViewProvider;
import net.nanitu.gfx.sprite.Brush;
import net.nanitu.gfx.sprite.MultiMesh;
import net.nanitu.gfx.text.Font;
import net.nanitu.gfx.text.Text;
import net.nanitu.gfx.texture.*;
import net.nanitu.math.*;
import net.nanitu.math.dim2.Camera2D;
import net.nanitu.math.dim3.CameraPerspective3D;
import net.nanitu.resource.ResourceFinder;
import net.nanitu.util.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;

/**
 * Textured rotating cube with Phong lighting.
 */
public class Main {

  // ---------------------------------------------------------------------------
  // Shaders
  // ---------------------------------------------------------------------------

  private static final String VERT_SRC = """
      #version 330 core
      layout(location = 0) in vec3 aPos;
      layout(location = 1) in vec3 aNormal;
      layout(location = 2) in vec2 aUV;
      
      layout(std140) uniform Matrices {
          mat4 uMVP;
          mat4 uModel;
          mat3 uNormalMatrix;
      };
      
      out vec3 vNormal;
      out vec3 vWorldPos;
      out vec2 vUV;
      
      void main() {
          vec4 worldPos = uModel * vec4(aPos, 1.0);
          vWorldPos = worldPos.xyz;
          vNormal = uNormalMatrix * aNormal;
          vUV = aUV;
          gl_Position = uMVP * vec4(aPos, 1.0);
      }
      """;

  private static final String FRAG_SRC = """
      #version 330 core
      in vec3 vNormal;
      in vec3 vWorldPos;
      in vec2 vUV;
      
      uniform sampler2D uTexture;
      
      layout(std140) uniform Lighting {
          vec3 uLightPos;
          vec3 uLightColor;
          vec3 uAmbient;
          vec3 uViewPos;
      };
      
      out vec4 fragColor;
      
      void main() {
          vec3 normal = normalize(vNormal);
          vec3 ambient = uAmbient;
          vec3 lightDir = normalize(uLightPos - vWorldPos);
          float diff = max(dot(normal, lightDir), 0.0);
          vec3 diffuse = diff * uLightColor;
          vec3 viewDir = normalize(uViewPos - vWorldPos);
          vec3 reflectDir = reflect(-lightDir, normal);
          float spec = pow(max(dot(viewDir, reflectDir), 0.0), 32.0);
          vec3 specular = spec * uLightColor * 0.5;
          vec3 texColor = texture(uTexture, vUV).rgb;
          vec3 result = (ambient + diffuse) * texColor + specular;
          fragColor = vec4(result, 1.0);
      }
      """;

  // ---------------------------------------------------------------------------
  // Cube — 24 vertices, CCW winding, pos + normal + uv
  // ---------------------------------------------------------------------------

  private static final float[] CUBE_DATA = {
      // Front  (+z)
      -0.5f, -0.5f, 0.5f, 0, 0, 1, 0, 1, 0.5f, -0.5f, 0.5f, 0, 0, 1, 1, 1, 0.5f, 0.5f, 0.5f, 0, 0, 1, 1, 0, -0.5f,
      0.5f, 0.5f, 0, 0, 1, 0, 0,
      // Back   (-z)
      0.5f, -0.5f, -0.5f, 0, 0, -1, 0, 1, -0.5f, -0.5f, -0.5f, 0, 0, -1, 1, 1, -0.5f, 0.5f, -0.5f, 0, 0, -1, 1, 0,
      0.5f, 0.5f, -0.5f, 0, 0, -1, 0, 0,
      // Right  (+x)
      0.5f, -0.5f, 0.5f, 1, 0, 0, 0, 1, 0.5f, -0.5f, -0.5f, 1, 0, 0, 1, 1, 0.5f, 0.5f, -0.5f, 1, 0, 0, 1, 0, 0.5f,
      0.5f, 0.5f, 1, 0, 0, 0, 0,
      // Left   (-x)
      -0.5f, -0.5f, -0.5f, -1, 0, 0, 0, 1, -0.5f, -0.5f, 0.5f, -1, 0, 0, 1, 1, -0.5f, 0.5f, 0.5f, -1, 0, 0, 1, 0,
      -0.5f, 0.5f, -0.5f, -1, 0, 0, 0, 0,
      // Top    (+y)
      -0.5f, 0.5f, 0.5f, 0, 1, 0, 0, 1, 0.5f, 0.5f, 0.5f, 0, 1, 0, 1, 1, 0.5f, 0.5f, -0.5f, 0, 1, 0, 1, 0, -0.5f,
      0.5f, -0.5f, 0, 1, 0, 0, 0,
      // Bottom (-y)
      -0.5f, -0.5f, -0.5f, 0, -1, 0, 0, 1, 0.5f, -0.5f, -0.5f, 0, -1, 0, 1, 1, 0.5f, -0.5f, 0.5f, 0, -1, 0, 1, 0,
      -0.5f, -0.5f, 0.5f, 0, -1, 0, 0, 0,};

  private static final int[] CUBE_INDICES = {0, 1, 2, 0, 2, 3,    // Front  — CCW from +z
      4, 5, 6, 4, 6, 7,    // Back   — CCW from -z
      8, 9, 10, 8, 10, 11,  // Right  — CCW from +x
      12, 13, 14, 12, 14, 15, // Left  — CCW from -x
      16, 17, 18, 16, 18, 19, // Top   — CCW from +y
      20, 21, 22, 20, 22, 23, // Bottom— CCW from -y
  };

  // ---------------------------------------------------------------------------
  // Entry point
  // ---------------------------------------------------------------------------

  static void main(String[] args) throws IOException {
    // --- Audio ---
    Mixer mixer = Service.get(MixerProvider.class).create();
    Clip clip = null;
    try {
      Path p = ResourceFinder.getAppRoot().resolve(".ref", "dopd.wav");
      AudioInputStream audio = AudioInputStream.open(new FileInputStream(p.toFile()));
      byte[] dat = audio.readAllBytes();
      clip = mixer.getClip();
      clip.open(audio.format(), dat);
      clip.set(Controller.VOLUME, 1);
      clip.set(Controller.PITCH, 1);
      clip.loop(1);
    } catch (AudioFormatException e) {
      throw new RuntimeException(e);
    }

    // --- Window ---
    View theView = Service.get(ViewProvider.class).create();
    ViewController ct = theView.controller();
    ct.setTitle("Nanitu Test");
    ct.setDecorated(false);
    ct.setMaximized(true);
    theView.initialize();
    Device dev = Service.get(DeviceProvider.class).create();
    dev.load(theView);

    // --- Camera ---
    CameraPerspective3D camera = new CameraPerspective3D();
    camera.setAspectRatio(theView.controller().size().x() / theView.controller().size().y());
    camera.setNearPlane(0.1f);
    camera.setFarPlane(100.0f);
    camera.setPosition(new Vector3(2.0f, 1.5f, 3.0f));
    camera.setTarget(Vector3.ZERO);

    // --- Light ---
    Vector3 lightPos = new Vector3(3.0f, 5.0f, 2.0f);
    Vector3 lightColor = new Vector3(1.0f, 0.95f, 0.8f);
    Vector3 ambient = new Vector3(0.25f, 0.25f, 0.28f);

    // --- Shaders ---
    ShaderModule vert = dev.getShaderModule(new ShaderModuleDesc(ShaderType.VERTEX, VERT_SRC));
    ShaderModule frag = dev.getShaderModule(new ShaderModuleDesc(ShaderType.FRAGMENT, FRAG_SRC));
    ShaderProgram prog = dev.getShaderProgram(vert, frag);

    // --- Texture ---
    ImageInputStream img = ImageInputStream.open(new FileInputStream(ResourceFinder.getAppRoot().resolve(".ref",
        "a" + ".png").toFile()));
    byte[] texData = img.readAllBytes();
    TextureDesc texDesc =
        new TextureDesc.Builder().width(img.info().width()).height(img.info().height()).initialBytes(texData).mipLevels(4).build();
    Texture texture = dev.getTexture(texDesc);
    Texture resized = dev.getTexture(new TextureDesc.Builder().width(500).height(500).build());
    texture.blit(resized, 0, 0, texture.width(), texture.height(), 0, 0, 500, 500);
    resized.blit(texture, 0, 0, 500, 500, 0, 0, 500, 500);

    Sampler sampler =
        dev.getSampler(new SamplerDesc.Builder().minFilter(TextureFilter.LINEAR_MIPMAP_LINEAR).magFilter(TextureFilter.LINEAR).build());

    // --- Vertex layout ---
    VertexLayout layout = VertexLayout.bake(new VertexLayout.Attr(3, VertexAttributeType.FLOAT32, false),
        new VertexLayout.Attr(3, VertexAttributeType.FLOAT32, false), new VertexLayout.Attr(2,
            VertexAttributeType.FLOAT32, false));

    // --- Resource set layout ---
    ResourceSetLayout rsLayout = ResourceSetLayout.bake(new Slot(1, "uTexture", ShaderType.FRAGMENT_BIT,
        ResourceType.TEXTURE), new Slot(1, "Matrices", ShaderType.VERTEX_BIT, ResourceType.UNIFORM_BUFFER),
        new Slot(1, "Lighting", ShaderType.FRAGMENT_BIT, ResourceType.UNIFORM_BUFFER));

    // --- Pipeline ---
    PipelineDesc pipeDesc = new PipelineDesc.Builder().vertexLayout(layout).shaderProgram(prog).depth(new Depth(true,
        true, CompareOp.LESS_OR_EQUAL)).resourceLayouts(rsLayout).build();
    Pipeline pipe = dev.getRenderPipeline(pipeDesc);

    // --- Buffers ---
    BufferObject vbo = dev.getBuffer(BufferObjectDesc.vertex(BufferFrequency.STATIC));
    vbo.submit(floatArrayToBytes(CUBE_DATA), 0);
    BufferObject ibo = dev.getBuffer(BufferObjectDesc.index(BufferFrequency.STATIC));
    ibo.submit(intArrayToBytes(CUBE_INDICES), 0);

    // --- Uniforms ---
    // UBO 0: Matrices — MVP(64) + Model(64) + NormalMatrix(48) = 176
    BufferObject uboMatrices = dev.getBuffer(new BufferObjectDesc(BufferFrequency.DYNAMIC, BufferType.UNIFORM));
    uboMatrices.allocate(176, null);
    // UBO 1: Lighting — LightPos(16) + LightColor(16) + Ambient(16) + ViewPos(16) = 64
    BufferObject uboLighting = dev.getBuffer(new BufferObjectDesc(BufferFrequency.DYNAMIC, BufferType.UNIFORM));
    uboLighting.allocate(64, null);

    // --- Resource set ---
    ResourceSet resourceSet = dev.getResourceSet(rsLayout);
    resourceSet.bindTexture(0, texture, sampler);
    resourceSet.bindUniform(1, uboMatrices, 176, 0);
    resourceSet.bindUniform(2, uboLighting, 64, 0);

    Encoder enc = dev.getEncoder(EncoderDesc.DEFAULT);
    RenderPassDesc clearDesc = RenderPassDesc.of(new Color(0.1f, 0.12f, 0.15f, 1.0f));

    Font font = Font.open(".ref/pix.otf");

    // --- Main loop ---
    float angle = 0.0f;
    double lastTime = System.nanoTime();
    int frame = 0;
    final int MAX_FRAMES = 6000000;

    MultiMesh mesh = new MultiMesh(dev, true);
    mesh.setDirect(true);

    while (!theView.shouldClose() && frame < MAX_FRAMES) {
      double now = System.nanoTime();
      float delta = (float) ((now - lastTime) * 1e-9);
      lastTime = now;

      theView.pollEvents();
      mixer.pollEvents();
      dev.pollEvents();

      angle += 45.0f * delta;
      float rad = (float) Math.toRadians(angle * 0.5f);
      camera.setPosition(new Vector3((float) Math.sin(rad) * 3.0f, 1.5f + (float) Math.sin(rad * 1.3f) * 0.5f,
          (float) Math.cos(rad) * 3.0f));
      camera.setTarget(Vector3.ZERO);

      Matrix4x4 model = Matrix4x4.createRotation(Vector3.UNIT_Y, (float) Math.toRadians(angle));
      Matrix4x4 view = camera.getViewMatrix();
      Matrix4x4 projection = camera.getProjectionMatrix();
      Matrix4x4 mvp = projection.multiply(view).multiply(model);
      Matrix3x3 normalMatrix = model.invert().transpose().toMatrix3x3();

      updateMatricesUbo(uboMatrices, mvp, model, normalMatrix);
      updateLightingUbo(uboLighting, lightPos, lightColor, ambient, camera.getPosition());

      /*
      dev.getSwapchain().acquire(clearDesc);
      enc.reset();
      enc.setRenderPipe(pipe);
      enc.setTopology(Topology.TRIANGLE);
      enc.setBuffer(vbo);
      enc.setBuffer(ibo);
      enc.setViewport(0, 0, (int) theView.controller().size().x(), (int) theView.controller().size().y());
      enc.setResource(0, resourceSet);
      enc.drawIndexed(CUBE_INDICES.length, 0);
      enc.queuedExecute();
       */

      Brush brush = mesh.begin(RenderPassDesc.DEFAULT);
      brush.setDepth(0.0F);
      brush.setCamera(new Camera2D(800, 450));
      brush.setViewport(Box2.create(0, 0, (int) theView.controller().size().x(),
          (int) theView.controller().size().y()));
      brush.drawTexture(texture, 0, 0, 100, 100);
      brush.drawLine(0, 100, 200, 200);
      Text text = new Text.Builder(font, "Text rendering test").widthLimit(100).fontSize(16).build();
      brush.drawText(text, 200, 200);

      mesh.end();

      // dev.getSwapchain().present();
      dev.execute();

      frame++;
    }

    // --- Cleanup ---
    enc.close();
    resourceSet.close();
    texture.close();
    sampler.close();
    uboMatrices.close();
    uboLighting.close();
    ibo.close();
    vbo.close();
    pipe.close();
    prog.close();
    vert.close();
    frag.close();
    theView.close();
  }

  private static byte[] floatArrayToBytes(float[] a) {
    ByteBuffer bb = ByteBuffer.allocate(a.length * 4).order(ByteOrder.LITTLE_ENDIAN);
    for (float f : a) {
      bb.putFloat(f);
    }
    return bb.array();
  }

  private static byte[] intArrayToBytes(int[] a) {
    ByteBuffer bb = ByteBuffer.allocate(a.length * 4).order(ByteOrder.LITTLE_ENDIAN);
    for (int i : a) {
      bb.putInt(i);
    }
    return bb.array();
  }

  private static void updateMatricesUbo(BufferObject ubo, Matrix4x4 mvp, Matrix4x4 model, Matrix3x3 normal) {
    ubo.submit(matrixToBytes(mvp), 0);
    ubo.submit(matrixToBytes(model), 64);
    ubo.submit(matrix3ToBytes(normal), 128);
  }

  private static void updateLightingUbo(BufferObject ubo, Vector3 lp, Vector3 lc, Vector3 amb, Vector3 vp) {
    ubo.submit(vec3ToBytes16(lp), 0);
    ubo.submit(vec3ToBytes16(lc), 16);
    ubo.submit(vec3ToBytes16(amb), 32);
    ubo.submit(vec3ToBytes16(vp), 48);
  }

  private static byte[] matrixToBytes(Matrix4x4 m) {
    ByteBuffer bb = ByteBuffer.allocate(64).order(ByteOrder.LITTLE_ENDIAN);
    for (float f : m.toFloatArray()) {
      bb.putFloat(f);
    }
    return bb.array();
  }

  private static byte[] matrix3ToBytes(Matrix3x3 m) {
    ByteBuffer bb = ByteBuffer.allocate(36).order(ByteOrder.LITTLE_ENDIAN);
    bb.putFloat(m.m00()).putFloat(m.m10()).putFloat(m.m20());
    bb.putFloat(m.m01()).putFloat(m.m11()).putFloat(m.m21());
    bb.putFloat(m.m02()).putFloat(m.m12()).putFloat(m.m22());
    return bb.array();
  }

  private static byte[] vec3ToBytes16(Vector3 v) {
    ByteBuffer bb = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
    bb.putFloat(v.x()).putFloat(v.y()).putFloat(v.z()).putFloat(0);
    return bb.array();
  }
}
