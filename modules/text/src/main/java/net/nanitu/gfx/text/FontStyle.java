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

package net.nanitu.gfx.text;

/**
 * Constants and utility functions for font style bitmask flags.
 *
 * <p>Each style occupies a distinct bit, so multiple styles can be combined
 * with bitwise OR. This is a utility class and cannot be instantiated.
 */
public final class FontStyle {
  /** Plain weight, no italic — the default style. */
  public static final int REGULAR = 0;
  /** Bold weight bit. */
  public static final int BOLD = 1;
  /** Italic slant bit. */
  public static final int ITALIC = 2;
  /** Bold and italic bits both set. */
  public static final int BOLD_ITALIC = BOLD | ITALIC;

  private FontStyle() {
  }

  /**
   * Tests whether the bold bit is set in the given style bitmask.
   *
   * @param style the style bitmask
   * @return {@code true} if the style includes bold weight
   */
  public static boolean isBold(int style) {
    return (style & BOLD) != 0;
  }

  /**
   * Tests whether the italic bit is set in the given style bitmask.
   *
   * @param style the style bitmask
   * @return {@code true} if the style includes italic slant
   */
  public static boolean isItalic(int style) {
    return (style & ITALIC) != 0;
  }
}
