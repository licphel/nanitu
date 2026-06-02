rootProject.name = "nanitu"

include("nanitu-core")
include("nanitu-audio")
include("nanitu-graphics")
include("nanitu-ai")

include("nanitu-natives-openal")
include("nanitu-natives-opengl")
include("nanitu-natives-glfw")

/*
 * Comment this line after pulling.
 * Nanitu-test is excluded because it's a personal test module.
 */
include("nanitu-test")
