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

package net.nanitu.gfx;

import net.nanitu.gfx.io.ImageInputStream;
import net.nanitu.math.Vector2;
import org.jspecify.annotations.Nullable;

/**
 * Controls window state for a {@link View}.
 *
 * <p>This interface provides typed getters and setters for all mutable view properties
 * such as size, position, title, visibility, and window decorations.
 *
 * <p>Obtain an instance via {@link View#controller()}.
 *
 * @see View
 * @see ViewHook
 */
public interface ViewController {
  /**
   * Returns the current view size in pixels.
   *
   * @return the current size
   */
  Vector2 size();

  /**
   * Sets the view size in pixels.
   *
   * @param size the new size
   */
  void setSize(Vector2 size);

  /**
   * Returns the current view position in screen coordinates.
   *
   * @return the current position
   */
  Vector2 position();

  /**
   * Sets the view position in screen coordinates.
   *
   * @param position the new position
   */
  void setPosition(Vector2 position);

  /**
   * Returns the current window title.
   *
   * @return the current title
   */
  String title();

  /**
   * Sets the window title.
   *
   * @param title the new title
   */
  void setTitle(String title);

  /**
   * Sets the window icon.
   *
   * @param image the icon image data
   */
  void setIcon(ImageInputStream image);

  /**
   * Returns the current cursor position in screen coordinates relative to the window.
   *
   * @return the cursor position
   */
  Vector2 cursorPosition();

  /**
   * Sets the cursor position in screen coordinates.
   *
   * @param position the new cursor position
   */
  void setCursorPosition(Vector2 position);

  /**
   * Sets a custom cursor image with an explicit hotspot.
   *
   * <p>The hotspot defines which pixel of the cursor image is the click point,
   * relative to the image origin (top-left).
   *
   * @param image   the cursor image data
   * @param hotspot the hotspot offset in pixels
   */
  void setCursor(ImageInputStream image, Vector2 hotspot);

  /**
   * Returns the cursor image last set via {@link #setCursor(ImageInputStream, Vector2)}, or {@code null} if no custom
   * cursor has been set.
   *
   * @return the cursor image, or {@code null}
   */
  @Nullable ImageInputStream cursorImage();

  /**
   * Returns the hotspot last set via {@link #setCursor(ImageInputStream, Vector2)}.
   *
   * @return the cursor hotspot
   */
  Vector2 cursorHotspot();

  /**
   * Returns whether the cursor is in relative (raw) mode.
   *
   * <p>In relative mode, the cursor is hidden and its position is reported as deltas
   * rather than absolute screen coordinates.
   *
   * @return {@code true} if relative cursor mode is enabled
   */
  boolean isCursorRelativeMode();

  /**
   * Sets whether the cursor is in relative (raw) mode.
   *
   * @param enabled {@code true} to enable relative mode
   */
  void setCursorRelativeMode(boolean enabled);

  /**
   * Returns the current system clipboard text.
   *
   * @return the clipboard text, or empty string if empty
   */
  String clipboardText();

  /**
   * Sets the system clipboard text.
   *
   * @param text the text to copy to the clipboard
   */
  void setClipboardText(String text);

  /**
   * Returns whether the window has title bar and borders.
   *
   * @return {@code true} if decorated
   */
  boolean isDecorated();

  /**
   * Sets whether the window has title bar and borders.
   *
   * @param decorated {@code true} to show decorations
   */
  void setDecorated(boolean decorated);

  /**
   * Returns whether the window is maximized.
   *
   * @return {@code true} if maximized
   */
  boolean isMaximized();

  /**
   * Sets whether the window is maximized.
   *
   * @param maximized {@code true} to maximize, {@code false} to restore
   */
  void setMaximized(boolean maximized);

  /**
   * Returns whether the window iconifies when it loses focus.
   *
   * @return {@code true} if auto-iconify is enabled
   */
  boolean isAutoIconify();

  /**
   * Sets whether the window iconifies when it loses focus.
   *
   * @param autoIconify {@code true} to auto-iconify on focus loss
   */
  void setAutoIconify(boolean autoIconify);

  /**
   * Returns whether the window floats above other windows.
   *
   * @return {@code true} if floating
   */
  boolean isFloating();

  /**
   * Sets whether the window floats above other windows.
   *
   * @param floating {@code true} to keep on top
   */
  void setFloating(boolean floating);

  /**
   * Returns whether the window currently has input focus.
   *
   * @return {@code true} if focused
   */
  boolean isFocused();

  /**
   * Sets whether the window gains input focus.
   *
   * @param focused {@code true} to give focus to this window
   */
  void setFocused(boolean focused);

  /**
   * Returns whether the window is visible.
   *
   * @return {@code true} if visible
   */
  boolean isVisible();

  /**
   * Sets whether the window is visible.
   *
   * @param visible {@code true} to show the window, {@code false} to hide
   */
  void setVisible(boolean visible);

  /**
   * Returns whether the window is resizable by the user.
   *
   * @return {@code true} if resizable
   */
  boolean isResizable();

  /**
   * Sets whether the window is resizable by the user.
   *
   * @param resizable {@code true} to allow resizing
   */
  void setResizable(boolean resizable);

  /**
   * Returns whether vertical synchronization is enabled.
   *
   * @return {@code true} if v-sync is enabled
   */
  boolean isVsync();

  /**
   * Sets whether vertical synchronization is enabled.
   *
   * @param vsync {@code true} to enable v-sync, {@code false} to disable
   */
  void setVsync(boolean vsync);

  /**
   * Returns whether graphics debug output is enabled.
   *
   * @return {@code true} if debug is enabled
   */
  boolean isDebug();

  /**
   * Sets whether to enable graphics debug output.
   *
   * <p>Must be set before the view is initialized. Setting after initialization
   * throws an exception.
   *
   * @param debug {@code true} to enable debug output
   * @throws GraphicsException if called after view initialization
   */
  void setDebug(boolean debug);
}
