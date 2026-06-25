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

package net.nanitu.gfx.input;

import net.nanitu.gfx.input.event.KeyEvent;
import net.nanitu.gfx.input.event.MouseButtonEvent;

import java.util.EnumMap;
import java.util.Map;

/**
 * Per-frame pollable input state for game-style queries.
 *
 * <p>{@code InputState} accumulates keyboard, mouse, and scroll state as events
 * are dispatched through the {@link net.nanitu.gfx.back.View}. At the end of each
 * frame, {@link #clearFrameState()} resets transient accumulators and transitions
 * single-frame press states to release, so polled queries reflect the latest frame
 * only.
 *
 * <p>This class is not thread-safe. All methods must be called from the rendering
 * thread.
 */
public final class InputState {
  private final EnumMap<KeyCode, Byte> keyStates = new EnumMap<>(KeyCode.class);
  private final EnumMap<KeyCode, Integer> keyMods = new EnumMap<>(KeyCode.class);
  private final EnumMap<MouseButton, KeyAction> mouseStates = new EnumMap<>(MouseButton.class);
  private double cursorX;
  private double cursorY;
  private double scrollX;
  private double scrollY;

  /**
   * Returns the current action state of the given key.
   *
   * @param code the key to query
   * @return the action state; defaults to {@link KeyAction#RELEASE} if the key
   *         has not been recorded
   */
  public KeyAction keyAction(KeyCode code) {
    byte s = keyStates.getOrDefault(code, (byte) 0);
    return switch (s) {
      case 1 -> KeyAction.PRESS;
      case 2 -> KeyAction.REPEAT;
      default -> KeyAction.RELEASE;
    };
  }

  /**
   * Returns whether the given key is currently down.
   *
   * <p>A key is considered down while in {@link KeyAction#PRESS} or
   * {@link KeyAction#REPEAT} state.
   *
   * @param code the key to query
   * @return {@code true} if the key is pressed or repeating
   */
  public boolean isKeyDown(KeyCode code) {
    byte s = keyStates.getOrDefault(code, (byte) 0);
    return s == 1 || s == 2;
  }

  /**
   * Returns the modifier bitmask last recorded for the given key.
   *
   * @param code the key to query
   * @return the modifier bitmask, or {@code 0} if not recorded
   */
  public int keyModifiers(KeyCode code) {
    return keyMods.getOrDefault(code, 0);
  }

  /**
   * Returns the current action state of the given mouse button.
   *
   * @param button the mouse button to query
   * @return the action state; defaults to {@link KeyAction#RELEASE} if the
   *         button has not been recorded
   */
  public KeyAction mouseButtonAction(MouseButton button) {
    return mouseStates.getOrDefault(button, KeyAction.RELEASE);
  }

  /**
   * Returns whether the given mouse button is currently down.
   *
   * @param button the mouse button to query
   * @return {@code true} if the button is pressed
   */
  public boolean isMouseButtonDown(MouseButton button) {
    return mouseStates.getOrDefault(button, KeyAction.RELEASE) == KeyAction.PRESS;
  }

  /**
   * Returns the current cursor X position in screen coordinates.
   *
   * @return the cursor X position
   */
  public double cursorX() {
    return cursorX;
  }

  /**
   * Returns the current cursor Y position in screen coordinates.
   *
   * @return the cursor Y position
   */
  public double cursorY() {
    return cursorY;
  }

  /**
   * Returns the accumulated horizontal scroll delta since the last frame.
   *
   * @return the horizontal scroll delta
   */
  public double scrollDeltaX() {
    return scrollX;
  }

  /**
   * Returns the accumulated vertical scroll delta since the last frame.
   *
   * @return the vertical scroll delta
   */
  public double scrollDeltaY() {
    return scrollY;
  }

  /**
   * Updates the keyboard state from a key event.
   *
   * <p>Called during event dispatch to record the key action and modifier
   * bitmask for subsequent polling via {@link #keyAction(KeyCode)} and
   * {@link #keyModifiers(KeyCode)}.
   *
   * @param event the key event to apply
   */
  public void applyKeyEvent(KeyEvent event) {
    keyStates.put(event.code(), (byte) event.action().ordinal());
    keyMods.put(event.code(), event.modifiers());
  }

  /**
   * Updates the cursor position from a mouse move.
   *
   * <p>Called during event dispatch to record the position for subsequent
   * polling via {@link #cursorX()} and {@link #cursorY()}.
   *
   * @param x the cursor X position in screen coordinates
   * @param y the cursor Y position in screen coordinates
   */
  public void applyMouseMove(double x, double y) {
    cursorX = x;
    cursorY = y;
  }

  /**
   * Updates the mouse button state from a mouse button event.
   *
   * <p>Called during event dispatch to record the button action and cursor
   * position for subsequent polling via {@link #mouseButtonAction(MouseButton)}
   * and {@link #isMouseButtonDown(MouseButton)}.
   *
   * @param event the mouse button event to apply
   */
  public void applyMouseButton(MouseButtonEvent event) {
    mouseStates.put(event.button(), event.action());
    cursorX = event.x();
    cursorY = event.y();
  }

  /**
   * Accumulates scroll deltas from a scroll event.
   *
   * <p>Called during event dispatch to accumulate deltas for subsequent
   * polling via {@link #scrollDeltaX()} and {@link #scrollDeltaY()}.
   *
   * @param dx the horizontal scroll delta
   * @param dy the vertical scroll delta
   */
  public void applyScroll(double dx, double dy) {
    scrollX += dx;
    scrollY += dy;
  }

  /**
   * Resets transient per-frame state.
   *
   * <p>Call at the end of each frame. Resets scroll accumulators to zero and
   * transitions any {@link KeyAction#PRESS} states to
   * {@link KeyAction#RELEASE}, so pressed states are only visible for a single
   * frame when polling.
   */
  public void clearFrameState() {
    scrollX = 0;
    scrollY = 0;
    for (Map.Entry<KeyCode, Byte> entry : keyStates.entrySet()) {
      if (entry.getValue() == 1) {
        entry.setValue((byte) 0);
      }
    }
    for (Map.Entry<MouseButton, KeyAction> entry : mouseStates.entrySet()) {
      if (entry.getValue() == KeyAction.PRESS) {
        entry.setValue(KeyAction.RELEASE);
      }
    }
  }
}
