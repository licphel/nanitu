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
 * Constants and utility methods for font style flags.
 *
 * <p>Styles are represented as bitmask integers, allowing combination
 * via bitwise OR. This class cannot be instantiated.
 */
public final class FontStyle {
  /** Normal weight, no italic. */
  public static final int REGULAR = 0;
  /** Bold weight. */
  public static final int BOLD = 1;
  /** Italic (slanted) glyphs. */
  public static final int ITALIC = 2;
  /** Bold and italic combined. */
  public static final int BOLD_ITALIC = BOLD | ITALIC;

  private FontStyle() {
  }

  /**
   * Returns whether the given style includes bold weight.
   *
   * @param style the style bitmask
   * @return {@code true} if the bold bit is set
   */
  public static boolean isBold(int style) {
    return (style & BOLD) != 0;
  }

  /**
   * Returns whether the given style includes italic.
   *
   * @param style the style bitmask
   * @return {@code true} if the italic bit is set
   */
  public static boolean isItalic(int style) {
    return (style & ITALIC) != 0;
  }
}
