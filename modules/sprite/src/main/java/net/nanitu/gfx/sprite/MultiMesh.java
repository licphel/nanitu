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

package net.nanitu.gfx.sprite;

import net.nanitu.gfx.Device;
import net.nanitu.gfx.buffer.BufferFrequency;
import net.nanitu.gfx.buffer.BufferObject;
import net.nanitu.gfx.buffer.BufferObjectDesc;
import net.nanitu.memory.Buffer;
import net.nanitu.memory.Endianness;
import net.nanitu.memory.MemoryAllocator;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A mesh that manages a sequence of {@link Node}s for batched 2D rendering via {@link Brush}.
 *
 * <p>A MultiMesh can operate in two modes:
 * <ul>
 *   <li><strong>Recording mode:</strong> each {@link Brush#flush()} records a new Node.
 *       Nodes can be replayed later with {@link Brush#replay(MultiMesh)}.</li>
 *   <li><strong>Direct mode:</strong> the current Node is submitted to the GPU
 *       immediately on flush. Set via {@link #setDirect(boolean)}.</li>
 * </ul>
 *
 * <p>Call {@link #begin()} to start a recording session and {@link #end()} to finish it.
 * When done, call {@link #close()} to release GPU resources.
 *
 * @see Brush
 */
public final class MultiMesh implements Iterable<MultiMesh.Node>, AutoCloseable {
  private final Device device;
  private final List<Node> nodes = new ArrayList<>();
  @Nullable Brush brush;
  private int curNode;
  private boolean disposed;
  private boolean isDirect;

  /**
   * Creates a MultiMesh.
   *
   * @param device the graphics device
   * @param merge  whether adjacent nodes may be merged into one
   */
  public MultiMesh(Device device, boolean merge) {
    this.device = device;
    nodes.add(new Node(device));
  }

  /**
   * Creates a MultiMesh with node merging disabled.
   *
   * @param device the graphics device
   */
  public MultiMesh(Device device) {
    this(device, false);
  }

  /**
   * Returns whether this mesh operates in direct-to-screen mode.
   */
  boolean isDirect() {
    return isDirect;
  }

  /**
   * Sets whether this mesh operates in direct-to-screen mode.
   *
   * @param direct {@code true} to submit draws directly to the GPU without recording
   */
  public void setDirect(boolean direct) {
    isDirect = direct;
  }

  /**
   * Acquires the next available node, creating one if necessary.
   */
  Node acquire() {
    if (curNode < nodes.size()) {
      return nodes.get(curNode++);
    }
    Node node = new Node(device);
    nodes.add(node);
    curNode++;
    return node;
  }

  /**
   * Begins a recording session and starts a render cycle.
   *
   * @return the Brush for issuing draw commands
   */
  public Brush begin() {
    curNode = 0;
    if (brush == null) {
      brush = new Brush(this, device);
    }
    brush.moveToNextNode();
    brush.begin();
    return brush;
  }

  /**
   * Ends the current recording session and presents the render target.
   *
   * @throws IllegalStateException if {@link #begin()} was not called first
   */
  public void end() {
    if (brush == null) {
      throw new IllegalStateException("Brush is not initialized");
    }
    brush.end0();
  }

  @Override
  public Iterator<Node> iterator() {
    return nodes.iterator();
  }

  @Override
  public void close() {
    if (disposed) {
      return;
    }
    disposed = true;
    if (brush != null) {
      brush.close();
    }
    for (Node node : nodes) {
      node.close();
    }
  }

  /**
   * A single mesh node holding a vertex buffer, an index buffer, and staging buffers for batched drawing.
   *
   * <p>Each node tracks the {@link BrushState} that was active when data was recorded into it.
   */
  public static final class Node {
    static final int INITIAL_CAP = 128;

    final Buffer vertexBuf = allocateBuffer();
    final Buffer indexBuf = allocateBuffer();
    final BufferObject vbo;
    final BufferObject ibo;
    boolean dirty;
    int vertexCount;
    int indexCount;
    BrushState recordedState = new BrushState();

    Node(Device device) {
      vbo = device.getBuffer(BufferObjectDesc.vertex(BufferFrequency.STREAM));
      vbo.allocate(INITIAL_CAP, null);

      ibo = device.getBuffer(BufferObjectDesc.index(BufferFrequency.STREAM));
      ibo.allocate(INITIAL_CAP, null);
    }

    private static Buffer allocateBuffer() {
      return new Buffer(MemoryAllocator.NATIVE.allocate(INITIAL_CAP), Endianness.NATIVE);
    }

    /**
     * Returns whether this node contains no vertex data.
     *
     * @return {@code true} if the vertex count is zero
     */
    public boolean isEmpty() {
      return vertexCount == 0;
    }

    /**
     * Accumulates vertex and index counts and marks the node as dirty.
     *
     * @param vertex the number of vertices added
     * @param index  the number of indices added
     */
    void write(int vertex, int index) {
      vertexCount += vertex;
      indexCount += index;
      dirty = true;
    }

    /**
     * Resets the node to an empty state, clearing all staging buffers and counts.
     */
    void reset() {
      vertexCount = 0;
      indexCount = 0;
      vertexBuf.clear();
      indexBuf.clear();
      dirty = false;
    }

    void close() {
      vbo.close();
      ibo.close();
    }
  }
}
