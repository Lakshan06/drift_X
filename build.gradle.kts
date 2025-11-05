// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.13.0" apply false
    id("com.android.library") version "8.13.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
}

// Force Java 17 for all projects to fix "Unsupported class file major version 69" error
allprojects {
    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
