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

package net.nanitu.event;

/**
 * Lifecycle phase of an event, used to post the same event type at different
 * points in processing.
 *
 * <p>Inspired by Minecraft Forge's {@code TickEvent.Phase}, this allows one
 * event class to be posted twice — once before processing ({@link #START})
 * and once after ({@link #END}). Handlers can filter by phase via
 * {@link Subscribe#phase()} or inspect {@link EventContext#phase()} at runtime.
 *
 * <p>{@link #NONE} is the default for events that do not use phases.
 *
 * @see EventContext
 * @see Subscribe
 */
public enum Phase {
  /**
   * No phase — the event is phase-agnostic.
   */
  NONE,
  /**
   * Before the action is performed.
   */
  START,
  /**
   * After the action is completed.
   */
  END;

  /**
   * Returns whether this phase filter matches the given dispatch phase.
   *
   * <p>{@link #NONE} matches all phases; other values match only themselves.
   *
   * @param other the dispatch phase
   * @return {@code true} if this filter accepts the phase
   */
  boolean matches(Phase other) {
    return this == NONE || this == other;
  }
}
