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

package net.nanitu.natives.opengl;

import net.nanitu.graphics.*;
import net.nanitu.util.InternalApi;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * OpenGL implementation of {@link Device}.
 *
 * <p>GL commands are submitted from any thread via {@link #submit(Runnable)}
 * into a lock-free queue, then executed on the calling thread by
 * {@link #pollEvents()} (the "command buffer execution" pattern).
 *
 * <p><b>Startup:</b>
 * <pre>{@code
 * Surface surface = SurfaceProvider.create();
 * Device device = DeviceProvider.create();
 * device.load(surface);
 *
 * Texture tex = device.createTexture(desc);
 *
 * // Main loop:
 * while (!surface.shouldClose()) {
 *     surface.pollEvents();
 *     device.pollEvents();   // executes pending GL commands
 *     device.getSwapchain().present();
 * }
 * device.close();
 * surface.close();
 * }</pre>
 *
 * <p><b>Thread safety:</b> {@link #submit} is safe from any thread.
 * {@link #pollEvents()} drains and executes queued work on the calling
 * thread (which must be the GL context thread). The {@link #cache} is
 * only touched during {@code pollEvents}.
 *
 * @see OpenGLCache
 * @see Surface
 * @see Device
 */
@InternalApi
final class OpenGLDevice implements Device {
  /**
   * Shared GL state cache — only accessed during {@link #pollEvents()}.
   */
  final OpenGLCache cache = new OpenGLCache();

  private final Queue<Runnable> queue = new ConcurrentLinkedQueue<>();
  private final OpenGLSwapchain swapchain = new OpenGLSwapchain(this);
  private @Nullable Surface surface;

  private volatile int fbHeight = 1;

  /**
   * Creates a new device.
   *
   * <p>Call {@link #load(Surface)} before any GPU resource creation.
   */
  OpenGLDevice() {
  }

  // -------------------------------------------------------------------------
  // Device API
  // -------------------------------------------------------------------------

  @Override
  public DeviceInfo info() {
    return new DeviceInfo("OpenGL (LWJGL 3)", true);
  }

  /**
   * Loads this device onto the given surface.
   *
   * <p>Binds the GL context to the calling thread and initializes GL
   * capabilities. Must be called before any resource creation.
   */
  @Override
  public void load(Surface surface) {
    this.surface = surface;
    ((Runnable) surface.procAddress()).run();
    GL.createCapabilities();
    fbHeight = surface.height();
    surface.onResize((w, h) -> submit(() -> onResize(w, h)));
    surface.initializeHooks(this);
  }

  @Override
  public BufferObject getBuffer(BufferObjectDesc desc) {
    return new OpenGLBufferObject(this, desc);
  }

  @Override
  public Texture getTexture(TextureDesc desc) {
    return new OpenGLTexture(this, desc);
  }

  @Override
  public Sampler getSampler(SamplerDesc desc) {
    return new OpenGLSampler(this, desc);
  }

  @Override
  public ShaderModule getShaderModule(ShaderModuleDesc desc) {
    return new OpenGLShaderModule(this, desc);
  }

  @Override
  public ShaderProgram getShaderProgram(ShaderModule... modules) {
    return new OpenGLShaderProgram(this, modules);
  }

  @Override
  public RenderPipe getRenderPipeline(RenderPipeDesc desc) {
    return new OpenGLRenderPipe(this, desc);
  }

  @Override
  public ResourceSet getResourceSet() {
    return new OpenGLResourceSet(this);
  }

  @Override
  public Encoder getEncoder() {
    return new OpenGLEncoder(this);
  }

  @Override
  public RenderTarget getRenderTarget() {
    return swapchain;
  }

  @Override
  public RenderTarget getRenderTarget(int width, int height) {
    return new OpenGLRenderTarget(this, width, height);
  }

  @Override
  public RenderTarget getSwapchain() {
    return swapchain;
  }

  // -------------------------------------------------------------------------
  // Command execution
  // -------------------------------------------------------------------------

  @Override
  public void submit(Runnable work) {
    queue.add(work);
  }

  @Override
  public void execute() {
    Runnable task;
    while ((task = queue.poll()) != null) {
      task.run();
    }
  }

  @Override
  public void pollEvents() {
    submit(() -> {
      int err;
      while ((err = GL11.glGetError()) != GL11.GL_NO_ERROR) {
        System.err.println("[OpenGL] Error: 0x" + Integer.toHexString(err));
      }
    });
  }

  @Override
  public void close() {
    execute();
  }

  // -------------------------------------------------------------------------
  // Resize
  // -------------------------------------------------------------------------

  /**
   * Called when the framebuffer is resized.
   */
  public void onResize(int w, int h) {
    fbHeight = Math.max(1, h);
  }

  /**
   * Returns the current framebuffer height, guaranteed to be ≥ 1.
   */
  public int framebufferHeight() {
    return fbHeight;
  }
}
