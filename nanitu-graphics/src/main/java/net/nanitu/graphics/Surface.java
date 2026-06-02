/*
 * MIT License
 *
 * Copyright (c) 2026 Mellowhue
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

package net.nanitu.graphics;

import net.nanitu.util.InternalApi;
import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A window or drawable surface that a {@link Device} renders into.
 *
 * <p>{@code Surface} is a backend-agnostic abstraction over the native windowing
 * system. All communication with the graphics backend goes through
 * {@link #procAddress()} — an opaque handle whose concrete type is known only
 * to the backend. For OpenGL/GLFW this is the GLFW window handle; for SDL it
 * would be an {@code SDL_Window} pointer.
 *
 * <p><b>Lifecycle:</b>
 * <ol>
 *   <li>Create a concrete surface (e.g. {@code GLFWWindow}).
 *   <li>Attach it to the device via {@code device.attachSurface(surface)}.
 *   <li>In the main loop, call {@link #pollEvents()}.
 *   <li>Call {@link #close()} when the window is destroyed.
 * </ol>
 *
 * <p><b>Typical usage:</b>
 * <pre>{@code
 * Surface surface = new GLFWWindow("My App", 1280, 720);
 * Device device = DeviceProvider.create();
 * device.attachSurface(surface);
 *
 * surface.onResize((w, h) -> device.onResize(w, h));
 *
 * while (!surface.shouldClose()) {
 *     surface.pollEvents();
 *     // ... render ...
 *     surface.swapBuffers();
 * }
 * surface.close();
 * device.close();
 * }</pre>
 *
 * @see Device
 */
public interface Surface extends AutoCloseable {
  /**
   * Returns static information about this surface.
   *
   * @return name for this windowing backend
   */
  SurfaceInfo info();

  /**
   * Returns an opaque native handle for backend communication.
   *
   * <p>The concrete type is backend-specific — a GLFW window {@code long},
   * an SDL {@code SDL_Window} pointer, etc. Callers outside the graphics
   * backend must treat this as completely opaque.
   *
   * @return the native surface handle
   */
  Object procAddress();

  /**
   * Returns the current surface width in pixels.
   *
   * @return width in pixels
   */
  int width();

  /**
   * Returns the current surface height in pixels.
   *
   * @return height in pixels
   */
  int height();

  /**
   * Called by {@link Device#load(Surface)} after binding the graphics
   * context. Implementations hook into device services here (e.g. registering
   * a swap-buffers callback on the swapchain {@code onPresent} event).
   *
   * <p>Default is a no-op.
   *
   * @param device the graphics device that was just loaded
   */
  @InternalApi
  default void initializeHooks(Device device) {
  }

  /**
   * Returns whether the surface has been requested to close.
   *
   * @return {@code true} if the surface should close
   */
  boolean shouldClose();

  /**
   * Processes pending window events (input, resize, etc.).
   *
   * <p>Must be called once per frame, typically before rendering.
   */
  void pollEvents();

  /**
   * Sets a callback invoked when a key is pressed.
   *
   * @param cb callback receiving the key code, or {@code null} to clear
   */
  void onKeyPress(@Nullable Consumer<Integer> cb);

  /**
   * Sets a callback invoked when a key is released.
   *
   * @param cb callback receiving the key code, or {@code null} to clear
   */
  void onKeyRelease(@Nullable Consumer<Integer> cb);

  /**
   * Sets a callback invoked when the mouse moves.
   *
   * @param cb callback receiving (x, y) in screen coordinates, or {@code null} to clear
   */
  void onMouseMove(@Nullable BiConsumer<Double, Double> cb);

  /**
   * Sets a callback invoked on mouse button events.
   *
   * @param cb callback receiving (button, pressed), or {@code null} to clear
   */
  void onMouseButton(@Nullable BiConsumer<Integer, Boolean> cb);

  /**
   * Sets a callback invoked on scroll events.
   *
   * @param cb callback receiving the vertical scroll offset, or {@code null} to clear
   */
  void onScroll(@Nullable Consumer<Double> cb);

  /**
   * Sets a callback invoked when the surface is resized.
   *
   * @param cb callback receiving (width, height) in pixels, or {@code null} to clear
   */
  void onResize(@Nullable BiConsumer<Integer, Integer> cb);

  /**
   * Sets a callback invoked when files are dropped onto the surface.
   *
   * @param cb callback receiving the file paths, or {@code null} to clear
   */
  void onFileDrop(@Nullable Consumer<String[]> cb);

  /**
   * Destroys the surface and frees all associated native resources.
   *
   * <p>Idempotent — safe to call multiple times.
   */
  @Override
  void close();
}
