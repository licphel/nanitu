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

package net.nanitu.audio;

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A node in the audio volume hierarchy.
 *
 * <p>Emitters form a tree: each emitter's <em>effective</em> volume is the
 * product of its own {@link #volume() local volume} and the effective volumes
 * of all its ancestors. This makes it easy to implement category-level volume
 * controls — muting a "music" emitter silences every clip that plays through
 * it, regardless of each clip's individual volume setting.
 *
 * <pre>{@code
 * Emitter master = new Emitter("master");
 * Emitter music  = master.derive("music");
 * Emitter sfx    = master.derive("sfx");
 *
 * master.setVolume(0.8f);  // 80% overall
 * music.setVolume(0.5f);   // music plays at 0.8 × 0.5 = 40% effective volume
 * }</pre>
 *
 * <p>This class is thread-safe: {@link #setVolume} and all hierarchy
 * mutations use {@code volatile} writes and a {@link CopyOnWriteArrayList}.
 *
 * @see Clip
 */
public final class Emitter {
  private final String name;
  private final List<Emitter> children = new CopyOnWriteArrayList<>();
  private @Nullable Emitter parent;
  private volatile float volume = 1.0F;

  /**
   * Creates a root emitter with the given name and a local volume of {@code 1.0}.
   *
   * @param name human-readable label, used in {@link #toString()} and for debugging
   */
  public Emitter(String name) {
    this.name = name;
    parent = null;
  }

  private Emitter(String name, Emitter parent) {
    this.name = name;
    this.parent = parent;
  }

  /**
   * Returns this emitter's name.
   *
   * @return the name passed to the constructor or {@link #derive}
   */
  public String name() {
    return name;
  }

  /**
   * Returns the parent emitter, or {@code null} if this is a root emitter.
   *
   * @return parent emitter, or {@code null} for a root
   */
  public @Nullable Emitter parent() {
    return parent;
  }

  /**
   * Returns a live, unmodifiable view of the direct children of this emitter.
   *
   * <p>The list reflects subsequent calls to {@link #derive} and {@link #remove}
   * without any additional synchronization required by the caller.
   *
   * @return unmodifiable view of the child emitters
   */
  public List<Emitter> children() {
    return Collections.unmodifiableList(children);
  }

  /**
   * Creates a new child emitter under this one and returns it.
   *
   * <p>The child starts with a local volume of {@code 1.0}, so it inherits
   * this emitter's effective volume unchanged until its own volume is changed.
   *
   * @param childName name for the new child emitter
   * @return the newly created child
   */
  public Emitter derive(String childName) {
    Emitter child = new Emitter(childName, this);
    children.add(child);
    return child;
  }

  /**
   * Detaches a direct child from this emitter's hierarchy.
   *
   * <p>After removal, {@code child.parent()} returns {@code null} and the
   * child's {@link #effectiveVolume()} no longer incorporates this emitter's
   * volume. Has no effect if {@code child} is not a direct child.
   *
   * @param child the child emitter to detach
   */
  public void remove(Emitter child) {
    if (children.remove(child)) {
      child.parent = null;
    }
  }

  /**
   * Returns this emitter's local volume, not accounting for ancestors.
   *
   * <p>The initial value is {@code 1.0}. Use {@link #effectiveVolume()} to
   * get the volume that clips actually hear.
   *
   * @return local volume ({@code 0.0} = silent, {@code 1.0} = full, {@code >1.0} = amplification)
   */
  public float volume() {
    return volume;
  }

  /**
   * Sets this emitter's local volume.
   *
   * <p>Values less than {@code 0.0} are clamped to {@code 0.0}.
   * The change takes effect immediately and propagates to all descendants
   * via their {@link #effectiveVolume()} calculations.
   *
   * @param volume new local volume ({@code 0.0} = silent, {@code 1.0} = full,
   *               {@code >1.0} = amplification)
   */
  public void setVolume(float volume) {
    this.volume = Math.max(0.0F, volume);
  }

  /**
   * Returns the effective volume seen by clips that play through this emitter.
   *
   * <p>Computed as the product of this emitter's {@link #volume() local volume}
   * and the effective volumes of all its ancestors, walking up to the root.
   * For a root emitter, this equals the local volume.
   *
   * @return effective volume in the range {@code [0.0, ∞)}
   */
  public float effectiveVolume() {
    return parent == null ? volume : volume * parent.effectiveVolume();
  }

  @Override
  public String toString() {
    return String.format("Emitter[name='%s', volume=%.2f, effective=%.2f, children=%d]", name, volume,
        effectiveVolume(), children.size());
  }
}