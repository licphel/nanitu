<div style="text-align: center;">

# Nanitu

[![License](https://img.shields.io/badge/License-MIT-2563eb?style=for-the-badge&logo=openSourceInitiative&logoColor=white)](./LICENSE)
[![Java](https://img.shields.io/badge/Java-25%2B-f59e0b?style=for-the-badge&logo=openjdk&logoColor=white)](https://jdk.java.net/25/)
[![Gradle](https://img.shields.io/badge/Gradle-9.x-02303a?style=for-the-badge&logo=gradle&logoColor=white)](https://gradle.org)

A modular application framework for Java — 3D graphics, spatial audio, and
LLM integration under a unified, backend-agnostic API.

</div>

## Prerequisites

- JDK 25 or later
- Gradle 9.x (wrapper included)

## Building

```bash
./gradlew build
```

All modules compile with `-Xlint:all -Werror`. Zero warnings in the main
source tree.

## Modules

### `nanitu-core`

The foundation. Zero runtime dependencies beyond the Jspecify nullness
annotations (`org.jspecify:jspecify`).

| Package               | Description                                                                                                                                                                                        |
|-----------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `net.nanitu.math`     | Full linear-algebra suite — `Matrix4x4`, `Matrix3x3`, `Vector2/3/4`, `Quaternion`, `Color` (RGBA float), `Box2`/`Box3`, `Half` (IEEE 754 half-precision), 2D/3D perspective cameras                |
| `net.nanitu.event`    | Thread-safe event bus. Listeners subscribe with `@Subscribe`, can specify priority and intercept lifecycle phases (`PRE`, `MAIN`, `POST`). Returns `Result.PASS`/`Result.CANCEL` for chain control |
| `net.nanitu.util`     | `Service<T>` — type-safe `ServiceLoader` wrapper with `get()`/`collect()`; `@Internal` marker for implementation classes hidden from the public API                                                |
| `net.nanitu.resource` | `ResourceFinder` — resolves paths relative to the application root regardless of execution environment (IDE, fat-jar, native image)                                                                |

### `nanitu-audio`

Spatial audio with automatic format detection.

- **Core**: `Mixer` (device), `Clip` (in-memory PCM buffer), `Emitter` (3D positional sound), `ClipManager` (
  auto-recycle stopped clips)
- **Format**: `AudioInputStream.open(InputStream)` probes the container header and returns the appropriate decoder.
  Currently, it supports RIFF/WAVE in all common PCM variants (signed/unsigned integer, 32-bit float, extensible format)
- **Backend**: LWJGL/OpenAL. All AL/ALC calls run on a single daemon thread; callers enqueue work via
  `Mixer.submit(Runnable)`

### `nanitu-graphics`

Low-level 3D rendering with a Vulkan-compatible resource-binding model.

- **Core**: `Device` (GPU), `Surface` (window), `Encoder` (command buffer), `BufferObject` (VBO/IBO/UBO), `Texture` (
  1D/2D/3D), `Sampler`, `ShaderModule`/`ShaderProgram`, `RenderPipe` (immutable pipeline state), `RenderTarget` (
  FBO/swapchain), `ResourceSet` (descriptor bindings)
- **Descriptor model**: textures and uniform buffers share a unified slot namespace — compatible with Vulkan's
  `VkDescriptorSet` semantics. OpenGL binding-point assignment is handled internally at apply-time
- **Image I/O**: `ImageInputStream.open(InputStream)` detects PNG and returns decoded RGBA pixel data as a raw byte
  stream. Additional formats added via subclasses
- **Backend**: LWJGL/OpenGL 3.3 Core Profile + GLFW. Single-threaded command-buffer execution model — `Encoder` records,
  `Device.execute()` dispatches
- **Windowing**: GLFW surfaces expose an opaque `procAddress()` (a `Runnable` that binds the GL context). The graphics
  backend never imports GLFW types — this enables drop-in replacement with SDL or other windowing libraries

### `nanitu-ai`

LLM abstraction layer.

- `Model` interface for chat backends (OpenAI, Anthropic, local models)
- `ChatMessage`, `ChatRequest`/`ChatResponse`, `Tool` definitions for function calling
- `StreamHandler` for token-by-token streaming responses
- Backends discovered via `ModelProvider` SPI

## Future

| Module           | Description                                                                                                                                                      |
|------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `nanitu-network` | Packet-based network transport with session management. Protocol-agnostic — suitable for game networking and real-time communication                             |
| `nanitu-fastgfx` | High-level 2D graphics API (shapes, text, images, transformations). Convenience layer over `nanitu-graphics` for applications that don't need a full 3D pipeline |
| `nanitu-ui`      | 2D GUI toolkit — widgets, layout managers, event dispatch. Built on `nanitu-fastgfx`                                                                             |
