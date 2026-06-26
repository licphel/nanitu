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

package net.nanitu.ui.look;

import net.nanitu.gfx.sprite.Alignment;
import net.nanitu.gfx.sprite.Graphics;
import net.nanitu.gfx.text.Text;
import net.nanitu.math.Box2;
import net.nanitu.math.Color;
import net.nanitu.ui.ButtonState;
import net.nanitu.ui.Look;
import net.nanitu.ui.WindowState;
import org.jspecify.annotations.Nullable;

/**
 * Default flat dark look implementation requiring no textures.
 *
 * <p>All widgets are drawn with colored rectangles and lines. The palette uses deep navy-purple
 * tones common in modern game tool UIs. All color constants are shared, so draw methods perform
 * no per-frame allocation.
 *
 * <p>Every draw method restores the brush color to {@code Color.WHITE} before returning.
 */
public final class ModernFlat implements Look {
  private static final Color SURFACE = Color.create(0x2a, 0x2a, 0x3e);
  private static final Color SURFACE_MID = Color.create(0x33, 0x33, 0x50);
  private static final Color ACCENT = Color.create(0x4a, 0x4a, 0x80);
  private static final Color ACCENT_HOV = Color.create(0x5a, 0x5a, 0x90);
  private static final Color ACCENT_PRESS = Color.create(0x3a, 0x3a, 0x70);
  private static final Color BTN_IDLE = Color.create(0x3d, 0x3d, 0x5c);
  private static final Color BORDER = Color.create(0x55, 0x55, 0x80);
  private static final Color BORDER_FOCUS = Color.create(0x88, 0x88, 0xc0);
  private static final Color TEXT_COLOR = Color.WHITE;
  private static final Color TEXT_DIM = new Color(Color.WHITE, 0.45F);
  private static final Color CLOSE_HOV = Color.create(0xc0, 0x30, 0x30);
  private static final Color WIN_SHADOW = new Color(0.0F, 0.0F, 0.0F, 0.35F);
  private static final Color WIN_BG = Color.create(0x22, 0x22, 0x38);
  private static final Color TITLE_UNFOCUS = new Color(ACCENT, 0.65F);
  private static final Alignment LEFT_CENTER = new Alignment(-1, 0);

  @Override
  public float titleBarHeight() {
    return 22.0F;
  }

  @Override
  public float resizeHandleSize() {
    return 10.0F;
  }

  @Override
  public float borderWidth() {
    return 1.0F;
  }

  @Override
  public void drawButton(Graphics brush, ButtonState state, Box2 bounds, @Nullable Text label) {
    Color bg = switch (state) {
      case IDLE -> BTN_IDLE;
      case HOVERED -> ACCENT_HOV;
      case PRESSED -> ACCENT_PRESS;
      case DISABLED -> new Color(BTN_IDLE, 0.4F);
    };
    brush.setColor(bg);
    brush.drawRectangle(bounds);
    brush.setColor(BORDER);
    brush.drawRectangleFrame(bounds);
    if (label != null) {
      brush.setColor(state == ButtonState.DISABLED ? TEXT_DIM : TEXT_COLOR);
      brush.drawText(label, bounds.centralX(), bounds.centralY(), Alignment.CENTRAL);
    }
    brush.setColor(Color.WHITE);
  }

  @Override
  public void drawPanel(Graphics brush, Box2 bounds) {
    brush.setColor(SURFACE);
    brush.drawRectangle(bounds);
    brush.setColor(BORDER);
    brush.drawRectangleFrame(bounds);
    brush.setColor(Color.WHITE);
  }

  @Override
  public void drawScrollPane(Graphics brush, Box2 bounds) {
    brush.setColor(WIN_BG);
    brush.drawRectangle(bounds);
    brush.setColor(BORDER);
    brush.drawRectangleFrame(bounds);
    brush.setColor(Color.WHITE);
  }

  @Override
  public void drawWindowFrame(Graphics brush, WindowState state, Box2 bounds) {
    Box2 shadow = bounds.translate(4.0F, 4.0F);
    brush.setColor(WIN_SHADOW);
    brush.drawRectangle(shadow);
    brush.setColor(SURFACE_MID);
    brush.drawRectangle(bounds);
    brush.setColor(state == WindowState.FOCUSED ? BORDER_FOCUS : BORDER);
    brush.drawRectangleFrame(bounds);
    brush.setColor(Color.WHITE);
  }

  @Override
  public void drawWindowTitleBar(Graphics brush, boolean focused, Box2 titleBarBounds, @Nullable Text title) {
    brush.setColor(focused ? ACCENT : TITLE_UNFOCUS);
    brush.drawRectangle(titleBarBounds);
    if (title != null) {
      float tx = titleBarBounds.minX() + 6.0F;
      float ty = titleBarBounds.centralY();
      brush.setColor(TEXT_COLOR);
      brush.drawText(title, tx, ty, LEFT_CENTER);
    }
    brush.setColor(Color.WHITE);
  }

  @Override
  public void drawWindowCloseButton(Graphics brush, boolean hovered, Box2 bounds) {
    if (hovered) {
      brush.setColor(CLOSE_HOV);
      brush.drawRectangle(bounds);
    }
    float pad = 4.0F;
    brush.setColor(TEXT_COLOR);
    brush.drawLine(bounds.minX() + pad, bounds.minY() + pad, bounds.maxX() - pad, bounds.maxY() - pad);
    brush.drawLine(bounds.maxX() - pad, bounds.minY() + pad, bounds.minX() + pad, bounds.maxY() - pad);
    brush.setColor(Color.WHITE);
  }

  @Override
  public void drawWindowMinimizeButton(Graphics brush, boolean hovered, Box2 bounds) {
    if (hovered) {
      brush.setColor(ACCENT_HOV);
      brush.drawRectangle(bounds);
    }
    float pad = 4.0F;
    float midY = bounds.centralY();
    brush.setColor(TEXT_COLOR);
    brush.drawLine(bounds.minX() + pad, midY, bounds.maxX() - pad, midY);
    brush.setColor(Color.WHITE);
  }

  @Override
  public void drawWindowMaximizeButton(Graphics brush, boolean hovered, Box2 bounds) {
    if (hovered) {
      brush.setColor(ACCENT_HOV);
      brush.drawRectangle(bounds);
    }
    float pad = 3.0F;
    brush.setColor(TEXT_COLOR);
    brush.drawRectangleFrame(bounds.inflate(-pad, -pad));
    brush.setColor(Color.WHITE);
  }
}
