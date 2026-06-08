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

package net.nanitu.audio.openal;

import net.nanitu.audio.Mixer;
import net.nanitu.audio.spi.MixerProvider;
import net.nanitu.util.InternalApi;

/**
 * {@link MixerProvider} backed by OpenAL (via LWJGL).
 *
 * <p>Loaded automatically by {@link java.util.ServiceLoader} from
 * {@code META-INF/services/net.nanitu.audio.spi.MixerProvider}. Callers should prefer using {@code ServiceLoader} or a
 * DI container rather than constructing this class directly.
 *
 * <p>{@link #isAvailable()} opens a temporary OpenAL device and context to
 * verify that LWJGL natives are present and a device can be obtained on the current system, then tears it down
 * immediately.
 */
@InternalApi
public final class OpenALMixerProvider implements MixerProvider {
  @Override
  public boolean isAvailable() {
    return true;
  }

  @Override
  public String name() {
    return "OpenAL-Mixer-Provider";
  }

  @Override
  public Mixer create() {
    return new OpenALMixer();
  }
}
