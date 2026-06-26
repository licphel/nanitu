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

import net.nanitu.gfx.pass.RenderTarget;
import net.nanitu.gfx.text.Text;
import net.nanitu.gfx.texture.FragileTexture;
import net.nanitu.gfx.texture.Sampler;
import net.nanitu.gfx.texture.Texture;
import net.nanitu.gfx.texture.TexturePart;
import net.nanitu.math.*;
import net.nanitu.math.dim2.Camera2D;
import org.jspecify.annotations.Nullable;

/**
 * A batched 2D drawing context created from a {@link MultiMesh}.
 *
 * <p>The Graphics records draw commands — textures, rectangles, lines, points, and text — into mesh
 * nodes on the parent MultiMesh. Call {@link #flush()} to submit pending draws to the GPU.
 *
 * <p>Each Graphics carries its own transform stack, camera, color tint, sampler, 0.0F,
 * and view-projection matrix. These affect subsequent draw calls until changed.
 *
 * <p>This class is not thread-safe.
 *
 * @see MultiMesh#begin()
 */
public abstract class Graphics implements AutoCloseable {
  /** Transform matrix stack applied to all draw calls. */
  public final MatrixStack transform = new MatrixStack();

  /**
   * Returns the current rendering color tint.
   *
   * @return the color tint
   */
  public abstract Color color();

  /**
   * Sets the rendering color tint.
   *
   * @param color the color tint
   */
  public abstract void setColor(Color color);

  /**
   * Returns the current draw flags bitmask.
   *
   * @return the flags bitmask, see {@link GraphicsFlag}
   */
  public abstract int flags();

  /**
   * Sets the draw flags bitmask.
   *
   * @param flags the flags bitmask, see {@link GraphicsFlag}
   */
  public abstract void setFlags(int flags);

  /**
   * Returns the current camera.
   *
   * @return the camera
   */
  public abstract Camera2D camera();

  /**
   * Sets the camera and updates the view-projection matrix.
   *
   * @param camera the camera
   */
  public abstract void setCamera(Camera2D camera);

  /**
   * Returns the current sampler.
   *
   * @return the sampler
   */
  public abstract Sampler sampler();

  /**
   * Sets the sampler, flushing pending draws first.
   *
   * @param sampler the sampler
   */
  public abstract void setSampler(Sampler sampler);

  /**
   * Returns the current render target.
   *
   * @return the render target
   */
  public abstract RenderTarget renderTarget();

  /**
   * Sets the render target, flushing pending draws first.
   *
   * @param renderTarget the render target
   */
  public abstract void setRenderTarget(RenderTarget renderTarget);

  /**
   * Returns the current viewport rectangle.
   *
   * @return the viewport
   */
  public abstract Box2 currentViewport();

  /**
   * Replays the recorded nodes of a non-direct mesh, submitting them with the current brush state.
   *
   * <p>Direct meshes are skipped — they cannot be replayed.
   *
   * @param mesh the mesh to replay
   */
  public abstract void replay(MultiMesh mesh);

  /**
   * Submits all recorded draw data to the GPU.
   *
   * <p>In direct mode the current node is submitted immediately. In recording mode,
   * the current node is finalized only if it contains data, and the brush advances to the next node.
   *
   * @param force if {@code true}, submits the node even when no vertex data has been recorded
   */
  public abstract void flush(boolean force);

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
  public abstract void setViewport(Box2 box);

  /**
   * Sets the scissor rectangle in world coordinates, flushing pending draws first.
   *
   * <p>The world-space rectangle is projected to screen coordinates using the current
   * camera and viewport.
   *
   * @param box the scissor rectangle in world coordinates
   */
  public abstract void setScissor(Box2 box);

  /**
   * Disables the scissor test, flushing pending draws first.
   */
  public abstract void disableScissor();

  /**
   * Sets the view-projection matrix, flushing pending draws first.
   *
   * @param vpm the view-projection matrix
   */
  public abstract void setViewProjection(Matrix4x4 vpm);

  /**
   * Draws a region of a texture at the given position and size.
   *
   * <p>The source region {@code (u, v, uw, vh)} is specified in texel coordinates.
   * The destination rectangle {@code (x, y, w, h)} is specified in world units and transformed by the current transform
   * stack. Current draw flags, color, and 0.0F are applied.
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
  public abstract void drawTexture(@Nullable FragileTexture tex, float x, float y, float w, float h, float u, float v
      , float uw, float vh);

  /**
   * Draws a source region of a texture into a destination rectangle.
   *
   * @param tex the texture
   * @param dst the destination rectangle in world units
   * @param src the source region in texel coordinates
   */
  public void drawTexture(@Nullable FragileTexture tex, Box2 dst, Box2 src) {
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
  public void drawTexture(@Nullable FragileTexture tex, Box2 dst) {
    if (tex == null) {
      return;
    }
    Texture pinned = tex.pin();
    if (pinned == null) {
      return;
    }
    drawTexture(tex, dst, Box2.create(0.0F, 0.0F, pinned.width(), pinned.height()));
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
  public void drawTexture(@Nullable FragileTexture tex, float x, float y, float w, float h) {
    if (tex == null) {
      return;
    }
    Texture pinned = tex.pin();
    if (pinned == null) {
      return;
    }
    drawTexture(tex, x, y, w, h, 0, 0, pinned.width(), pinned.height());
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
    drawTexture(new TexturePart(texPart, src), dst);
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
  public abstract void drawRectangle(float x, float y, float w, float h);

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
  public abstract void drawLine(float x1, float y1, float x2, float y2);

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
  public abstract void drawPoint(float x, float y);

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
  public abstract void drawText(@Nullable Text text, float x, float y, Alignment alignment);

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

  @Override
  public abstract void close();

  protected record Scissor(int x, int y, int width, int height, boolean enable) {
    static final Scissor DISABLED = new Scissor(0, 0, 0, 0, false);
  }
}
