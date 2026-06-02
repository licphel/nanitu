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

package net.nanitu.audio.spi;

import net.nanitu.audio.Mixer;
import net.nanitu.util.Service;

/**
 * Service interface for discovering and instantiating audio backend implementations.
 *
 * <p>Implementations are loaded via {@link java.util.ServiceLoader} and represent
 * concrete audio backends such as OpenAL, DirectSound, or CoreAudio.
 * Each implementation should register itself in
 * {@code META-INF/services/net.nanitu.audio.spi.MixerProvider}.
 *
 * <p>Before calling {@link #create(String)}, check {@link #isAvailable()} to verify
 * that the backend's native libraries are present and a device can be opened
 * on the current system.
 *
 * @see Mixer
 */
public interface MixerProvider extends Service<Mixer> {
}
