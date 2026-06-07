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

package net.nanitu.gfx.text.spi;

import net.nanitu.gfx.text.Font;
import net.nanitu.gfx.text.sketch.ShapedText;
import net.nanitu.util.Service;

/**
 * Service provider interface for text shaping backends.
 *
 * <p>Implementations are discovered via {@link java.util.ServiceLoader} and convert
 * a text string into a {@link ShapedText} using a shaping engine (e.g., HarfBuzz).
 */
public interface ShaperProvider extends Service {
  /**
   * Shapes the given text string into glyph identifiers, advances, and offsets.
   *
   * @param font      the font to shape with
   * @param text      the text string to shape
   * @param fontSize  the font size in pixels
   * @param flipY     whether the Y axis is flipped
   * @param fontStyle the style bitmask, combining flags from {@link net.nanitu.gfx.text.FontStyle}
   * @return the shaped text result
   */
  ShapedText shape(Font font, String text, float fontSize, boolean flipY, int fontStyle);
}
