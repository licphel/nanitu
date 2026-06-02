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
 * Three-state result carried by an {@link EventContext}.
 *
 * <p>Unlike the binary {@link EventContext#isCanceled() canceled} flag, this
 * captures semantic intent:
 *
 * <ul>
 *   <li>{@link #DENY} — explicitly disallow the action</li>
 *   <li>{@link #DEFAULT} — no opinion, let subsequent handlers or default logic decide</li>
 *   <li>{@link #ALLOW} — explicitly allow the action</li>
 * </ul>
 *
 * <p>Typical usage pattern: every handler checks the current result and only
 * upgrades it — e.g. from {@code DEFAULT} to {@code DENY}. This way the
 * most-restrictive handler wins.
 *
 * <pre>{@code
 * bus.register(SpawnEvent.class, (ctx, event) -> {
 *     if (ctx.result() != Result.DENY && isTooCrowded(event.pos())) {
 *         ctx.setResult(Result.DENY);
 *     }
 * });
 * }</pre>
 *
 * @see EventContext
 */
public enum Result {
  /**
   * Disallow the action.
   */
  DENY,
  /**
   * No opinion — use the default behavior.
   */
  DEFAULT,
  /**
   * Explicitly allow the action.
   */
  ALLOW
}
