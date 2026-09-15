plugins {
    java
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = "cage433.intellij"
version = "0.1.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaUltimate("262.10315.125")
        bundledPlugin("com.intellij.java")
        plugin("org.intellij.scala", "2026.2.19")
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "262"
            untilBuild = provider { null }
        }
    }
}
