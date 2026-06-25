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

package net.nanitu.gfx.spi;

import net.nanitu.gfx.back.View;
import net.nanitu.util.Service;

/**
 * Service interface for discovering and instantiating windowing backends.
 *
 * <p>Implementations are loaded via {@link java.util.ServiceLoader} and represent
 * concrete windowing systems such as GLFW or SDL. Each implementation should register itself in
 * {@code META-INF/services/net.nanitu.graphics.spi.ViewProvider}.
 *
 * <p>Before calling {@link #create()}, check {@link #isAvailable()} to verify
 * that the windowing system's native libraries are present.
 *
 * @see View
 * @see Service
 */
public interface ViewProvider extends Service {
  /**
   * Creates a graphics backend (i.e. a view) of current provider.
   *
   * @return a new graphics backend
   */
  View create();
}
