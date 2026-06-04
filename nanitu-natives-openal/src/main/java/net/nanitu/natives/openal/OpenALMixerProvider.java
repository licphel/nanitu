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

package net.nanitu.natives.openal;

import net.nanitu.audio.Mixer;
import net.nanitu.audio.spi.MixerProvider;
import net.nanitu.util.InternalApi;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.openal.ALC10.*;

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
  /**
   * Creates an OpenAL mixer provider.
   */
  public OpenALMixerProvider() {
  }

  /**
   * Returns whether an OpenAL device and context can be created on this system.
   *
   * <p>Opens a temporary device and context to probe availability,
   * then destroys them immediately regardless of outcome.
   *
   * @return true if OpenAL is available
   */
  @Override
  public boolean isAvailable() {
    long device = 0L;
    long context = 0L;
    try {
      device = alcOpenDevice((ByteBuffer) null);
      if (device == 0L) {
        return false;
      }

      context = alcCreateContext(device, (IntBuffer) null);
      return context != 0L;
    } catch (Throwable e) {
      return false;
    } finally {
      if (context != 0L) {
        alcDestroyContext(context);
      }
      if (device != 0L) {
        alcCloseDevice(device);
      }
    }
  }

  /**
   * Creates a new {@link OpenALMixer} and initializes the OpenAL context.
   *
   * @return a new OpenAL mixer
   */
  @Override
  public Mixer create(String args) {
    return new OpenALMixer();
  }

  @Override
  public String name() {
    return "OpenAL-Mixer-Provider";
  }
}
