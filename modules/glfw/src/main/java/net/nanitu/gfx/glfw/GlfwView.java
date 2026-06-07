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

package net.nanitu.gfx.glfw;

import net.nanitu.gfx.*;
import net.nanitu.gfx.io.ImageInfo;
import net.nanitu.gfx.io.ImageInputStream;
import net.nanitu.math.Vector2;
import net.nanitu.util.InternalApi;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.*;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * GLFW window implementing {@link View} with an OpenGL 3.3 Core Profile context.
 *
 * <p>This class manages the lifecycle of a GLFW window and its associated
 * OpenGL context. Window state is exposed through {@link #controller()} and input events through {@link #hook()}. All
 * GLFW callback objects are stored in {@link #callbacks} as strong references to prevent GC from reclaiming them while
 * native code holds function pointers.
 */
@InternalApi
public final class GlfwView implements View, ViewController, ViewHook {
  private static final int[] DEFAULT_WINDOW_SIZE = {800, 450};
  private static final int CALLBACK_FRAMEBUFFER_SIZE = 0;
  private static final int CALLBACK_KEY = 1;
  private static final int CALLBACK_CURSOR_POS = 2;
  private static final int CALLBACK_MOUSE_BUTTON = 3;
  private static final int CALLBACK_SCROLL = 4;
  private static final int CALLBACK_DROP = 5;
  private static final int CALLBACK_CHAR = 6;
  private static final int CALLBACK_CURSOR_ENTER = 7;
  private static final int CALLBACK_WINDOW_FOCUS = 8;
  private static final int CALLBACK_WINDOW_ICONIFY = 9;
  private static final int CALLBACK_WINDOW_MAXIMIZE = 10;
  private static final int CALLBACK_WINDOW_MOVE = 11;
  private static final int CALLBACK_WINDOW_CLOSE = 12;

  private final ThreadLocal<int[][]> BUFFERS = ThreadLocal.withInitial(() -> new int[1][2]);

  /** Holds strong references to all GLFW callback objects to prevent GC. */
  private final Object[] callbacks = new Object[16];
  // Key state tracking
  private final ConcurrentHashMap<Integer, Byte> keyStatusMap = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<Integer, Integer> keyModMap = new ConcurrentHashMap<>();
  private long handle;
  private int width = DEFAULT_WINDOW_SIZE[0];
  private int height = DEFAULT_WINDOW_SIZE[1];
  private int x;
  private int y;
  private String title = "Nanitu - GLFW (LWJGL 3)";
  // User callbacks — stored as fields for GC safety (these are passed to GLFW lambdas)
  private @Nullable Consumer<Integer> onKeyPress;
  private @Nullable Consumer<Integer> onKeyRelease;
  private @Nullable Consumer<Character> onCharInput;
  private @Nullable BiConsumer<Double, Double> onMouseMove;
  private @Nullable BiConsumer<Integer, Boolean> onMouseButton;
  private @Nullable BiConsumer<Double, Double> onScroll;
  private @Nullable Consumer<Boolean> onCursorEnter;
  private @Nullable BiConsumer<Integer, Integer> onResize;
  private @Nullable Consumer<Boolean> onWindowFocus;
  private @Nullable Consumer<Boolean> onWindowIconify;
  private @Nullable Consumer<Boolean> onWindowMaximize;
  private @Nullable BiConsumer<Integer, Integer> onWindowMove;
  private @Nullable Consumer<String[]> onFileDrop;
  // Cursor state
  private double cursorX;
  private double cursorY;
  private @Nullable ImageInputStream cursorImage;
  private Vector2 cursorHotspot = Vector2.ZERO;
  private long cursorHandle;
  // Scroll accumulator
  private double scrollAccumX;
  private double scrollAccumY;
  private boolean vsync = true;

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
  public ViewInfo info() {
    return new ViewInfo("GLFW (LWJGL 3)");
  }

  @Override
  public Object procAddress() {
    long h = handle;
    return (Runnable) () -> glfwMakeContextCurrent(h);
  }

  @Override
  public ViewController controller() {
    return this;
  }

  @Override
  public ViewHook hook() {
    return this;
  }

  @Override
  public synchronized void initialize() {
    GLFWErrorCallback.createPrint(System.err).set();

    if (!glfwInit()) {
      throw new IllegalStateException("Failed to initialize GLFW");
    }

    configureWindowHints();
    handle = glfwCreateWindow(width, height, title, NULL, NULL);

    if (handle == NULL) {
      glfwTerminate();
      throw new RuntimeException("Failed to create GLFW window");
    }

    centreWindowOnPrimaryMonitor();
    setupCallbacks();
  }

  @Override
  public void initializeHooks(Device device) {
    device.getSwapchain().onPresent(this::swapBuffers);

    glfwSwapInterval(vsync ? 1 : 0);
    glfwShowWindow(handle);
  }

  @Override
  public boolean shouldClose() {
    return handle != NULL && glfwWindowShouldClose(handle);
  }

  @Override
  public void pollEvents() {
    glfwPollEvents();
  }

  @Override
  public void close() {
    if (cursorHandle != NULL) {
      glfwDestroyCursor(cursorHandle);
      cursorHandle = NULL;
    }
    if (handle != NULL) {
      glfwFreeCallbacks(handle);
      glfwDestroyWindow(handle);
      handle = NULL;
    }
  }

  @Override
  public Vector2 size() {
    return new Vector2(width, height);
  }

  @Override
  public void setSize(Vector2 size) {
    if (handle == NULL) {
      width = (int) size.x();
      height = (int) size.y();
    } else {
      glfwSetWindowSize(handle, (int) size.x(), (int) size.y());
    }
  }

  @Override
  public Vector2 position() {
    if (handle != NULL) {
      int[][] buf = BUFFERS.get();
      glfwGetWindowPos(handle, buf[0], buf[1]);
      x = buf[0][0];
      y = buf[1][0];
    }
    return new Vector2(x, y);
  }

  @Override
  public void setPosition(Vector2 position) {
    x = (int) position.x();
    y = (int) position.y();
    if (handle != NULL) {
      glfwSetWindowPos(handle, x, y);
    }
  }

  @Override
  public String title() {
    return title;
  }

  @Override
  public void setTitle(String title) {
    this.title = title;
    if (handle != NULL) {
      glfwSetWindowTitle(handle, title);
    }
  }

  @Override
  public void setIcon(ImageInputStream image) {
    ImageInfo info = image.info();
    try (image) {
      byte[] pixels = image.readAllBytes();
      var buf = MemoryUtil.memAlloc(pixels.length);
      try {
        buf.put(pixels).flip();
        try (var gBuf = GLFWImage.malloc(1)) {
          gBuf.width(info.width()).height(info.height()).pixels(buf);
          if (handle != NULL) {
            glfwSetWindowIcon(handle, gBuf);
          }
        }
      } finally {
        MemoryUtil.memFree(buf);
      }
    } catch (IOException e) {
      throw new GraphicsException("Failed to read icon image", e);
    }
  }

  @Override
  public Vector2 cursorPosition() {
    return new Vector2((float) cursorX, (float) cursorY);
  }

  @Override
  public void setCursorPosition(Vector2 position) {
    cursorX = position.x();
    cursorY = position.y();
    if (handle != NULL) {
      glfwSetCursorPos(handle, cursorX, cursorY);
    }
  }

  @Override
  public void setCursor(ImageInputStream image, Vector2 hotspot) {
    ImageInfo info = image.info();
    try (image) {
      byte[] pixels = image.readAllBytes();
      var buf = MemoryUtil.memAlloc(pixels.length);
      try {
        buf.put(pixels).flip();
        GLFWImage gImg = GLFWImage.create().set(info.width(), info.height(), buf);
        long newCursor = glfwCreateCursor(gImg, (int) hotspot.x(), (int) hotspot.y());
        if (newCursor == NULL) {
          throw new GraphicsException("Failed to create GLFW cursor");
        }
        if (handle != NULL) {
          glfwSetCursor(handle, newCursor);
        }
        if (cursorHandle != NULL) {
          glfwDestroyCursor(cursorHandle);
        }
        cursorHandle = newCursor;
        this.cursorImage = image;
        this.cursorHotspot = hotspot;
      } finally {
        MemoryUtil.memFree(buf);
      }
    } catch (IOException e) {
      throw new GraphicsException("Failed to read cursor image", e);
    }
  }

  @Override
  public @Nullable ImageInputStream cursorImage() {
    return cursorImage;
  }

  @Override
  public Vector2 cursorHotspot() {
    return cursorHotspot;
  }

  @Override
  public boolean isCursorRelativeMode() {
    return handle != NULL && glfwGetInputMode(handle, GLFW_CURSOR) == GLFW_CURSOR_DISABLED;
  }

  @Override
  public void setCursorRelativeMode(boolean enabled) {
    if (handle != NULL) {
      glfwSetInputMode(handle, GLFW_CURSOR, enabled ? GLFW_CURSOR_DISABLED : GLFW_CURSOR_NORMAL);
    }
  }

  @Override
  public String clipboardText() {
    if (handle != NULL) {
      String text = glfwGetClipboardString(handle);
      return text != null ? text : "";
    }
    return "";
  }

  @Override
  public void setClipboardText(String text) {
    if (handle != NULL) {
      glfwSetClipboardString(handle, text);
    }
  }

  @Override
  public boolean isDecorated() {
    return handle != NULL && glfwGetWindowAttrib(handle, GLFW_DECORATED) == GLFW_TRUE;
  }

  @Override
  public void setDecorated(boolean decorated) {
    if (handle != NULL) {
      glfwSetWindowAttrib(handle, GLFW_DECORATED, decorated ? GLFW_TRUE : GLFW_FALSE);
    }
  }

  @Override
  public boolean isMaximized() {
    return handle != NULL && glfwGetWindowAttrib(handle, GLFW_MAXIMIZED) == GLFW_TRUE;
  }

  @Override
  public void setMaximized(boolean maximized) {
    if (handle != NULL) {
      if (maximized) {
        glfwMaximizeWindow(handle);
      } else {
        glfwRestoreWindow(handle);
      }
    }
  }

  @Override
  public boolean isAutoIconify() {
    return handle != NULL && glfwGetWindowAttrib(handle, GLFW_AUTO_ICONIFY) == GLFW_TRUE;
  }

  @Override
  public void setAutoIconify(boolean autoIconify) {
    if (handle != NULL) {
      glfwSetWindowAttrib(handle, GLFW_AUTO_ICONIFY, autoIconify ? GLFW_TRUE : GLFW_FALSE);
    }
  }

  @Override
  public boolean isFloating() {
    return handle != NULL && glfwGetWindowAttrib(handle, GLFW_FLOATING) == GLFW_TRUE;
  }

  @Override
  public void setFloating(boolean floating) {
    if (handle != NULL) {
      glfwSetWindowAttrib(handle, GLFW_FLOATING, floating ? GLFW_TRUE : GLFW_FALSE);
    }
  }

  @Override
  public boolean isFocused() {
    return handle != NULL && glfwGetWindowAttrib(handle, GLFW_FOCUSED) == GLFW_TRUE;
  }

  @Override
  public void setFocused(boolean focused) {
    if (handle != NULL) {
      glfwSetWindowAttrib(handle, GLFW_FOCUSED, focused ? GLFW_TRUE : GLFW_FALSE);
    }
  }

  @Override
  public boolean isVisible() {
    return handle != NULL && glfwGetWindowAttrib(handle, GLFW_VISIBLE) == GLFW_TRUE;
  }

  @Override
  public void setVisible(boolean visible) {
    if (handle != NULL) {
      if (visible) {
        glfwShowWindow(handle);
      } else {
        glfwHideWindow(handle);
      }
    }
  }

  @Override
  public boolean isResizable() {
    return handle != NULL && glfwGetWindowAttrib(handle, GLFW_RESIZABLE) == GLFW_TRUE;
  }

  @Override
  public void setResizable(boolean resizable) {
    if (handle != NULL) {
      glfwSetWindowAttrib(handle, GLFW_RESIZABLE, resizable ? GLFW_TRUE : GLFW_FALSE);
    }
  }

  @Override
  public boolean isVsync() {
    return vsync;
  }

  @Override
  public void setVsync(boolean vsync) {
    this.vsync = vsync;
    if (handle != NULL) {
      glfwSwapInterval(vsync ? 1 : 0);
    }
  }

  @Override
  public boolean isDebug() {
    return handle != NULL && glfwGetWindowAttrib(handle, GLFW_CONTEXT_DEBUG) == GLFW_TRUE;
  }

  @Override
  public void setDebug(boolean debug) {
    if (handle != NULL) {
      throw new GraphicsException("Cannot set debug after view initialization");
    }
  }

  @Override
  public KeyStatus keyStatus(int keycode) {
    byte status = keyStatusMap.getOrDefault(keycode, (byte) 0);
    return switch (status) {
      case 1 -> KeyStatus.PRESS;
      case 2 -> KeyStatus.REPEAT;
      default -> KeyStatus.RELEASE;
    };
  }

  @Override
  public int keyModifiers(int keycode) {
    return keyModMap.getOrDefault(keycode, 0);
  }

  @Override
  public Vector2 scrollDelta() {
    double sx = scrollAccumX;
    double sy = scrollAccumY;
    scrollAccumX = 0;
    scrollAccumY = 0;
    return new Vector2((float) sx, (float) sy);
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
  public void onCharInput(@Nullable Consumer<Character> cb) {
    onCharInput = cb;
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
  public void onScroll(@Nullable BiConsumer<Double, Double> cb) {
    onScroll = cb;
  }

  @Override
  public void onCursorEnter(@Nullable Consumer<Boolean> cb) {
    onCursorEnter = cb;
  }

  @Override
  public void onResize(@Nullable BiConsumer<Integer, Integer> cb) {
    onResize = cb;
  }

  @Override
  public void onWindowFocus(@Nullable Consumer<Boolean> cb) {
    onWindowFocus = cb;
  }

  @Override
  public void onWindowIconify(@Nullable Consumer<Boolean> cb) {
    onWindowIconify = cb;
  }

  @Override
  public void onWindowMaximize(@Nullable Consumer<Boolean> cb) {
    onWindowMaximize = cb;
  }

  @Override
  public void onWindowMove(@Nullable BiConsumer<Integer, Integer> cb) {
    onWindowMove = cb;
  }

  @Override
  public void onFileDrop(@Nullable Consumer<String[]> cb) {
    onFileDrop = cb;
  }

  /**
   * Returns the native GLFW window handle.
   *
   * @return the GLFW window pointer
   */
  public long handle() {
    return handle;
  }

  /** Swaps the front and back buffers. Called by the swapchain hook. */
  public void swapBuffers() {
    if (handle != NULL) {
      glfwSwapBuffers(handle);
    }
  }

  /**
   * Configures OpenGL window hints for a 3.3 Core Profile context.
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
   * Centers the window on the primary monitor.
   */
  private void centreWindowOnPrimaryMonitor() {
    GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
    if (vidMode != null) {
      x = (vidMode.width() - width) / 2;
      y = (vidMode.height() - height) / 2;
      glfwSetWindowPos(handle, x, y);
    }
  }

  /**
   * Registers all GLFW callbacks and stores them as strong references in {@link #callbacks} to prevent GC from
   * reclaiming objects whose function pointers are held by native code.
   */
  private void setupCallbacks() {
    callbacks[CALLBACK_FRAMEBUFFER_SIZE] = (GLFWFramebufferSizeCallbackI) (win, w, h) -> {
      this.width = w;
      this.height = h;
      if (onResize != null) {
        onResize.accept(w, h);
      }
    };
    glfwSetFramebufferSizeCallback(handle, (GLFWFramebufferSizeCallbackI) callbacks[CALLBACK_FRAMEBUFFER_SIZE]);

    callbacks[CALLBACK_KEY] = (GLFWKeyCallbackI) (win, key, scancode, action, mods) -> {
      keyStatusMap.put(key, (byte) action);
      keyModMap.put(key, mods);
      if (action == GLFW_PRESS && onKeyPress != null) {
        onKeyPress.accept(key);
      } else if (action == GLFW_RELEASE && onKeyRelease != null) {
        onKeyRelease.accept(key);
      }
    };
    glfwSetKeyCallback(handle, (GLFWKeyCallbackI) callbacks[CALLBACK_KEY]);

    callbacks[CALLBACK_CHAR] = (GLFWCharCallbackI) (win, codepoint) -> {
      if (onCharInput != null) {
        onCharInput.accept((char) codepoint);
      }
    };
    glfwSetCharCallback(handle, (GLFWCharCallbackI) callbacks[CALLBACK_CHAR]);

    callbacks[CALLBACK_CURSOR_POS] = (GLFWCursorPosCallbackI) (win, cx, cy) -> {
      cursorX = cx;
      cursorY = cy;
      if (onMouseMove != null) {
        onMouseMove.accept(cx, cy);
      }
    };
    glfwSetCursorPosCallback(handle, (GLFWCursorPosCallbackI) callbacks[CALLBACK_CURSOR_POS]);

    callbacks[CALLBACK_MOUSE_BUTTON] = (GLFWMouseButtonCallbackI) (win, button, action, mods) -> {
      keyStatusMap.put(button, (byte) action);
      keyModMap.put(button, mods);
      if (onMouseButton != null) {
        onMouseButton.accept(button, action == GLFW_PRESS);
      }
    };
    glfwSetMouseButtonCallback(handle, (GLFWMouseButtonCallbackI) callbacks[CALLBACK_MOUSE_BUTTON]);

    callbacks[CALLBACK_SCROLL] = (GLFWScrollCallbackI) (win, xOffset, yOffset) -> {
      scrollAccumX += xOffset;
      scrollAccumY += yOffset;
      if (onScroll != null) {
        onScroll.accept(xOffset, yOffset);
      }
    };
    glfwSetScrollCallback(handle, (GLFWScrollCallbackI) callbacks[CALLBACK_SCROLL]);

    callbacks[CALLBACK_CURSOR_ENTER] = (GLFWCursorEnterCallbackI) (win, entered) -> {
      if (onCursorEnter != null) {
        onCursorEnter.accept(entered);
      }
    };
    glfwSetCursorEnterCallback(handle, (GLFWCursorEnterCallbackI) callbacks[CALLBACK_CURSOR_ENTER]);

    callbacks[CALLBACK_WINDOW_CLOSE] = (GLFWWindowCloseCallbackI) (win) -> {
      glfwSetWindowShouldClose(handle, true);
    };
    glfwSetWindowCloseCallback(handle, (GLFWWindowCloseCallbackI) callbacks[CALLBACK_WINDOW_CLOSE]);

    callbacks[CALLBACK_WINDOW_FOCUS] = (GLFWWindowFocusCallbackI) (win, focused) -> {
      if (onWindowFocus != null) {
        onWindowFocus.accept(focused);
      }
    };
    glfwSetWindowFocusCallback(handle, (GLFWWindowFocusCallbackI) callbacks[CALLBACK_WINDOW_FOCUS]);

    callbacks[CALLBACK_WINDOW_ICONIFY] = (GLFWWindowIconifyCallbackI) (win, iconified) -> {
      if (onWindowIconify != null) {
        onWindowIconify.accept(iconified);
      }
    };
    glfwSetWindowIconifyCallback(handle, (GLFWWindowIconifyCallbackI) callbacks[CALLBACK_WINDOW_ICONIFY]);

    callbacks[CALLBACK_WINDOW_MAXIMIZE] = (GLFWWindowMaximizeCallbackI) (win, maximized) -> {
      if (onWindowMaximize != null) {
        onWindowMaximize.accept(maximized);
      }
    };
    glfwSetWindowMaximizeCallback(handle, (GLFWWindowMaximizeCallbackI) callbacks[CALLBACK_WINDOW_MAXIMIZE]);

    callbacks[CALLBACK_WINDOW_MOVE] = (GLFWWindowPosCallbackI) (win, xpos, ypos) -> {
      x = xpos;
      y = ypos;
      if (onWindowMove != null) {
        onWindowMove.accept(xpos, ypos);
      }
    };
    glfwSetWindowPosCallback(handle, (GLFWWindowPosCallbackI) callbacks[CALLBACK_WINDOW_MOVE]);

    callbacks[CALLBACK_DROP] = (GLFWDropCallbackI) (win, count, names) -> {
      if (onFileDrop != null) {
        String[] paths = new String[count];
        for (int i = 0; i < count; i++) {
          long pointer = MemoryUtil.memGetAddress(names + (long) i * Long.BYTES);
          paths[i] = MemoryUtil.memUTF8(pointer);
        }
        onFileDrop.accept(paths);
      }
    };
    glfwSetDropCallback(handle, (GLFWDropCallbackI) callbacks[CALLBACK_DROP]);
  }
}
