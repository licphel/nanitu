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
 * Platform-agnostic mouse buttons.
 *
 * <p>The button IDs follow the de facto standard: 0 = left, 1 = right, 2 = middle,
 * 3 = back, 4 = forward. Additional buttons (5–7) are supported via
 * {@link #fromId(int)}.
 */
public enum MouseButton {
  LEFT(0),
  RIGHT(1),
  MIDDLE(2),
  BACK(3),
  FORWARD(4),
  BUTTON_5(5),
  BUTTON_6(6),
  BUTTON_7(7);

  private final int id;
  private static final MouseButton[] LOOKUP = new MouseButton[8];

  static {
    for (MouseButton b : values()) {
      LOOKUP[b.id] = b;
    }
  }

  MouseButton(int id) {
    this.id = id;
  }

  /**
   * Returns the standard button ID (0 = left, etc.).
   *
   * @return the standard button ID
   */
  public int id() {
    return id;
  }

  /**
   * Returns the {@code MouseButton} for the given button ID, or {@code null}
   * if the ID is out of range.
   *
   * @param id the integer ID
   * @return the standard button ID
   */
  public static MouseButton fromId(int id) {
    return (id >= 0 && id < LOOKUP.length) ? LOOKUP[id] : null;
  }
}
