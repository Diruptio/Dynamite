plugins {
    id("com.diffplug.spotless") version "6.25.0"
    id("java")
}

group = "diruptio"
version = "2.0.1"

repositories {
    mavenCentral()
    maven("https://repo.diruptio.de/repository/maven-public")
}

dependencies {
    compileOnly("diruptio:Spikedog:2.0.0-beta.9")
    compileOnly("org.jetbrains:annotations:26.0.1")
}

spotless {
    kotlinGradle {
        target("**/*.gradle.kts")
        ktlint()
        endWithNewline()
    }
    java {
        target("**/src/**/*.java")
        palantirJavaFormat("2.50.0").formatJavadoc(true)
        removeUnusedImports()
        indentWithSpaces()
        endWithNewline()
    }
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release = 21
    }

    jar {
        archiveFileName = "Dynamite.jar"
    }
}
