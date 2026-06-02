group = "net.nanitu"
version = "1.0-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }

    dependencies {
        // JUnit
        "testImplementation"(platform("org.junit:junit-bom:5.10.0"))
        "testImplementation"("org.junit.jupiter:junit-jupiter")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")

        // JSpecify
        "implementation"("org.jspecify:jspecify:1.0.0")

        // Log4j
        "implementation"("org.apache.logging.log4j:log4j-core:2.25.4")
        "implementation"("org.apache.logging.log4j:log4j-api:2.25.4")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-Xlint:all")
        options.compilerArgs.add("-Werror")
    }
}