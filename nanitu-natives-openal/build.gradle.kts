dependencies {
    implementation(project(":nanitu-core"))
    implementation(project(":nanitu-audio"))

    // LWJGL OpenGL
    implementation(platform("org.lwjgl:lwjgl-bom:3.4.1"))
    implementation("org.lwjgl:lwjgl")
    implementation("org.lwjgl:lwjgl-openal")

    runtimeOnly("org.lwjgl:lwjgl::natives-windows")
    runtimeOnly("org.lwjgl:lwjgl::natives-macos")
    runtimeOnly("org.lwjgl:lwjgl::natives-linux")
    runtimeOnly("org.lwjgl:lwjgl-openal::natives-windows")
    runtimeOnly("org.lwjgl:lwjgl-openal::natives-macos")
    runtimeOnly("org.lwjgl:lwjgl-openal::natives-linux")
}
