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
import net.nanitu.gfx.buffer.BufferObject;
import net.nanitu.gfx.buffer.BufferObjectDesc;
import net.nanitu.gfx.cmd.Encoder;
import net.nanitu.gfx.cmd.EncoderDesc;
import net.nanitu.gfx.pass.RenderPassDesc;
import net.nanitu.gfx.pass.RenderTarget;
import net.nanitu.gfx.pipe.Pipeline;
import net.nanitu.gfx.pipe.Topology;
import net.nanitu.gfx.shader.ResourceSet;
import net.nanitu.gfx.text.Text;
import net.nanitu.gfx.text.raster.Glyph;
import net.nanitu.gfx.text.raster.Raster;
import net.nanitu.gfx.texture.FragileTexture;
import net.nanitu.gfx.texture.Sampler;
import net.nanitu.gfx.texture.SamplerDesc;
import net.nanitu.gfx.texture.Texture;
import net.nanitu.math.*;
import net.nanitu.math.dim2.Camera2D;
import net.nanitu.memory.Buffer;
import net.nanitu.util.InternalApi;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

@InternalApi
final class Graphics2D extends Graphics {
  private final Pipeline[] pipes = new Pipeline[2];
  private final ResourceSet[] sets = new ResourceSet[2];
  private final MultiMesh parent;
  private final boolean isDirect;
  private final Encoder encoder;
  private final BufferObject ubo;
  private final Recorder rec = new Recorder();
  public @Nullable Consumer<Graphics> onFlushed;
  private RenderTarget renderTarget;
  private Sampler sampler;
  private MultiMesh.@Nullable Node target;
  private Camera2D camera;
  private boolean disposed;
  private int flags = GraphicsFlag.NONE;
  private Box2 currentViewport = Box2.ZERO;
  private Scissor currentScissor = Scissor.DISABLED;
  private Color color = Color.WHITE;

  Graphics2D(MultiMesh parent, Device ctx) {
    this.parent = parent;
    this.isDirect = parent.isDirect();

    InternalResources.init(ctx);

    renderTarget = ctx.getSwapchain();
    sampler = ctx.getSampler(SamplerDesc.PIXEL);
    camera = new Camera2D(800, 450);
    ubo = ctx.getBuffer(BufferObjectDesc.uniform());
    encoder = ctx.getEncoder(EncoderDesc.DEFAULT);

    assert InternalResources.p4c != null;
    assert InternalResources.p4t != null;
    assert InternalResources.rl4c != null;
    assert InternalResources.rl4t != null;
    pipes[0] = InternalResources.p4c;
    pipes[1] = InternalResources.p4t;
    sets[0] = ctx.getResourceSet(InternalResources.rl4c);
    sets[1] = ctx.getResourceSet(InternalResources.rl4t);

    // Upload identity view-projection
    uploadViewProjection(Matrix4x4.IDENTITY);
  }

  @Override
  public Color color() {
    return color;
  }

  @Override
  public void setColor(Color color) {
    this.color = color;
  }

  @Override
  public int flags() {
    return flags;
  }

  @Override
  public void setFlags(int flags) {
    this.flags = flags;
  }

  @Override
  public Camera2D camera() {
    return camera;
  }

  @Override
  public void setCamera(Camera2D camera) {
    setViewProjection(camera.viewProjectionMatrix());
    this.camera = camera;
  }

  @Override
  public Sampler sampler() {
    return sampler;
  }

  @Override
  public void setSampler(Sampler sampler) {
    flush();
    this.sampler = sampler;
  }

  @Override
  public RenderTarget renderTarget() {
    return renderTarget;
  }

  @Override
  public void setRenderTarget(RenderTarget renderTarget) {
    flush();
    this.renderTarget = renderTarget;
  }

  @Override
  public Box2 currentViewport() {
    return currentViewport;
  }

  @Override
  public void replay(MultiMesh mesh) {
    if (mesh.isDirect()) {
      return; // Direct meshes cannot be replayed
    }
    Recorder oldState = new Recorder();
    oldState.set(rec.pipe, rec.primitive, rec.set, rec.tex);

    for (MultiMesh.Node node : mesh) {
      if (node.isEmpty()) {
        continue;
      }
      rec.set(node.recordedState.pipe, node.recordedState.primitive, node.recordedState.set, node.recordedState.tex);
      submitNode(node);
    }

    rec.set(oldState.pipe, oldState.primitive, oldState.set, oldState.tex);
  }

  @Override
  public void flush(boolean force) {
    if (target == null) {
      return;
    }
    if (isDirect) {
      submitNode(target, force);
      target.reset();
    } else if (!target.isEmpty()) {
      moveToNextNode();
    }
  }

  @Override
  public void setViewport(Box2 box) {
    flush();
    currentViewport = box;
  }

  @Override
  public void setScissor(Box2 box) {
    flush();
    Vector2 min = camera.project(new Vector2(box.minX(), box.minY()), currentViewport);
    Vector2 max = camera.project(new Vector2(box.maxX(), box.maxY()), currentViewport);
    currentScissor = new Scissor((int) Math.floor(min.x()), (int) Math.floor(min.y()),
        (int) Math.ceil(max.x() - min.x()), (int) Math.ceil(max.y() - min.y()), true);
  }

  @Override
  public void disableScissor() {
    flush();
    currentScissor = Scissor.DISABLED;
  }

  @Override
  public void setViewProjection(Matrix4x4 vpm) {
    flush();
    uploadViewProjection(vpm);
  }

  @Override
  public void drawTexture(@Nullable FragileTexture tex, float x, float y, float w, float h, float u, float v,
                          float uw, float vh) {
    if (tex == null || target == null) {
      return;
    }
    Texture pinned = tex.pin();
    if (pinned == null) {
      return;
    }
    assertPrimitive(Primitive.TEXTURE_SPRITE);
    assertTexture(pinned);

    Buffer vBuf = target.vertexBuf;
    Buffer iBuf = target.indexBuf;

    float invW = 1.0F / pinned.width();
    float invH = 1.0F / pinned.height();
    u *= invW;
    v *= invH;
    float u2 = u + uw * invW;
    float v2 = v + vh * invH;

    if ((flags & GraphicsFlag.FLIP_X) != 0) {
      float tmp = u;
      u = u2;
      u2 = tmp;
    }
    if ((flags & GraphicsFlag.FLIP_Y) != 0) {
      float tmp = v;
      v = v2;
      v2 = tmp;
    }

    // Transform corners
    Vector3 p0 = transform.top().transform(new Vector3(x, y, 0.0F));
    Vector3 p1 = transform.top().transform(new Vector3(x + w, y, 0.0F));
    Vector3 p2 = transform.top().transform(new Vector3(x + w, y + h, 0.0F));
    Vector3 p3 = transform.top().transform(new Vector3(x, y + h, 0.0F));

    float dMid = (p0.z() + p2.z()) * 0.5F;

    // Vertex 0
    vBuf.putFloat(p0.x());
    vBuf.putFloat(p0.y());
    vBuf.putFloat(p0.z());
    vBuf.putLong(color.packToHalves());
    vBuf.putFloat(u);
    vBuf.putFloat(v);
    // Vertex 1
    vBuf.putFloat(p1.x());
    vBuf.putFloat(p1.y());
    vBuf.putFloat(dMid);
    vBuf.putLong(color.packToHalves());
    vBuf.putFloat(u2);
    vBuf.putFloat(v);
    // Vertex 2
    vBuf.putFloat(p2.x());
    vBuf.putFloat(p2.y());
    vBuf.putFloat(p2.z());
    vBuf.putLong(color.packToHalves());
    vBuf.putFloat(u2);
    vBuf.putFloat(v2);
    // Vertex 3
    vBuf.putFloat(p3.x());
    vBuf.putFloat(p3.y());
    vBuf.putFloat(dMid);
    vBuf.putLong(color.packToHalves());
    vBuf.putFloat(u);
    vBuf.putFloat(v2);

    int baseVertex = target.vertexCount;
    iBuf.putInt(baseVertex);
    iBuf.putInt(baseVertex + 2);
    iBuf.putInt(baseVertex + 1);
    iBuf.putInt(baseVertex + 2);
    iBuf.putInt(baseVertex);
    iBuf.putInt(baseVertex + 3);

    target.write(4, 6);
  }

  @Override
  public void drawRectangle(float x, float y, float w, float h) {
    if (target == null) {
      return;
    }
    assertPrimitive(Primitive.COLOR_SPRITE);

    Buffer vBuf = target.vertexBuf;
    Buffer iBuf = target.indexBuf;

    Vector3 p0 = transform.top().transform(new Vector3(x, y, 0.0F));
    Vector3 p1 = transform.top().transform(new Vector3(x + w, y, 0.0F));
    Vector3 p2 = transform.top().transform(new Vector3(x + w, y + h, 0.0F));
    Vector3 p3 = transform.top().transform(new Vector3(x, y + h, 0.0F));

    float dMid = (p0.z() + p2.z()) * 0.5F;

    // Vertex 0
    vBuf.putFloat(p0.x());
    vBuf.putFloat(p0.y());
    vBuf.putFloat(p0.z());
    vBuf.putLong(color.packToHalves());
    // Vertex 1
    vBuf.putFloat(p1.x());
    vBuf.putFloat(p1.y());
    vBuf.putFloat(dMid);
    vBuf.putLong(color.packToHalves());
    // Vertex 2
    vBuf.putFloat(p2.x());
    vBuf.putFloat(p2.y());
    vBuf.putFloat(p2.z());
    vBuf.putLong(color.packToHalves());
    // Vertex 3
    vBuf.putFloat(p3.x());
    vBuf.putFloat(p3.y());
    vBuf.putFloat(dMid);
    vBuf.putLong(color.packToHalves());

    int baseVertex = target.vertexCount;
    iBuf.putInt(baseVertex);
    iBuf.putInt(baseVertex + 2);
    iBuf.putInt(baseVertex + 1);
    iBuf.putInt(baseVertex + 2);
    iBuf.putInt(baseVertex);
    iBuf.putInt(baseVertex + 3);

    target.write(4, 6);
  }

  @Override
  public void drawLine(float x1, float y1, float x2, float y2) {
    if (target == null) {
      return;
    }
    assertPrimitive(Primitive.COLOR_LINE);

    Buffer vBuf = target.vertexBuf;

    Vector3 t1 = transform.top().transform(new Vector3(x1, y1, 0.0F));
    Vector3 t2 = transform.top().transform(new Vector3(x2, y2, 0.0F));

    vBuf.putFloat(t1.x());
    vBuf.putFloat(t1.y());
    vBuf.putFloat(t1.z());
    vBuf.putLong(color.packToHalves());

    vBuf.putFloat(t2.x());
    vBuf.putFloat(t2.y());
    vBuf.putFloat(t2.z());
    vBuf.putLong(color.packToHalves());

    target.write(2, 0);
  }

  @Override
  public void drawPoint(float x, float y) {
    if (target == null) {
      return;
    }
    assertPrimitive(Primitive.COLOR_POINT);

    Buffer vBuf = target.vertexBuf;

    Vector3 t = transform.top().transform(new Vector3(x, y, 0.0F));

    vBuf.putFloat(t.x());
    vBuf.putFloat(t.y());
    vBuf.putFloat(t.z());
    vBuf.putLong(color.packToHalves());

    target.write(1, 0);
  }

  @Override
  public void drawText(@Nullable Text text, float x, float y, Alignment alignment) {
    if (text == null) {
      return;
    }

    Raster raster = text.raster();
    Box2 rasterBd = raster.bounds();

    float tx = x;
    float ty = y;
    switch (alignment.horizontal()) {
      case 0 -> tx -= rasterBd.width() / 2.0F;
      case 1 -> tx -= rasterBd.width();
    }
    switch (alignment.vertical()) {
      case 0 -> ty -= rasterBd.height() / 2.0F;
      case 1 -> ty -= rasterBd.height();
    }

    // Normalize entry coordinates: rasterBd.minY() is the pen-space origin offset.
    // Subtract it so that the text block's visual top aligns with (tx, ty).
    float originY = rasterBd.minY();

    Color originalColor = color();

    for (Raster.Entry entry : raster.entries()) {
      Glyph cg = entry.glyph();
      if (cg == null) {
        continue;
      }

      // Bearings are baked in (gx, gy, pixelW, pixelH)
      setColor(entry.color());
      Box2 bounds = entry.bounds();
      drawTexture(cg.texPart(), tx + bounds.minX(), ty + bounds.minY() - originY, bounds.width(), bounds.height());
    }

    for (Raster.Stroke stroke : raster.strokes()) {
      setColor(stroke.color());
      Box2 bounds = stroke.bounds();
      drawRectangle(tx + bounds.minX(), ty + bounds.minY() - originY, bounds.width(), bounds.height());
    }

    setColor(originalColor);
  }

  @Override
  public void close() {
    if (disposed) {
      return;
    }
    disposed = true;
    sampler.close();
    encoder.close();
    ubo.close();
  }

  /**
   * Acquires the render target, beginning a render pass.
   */
  public void begin() {
    encoder.beginPass(RenderPassDesc.of(renderTarget, Color.BLACK));
  }

  /**
   * Flushes all pending draws and presents the render target.
   */
  void end0() {
    flush(true);
  }

  void moveToNextNode() {
    if (target != null) {
      target.recordedState.set(rec.pipe, rec.primitive, rec.set, rec.tex);
    }
    target = parent.acquire();
  }

  private void uploadViewProjection(Matrix4x4 vpm) {
    float[] m = vpm.toFloatArray();
    byte[] bytes = new byte[64];
    for (int i = 0; i < 16; i++) {
      int bits = Float.floatToRawIntBits(m[i]);
      int off = i * 4;
      bytes[off] = (byte) (bits);
      bytes[off + 1] = (byte) (bits >> 8);
      bytes[off + 2] = (byte) (bits >> 16);
      bytes[off + 3] = (byte) (bits >> 24);
    }
    ubo.submit(bytes, 0, bytes.length);
  }

  private void submitNode(MultiMesh.Node node, boolean force) {
    if (rec.pipe == null || rec.set == null || rec.primitive == null) {
      return;
    }
    if (node.vertexCount <= 0 && !force) {
      return;
    }

    encoder.setViewport((int) currentViewport.minX(), (int) currentViewport.minY(), (int) currentViewport.width(),
        (int) currentViewport.height());
    encoder.setScissor(currentScissor.x(), currentScissor.y(), currentScissor.width(), currentScissor.height(),
        currentScissor.enable());

    if (node.vertexCount <= 0) {
      if (force) {
        encoder.queuedExecute();
        encoder.reset();
      }
      return;
    }

    encoder.setRenderPipe(rec.pipe);

    if (node.dirty) {
      node.vbo.submit(node.vertexBuf.slice());
    }
    encoder.setBuffer(node.vbo);

    rec.set.bindUniform(0, ubo, 64);
    encoder.setResource(0, rec.set);

    switch (rec.primitive) {
      case TEXTURE_SPRITE -> {
        if (rec.tex != null) {
          rec.set.bindTexture(1, rec.tex, sampler);
        }
        encoder.setTopology(Topology.TRIANGLE);
        if (node.dirty) {
          node.ibo.submit(node.indexBuf.slice());
        }
        encoder.setBuffer(node.ibo);
        encoder.drawIndexed(node.indexCount, 0);
      }
      case COLOR_SPRITE -> {
        encoder.setTopology(Topology.TRIANGLE);
        if (node.dirty) {
          node.ibo.submit(node.indexBuf.slice());
        }
        encoder.setBuffer(node.ibo);
        encoder.drawIndexed(node.indexCount, 0);
      }
      case COLOR_LINE -> {
        encoder.setTopology(Topology.LINE);
        encoder.draw(node.vertexCount, 0);
      }
      case COLOR_POINT -> {
        encoder.setTopology(Topology.POINT);
        encoder.draw(node.vertexCount, 0);
      }
    }

    encoder.endPass();
    encoder.queuedExecute();
    encoder.reset();

    node.dirty = false;

    if (onFlushed != null) {
      onFlushed.accept(this);
    }

    // Begin a new render pass for future rendering.
    encoder.beginPass(new RenderPassDesc.Builder().target(renderTarget).clearMask(0).build());
  }

  private void submitNode(MultiMesh.Node node) {
    submitNode(node, false);
  }

  private void assertPrimitive(Primitive primitive) {
    if (rec.primitive == primitive) {
      return;
    }
    flush();

    if (primitive == Primitive.TEXTURE_SPRITE) {
      rec.set(pipes[1], primitive, sets[1], rec.tex);
    } else {
      rec.set(pipes[0], primitive, sets[0], null);
    }
  }

  private void assertTexture(Texture tex) {
    if (rec.tex == tex) {
      return;
    }
    flush();
    rec.tex = tex;
  }
}
