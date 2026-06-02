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

import net.nanitu.graphics.RenderPassDesc;
import net.nanitu.graphics.RenderTarget;
import net.nanitu.graphics.TextureFilter;
import net.nanitu.util.InternalApi;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Swapchain render target wrapping the default framebuffer ({@code FBO 0}).
 *
 * <p>Obtain the singleton via {@link OpenGLDevice#getSwapchain()}.
 * Before presenting, listeners registered via {@link #onPresent(Runnable)} are
 * invoked on the render thread — this is where the windowing system hooks
 * {@code swapBuffers}.
 *
 * <p><b>Thread safety:</b> all GL work is submitted via
 * {@link OpenGLDevice#submit(Runnable)}. The listener list is a
 * {@link CopyOnWriteArrayList}.
 */
@InternalApi
public final class OpenGLSwapchain implements RenderTarget {

  private final OpenGLDevice device;
  private final List<Runnable> presentHooks = new CopyOnWriteArrayList<>();

  OpenGLSwapchain(OpenGLDevice device) {
    this.device = device;
  }

  @Override
  public void acquire(@Nullable RenderPassDesc desc) {
    device.submit(() -> {
      device.cache.bindFbo(0);
      if (desc == null) {
        return;
      }
      int glMask = 0;
      if ((desc.clearMask() & RenderPassDesc.CLEAR_COLOR) != 0) {
        glClearColor(desc.clearColor().red(), desc.clearColor().green(), desc.clearColor().blue(), desc.clearColor().alpha());
        glMask |= GL_COLOR_BUFFER_BIT;
      }
      if ((desc.clearMask() & RenderPassDesc.CLEAR_DEPTH) != 0) {
        glClearDepth(desc.clearDepth());
        glMask |= GL_DEPTH_BUFFER_BIT;
      }
      if ((desc.clearMask() & RenderPassDesc.CLEAR_STENCIL) != 0) {
        glClearStencil(desc.clearStencil());
        glMask |= GL_STENCIL_BUFFER_BIT;
      }
      if (glMask != 0) {
        glClear(glMask);
      }
    });
  }

  @Override
  public void present() {
    device.submit(() -> {
      for (Runnable hook : presentHooks) {
        hook.run();
      }
    });
  }

  @Override
  public void blit(RenderTarget target, int srcX, int srcY, int srcW, int srcH, int dstX, int dstY, int dstW,
                   int dstH, TextureFilter filter) {
    device.submit(() -> {
      OpenGLRenderTarget dst = (OpenGLRenderTarget) target;
      int glFilter = OpenGLUtils.textureFilter(filter);
      glBindFramebuffer(GL_READ_FRAMEBUFFER, 0);
      glBindFramebuffer(GL_DRAW_FRAMEBUFFER, dst.fboHandle());
      glBlitFramebuffer(srcX, srcY, srcX + srcW, srcY + srcH, dstX, dstY, dstX + dstW, dstY + dstH,
          GL_COLOR_BUFFER_BIT, glFilter);
      glBindFramebuffer(GL_FRAMEBUFFER, 0);
      device.cache.fbo = 0;
    });
  }

  /**
   * Registers a hook invoked (on the render thread) just before
   * {@code swapBuffers} during {@link #present()}.
   *
   * @param hook the hook to add
   */
  @Override
  public void onPresent(Runnable hook) {
    presentHooks.add(hook);
  }

  @Override
  public void close() {
    // Swapchain is owned by the device — no-op here
  }
}
