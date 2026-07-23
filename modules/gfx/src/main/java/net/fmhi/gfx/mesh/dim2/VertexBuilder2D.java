package net.fmhi.gfx.mesh.dim2;

import net.fmhi.gfx.mesh.VertexBuilder;
import net.fmhi.gfx.text.Text;
import net.fmhi.gfx.text.raster.Glyph;
import net.fmhi.gfx.text.raster.Raster;
import net.fmhi.gfx.texture.FragileTexture;
import net.fmhi.gfx.texture.Texture;
import net.fmhi.gfx.texture.TexturePart;
import net.fmhi.math.Box2;
import net.fmhi.math.Color;
import net.fmhi.math.Vector2;
import org.jspecify.annotations.Nullable;

/**
 * A 2D vertex builder that extends {@link VertexBuilder} with texture, shape, and text drawing
 * operations.
 *
 * <p>All coordinates are in world units and are transformed by the current transform stack.
 * Draw flags, color tint, and the current sampler affect subsequent draw calls until changed.
 * Texture source regions are specified in texel coordinates.
 *
 * @see VertexBuilder
 * @see Graphics2D
 */
public interface VertexBuilder2D extends VertexBuilder {
  /**
   * Draws a region of a texture at the given position and size.
   *
   * <p>The source region {@code (u, v, uw, vh)} is specified in texel coordinates.
   * The destination rectangle {@code (x, y, w, h)} is in world units and is transformed by
   * the current transform stack. Current draw flags, color, and sampler are applied.
   *
   * @param tex the texture to draw; does nothing if {@code null}
   * @param x   the X position in world units
   * @param y   the Y position in world units
   * @param w   the width in world units
   * @param h   the height in world units
   * @param u   the U coordinate of the source region in texels
   * @param v   the V coordinate of the source region in texels
   * @param uw  the width of the source region in texels
   * @param vh  the height of the source region in texels
   */
  void drawTexture(@Nullable FragileTexture tex, float x, float y, float w, float h, float u, float v
      , float uw, float vh);

  /**
   * Draws a source region of a texture into a destination rectangle.
   *
   * @param tex the texture to draw; does nothing if {@code null}
   * @param dst the destination rectangle in world units
   * @param src the source region in texel coordinates
   */
  default void drawTexture(@Nullable FragileTexture tex, Box2 dst, Box2 src) {
    if (tex == null) {
      return;
    }
    drawTexture(tex, dst.minX(), dst.minY(), dst.width(), dst.height(), src.minX(), src.minY(), src.width(),
        src.height());
  }

  /**
   * Draws the entire texture into a destination rectangle.
   *
   * @param tex the texture to draw; does nothing if {@code null}
   * @param dst the destination rectangle in world units
   */
  default void drawTexture(@Nullable FragileTexture tex, Box2 dst) {
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
   * @param tex the texture to draw; does nothing if {@code null}
   * @param x   the X position in world units
   * @param y   the Y position in world units
   * @param w   the width in world units
   * @param h   the height in world units
   */
  default void drawTexture(@Nullable FragileTexture tex, float x, float y, float w, float h) {
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
  default void drawTexture(@Nullable TexturePart texPart, Box2 dst) {
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
  default void drawTexture(@Nullable TexturePart texPart, Box2 dst, Box2 src) {
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
  default void drawTexture(@Nullable TexturePart texPart, float x, float y, float w, float h) {
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
  default void drawTexture(@Nullable TexturePart texPart, float x, float y, float w, float h, float u, float v,
                           float uw, float vh) {
    if (texPart == null) {
      return;
    }
    drawTexture(texPart.src(), x, y, w, h, u + texPart.u(), v + texPart.v(), uw, vh);
  }

  /**
   * Draws a {@link Drawable} into a destination rectangle.
   *
   * @param t   the drawable to draw; does nothing if {@code null}
   * @param dst the destination rectangle in world units
   */
  default void draw(@Nullable Drawable t, Box2 dst) {
    if (t == null) {
      return;
    }
    t.draw(this, dst.minX(), dst.minY(), dst.width(), dst.height(), 0, 0, 0, 0);
  }

  /**
   * Draws a {@link Drawable} with a source region into a destination rectangle.
   *
   * @param t   the drawable to draw; does nothing if {@code null}
   * @param dst the destination rectangle in world units
   * @param src the source region in texel coordinates
   */
  default void draw(@Nullable Drawable t, Box2 dst, Box2 src) {
    if (t == null) {
      return;
    }
    t.draw(this, dst.minX(), dst.minY(), dst.width(), dst.height(), src.minX(), src.minY(), src.width(), src.height());
  }

  /**
   * Draws a {@link Drawable} at the given position and size.
   *
   * @param t the drawable to draw; does nothing if {@code null}
   * @param x the X position in world units
   * @param y the Y position in world units
   * @param w the width in world units
   * @param h the height in world units
   */
  default void draw(@Nullable Drawable t, float x, float y, float w, float h) {
    if (t == null) {
      return;
    }
    t.draw(this, x, y, w, h, 0, 0, 0, 0);
  }

  /**
   * Draws a {@link Drawable} at the given position, size, and source region.
   *
   * @param t  the drawable to draw; does nothing if {@code null}
   * @param x  the X position in world units
   * @param y  the Y position in world units
   * @param w  the width in world units
   * @param h  the height in world units
   * @param u  the U coordinate offset in texels
   * @param v  the V coordinate offset in texels
   * @param uw the U coordinate range in texels
   * @param vh the V coordinate range in texels
   */
  default void draw(@Nullable Drawable t, float x, float y, float w, float h, float u, float v, float uw, float vh) {
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
  void drawRectangle(float x, float y, float w, float h);

  /**
   * Draws a filled rectangle.
   *
   * @param dst the destination rectangle in world units
   */
  default void drawRectangle(Box2 dst) {
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
  default void drawRectangleFrame(float x, float y, float w, float h) {
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
  default void drawRectangleFrame(Box2 dst) {
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
  void drawLine(float x1, float y1, float x2, float y2);

  /**
   * Draws a line segment between two vectors.
   *
   * @param from the first endpoint
   * @param to   the second endpoint
   */
  default void drawLine(Vector2 from, Vector2 to) {
    drawLine(from.x(), from.y(), to.x(), to.y());
  }

  /**
   * Draws a single point at the given coordinates.
   *
   * @param x the X coordinate in world units
   * @param y the Y coordinate in world units
   */
  void drawPoint(float x, float y);

  /**
   * Draws a single point at the given position.
   *
   * @param at the position in world units
   */
  default void drawPoint(Vector2 at) {
    drawPoint(at.x(), at.y());
  }

  /**
   * Draws a text blob at the given position with the specified alignment.
   *
   * <p>The alignment determines how the text is positioned relative to the anchor point
   * {@code (x, y)}. For example, {@link Alignment#CENTRAL} centers the text both horizontally
   * and vertically.
   *
   * @param text      the text blob to draw; does nothing if {@code null}
   * @param x         the anchor X coordinate in world units
   * @param y         the anchor Y coordinate in world units
   * @param alignment the alignment relative to the anchor point
   */
  default void drawText(@Nullable Text text, float x, float y, Alignment alignment) {
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

  /**
   * Draws a text blob at the given position with {@link Alignment#LEFT_UP} alignment.
   *
   * @param text the text blob to draw; does nothing if {@code null}
   * @param x    the anchor X coordinate in world units
   * @param y    the anchor Y coordinate in world units
   */
  default void drawText(@Nullable Text text, float x, float y) {
    drawText(text, x, y, Alignment.LEFT_UP);
  }

  /**
   * Draws a text blob at the given position with the specified alignment.
   *
   * @param text      the text blob to draw; does nothing if {@code null}
   * @param pos       the anchor position in world units
   * @param alignment the alignment relative to the anchor point
   */
  default void drawText(@Nullable Text text, Vector2 pos, Alignment alignment) {
    drawText(text, pos.x(), pos.y(), alignment);
  }

  /**
   * Draws a text blob at the given position with {@link Alignment#LEFT_UP} alignment.
   *
   * @param text the text blob to draw; does nothing if {@code null}
   * @param pos  the anchor position in world units
   */
  default void drawText(@Nullable Text text, Vector2 pos) {
    drawText(text, pos.x(), pos.y());
  }
}
