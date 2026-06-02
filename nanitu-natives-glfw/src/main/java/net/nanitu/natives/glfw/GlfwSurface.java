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

package net.nanitu.natives.glfw;

import net.nanitu.graphics.Device;
import net.nanitu.graphics.Surface;
import net.nanitu.graphics.SurfaceInfo;
import net.nanitu.util.InternalApi;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryUtil;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * GLFW window implementing {@link Surface} with an OpenGL 3.3 Core Profile context.
 *
 * <p>This class manages the lifecycle of a GLFW window and its associated
 * OpenGL context. Through {@link Surface}, it communicates with the graphics
 * backend solely via {@link #procAddress()} — the backend casts the opaque
 * handle to a GLFW window handle internally.
 *
 * <p><b>Important:</b> Callbacks are stored as strong references to prevent GC
 * from reclaiming them, which would cause native code to call into freed memory.
 */
@InternalApi
public final class GlfwSurface implements Surface {
  private long handle;
  private int width;
  private int height;
  private @Nullable Consumer<Integer> onKeyPress;
  private @Nullable Consumer<Integer> onKeyRelease;
  private @Nullable BiConsumer<Double, Double> onMouseMove;
  private @Nullable BiConsumer<Integer, Boolean> onMouseButton;
  private @Nullable Consumer<Double> onScroll;
  private @Nullable BiConsumer<Integer, Integer> onResize;
  private @Nullable Consumer<String[]> onFileDrop;

  /**
   * Creates and shows a GLFW window with an OpenGL 3.3 Core context.
   *
   * @throws IllegalStateException if GLFW initialization fails
   * @throws RuntimeException      if window creation fails
   */
  public GlfwSurface() {
    this("Nanitu", 800, 450, null);
  }

  /**
   * Creates and shows a GLFW window with an OpenGL 3.3 Core context.
   *
   * @param title  window title
   * @param width  initial width in pixels
   * @param height initial height in pixels
   * @throws IllegalStateException if GLFW initialization fails
   * @throws RuntimeException      if window creation fails
   */
  public GlfwSurface(String title, int width, int height) {
    this(title, width, height, null);
  }

  /**
   * Creates and shows a GLFW window with an OpenGL 3.3 Core context.
   *
   * @param title        window title
   * @param width        initial width in pixels
   * @param height       initial height in pixels
   * @param shareContext window whose GL context to share (maybe {@code null})
   * @throws IllegalStateException if GLFW initialization fails
   * @throws RuntimeException      if window creation fails
   */
  public GlfwSurface(String title, int width, int height, @Nullable GlfwSurface shareContext) {
    this.width = width;
    this.height = height;

    GLFWErrorCallback.createPrint(System.err).set();

    if (!glfwInit()) {
      throw new IllegalStateException("Failed to initialize GLFW");
    }

    configureWindowHints();
    long share = shareContext != null ? shareContext.handle : NULL;
    handle = glfwCreateWindow(width, height, title, NULL, share);

    if (handle == NULL) {
      glfwTerminate();
      throw new RuntimeException("Failed to create GLFW window");
    }

    centreWindowOnPrimaryMonitor();
    setupCallbacks();
  }

  /**
   * Terminates GLFW globally.
   *
   * <p>This should be called once when the application exits, after all
   * windows have been closed.
   */
  public static void terminate() {
    glfwTerminate();
    GLFWErrorCallback cb = glfwSetErrorCallback(null);
    if (cb != null) {
      cb.free();
    }
  }

  @Override
  public SurfaceInfo info() {
    return new SurfaceInfo("GLFW (LWJGL 3)");
  }

  @Override
  public Object procAddress() {
    long h = handle;
    return (Runnable) () -> glfwMakeContextCurrent(h);
  }

  @Override
  public int width() {
    return width;
  }

  @Override
  public int height() {
    return height;
  }

  @Override
  public void initializeHooks(Device device) {
    device.getSwapchain().onPresent(this::swapBuffers);

    glfwSwapInterval(1);
    glfwShowWindow(handle);
  }

  @Override
  public boolean shouldClose() {
    return glfwWindowShouldClose(handle);
  }

  @Override
  public void pollEvents() {
    glfwPollEvents();
  }

  @Override
  public void onKeyPress(@Nullable Consumer<Integer> cb) {
    onKeyPress = cb;
  }

  @Override
  public void onKeyRelease(@Nullable Consumer<Integer> cb) {
    onKeyRelease = cb;
  }

  @Override
  public void onMouseMove(@Nullable BiConsumer<Double, Double> cb) {
    onMouseMove = cb;
  }

  @Override
  public void onMouseButton(@Nullable BiConsumer<Integer, Boolean> cb) {
    onMouseButton = cb;
  }

  @Override
  public void onScroll(@Nullable Consumer<Double> cb) {
    onScroll = cb;
  }

  @Override
  public void onResize(@Nullable BiConsumer<Integer, Integer> cb) {
    onResize = cb;
  }

  @Override
  public void onFileDrop(@Nullable Consumer<String[]> cb) {
    onFileDrop = cb;
  }

  @Override
  public void close() {
    if (handle != NULL) {
      glfwFreeCallbacks(handle);
      glfwDestroyWindow(handle);
      handle = NULL;
    }
  }

  /**
   * Returns the native GLFW window handle.
   *
   * @return GLFW window pointer
   */
  public long handle() {
    return handle;
  }

  /**
   * Swaps buffers. Called by the swapchain hook, not by user code.
   */
  public void swapBuffers() {
    glfwSwapBuffers(handle);
  }

  /**
   * Sets the window title.
   *
   * @param title new window title
   */
  public void setTitle(String title) {
    glfwSetWindowTitle(handle, title);
  }

  /**
   * Enables or disables v-sync (swap interval).
   *
   * @param enabled {@code true} to enable v-sync (1), {@code false} to disable (0)
   */
  public void setVSync(boolean enabled) {
    glfwSwapInterval(enabled ? 1 : 0);
  }

  /**
   * Sets the window size.
   *
   * @param width  new width in pixels
   * @param height new height in pixels
   */
  public void setSize(int width, int height) {
    glfwSetWindowSize(handle, width, height);
  }

  /**
   * Returns whether the window is currently focused.
   *
   * @return {@code true} if focused
   */
  public boolean isFocused() {
    return glfwGetWindowAttrib(handle, GLFW_FOCUSED) == 1;
  }

  /**
   * Returns whether the window is iconified (minimized).
   *
   * @return {@code true} if iconified
   */
  public boolean isIconified() {
    return glfwGetWindowAttrib(handle, GLFW_ICONIFIED) == 1;
  }

  /**
   * Configures OpenGL window hints for 3.3 Core Profile.
   */
  private void configureWindowHints() {
    glfwDefaultWindowHints();
    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
    glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE); // required on macOS
    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
    glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
  }

  /**
   * Centres the window on the primary monitor.
   */
  private void centreWindowOnPrimaryMonitor() {
    GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
    if (vidMode != null) {
      glfwSetWindowPos(handle, (vidMode.width() - width) / 2, (vidMode.height() - height) / 2);
    }
  }

  /**
   * Sets up all GLFW callbacks and stores them as fields to prevent GC.
   */
  private void setupCallbacks() {
    glfwSetFramebufferSizeCallback(handle, (win, w, h) -> {
      this.width = w;
      this.height = h;
      if (onResize != null) {
        onResize.accept(w, h);
      }
    });

    glfwSetKeyCallback(handle, (win, key, scancode, action, mods) -> {
      if (action == GLFW_PRESS && onKeyPress != null) {
        onKeyPress.accept(key);
      } else if (action == GLFW_RELEASE && onKeyRelease != null) {
        onKeyRelease.accept(key);
      }
    });

    glfwSetCursorPosCallback(handle, (win, x, y) -> {
      if (onMouseMove != null) {
        onMouseMove.accept(x, y);
      }
    });

    glfwSetMouseButtonCallback(handle, (win, button, action, mods) -> {
      if (onMouseButton != null) {
        onMouseButton.accept(button, action == GLFW_PRESS);
      }
    });

    glfwSetScrollCallback(handle, (win, xOffset, yOffset) -> {
      if (onScroll != null) {
        onScroll.accept(yOffset);
      }
    });

    glfwSetDropCallback(handle, (win, count, names) -> {
      if (onFileDrop != null) {
        String[] paths = new String[count];
        for (int i = 0; i < count; i++) {
          long pointer = MemoryUtil.memGetAddress(names + (long) i * Long.BYTES);
          paths[i] = MemoryUtil.memUTF8(pointer);
        }
        onFileDrop.accept(paths);
      }
    });
  }
}
