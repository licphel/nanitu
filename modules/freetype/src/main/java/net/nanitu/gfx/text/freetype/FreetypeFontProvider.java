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

package net.nanitu.gfx.text.freetype;

import net.nanitu.gfx.Device;
import net.nanitu.gfx.text.Font;
import net.nanitu.gfx.text.spi.FontProvider;
import net.nanitu.util.InternalApi;

/**
 * {@link net.nanitu.gfx.text.spi.FontProvider} implementation backed by the FreeType library.
 *
 * <p>Creates {@link FreetypeFont} instances from font file paths.
 */
@InternalApi
public final class FreetypeFontProvider implements FontProvider {
  @Override
  public boolean isAvailable() {
    return true;
  }

  @Override
  public String name() {
    return "Freetype-Font-Provider";
  }

  @Override
  public Font create(Device device, String path) {
    return new FreetypeFont(device, path, 0);
  }
}