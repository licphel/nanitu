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

/**
 * A logical key binding that wraps a physical {@link KeyCode} with persistent press tracking and modifier-aware
 * transition detection.
 *
 * <p>Acquire instances via {@link Snapshot#key(KeyCode)} — each
 * {@code KeyCode} maps to at most one {@code Key}. The bound key code can be changed at any time with
 * {@link #rebind(KeyCode)}.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * Snapshot input = view.inputState();
 * Key jump = input.key(KeyCode.SPACE);
 * Key crouch = input.key(KeyCode.C);
 *
 * if (jump.transitioned()) {
 *     // SPACE was just pressed this frame
 * }
 * if (jump.isDown()) {
 *     // SPACE is currently held
 * }
 * if (crouch.transitioned(Modifiers.CONTROL)) {
 *     // Ctrl+C was just pressed this frame
 * }
 * }</pre>
 *
 * <p>This class is not thread-safe. All methods must be called from the
 * rendering thread.
 */
public final class Key {
  final Snapshot owner;
  KeyCode code;
  boolean down;
  boolean pressTransitioning;
  boolean repeating;
  int pressMods;
  long pressTime = -1;

  Key(KeyCode code, Snapshot owner) {
    this.code = code;
    this.owner = owner;
  }

  /**
   * Returns the currently bound physical key code.
   *
   * @return the bound key code
   */
  public KeyCode code() {
    return code;
  }

  /**
   * Rebinds this logical key to a different physical key.
   *
   * <p>The old mapping is removed from the cache; future calls to
   * {@code Snapshot.key(oldCode)} will create a new {@code Key}.
   *
   * @param code the new physical key code to bind
   */
  public void rebind(KeyCode code) {
    owner.rebindKey(this, code);
  }

  /**
   * Returns whether this key is currently held down.
   *
   * @return {@code true} if the key is pressed
   */
  public boolean isDown() {
    return down;
  }

  /**
   * Returns whether this key was just pressed this frame, regardless of active modifier keys.
   *
   * <p>Equivalent to {@code transitioned(Modifiers.ANY)}.
   *
   * @return {@code true} if the key transitioned to pressed this frame
   */
  public boolean transitioned() {
    return transitioned(Modifiers.ANY);
  }

  /**
   * Returns whether this key was just pressed this frame with matching modifiers.
   *
   * <p>Pass {@link Modifiers#ANY} to match any modifier combination.
   * Pass {@link Modifiers#NONE} to require no modifiers.
   *
   * @param mods the required modifier bitmask, or {@link Modifiers#ANY}
   * @return {@code true} if the key transitioned to pressed this frame with matching modifiers
   */
  public boolean transitioned(int mods) {
    return pressTransitioning && (mods == Modifiers.ANY || mods == pressMods);
  }

  /**
   * Returns whether this key was just pressed or repeated this frame, regardless of active modifier keys.
   *
   * <p>Equivalent to {@code transitionedOrRepeated(Modifiers.ANY)}.
   *
   * @return {@code true} if the key transitioned or repeated this frame
   */
  public boolean transitionedOrRepeated() {
    return transitionedOrRepeated(Modifiers.ANY);
  }

  /**
   * Returns whether this key was just pressed or repeated this frame with matching modifiers.
   *
   * <p>Pass {@link Modifiers#ANY} to match any modifier combination.
   *
   * @param mods the required modifier bitmask, or {@link Modifiers#ANY}
   * @return {@code true} if the key transitioned or repeated this frame with matching modifiers
   */
  public boolean transitionedOrRepeated(int mods) {
    return (pressTransitioning || repeating) && (mods == Modifiers.ANY || mods == pressMods);
  }

  /**
   * Returns the timestamp of the most recent press, in nanoseconds as reported by {@link System#nanoTime()}.
   *
   * <p>Returns {@code -1} if the key has never been pressed.
   *
   * @return the press timestamp in nanoseconds, or {@code -1}
   */
  public long pressTime() {
    return pressTime;
  }

  void apply(KeyAction action, int mods) {
    switch (action) {
      case PRESS:
        if (!down) {
          pressTransitioning = true;
          pressTime = System.nanoTime();
        }
        down = true;
        pressMods = mods;
        break;
      case REPEAT:
        down = true;
        repeating = true;
        pressMods = mods;
        break;
      case RELEASE:
        down = false;
        pressTransitioning = false;
        repeating = false;
        break;
    }
  }

  void endFrame() {
    pressTransitioning = false;
  }
}
