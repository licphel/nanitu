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

package net.nanitu.graphics.pass;

import net.nanitu.graphics.texture.TextureFilter;
import net.nanitu.math.Color;
import org.jspecify.annotations.Nullable;

/**
 * A framebuffer target that receives rendered output.
 *
 * <p>A {@code RenderTarget} may represent either:
 * <ul>
 *   <li>The default swapchain view (the visible window), or
 *   <li>An off-screen framebuffer object (FBO) with color and depth/stencil
 *       attachments.
 * </ul>
 *
 * <p>For off-screen targets, {@link #present()} is typically a no-op;
 * the rendered result is consumed via {@link #blit} or by reading the
 * color attachment texture.
 *
 * <p><b>Thread safety:</b> {@link #acquire}, {@link #present}, and
 * {@link #blit} submit work to the render thread. They are safe to call
 * from any thread.
 *
 * @see RenderPassDesc
 */
public interface RenderTarget extends AutoCloseable {
  /**
   * Binds this render target for subsequent draw calls and optionally performs clear operations.
   *
   * <p>If {@code desc} is non-null, the attachments specified by its
   * {@link RenderPassDesc#of(Color)} are cleared to the corresponding clear values.
   *
   * @param desc clear configuration, or {@code null} to skip clearing
   */
  void acquire(@Nullable RenderPassDesc desc);

  /**
   * Binds this render target without clearing.
   *
   * <p>Equivalent to {@link #acquire(RenderPassDesc) acquire(null)}.
   */
  default void acquire() {
    acquire(null);
  }

  /**
   * Presents (swaps) the rendered frame to the display.
   *
   * <p>For the default swapchain view, this flips the back buffer to
   * the front. For off-screen targets, this is a no-op — the results stay in GPU memory for later use via
   * {@link #blit}.
   */
  void present();

  /**
   * Blits (copies) a rectangular region of this render target into another.
   *
   * <p>Both the source and destination must be render targets. The filter
   * only applies when the regions differ in size; for 1:1 copies use {@link TextureFilter#NEAREST} to avoid unnecessary
   * sampling overhead.
   *
   * @param target the destination render target
   * @param srcX   left edge of the source region
   * @param srcY   top edge of the source region
   * @param srcW   width of the source region
   * @param srcH   height of the source region
   * @param dstX   left edge of the destination region
   * @param dstY   top edge of the destination region
   * @param dstW   width of the destination region
   * @param dstH   height of the destination region
   * @param filter filtering mode when scaling (ignored for 1:1 copies)
   */
  void blit(RenderTarget target, int srcX, int srcY, int srcW, int srcH, int dstX, int dstY, int dstW, int dstH,
            TextureFilter filter);

  /**
   * Registers a hook invoked (on the render thread) just before buffer swap during {@link #present()}. Default no-op;
   * swapchain targets override.
   *
   * @param hook the hook to add
   */
  default void onPresent(Runnable hook) {
  }

  @Override
  void close();
}
