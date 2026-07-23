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

package net.fmhi.gfx.glfw;

import net.fmhi.gfx.View;
import net.fmhi.gfx.spi.ViewProvider;
import net.fmhi.util.InternalApi;

/**
 * {@link ViewProvider} backed by GLFW (via LWJGL).
 *
 * <p>Loaded automatically by {@link java.util.ServiceLoader} from
 * {@code META-INF/services/net.fmhi.graphics.spi.ViewProvider}.
 *
 * <p>{@link #isAvailable()} initializes and immediately terminates GLFW to
 * verify that LWJGL natives are present.
 */
@InternalApi
public final class GlfwViewProvider implements ViewProvider {
  @Override
  public boolean isAvailable() {
    return true;
  }

  @Override
  public String name() {
    return "GLFW-View-Provider";
  }

  @Override
  public View create() {
    return new GlfwView();
  }
}
