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

package net.nanitu.natives.glfw;

import net.nanitu.gfx.View;
import net.nanitu.gfx.spi.ViewProvider;
import net.nanitu.util.InternalApi;

import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwTerminate;

/**
 * {@link ViewProvider} backed by GLFW (via LWJGL).
 *
 * <p>Loaded automatically by {@link java.util.ServiceLoader} from
 * {@code META-INF/services/net.nanitu.graphics.spi.ViewProvider}.
 *
 * <p>{@link #isAvailable()} initializes and immediately terminates GLFW to
 * verify that LWJGL natives are present.
 */
@InternalApi
public final class GlfwViewProvider implements ViewProvider {
  /**
   * Returns whether GLFW can be initialized on this system.
   *
   * <p>Initializes and immediately terminates GLFW to probe availability.
   *
   * @return {@code true} if GLFW initialized successfully
   */
  @Override
  public boolean isAvailable() {
    try {
      if (!glfwInit()) {
        return false;
      }
      glfwTerminate();
      return true;
    } catch (Throwable e) {
      return false;
    }
  }

  /**
   * Creates a new {@link GlfwView}.
   *
   * <p>The {@code args} parameter is interpreted as
   * {@code "title,width,height"}.
   *
   * @param args creation arguments
   * @return a new GLFW window view
   */
  @Override
  public View create(String args) {
    return new GlfwView();
  }

  @Override
  public String name() {
    return "GLFW-View-Provider";
  }
}
