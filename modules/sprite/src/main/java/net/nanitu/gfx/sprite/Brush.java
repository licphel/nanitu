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
import net.nanitu.gfx.text.Font;
import net.nanitu.gfx.text.Glyph;
import net.nanitu.gfx.text.Text;
import net.nanitu.gfx.texture.Sampler;
import net.nanitu.gfx.texture.SamplerDesc;
import net.nanitu.gfx.texture.Texture;
import net.nanitu.math.*;
import net.nanitu.math.dim2.Camera2D;
import net.nanitu.memory.Buffer;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * A batched 2D drawing context created from a {@link MultiMesh}.
 *
 * <p>The Brush records draw commands — textures, rectangles, lines, points, and text — into mesh
 * nodes on the parent MultiMesh. Call {@link #flush()} to submit pending draws to the GPU,
 * {@link #begin(RenderPassDesc)} to start a render pass, and {@link #close()} when done.
 *
 * <p>Each Brush carries its own transform stack, camera, color tint, sampler, depth,
 * and view-projection matrix. These affect subsequent draw calls until changed.
 *
 * <p>This class is not thread-safe.
 *
 * @see MultiMesh#begin()
 */
public final class Brush implements AutoCloseable {
  /** Transform matrix stack applied to all draw calls. */
  public final MatrixStack transform = new MatrixStack();
  // Pipelines and resource sets: [0] = colored, [1] = textured
  private final Pipeline[] pipes = new Pipeline[2];
  private final ResourceSet[] sets = new ResourceSet[2];

  private final MultiMesh parent;
  private final boolean isDirect;
  private final Encoder encoder;
  private final BufferObject ubo;
  // State
  private final BrushState state = new BrushState();
  private final Device ctx;
  /** Callback invoked after each {@link #flush()}, may be {@code null}. */
  @Nullable
  public Consumer<Brush> onFlushed;
  private RenderTarget renderTarget;
  private Sampler sampler;
  private MultiMesh.@Nullable Node target;
  private Camera2D camera;
  private boolean disposed;
  private int flags = BrushFlag.NONE;
  private Box2 currentViewport = Box2.ZERO;
  private Scissor currentScissor = Scissor.DISABLED;
  /** Current rendering color tint. */
  private Color color = Color.WHITE;
  /** Depth value used for depth testing ({@code 0.0} = near). */
  private float depth;

  Brush(MultiMesh parent, Device ctx) {
    this.parent = parent;
    this.isDirect = parent.isDirect();

    InternalResources.init(ctx);

    renderTarget = ctx.getSwapchain();
    sampler = ctx.getSampler(SamplerDesc.PIXEL);
    camera = Camera2D.normal(800, 450);
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

    this.ctx = ctx;
  }

  /**
   * Returns the current rendering color tint.
   *
   * @return the color tint
   */
  public Color color() {
    return color;
  }

  /**
   * Sets the rendering color tint.
   *
   * @param color the color tint
   */
  public void setColor(Color color) {
    this.color = color;
  }

  /**
   * Returns the current depth value.
   *
   * @return the depth ({@code 0.0} = near)
   */
  public float depth() {
    return depth;
  }

  /**
   * Sets the depth value used for depth testing.
   *
   * @param depth the depth ({@code 0.0} = near)
   */
  public void setDepth(float depth) {
    this.depth = depth;
  }

  /**
   * Returns the current draw flags bitmask.
   *
   * @return the flags bitmask, see {@link BrushFlag}
   */
  public int flags() {
    return flags;
  }

  /**
   * Sets the draw flags bitmask.
   *
   * @param flags the flags bitmask, see {@link BrushFlag}
   */
  public void setFlags(int flags) {
    this.flags = flags;
  }

  /**
   * Returns the current camera.
   *
   * @return the camera
   */
  public Camera2D camera() {
    return camera;
  }

  /**
   * Sets the camera and updates the view-projection matrix.
   *
   * @param camera the camera
   */
  public void setCamera(Camera2D camera) {
    setViewProjection(camera.viewProjectionMatrix());
    this.camera = camera;
  }

  /**
   * Returns the current sampler.
   *
   * @return the sampler
   */
  public Sampler sampler() {
    return sampler;
  }

  /**
   * Sets the sampler, flushing pending draws first.
   *
   * @param sampler the sampler
   */
  public void setSampler(Sampler sampler) {
    flush();
    this.sampler = sampler;
  }

  /**
   * Returns the current render target.
   *
   * @return the render target
   */
  public RenderTarget renderTarget() {
    return renderTarget;
  }

  /**
   * Sets the render target, flushing pending draws first.
   *
   * @param renderTarget the render target
   */
  public void setRenderTarget(RenderTarget renderTarget) {
    flush();
    this.renderTarget = renderTarget;
  }

  /**
   * Returns the current viewport rectangle.
   *
   * @return the viewport
   */
  public Box2 currentViewport() {
    return currentViewport;
  }

  /**
   * Acquires the render target, beginning a render pass.
   *
   * @param desc the render pass configuration, may be {@code null}
   */
  public void begin(@Nullable RenderPassDesc desc) {
    renderTarget.acquire(desc);
  }

  /**
   * Flushes all pending draws and presents the render target.
   */
  void end0() {
    flush(true);
    renderTarget.present();
  }

  void moveToNextNode() {
    if (target != null) {
      target.recordedState.set(state.pipe, state.primitive, state.set, state.tex);
    }
    target = parent.acquire();
  }

  /**
   * Replays the recorded nodes of a non-direct mesh, submitting them with the current brush state.
   *
   * <p>Direct meshes are skipped — they cannot be replayed.
   *
   * @param mesh the mesh to replay
   */
  public void replay(MultiMesh mesh) {
    if (mesh.isDirect()) {
      return; // Direct meshes cannot be replayed
    }
    BrushState oldState = new BrushState();
    oldState.set(state.pipe, state.primitive, state.set, state.tex);

    for (MultiMesh.Node node : mesh) {
      if (node.isEmpty()) {
        continue;
      }
      state.set(node.recordedState.pipe, node.recordedState.primitive, node.recordedState.set, node.recordedState.tex);
      submitNode(node);
    }

    state.set(oldState.pipe, oldState.primitive, oldState.set, oldState.tex);
  }

  /**
   * Submits all recorded draw data to the GPU.
   *
   * <p>In direct mode the current node is submitted immediately. In recording mode,
   * the current node is finalized only if it contains data, and the brush advances to the next node.
   *
   * @param force if {@code true}, submits the node even when no vertex data has been recorded
   */
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

  /**
   * Flushes pending draws without forcing submission of empty nodes.
   */
  public void flush() {
    flush(false);
  }

  /**
   * Sets the viewport rectangle in screen coordinates.
   *
   * @param box the viewport rectangle
   */
  public void setViewport(Box2 box) {
    flush();
    currentViewport = box;
  }

  /**
   * Sets the scissor rectangle in world coordinates, flushing pending draws first.
   *
   * <p>The world-space rectangle is projected to screen coordinates using the current
   * camera and viewport.
   *
   * @param box the scissor rectangle in world coordinates
   */
  public void setScissor(Box2 box) {
    flush();
    Vector2 min = camera.project(new Vector2(box.minX(), box.minY()), currentViewport);
    Vector2 max = camera.project(new Vector2(box.maxX(), box.maxY()), currentViewport);
    currentScissor = new Scissor((int) Math.floor(min.x()), (int) Math.floor(min.y()),
        (int) Math.ceil(max.x() - min.x()), (int) Math.ceil(max.y() - min.y()), true);
  }

  /**
   * Disables the scissor test, flushing pending draws first.
   */
  public void disableScissor() {
    flush();
    currentScissor = Scissor.DISABLED;
  }

  /**
   * Sets the view-projection matrix, flushing pending draws first.
   *
   * @param vpm the view-projection matrix
   */
  public void setViewProjection(Matrix4x4 vpm) {
    flush();
    uploadViewProjection(vpm);
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

  /**
   * Draws a region of a texture at the given position and size.
   *
   * <p>The source region {@code (u, v, uw, vh)} is specified in texel coordinates.
   * The destination rectangle {@code (x, y, w, h)} is specified in world units and transformed by the current transform
   * stack. Current draw flags, color, and depth are applied.
   *
   * @param tex the texture to draw, does nothing if {@code null}
   * @param x   the X position in world units
   * @param y   the Y position in world units
   * @param w   the width in world units
   * @param h   the height in world units
   * @param u   the U coordinate of the source region in texels
   * @param v   the V coordinate of the source region in texels
   * @param uw  the width of the source region in texels
   * @param vh  the height of the source region in texels
   */
  public void drawTexture(@Nullable Texture tex, float x, float y, float w, float h, float u, float v, float uw,
                          float vh) {
    if (tex == null || target == null) {
      return;
    }
    assertPrimitive(BrushPrimitive.TEXTURE_SPRITE);
    assertTexture(tex);

    Buffer vBuf = target.vertexBuf;
    Buffer iBuf = target.indexBuf;

    float invW = 1.0F / tex.width();
    float invH = 1.0F / tex.height();
    u *= invW;
    v *= invH;
    float u2 = u + uw * invW;
    float v2 = v + vh * invH;

    if ((flags & BrushFlag.FLIP_X) != 0) {
      float tmp = u;
      u = u2;
      u2 = tmp;
    }
    if ((flags & BrushFlag.FLIP_Y) != 0) {
      float tmp = v;
      v = v2;
      v2 = tmp;
    }

    // Transform corners
    Vector3 p0 = transform.top().transform(new Vector3(x, y, depth));
    Vector3 p1 = transform.top().transform(new Vector3(x + w, y, depth));
    Vector3 p2 = transform.top().transform(new Vector3(x + w, y + h, depth));
    Vector3 p3 = transform.top().transform(new Vector3(x, y + h, depth));

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

  /**
   * Draws a source region of a texture into a destination rectangle.
   *
   * @param tex the texture
   * @param dst the destination rectangle in world units
   * @param src the source region in texel coordinates
   */
  public void drawTexture(@Nullable Texture tex, Box2 dst, Box2 src) {
    if (tex == null) {
      return;
    }
    drawTexture(tex, dst.minX(), dst.minY(), dst.width(), dst.height(), src.minX(), src.minY(), src.width(),
        src.height());
  }

  /**
   * Draws the entire texture into a destination rectangle.
   *
   * @param tex the texture
   * @param dst the destination rectangle in world units
   */
  public void drawTexture(@Nullable Texture tex, Box2 dst) {
    if (tex == null) {
      return;
    }
    drawTexture(tex, dst, Box2.create(0, 0, tex.width(), tex.height()));
  }

  /**
   * Draws the entire texture at the given position and size.
   *
   * @param tex the texture
   * @param x   the X position in world units
   * @param y   the Y position in world units
   * @param w   the width in world units
   * @param h   the height in world units
   */
  public void drawTexture(@Nullable Texture tex, float x, float y, float w, float h) {
    if (tex == null) {
      return;
    }
    drawTexture(tex, x, y, w, h, 0, 0, tex.width(), tex.height());
  }

  /**
   * Draws a texture part's region into a destination rectangle.
   *
   * @param texPart the texture part defining the source texture and region
   * @param dst     the destination rectangle in world units
   */
  public void drawTexture(@Nullable TexturePart texPart, Box2 dst) {
    if (texPart == null) {
      return;
    }
    drawTexture(texPart.src(), dst, texPart.region());
  }

  /**
   * Draws a texture part with an additional source region offset.
   *
   * @param texPart the texture part
   * @param dst     the destination rectangle in world units
   * @param src     the source region relative to the texture part's UV origin, in texels
   */
  public void drawTexture(@Nullable TexturePart texPart, Box2 dst, Box2 src) {
    if (texPart == null) {
      return;
    }
    drawTexture(new TexturePart(texPart.src(), src), dst);
  }

  /**
   * Draws a texture part's full region at the given position and size.
   *
   * @param texPart the texture part
   * @param x       the X position in world units
   * @param y       the Y position in world units
   * @param w       the width in world units
   * @param h       the height in world units
   */
  public void drawTexture(@Nullable TexturePart texPart, float x, float y, float w, float h) {
    if (texPart == null) {
      return;
    }
    drawTexture(texPart.src(), x, y, w, h, texPart.u(), texPart.v(), texPart.width(), texPart.height());
  }

  /**
   * Draws a region of a texture part at the given position and size.
   *
   * <p>The source region is offset by the texture part's UV origin.
   *
   * @param texPart the texture part
   * @param x       the X position in world units
   * @param y       the Y position in world units
   * @param w       the width in world units
   * @param h       the height in world units
   * @param u       the U coordinate offset relative to the part's UV origin, in texels
   * @param v       the V coordinate offset relative to the part's UV origin, in texels
   * @param uw      the width of the source region in texels
   * @param vh      the height of the source region in texels
   */
  public void drawTexture(@Nullable TexturePart texPart, float x, float y, float w, float h, float u, float v,
                          float uw, float vh) {
    if (texPart == null) {
      return;
    }
    drawTexture(texPart.src(), x, y, w, h, u + texPart.u(), v + texPart.v(), uw, vh);
  }

  /**
   * Draws a drawable into a destination rectangle.
   *
   * @param t   the drawable
   * @param dst the destination rectangle in world units
   */
  public void draw(@Nullable Drawable t, Box2 dst) {
    if (t == null) {
      return;
    }
    t.draw(this, dst.minX(), dst.minY(), dst.width(), dst.height(), 0, 0, 0, 0);
  }

  /**
   * Draws a drawable with a source region into a destination rectangle.
   *
   * @param t   the drawable
   * @param dst the destination rectangle in world units
   * @param src the source region
   */
  public void draw(@Nullable Drawable t, Box2 dst, Box2 src) {
    if (t == null) {
      return;
    }
    t.draw(this, dst.minX(), dst.minY(), dst.width(), dst.height(), src.minX(), src.minY(), src.width(), src.height());
  }

  /**
   * Draws a drawable at the given position and size.
   *
   * @param t the drawable
   * @param x the X position in world units
   * @param y the Y position in world units
   * @param w the width in world units
   * @param h the height in world units
   */
  public void draw(@Nullable Drawable t, float x, float y, float w, float h) {
    if (t == null) {
      return;
    }
    t.draw(this, x, y, w, h, 0, 0, 0, 0);
  }

  /**
   * Draws a drawable at the given position, size, and source region.
   *
   * @param t  the drawable
   * @param x  the X position in world units
   * @param y  the Y position in world units
   * @param w  the width in world units
   * @param h  the height in world units
   * @param u  the U coordinate offset
   * @param v  the V coordinate offset
   * @param uw the U coordinate range
   * @param vh the V coordinate range
   */
  public void draw(@Nullable Drawable t, float x, float y, float w, float h, float u, float v, float uw, float vh) {
    if (t == null) {
      return;
    }
    t.draw(this, x, y, w, h, u, v, uw, vh);
  }

  /**
   * Draws a filled rectangle at the given position and size.
   *
   * @param x the X position in world units
   * @param y the Y position in world units
   * @param w the width in world units
   * @param h the height in world units
   */
  public void drawRectangle(float x, float y, float w, float h) {
    if (target == null) {
      return;
    }
    assertPrimitive(BrushPrimitive.COLOR_SPRITE);

    Buffer vBuf = target.vertexBuf;
    Buffer iBuf = target.indexBuf;

    Vector3 p0 = transform.top().transform(new Vector3(x, y, depth));
    Vector3 p1 = transform.top().transform(new Vector3(x + w, y, depth));
    Vector3 p2 = transform.top().transform(new Vector3(x + w, y + h, depth));
    Vector3 p3 = transform.top().transform(new Vector3(x, y + h, depth));

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

  /**
   * Draws a filled rectangle.
   *
   * @param dst the destination rectangle in world units
   */
  public void drawRectangle(Box2 dst) {
    drawRectangle(dst.minX(), dst.minY(), dst.width(), dst.height());
  }

  /**
   * Draws the outline of a rectangle as four line segments.
   *
   * @param x the X position in world units
   * @param y the Y position in world units
   * @param w the width in world units
   * @param h the height in world units
   */
  public void drawRectangleFrame(float x, float y, float w, float h) {
    drawLine(x, y, x + w, y);
    drawLine(x, y, x, y + h);
    drawLine(x + w, y, x + w, y + h);
    drawLine(x, y + h, x + w, y + h);
  }

  /**
   * Draws the outline of a rectangle as four line segments.
   *
   * @param dst the destination rectangle in world units
   */
  public void drawRectangleFrame(Box2 dst) {
    drawRectangleFrame(dst.minX(), dst.minY(), dst.width(), dst.height());
  }

  /**
   * Draws a line segment between two points.
   *
   * @param x1 the X coordinate of the first endpoint
   * @param y1 the Y coordinate of the first endpoint
   * @param x2 the X coordinate of the second endpoint
   * @param y2 the Y coordinate of the second endpoint
   */
  public void drawLine(float x1, float y1, float x2, float y2) {
    if (target == null) {
      return;
    }
    assertPrimitive(BrushPrimitive.COLOR_LINE);

    Buffer vBuf = target.vertexBuf;

    Vector3 t1 = transform.top().transform(new Vector3(x1, y1, depth));
    Vector3 t2 = transform.top().transform(new Vector3(x2, y2, depth));

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

  /**
   * Draws a line segment between two points.
   *
   * @param from the first endpoint
   * @param to   the second endpoint
   */
  public void drawLine(Vector2 from, Vector2 to) {
    drawLine(from.x(), from.y(), to.x(), to.y());
  }

  /**
   * Draws a single point at the given coordinates.
   *
   * @param x the X coordinate in world units
   * @param y the Y coordinate in world units
   */
  public void drawPoint(float x, float y) {
    if (target == null) {
      return;
    }
    assertPrimitive(BrushPrimitive.COLOR_POINT);

    Buffer vBuf = target.vertexBuf;

    Vector3 t = transform.top().transform(new Vector3(x, y, depth));

    vBuf.putFloat(t.x());
    vBuf.putFloat(t.y());
    vBuf.putFloat(t.z());
    vBuf.putLong(color.packToHalves());

    target.write(1, 0);
  }

  /**
   * Draws a single point at the given position.
   *
   * @param at the position in world units
   */
  public void drawPoint(Vector2 at) {
    drawPoint(at.x(), at.y());
  }

  /**
   * Draws a text blob at the given position with the specified alignment.
   *
   * <p>The alignment determines how the text is positioned relative to the anchor point.
   * For example, {@link Alignment#CENTRAL} centers the text both horizontally and vertically.
   *
   * @param text      the text blob
   * @param x         the anchor X coordinate in world units
   * @param y         the anchor Y coordinate in world units
   * @param alignment the alignment relative to the anchor point
   */
  public void drawText(@Nullable Text text, float x, float y, Alignment alignment) {
    if (text == null) {
      return;
    }

    float tx = x, ty = y;
    switch (alignment.horizontal()) {
      case 0 -> tx -= text.bounds().width() / 2.0F;
      case 1 -> tx -= text.bounds().width();
    }
    switch (alignment.vertical()) {
      case 0 -> ty -= text.bounds().height() / 2.0F;
      case 1 -> ty -= text.bounds().height();
    }

    Font font = text.font();
    int[] glyphs = text.glyphs();
    float scale = text.scale();
    float yBearingSign = text.flipY() ? -1.0F : 1.0F;

    for (int i = 0; i < text.glyphCount(); i++) {
      Glyph cg = font.rasterizeGlyph(ctx, glyphs[i], text.fontStyle());
      if (cg == null) {
        continue;
      }

      float gx = tx + text.glyphX(i) + cg.bearingX() * scale;
      float gy = ty + text.glyphY(i) + yBearingSign * cg.bearingY() * scale;

      drawTexture(cg.texPart(), gx, gy, cg.texPart().width() * scale, cg.texPart().height() * scale);
    }
  }

  /**
   * Draws a text blob at the given position with {@link Alignment#LEFT_UP}.
   *
   * <p>The alignment determines how the text is positioned relative to the anchor point.
   * For example, {@link Alignment#CENTRAL} centers the text both horizontally and vertically.
   *
   * @param text the text blob
   * @param x    the anchor X coordinate in world units
   * @param y    the anchor Y coordinate in world units
   */
  public void drawText(@Nullable Text text, float x, float y) {
    drawText(text, x, y, Alignment.LEFT_UP);
  }

  /**
   * Draws a text blob at the given position with the specified alignment.
   *
   * <p>The alignment determines how the text is positioned relative to the anchor point.
   * For example, {@link Alignment#CENTRAL} centers the text both horizontally and vertically.
   *
   * @param text      the text blob
   * @param pos       the combined position 2D
   * @param alignment the alignment relative to the anchor point
   */
  public void drawText(@Nullable Text text, Vector2 pos, Alignment alignment) {
    drawText(text, pos.x(), pos.y(), alignment);
  }

  /**
   * Draws a text blob at the given position with {@link Alignment#LEFT_UP}.
   *
   * <p>The alignment determines how the text is positioned relative to the anchor point.
   * For example, {@link Alignment#CENTRAL} centers the text both horizontally and vertically.
   *
   * @param text the text blob
   * @param pos  the combined position 2D
   */
  public void drawText(@Nullable Text text, Vector2 pos) {
    drawText(text, pos.x(), pos.y());
  }

  private void submitNode(MultiMesh.Node node, boolean force) {
    if (state.pipe == null || state.set == null || state.primitive == null) {
      return;
    }
    if (node.vertexCount <= 0 && !force) {
      return;
    }

    encoder.setViewport((int) currentViewport.minX(), (int) currentViewport.minY(), (int) currentViewport.width(),
        (int) currentViewport.height());
    encoder.setScissor(currentScissor.x, currentScissor.y, currentScissor.width, currentScissor.height,
        currentScissor.enable);

    if (node.vertexCount <= 0) {
      if (force) {
        encoder.queuedExecute();
        encoder.reset();
      }
      return;
    }

    encoder.setRenderPipe(state.pipe);

    if (node.dirty) {
      node.vbo.submit(node.vertexBuf.slice());
    }
    encoder.setBuffer(node.vbo);

    state.set.bindUniform(0, ubo, 64);
    encoder.setResource(0, state.set);

    switch (state.primitive) {
      case TEXTURE_SPRITE -> {
        if (state.tex != null) {
          state.set.bindTexture(1, state.tex, sampler);
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

    encoder.queuedExecute();
    encoder.reset();

    node.dirty = false;

    if (onFlushed != null) {
      onFlushed.accept(this);
    }
  }

  private void submitNode(MultiMesh.Node node) {
    submitNode(node, false);
  }

  private void assertPrimitive(BrushPrimitive primitive) {
    if (state.primitive == primitive) {
      return;
    }
    flush();

    if (primitive == BrushPrimitive.TEXTURE_SPRITE) {
      state.set(pipes[1], primitive, sets[1], state.tex);
    } else {
      state.set(pipes[0], primitive, sets[0], null);
    }
  }

  private void assertTexture(Texture tex) {
    if (state.tex == tex) {
      return;
    }
    flush();
    state.tex = tex;
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

  private record Scissor(int x, int y, int width, int height, boolean enable) {
    static final Scissor DISABLED = new Scissor(0, 0, 0, 0, false);
  }
}
