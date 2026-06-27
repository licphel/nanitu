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

import net.nanitu.gfx.Device;
import net.nanitu.gfx.text.Font;
import net.nanitu.util.Service;

import java.io.IOException;
import java.io.InputStream;

/**
 * Service provider interface for creating {@link Font} instances from font data.
 *
 * <p>Implementations provide backend-specific font loading and are discovered
 * through the {@link Service} mechanism.
 *
 * <p>The provider reads the entire stream into a buffer, then creates the font
 * from memory — no file path is required.
 */
public interface FontProvider extends Service {
  /**
   * Creates a font from the given input stream.
   *
   * <p>The stream is fully consumed and buffered before font creation.
   *
   * @param device the graphics device
   * @param stream the font data stream (closed by the caller)
   * @return the loaded font
   * @throws IOException if reading the stream fails
   */
  Font create(Device device, InputStream stream) throws IOException;
}
