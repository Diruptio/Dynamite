plugins {
    id("com.diffplug.spotless") version "6.25.0"
    id("java")
}
apply(from = "spikedev.gradle.kts")

group = "diruptio"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.diruptio.de/repository/maven-public/")
}

dependencies {
    compileOnly("diruptio:Spikedog:1.2.7")
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
        palantirJavaFormat("2.48.0").formatJavadoc(true)
        removeUnusedImports()
        indentWithSpaces()
        endWithNewline()
    }
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(17)
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release = 17
    }

    jar {
        archiveFileName = "Dynamite.jar"
    }
}
