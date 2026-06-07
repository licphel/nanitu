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

import net.nanitu.math.Vector2;
import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Input event callbacks and polling for a {@link View}.
 *
 * <p>Each callback setter registers a handler that the view invokes when the
 * corresponding input event occurs. Passing {@code null} removes the callback. Polling methods return the instantaneous
 * state of an input at the time of the call.
 *
 * <p>Obtain an instance via {@link View#hook()}.
 *
 * @see View
 * @see ViewController
 */
public interface ViewHook {
  /**
   * Returns the current state of a key.
   *
   * @param keycode the GLFW key code
   * @return the key state
   */
  KeyStatus keyStatus(int keycode);

  /**
   * Returns the modifier flags active for the given key.
   *
   * @param keycode the GLFW key code
   * @return a bitmask of modifier flags
   */
  int keyModifiers(int keycode);

  /**
   * Returns the accumulated scroll delta since the last call, then resets to zero.
   *
   * @return the scroll delta as {@code (x, y)}
   */
  Vector2 scrollDelta();

  /**
   * Sets a callback invoked when a key is pressed.
   *
   * @param cb a callback receiving the key code, or {@code null} to remove
   */
  void onKeyPress(@Nullable Consumer<Integer> cb);

  /**
   * Sets a callback invoked when a key is released.
   *
   * @param cb a callback receiving the key code, or {@code null} to remove
   */
  void onKeyRelease(@Nullable Consumer<Integer> cb);

  /**
   * Sets a callback invoked when a Unicode character is input.
   *
   * <p>This is distinct from raw key events — it handles text input with
   * keyboard layout and IME composition applied.
   *
   * @param cb a callback receiving the character, or {@code null} to remove
   */
  void onCharInput(@Nullable Consumer<Character> cb);

  /**
   * Sets a callback invoked when the mouse moves.
   *
   * @param cb a callback receiving {@code (x, y)} in screen coordinates, or {@code null} to remove
   */
  void onMouseMove(@Nullable BiConsumer<Double, Double> cb);

  /**
   * Sets a callback invoked on mouse button events.
   *
   * @param cb a callback receiving {@code (button, pressed)}, or {@code null} to remove
   */
  void onMouseButton(@Nullable BiConsumer<Integer, Boolean> cb);

  /**
   * Sets a callback invoked on scroll events.
   *
   * @param cb a callback receiving {@code (x, y)} scroll offsets, or {@code null} to remove
   */
  void onScroll(@Nullable BiConsumer<Double, Double> cb);

  /**
   * Sets a callback invoked when the cursor enters or leaves the view.
   *
   * @param cb a callback receiving {@code true} if entered, {@code false} if left, or {@code null} to remove
   */
  void onCursorEnter(@Nullable Consumer<Boolean> cb);

  /**
   * Sets a callback invoked when the view is resized.
   *
   * @param cb a callback receiving {@code (width, height)} in pixels, or {@code null} to remove
   */
  void onResize(@Nullable BiConsumer<Integer, Integer> cb);

  /**
   * Sets a callback invoked when the view gains or loses focus.
   *
   * @param cb a callback receiving {@code true} if focused, {@code false} if unfocused, or {@code null} to remove
   */
  void onWindowFocus(@Nullable Consumer<Boolean> cb);

  /**
   * Sets a callback invoked when the view is iconified or restored.
   *
   * @param cb a callback receiving {@code true} if iconified, {@code false} if restored, or {@code null} to remove
   */
  void onWindowIconify(@Nullable Consumer<Boolean> cb);

  /**
   * Sets a callback invoked when the view is maximized or restored.
   *
   * @param cb a callback receiving {@code true} if maximized, {@code false} if restored, or {@code null} to remove
   */
  void onWindowMaximize(@Nullable Consumer<Boolean> cb);

  /**
   * Sets a callback invoked when the view position changes.
   *
   * @param cb a callback receiving {@code (x, y)} in screen coordinates, or {@code null} to remove
   */
  void onWindowMove(@Nullable BiConsumer<Integer, Integer> cb);

  /**
   * Sets a callback invoked when files are dropped onto the view.
   *
   * @param cb a callback receiving the file paths, or {@code null} to remove
   */
  void onFileDrop(@Nullable Consumer<String[]> cb);
}
