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

package net.nanitu.memory;

import java.nio.ByteOrder;

/**
 * Byte order for multibyte reads and writes in {@link Buffer} and {@link Memory}.
 *
 * <p>Most serialization formats have a fixed byte order. This enum lets you
 * specify the desired order when reading or writing integers, floats, and other multibyte values. Use {@link #LITTLE}
 * for local persistence and most game formats; use {@link #BIG} for network protocols (TCP/IP, WebSocket, etc.).
 *
 * <p>For operations that don't know their byte order ahead of time — typically
 * network streams where the sender negotiates the order — use {@link #UNSURE}. Most multibyte operations reject
 * {@code UNSURE} with an {@link IllegalArgumentException} to catch missing negotiation early.
 *
 * @see Buffer
 * @see Memory
 */
public enum Endianness {
  /**
   * Little-endian byte order: the least significant byte is stored first.
   *
   * <p>This is the native byte order on x86 and ARM processors. Use this
   * for local file formats, game saves, and most non-network serialization.
   */
  LITTLE,

  /**
   * Big-endian byte order: the most significant byte is stored first.
   *
   * <p>This is the network byte order used by TCP/IP, WebSocket, and many
   * binary network protocols. Also used by Java's default integer encoding.
   */
  BIG,

  /**
   * Sentinel indicating that the byte order is not yet determined.
   *
   * <p>Most multibyte operations throw {@link IllegalArgumentException}
   * when given {@code UNSURE}. Set the actual byte order before reading or writing multibyte values.
   */
  UNSURE;

  /**
   * The native byte order of the current platform.
   *
   * <p>This reflects {@link ByteOrder#nativeOrder()} and is suitable for
   * interop with native libraries. Serialization code should generally specify an explicit byte order rather than
   * relying on this value, as native order varies across platforms.
   */
  public static final Endianness NATIVE = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? LITTLE : BIG;
}
