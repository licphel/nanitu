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

/**
 * A window or drawable view that a {@link Device} renders into.
 *
 * <p>A view is a backend-agnostic abstraction over the native windowing system. All
 * communication with the graphics backend goes through {@link #procAddress()} — an opaque handle whose concrete type is
 * known only to the backend.
 *
 * <p>Window state (size, position, title, visibility, etc.) is managed through
 * {@link #controller()}. Input events (keyboard, mouse, scroll, drop) are received through {@link #hook()}.
 *
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li>Create a concrete view implementation.
 *   <li>Attach it to the device.
 *   <li>In the main loop, call {@link #pollEvents()}.
 *   <li>Call {@link #close()} to destroy the view and release native resources.
 * </ol>
 *
 * @see Device
 * @see ViewController
 * @see ViewHook
 */
public interface View extends AutoCloseable {
  /**
   * Returns static information about this view.
   *
   * @return the view information
   */
  ViewInfo info();

  /**
   * Returns an opaque native handle used for backend communication.
   *
   * <p>The concrete type is backend-specific. Outside the graphics backend, the returned
   * object must be treated as completely opaque.
   *
   * @return the native view handle
   */
  Object procAddress();

  /**
   * Returns the controller for managing window state.
   *
   * @return the view controller
   */
  ViewController controller();

  /**
   * Returns the hook for registering input event callbacks.
   *
   * @return the view hook
   */
  ViewHook hook();

  /**
   * Initializes the view after configuration.
   */
  void initialize();

  /**
   * Returns the current view width in pixels.
   *
   * @return the width in pixels
   */
  default int width() {
    return (int) controller().size().x();
  }

  /**
   * Returns the current view height in pixels.
   *
   * @return the height in pixels
   */
  default int height() {
    return (int) controller().size().y();
  }

  /**
   * Called after the graphics context is bound to initialize backend hooks.
   *
   * <p>Implementations register device services here, such as swap-buffers callbacks.
   * The default implementation is a no-op.
   *
   * @param device the graphics device being loaded
   */
  @InternalApi
  default void initializeHooks(Device device) {
  }

  /**
   * Returns whether the view has been requested to close.
   *
   * @return {@code true} if the view should close
   */
  boolean shouldClose();

  /**
   * Processes pending window events such as input, resize, and focus changes.
   *
   * <p>Should be called once per frame, typically before rendering.
   */
  void pollEvents();

  /**
   * Destroys the view and releases all associated native resources.
   *
   * <p>This method is idempotent — safe to call multiple times.
   */
  @Override
  void close();
}
