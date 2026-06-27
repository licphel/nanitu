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
import java.util.HashMap;
import java.util.Map;

/**
 * Per-frame pollable input state for game-style queries.
 *
 * <p>{@code Snapshot} accumulates keyboard, mouse, and scroll state as events
 * are dispatched through the {@link net.nanitu.gfx.back.View}. Key press state persists across frames — a held key
 * remains {@link KeyAction#PRESS} until the platform reports a release.
 *
 * <p>For higher-level key binding support, use {@link #key(KeyCode)} to obtain
 * a {@link Key} instance with persistent press tracking, transition detection, and modifier-aware queries.
 *
 * <p>This class is not thread-safe. All methods must be called from the rendering
 * thread.
 */
public final class Snapshot {
  private final EnumMap<KeyCode, KeyAction> keyStates = new EnumMap<>(KeyCode.class);
  private final EnumMap<KeyCode, Integer> keyMods = new EnumMap<>(KeyCode.class);
  private final Map<KeyCode, Key> keyCache = new HashMap<>();
  private double cursorX;
  private double cursorY;
  private double scrollX;
  private double scrollY;

  /**
   * Returns the current action state of the given key.
   *
   * <p>State persists across frames: a held key returns {@link KeyAction#PRESS}
   * or {@link KeyAction#REPEAT} until the platform reports a release.
   *
   * @param code the key to query
   * @return the action state; defaults to {@link KeyAction#RELEASE} if the key has not been recorded
   */
  public KeyAction get(KeyCode code) {
    return keyStates.getOrDefault(code, KeyAction.RELEASE);
  }

  /**
   * Returns whether the given key is currently held down.
   *
   * <p>This is a convenience for {@code get(code) != KeyAction.RELEASE}.
   *
   * @param code the key to query
   * @return {@code true} if the key is pressed or repeating
   */
  public boolean isDown(KeyCode code) {
    return keyStates.getOrDefault(code, KeyAction.RELEASE) != KeyAction.RELEASE;
  }

  /**
   * Returns the modifier bitmask last recorded for the given key.
   *
   * @param code the key to query
   * @return the modifier bitmask, or {@code 0} if not recorded
   */
  public int getMods(KeyCode code) {
    return keyMods.getOrDefault(code, 0);
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
   * Returns a {@link Key} instance for the given physical key code, creating it if necessary.
   *
   * <p>Subsequent calls with the same code return the same instance. The
   * returned key tracks persistent press state, transition detection, and supports rebinding via
   * {@link Key#rebind(KeyCode)}.
   *
   * @param code the physical key code
   * @return the cached or newly created {@code Key}
   */
  public Key key(KeyCode code) {
    return keyCache.computeIfAbsent(code, k -> new Key(k, this));
  }

  /**
   * Updates the keyboard state from a key event.
   *
   * <p>Called during event dispatch to record the key action and modifier
   * bitmask for subsequent polling. Also updates any cached {@link Key} instance bound to the event's key code.
   *
   * @param event the key event to apply
   */
  public void applyKeyEvent(KeyEvent event) {
    keyStates.put(event.code(), event.action());
    keyMods.put(event.code(), event.modifiers());

    Key key = keyCache.get(event.code());
    if (key != null) {
      key.apply(event.action(), event.modifiers());
    }
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
   * position. Also updates any cached {@link Key} instance bound to the mouse button's key code.
   *
   * @param event the mouse button event to apply
   */
  public void applyMouseButton(MouseButtonEvent event) {
    keyStates.put(event.button(), event.action());
    keyMods.put(event.button(), event.modifiers());
    cursorX = event.x();
    cursorY = event.y();

    Key key = keyCache.get(event.button());
    if (key != null) {
      key.apply(event.action(), event.modifiers());
    }
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
   * clears per-frame transition flags on all cached {@link Key} instances. Key press state is <em>not</em> modified —
   * held keys remain in their current state until the platform reports a release.
   */
  public void clearFrameState() {
    scrollX = 0;
    scrollY = 0;

    for (Key key : keyCache.values()) {
      key.endFrame();
    }
  }

  /**
   * Moves a key's cache entry from its current code to a new code.
   *
   * <p>Called by {@link Key#rebind(KeyCode)}.
   */
  void rebindKey(Key key, KeyCode newCode) {
    keyCache.remove(key.code);
    key.code = newCode;
    keyCache.put(newCode, key);
  }
}
