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
 * Mutable context carried alongside an {@link Event} during dispatch.
 *
 * <p>While events are immutable records, the context holds transient dispatch
 * state: whether the event has been {@link #isCanceled() canceled}, the
 * {@link #result() semantic result}, and the current {@link #phase() phase}.
 *
 * <pre>{@code
 * bus.register(DamageEvent.class, (ctx, event) -> {
 *     if (event.amount() > 100) ctx.setCanceled(true);
 *     if (event.amount() < 0) ctx.setResult(Result.DENY);
 * });
 * }</pre>
 *
 * @param <T> the event type
 * @see Event
 * @see EventListener
 * @see EventBus
 */
public final class EventContext<T extends Event> {
  private final T event;
  private final Phase phase;
  private boolean canceled;
  private Result result;

  EventContext(T event, Phase phase) {
    this.event = event;
    this.phase = phase;
    this.canceled = false;
    this.result = Result.DEFAULT;
  }

  /**
   * Returns the event being dispatched.
   *
   * @return the event
   */
  public T event() {
    return event;
  }

  /**
   * Returns the dispatch phase of this event.
   *
   * <p>{@link Phase#NONE} means the event is phase-agnostic.
   *
   * @return the phase
   */
  public Phase phase() {
    return phase;
  }

  /**
   * Returns whether the event has been canceled by a handler.
   *
   * @return {@code true} if canceled
   */
  public boolean isCanceled() {
    return canceled;
  }

  /**
   * Marks the event as canceled or un-canceled.
   *
   * <p>Canceled events continue to propagate to all remaining handlers
   * regardless — canceling does not stop dispatch. Handlers that wish to
   * respect cancellation should check {@link #isCanceled()} and return early.
   *
   * @param canceled {@code true} to cancel, {@code false} to un-cancel
   */
  public void setCanceled(boolean canceled) {
    this.canceled = canceled;
  }

  /**
   * Returns the semantic result of this event.
   *
   * <p>Defaults to {@link Result#DEFAULT} (no opinion).
   *
   * @return the result
   */
  public Result result() {
    return result;
  }

  /**
   * Sets the semantic result of this event.
   *
   * <p>Handlers typically only upgrade the result — from {@link Result#DEFAULT}
   * to {@link Result#ALLOW} or {@link Result#DENY} — so that the most
   * restrictive handler wins.
   *
   * @param result the result; must not be {@code null}
   */
  public void setResult(Result result) {
    this.result = result;
  }
}
