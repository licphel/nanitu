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

package net.fmhi.gfx.text;

import net.fmhi.gfx.text.raster.Raster;
import org.jspecify.annotations.Nullable;

/**
 * A node in a text component tree that can be rasterized for rendering.
 *
 * <p>Implementations represent either a single styled span ({@link TextLiteral}) or
 * a composite sequence of text components ({@link TextSequence}). Each node provides the concatenated plain text and an
 * on-demand {@link Raster} for drawing.
 */
public interface Text {
  /**
   * Returns the concatenated plain text of this component and all its descendants.
   *
   * @return the full text content
   */
  String text();

  /**
   * Appends a text component, returning a new {@link TextSequence} that starts with this component followed by the
   * given one.
   *
   * @param component the text component to append
   * @return a new sequence containing this component and {@code component}
   */
  TextSequence append(Text component);

  /**
   * Rasterizes this text component into a {@link Raster} suitable for rendering.
   *
   * @return the rasterized text output
   */
  Raster raster();

  /**
   * Returns the last style of the text component, for chaining text style or appending new lines.
   *
   * @return the last style
   */
  @Nullable Style forwadingStyle();
}
