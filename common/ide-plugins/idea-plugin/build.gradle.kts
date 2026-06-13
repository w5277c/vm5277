plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.7.1"
}

group = "ru.vm5277"
version = "1.2"

repositories {
    mavenCentral()
    mavenLocal()
    maven {
        url = uri("https://vm5277.ru/maven-repo")
    }
    intellijPlatform {
        defaultRepositories()
    }
}

    dependencies {
        implementation("ru.vm5277:lsp_server:+")
        intellijPlatform {
            create("IC", "2025.1.4.1")
            testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
            bundledPlugin("org.jetbrains.idea.maven")
        }
    }

intellijPlatform {
    pluginConfiguration {
        id = "ru.vm5277.idea-plugin"
        name = "vm5277 embedded toolkit plugin"

        ideaVersion {
            sinceBuild = "251"
        }

        changeNotes = """
            Initial version
        """.trimIndent()
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }

    buildPlugin {
        archiveFileName.set("vm5277-idea-plugin-${project.version}.zip")
    }
    patchPluginXml {
        sinceBuild.set("251")
        untilBuild.set("252.*")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}