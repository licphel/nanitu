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

/**
 * Immutable descriptor for a PCM audio stream.
 *
 * <p>Captures every property needed to interpret raw PCM bytes: encoding,
 * sample rate, bits depth, channel count, frame size, and byte order.
 * All arithmetic conversion helpers ({@link #bytesToFrames}, {@link #secondsToBytes},
 * etc.) are derived from these fields and carry no state of their own.
 *
 * <p>Use the convenience factory methods for the common cases:
 * <pre>{@code
 * AudioFormat mono   = AudioFormat.mono(44100, 16);   // 44.1 kHz, 16-bit, mono
 * AudioFormat stereo = AudioFormat.stereo(48000, 16); // 48 kHz, 16-bit, stereo
 * }</pre>
 *
 * @param encoding         sample encoding (e.g. {@link Encoding#PCM_SIGNED})
 * @param sampleRate       number of samples per second per channel, must be &gt; 0
 * @param sampleSizeInBits bits per sample, must be a positive multiple of 8
 * @param channels         number of interleaved audio channels, must be &gt; 0
 * @param frameSize        bytes per frame ({@code channels × sampleSizeInBits / 8}), must be &gt; 0
 * @param bigEndian        {@code true} for big-endian sample byte order
 * @param signed           {@code true} if samples use a signed representation
 */
public record AudioFormat(Encoding encoding, int sampleRate, int sampleSizeInBits, int channels, int frameSize,
                          boolean bigEndian, boolean signed) {
  /**
   * Creates an AudioFormat, validating constructor parameters.
   *
   * @param encoding         sample encoding (e.g. {@link Encoding#PCM_SIGNED})
   * @param sampleRate       number of samples per second per channel, must be &gt; 0
   * @param sampleSizeInBits bits per sample, must be a positive multiple of 8
   * @param channels         number of interleaved audio channels, must be &gt; 0
   * @param frameSize        bytes per frame ({@code channels × sampleSizeInBits / 8}), must be &gt;
   *                         0
   * @param bigEndian        {@code true} for big-endian sample byte order
   * @param signed           {@code true} if samples use a signed representation
   * @throws IllegalArgumentException if {@code sampleRate}, {@code sampleSizeInBits},
   *                                  {@code channels}, or {@code frameSize} are out of range, or
   *                                  if
   *                                  {@code frameSize} does not equal
   *                                  {@code channels * (sampleSizeInBits / 8)}
   */
  public AudioFormat {
    if (sampleRate <= 0) {
      throw new IllegalArgumentException("sampleRate must be a positive integer");
    }
    if (sampleSizeInBits <= 0 || sampleSizeInBits % 8 != 0) {
      throw new IllegalArgumentException("sampleSizeInBits must be a positive multiple of 8");
    }
    if (channels <= 0) {
      throw new IllegalArgumentException("channels must be a positive integer");
    }
    if (frameSize <= 0) {
      throw new IllegalArgumentException("frameSize must be a positive integer");
    }
    if (channels * (sampleSizeInBits / 8) != frameSize) {
      throw new IllegalArgumentException("frameSize does not match with other args");
    }
  }

  /**
   * Creates an AudioFormat with an explicit {@code signed} flag.
   *
   * <p>Prefer the shorter constructor that derives {@code signed} from
   * {@code encoding}, or use the factory methods {@link #mono} / {@link #stereo}
   * for the most common cases.
   *
   * @param encoding         sample encoding
   * @param sampleRate       samples per second per channel
   * @param sampleSizeInBits bits per sample
   * @param channels         number of interleaved channels
   * @param frameSize        bytes per frame
   * @param bigEndian        {@code true} for big-endian byte order
   */
  public AudioFormat(Encoding encoding, int sampleRate, int sampleSizeInBits, int channels, int frameSize,
                     boolean bigEndian) {
    this(encoding, sampleRate, sampleSizeInBits, channels, frameSize, bigEndian, encoding != Encoding.PCM_UNSIGNED);
  }

  /**
   * Creates a PCM format, deriving {@code frameSize} automatically.
   *
   * <p>{@code frameSize} is computed as {@code (sampleSizeInBits + 7) / 8 * channels}.
   * The {@code encoding} is set to {@link Encoding#PCM_SIGNED} or
   * {@link Encoding#PCM_UNSIGNED} based on {@code signed}.
   *
   * @param sampleRate       samples per second per channel
   * @param sampleSizeInBits bits per sample
   * @param channels         number of interleaved channels
   * @param signed           {@code true} for signed samples
   * @param bigEndian        {@code true} for big-endian byte order
   */
  public AudioFormat(int sampleRate, int sampleSizeInBits, int channels, boolean signed, boolean bigEndian) {
    this(signed ? Encoding.PCM_SIGNED : Encoding.PCM_UNSIGNED, sampleRate, sampleSizeInBits, channels,
        (sampleSizeInBits + 7) / 8 * channels, bigEndian, signed);
  }

  /**
   * Creates a signed, little-endian, mono PCM format.
   *
   * <p>Equivalent to {@code new AudioFormat(sampleRate, sampleSizeInBits, 1, true, false)}.
   *
   * @param sampleRate       samples per second
   * @param sampleSizeInBits bits per sample (e.g. 8, 16, 24, 32)
   * @return a mono {@link Encoding#PCM_SIGNED} format
   */
  public static AudioFormat mono(int sampleRate, int sampleSizeInBits) {
    return new AudioFormat(sampleRate, sampleSizeInBits, 1, true, false);
  }

  /**
   * Creates a signed, little-endian, stereo PCM format.
   *
   * <p>Equivalent to {@code new AudioFormat(sampleRate, sampleSizeInBits, 2, true, false)}.
   *
   * @param sampleRate       samples per second
   * @param sampleSizeInBits bits per sample (e.g. 8, 16, 24, 32)
   * @return a stereo {@link Encoding#PCM_SIGNED} format
   */
  public static AudioFormat stereo(int sampleRate, int sampleSizeInBits) {
    return new AudioFormat(sampleRate, sampleSizeInBits, 2, true, false);
  }

  /**
   * Returns the number of bytes transferred per second ({@code sampleRate × frameSize}).
   *
   * <p>Useful for pre-allocating read buffers and estimating playback duration.
   *
   * @return byte rate
   */
  public int getByteRate() {
    return sampleRate * frameSize;
  }

  /**
   * Converts a byte count to a frame count.
   *
   * <p>Truncates toward zero; any trailing partial frame is ignored.
   *
   * @param bytes number of PCM bytes
   * @return number of complete frames contained in {@code bytes}
   */
  public int bytesToFrames(int bytes) {
    return frameSize == 0 ? 0 : bytes / frameSize;
  }

  /**
   * Converts a frame count to a byte count.
   *
   * @param frames number of audio frames
   * @return number of bytes required to hold {@code frames} frames
   */
  public int framesToBytes(int frames) {
    return frames * frameSize;
  }

  /**
   * Converts a duration in seconds to a byte offset.
   *
   * <p>The result is truncated toward zero. Suitable for seeking or
   * pre-allocating read buffers.
   *
   * @param seconds duration in seconds (maybe fractional)
   * @return byte offset corresponding to {@code seconds}
   */
  public int secondsToBytes(double seconds) {
    return (int) (seconds * getByteRate());
  }

  /**
   * Converts a byte offset to a duration in seconds.
   *
   * @param bytes byte offset in the PCM data
   * @return corresponding duration in seconds
   */
  public double bytesToSeconds(int bytes) {
    return bytes / (double) getByteRate();
  }

  /**
   * Converts a duration in seconds to a frame count.
   *
   * <p>The result is truncated toward zero.
   *
   * @param seconds duration in seconds (maybe fractional)
   * @return number of frames corresponding to {@code seconds}
   */
  public int secondsToFrames(double seconds) {
    return (int) (seconds * sampleRate);
  }

  /**
   * Converts a frame count to a duration in seconds.
   *
   * @param frames number of audio frames
   * @return corresponding duration in seconds
   */
  public double framesToSeconds(int frames) {
    return frames / (double) sampleRate;
  }

  @Override
  public String toString() {
    return String.format("%s %.1f kHz, %d-bit, %dch, %s", encoding, sampleRate / 1000.0, sampleSizeInBits, channels,
        bigEndian ? "BE" : "LE");
  }
}