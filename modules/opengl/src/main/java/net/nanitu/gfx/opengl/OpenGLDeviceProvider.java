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

package net.nanitu.gfx.opengl;

import net.nanitu.gfx.Device;
import net.nanitu.gfx.View;
import net.nanitu.gfx.spi.DeviceProvider;
import net.nanitu.util.InternalApi;
import net.nanitu.util.Service;
import org.lwjgl.system.Platform;

import java.util.ServiceLoader;

/**
 * {@link DeviceProvider} backed by OpenGL (via LWJGL).
 *
 * <p>Loaded automatically by {@link ServiceLoader} from
 * {@code META-INF/services/net.nanitu.graphics.spi.DeviceProvider}. Callers should prefer using
 * {@link Service#get Service.get} or a DI container rather than constructing this class directly.
 *
 * <p>{@link #isAvailable()} verifies that the LWJGL OpenGL bindings are on
 * the classpath — it does not touch GLFW, keeping the graphics backend decoupled from the windowing system.
 */
@InternalApi
public final class OpenGLDeviceProvider implements DeviceProvider {
  /**
   * Returns whether the LWJGL OpenGL bindings are loadable.
   *
   * @return {@code true} if LWJGL OpenGL classes can be loaded
   */
  @Override
  public boolean isAvailable() {
    return Platform.get() == Platform.WINDOWS || Platform.get() == Platform.LINUX;
  }

  /**
   * Creates a new {@link OpenGLDevice}.
   *
   * <p>The device must be loaded onto a surface via
   * {@link Device#load(View)} before use.
   *
   * @return a new OpenGL graphics device
   */
  @Override
  public Device create() {
    return new OpenGLDevice();
  }

  @Override
  public String name() {
    return "OpenGL-Device-Provider";
  }
}
