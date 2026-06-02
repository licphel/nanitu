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
 * Priority of an event handler, controlling the order in which handlers are called.
 *
 * <p>Handlers with higher priority (lower ordinal) are invoked first. For example,
 * a {@link #HIGHEST} handler runs before a {@link #NORMAL} handler, which runs
 * before a {@link #LOWEST} handler.
 *
 * <p>This mirrors the Minecraft Forge event priority system:
 *
 * <ul>
 *   <li>{@link #HIGHEST} — runs first, for critical interception</li>
 *   <li>{@link #HIGH} — runs early, for overrides</li>
 *   <li>{@link #NORMAL} — the default, suitable for most handlers</li>
 *   <li>{@link #LOW} — runs late, for post-processing</li>
 *   <li>{@link #LOWEST} — runs last, for observation only</li>
 * </ul>
 *
 * @see Subscribe
 * @see EventBus
 */
public enum Priority {
  HIGHEST,
  HIGH,
  NORMAL,
  LOW,
  LOWEST
}
