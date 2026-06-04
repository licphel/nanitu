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

import net.nanitu.graphics.GraphicsException;
import net.nanitu.graphics.buffer.BufferObject;
import net.nanitu.graphics.buffer.BufferType;
import net.nanitu.graphics.cmd.Encoder;
import net.nanitu.graphics.pipe.Pipeline;
import net.nanitu.graphics.pipe.Topology;
import net.nanitu.graphics.shader.ResourceSet;
import net.nanitu.graphics.shader.ResourceSetLayout;
import net.nanitu.util.InternalApi;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.opengl.GL43.glDispatchCompute;

/**
 * OpenGL implementation of {@link Encoder} that records GPU commands into a list of {@link Runnable} lambdas.
 *
 * <p>Commands are accumulated in an {@link ArrayList} on the calling thread.
 * When {@link #queuedExecute()} is called, the entire list is snapshot and posted to the render thread as a single
 * {@link OpenGLDevice#submit(Runnable)} call. This batching avoids per-command queue overhead.
 *
 * <p><b>State tracking:</b> the encoder remembers the last-set pipeline,
 * vertex buffer, index buffer, and topology on the recording thread so that {@link #draw} and {@link #drawIndexed} can
 * capture the correct state without requiring it to be re-specified before every draw call.
 *
 * <p><b>Thread safety:</b> recording is single-threaded per instance.
 * {@link #queuedExecute()} and {@link #reset()} may be called from any thread.
 *
 * @see OpenGLDevice#submit(Runnable)
 */
@InternalApi
final class OpenGLEncoder implements Encoder {
  private final OpenGLDevice ctx;
  private final List<Runnable> commands = new ArrayList<>();
  private final @Nullable OpenGLResourceSet[] currentRss = new OpenGLResourceSet[64]; // Most support 64 sets
  // Per-frame recording state (single-threaded)
  private @Nullable OpenGLPipeline currentPipe;
  private @Nullable OpenGLBufferObject currentVbo;
  private @Nullable OpenGLBufferObject currentEbo;
  private int topology = GL_TRIANGLES;

  /**
   * Creates a new encoder backed by the given GL context.
   *
   * @param ctx the GL context whose render thread executes the commands
   */
  OpenGLEncoder(OpenGLDevice ctx) {
    this.ctx = ctx;
  }

  /**
   * Discards all recorded commands and resets per-frame tracking state.
   *
   * <p>Safe to call from any thread. After reset, the encoder has no
   * remembered pipeline, buffers, or viewport.
   */
  @Override
  public void reset() {
    commands.clear();
    currentPipe = null;
    currentVbo = null;
    currentEbo = null;
  }

  /**
   * Snapshots the command list and submits it to the render thread.
   *
   * <p>The snapshot decouples the submission from further recording:
   * {@link #reset()} may be called immediately without affecting the batch that is about to execute.
   */
  @Override
  public void queuedExecute() {
    List<Runnable> snapshot = new ArrayList<>(commands);
    ctx.submit(() -> {
      for (Runnable cmd : snapshot) {
        cmd.run();
      }
    });
  }

  @Override
  public void setTopology(Topology t) {
    int glTopo = OpenGLUtils.topology(t);
    commands.add(() -> topology = glTopo);
  }

  @Override
  public void setBuffer(BufferObject buffer) {
    OpenGLBufferObject glBuf = (OpenGLBufferObject) buffer;
    commands.add(() -> {
      if (buffer.desc().type() == BufferType.INDEX) {
        currentEbo = glBuf;
      } else {
        currentVbo = glBuf;
      }
    });
  }

  @Override
  public void setRenderPipe(Pipeline pipe) {
    OpenGLPipeline glPipe = (OpenGLPipeline) pipe;
    commands.add(() -> {
      currentPipe = glPipe;
      glPipe.apply(ctx.cache, ctx.framebufferHeight());
    });
  }

  @Override
  public void setViewport(int x, int y, int width, int height) {
    commands.add(() -> ctx.cache.setViewport(x, y, width, height, ctx.framebufferHeight()));
  }

  @Override
  public void setScissor(int x, int y, int width, int height, boolean enable) {
    commands.add(() -> ctx.cache.setScissor(x, y, width, height, enable, ctx.framebufferHeight()));
  }

  @Override
  public void setResource(int slot, ResourceSet set) {
    OpenGLResourceSet rs = (OpenGLResourceSet) set;
    commands.add(() -> {
      if (slot >= currentRss.length) {
        throw new GraphicsException("Mostly support 64 slots, but got " + slot);
      }
      currentRss[slot] = rs;
    });
  }

  /**
   * Records a non-indexed draw call.
   *
   * <p>Skips silently if no pipeline or vertex buffer has been set.
   * Acquires a VAO for the current (VBO, 0) pair from the pipeline's VAO cache, binds it, issues {@code glDrawArrays},
   * and unbinds.
   */
  @Override
  public void draw(int vertexCount, int firstVertex) {
    commands.add(() -> {
      if (currentVbo == null) {
        throw new GraphicsException("VBO not bound");
      }
      if (currentPipe == null) {
        throw new GraphicsException("Pipeline not bound");
      }

      applyResources();

      int vao = currentPipe.acquireVao(currentVbo.handle, 0);
      ctx.cache.bindVao(vao);
      glDrawArrays(topology, firstVertex, vertexCount);
      ctx.cache.bindVao(0);
    });
  }

  /**
   * Records an indexed draw call.
   *
   * <p>Skips silently if no pipeline, vertex buffer, or index buffer has
   * been set. The index buffer is treated as {@code GL_UNSIGNED_INT}.
   *
   * @param indexCount number of indices to draw
   * @param firstIndex index of the first element (byte offset = {@code firstIndex * 4})
   */
  @Override
  public void drawIndexed(int indexCount, int firstIndex) {
    commands.add(() -> {
      if (currentVbo == null) {
        throw new GraphicsException("VBO not bound");
      }
      if (currentEbo == null) {
        throw new GraphicsException("EBO not bound");
      }
      if (currentPipe == null) {
        throw new GraphicsException("Pipeline not bound");
      }

      applyResources();

      int vao = currentPipe.acquireVao(currentVbo.handle, currentEbo.handle);
      ctx.cache.bindVao(vao);
      glDrawElements(topology, indexCount, GL_UNSIGNED_INT, (long) firstIndex * Integer.BYTES);
      ctx.cache.bindVao(0);
    });
  }

  /**
   * Records a compute-shader dispatch.
   *
   * <p>The currently bound pipeline must contain a valid compute program.
   */
  @Override
  public void dispatch(int x, int y, int z) {
    commands.add(() -> glDispatchCompute(x, y, z));
  }

  @Override
  public void close() {
    commands.clear();
  }

  private void applyResources() {
    assert currentPipe != null;

    ResourceSetLayout[] layouts = currentPipe.desc().resourceLayouts();
    for (int i = 0; i < layouts.length; i++) {
      OpenGLResourceSet rs = currentRss[i];
      if (rs == null) {
        throw new GraphicsException("Null resource layout at slot " + i);
      }

      rs.validate(layouts[i]);
      rs.apply(ctx.cache);
    }
  }
}
