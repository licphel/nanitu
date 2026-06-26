# Nanitu

[![License](https://img.shields.io/badge/License-MIT-2563eb?style=for-the-badge)](./LICENSE)
[![Java](https://img.shields.io/badge/Java-25%2B-f59e0b?style=for-the-badge&logo=openjdk&logoColor=white)](https://jdk.java.net/25/)
[![Gradle](https://img.shields.io/badge/Gradle-9.x-02303a?style=for-the-badge&logo=gradle&logoColor=white)](https://gradle.org)

A modular, backend-agnostic application framework for Java — 2D/3D graphics,
spatial audio, text rendering, UI toolkit, and LLM integration under a unified API.

---

## Architecture

Nanitu is organized as a **layered stack**. Each layer depends only on the one
below it — you pull in exactly what your application needs.

**Backend implementations** plug in at the bottom of each abstraction:

| Abstraction        | Backends          |
|--------------------|-------------------|
| Graphics           | `nanitu-opengl`   |
| Windowing & input  | `nanitu-glfw`     |
| Audio              | `nanitu-openal`   |
| Text shaping       | `nanitu-harfbuzz` |
| Font rasterization | `nanitu-freetype` |

Backends are loaded via `ServiceLoader` — swap OpenGL for Vulkan or GLFW for SDL
without changing a line of application code.

### Module map

| Group          | Artifact          | Role                                                                                             |
|----------------|-------------------|--------------------------------------------------------------------------------------------------|
| **Foundation** | `nanitu-core`     | Math (`Vector/Matrix/Quaternion/Color`), event bus, resource I/O, NBT data format, SPI utilities |
| **Graphics**   | `nanitu-gfx`      | GPU abstraction: shaders, buffers, textures, pipelines, command encoding, render targets         |
| **Audio**      | `nanitu-audio`    | Spatial audio: mixer, emitters, clips, auto-format detection                                     |
| **AI**         | `nanitu-ai`       | LLM abstraction: chat, streaming, tools / function calling, model-provider SPI                   |
| **Text**       | `nanitu-text`     | Text layout, shaping, bidi, line breaking, glyph rasterization API                               |
| **Sprite**     | `nanitu-sprite`   | 2D batch rendering: multi-meshes, nine-patches, drawables                                        |
| **UI**         | `nanitu-ui`       | Widget toolkit: panels, buttons, windows, pluggable look-and-feel themes                         |
| **Backend**    | `nanitu-opengl`   | OpenGL 3.3 Core renderer                                                                         |
| **Backend**    | `nanitu-glfw`     | GLFW window surface & input                                                                      |
| **Backend**    | `nanitu-openal`   | OpenAL audio device                                                                              |
| **Backend**    | `nanitu-harfbuzz` | HarfBuzz text shaper                                                                             |
| **Backend**    | `nanitu-freetype` | FreeType font rasterizer                                                                         |

---

## Quick Start

### Prerequisites

- **JDK 25** or later
- Gradle wrapper is included — no global Gradle installation needed

### Add Nanitu to your project

All artifacts are published under group **`net.nanitu`** with version
**`1.0.0`**.

**Step 1 — Add the repository.** Nanitu snapshots are published to a local
Maven repository. Clone and publish to your local machine first:

```bash
git clone https://github.com/licphel/nanitu.git
cd nanitu
./gradlew publishAllPublicationsToLocalRepository
```

This publishes every module to `build/repo` under the root project.

**Step 2 — Point your project at the repo.** In your `settings.gradle` or
`build.gradle`:

```groovy
repositories {
    mavenCentral()
    maven {
        name = 'NanituLocal'
        url = 'file:///path/to/nanitu/build/repo'
    }
}
```

**Step 3 — Declare dependencies.** Pick the modules you need:

```groovy
dependencies {
    // Foundation — always needed
    implementation 'net.nanitu:nanitu-core:1.0.0'

    // Graphics + a backend
    implementation 'net.nanitu:nanitu-gfx:1.0.0'
    runtimeOnly    'net.nanitu:nanitu-opengl:1.0.0'
    runtimeOnly    'net.nanitu:nanitu-glfw:1.0.0'

    // Text rendering
    implementation 'net.nanitu:nanitu-text:1.0.0'
    runtimeOnly    'net.nanitu:nanitu-harfbuzz:1.0.0'
    runtimeOnly    'net.nanitu:nanitu-freetype:1.0.0'

    // Audio
    implementation 'net.nanitu:nanitu-audio:1.0.0'
    runtimeOnly    'net.nanitu:nanitu-openal:1.0.0'

    // 2D sprite / UI
    implementation 'net.nanitu:nanitu-sprite:1.0.0'
    implementation 'net.nanitu:nanitu-ui:1.0.0'

    // LLM integration
    implementation 'net.nanitu:nanitu-ai:1.0.0'
}
```

> **Key convention:** API modules (`-gfx`, `-audio`, `-text`) go on the
> `implementation` classpath; backend modules (`-opengl`, `-glfw`, `-openal`,
> `-harfbuzz`, `-freetype`) go on `runtimeOnly` — they are discovered via
> `ServiceLoader` and your code never imports them directly.

---

## Building from source

```bash
./gradlew build
```

All modules compile with `-Xlint:all -Werror`. Zero warnings in the main
source tree.

### Useful Gradle tasks

| Task                                                | What it does                          |
|-----------------------------------------------------|---------------------------------------|
| `./gradlew build`                                   | Compile + test all modules            |
| `./gradlew publishAllPublicationsToLocalRepository` | Publish all artifacts to `build/repo` |
| `./gradlew publishToMavenLocal`                     | Publish to `~/.m2/repository`         |

---

## License

MIT — see [LICENSE](./LICENSE).
