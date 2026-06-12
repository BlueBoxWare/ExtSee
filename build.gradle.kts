import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.3.0"
    id("org.jetbrains.intellij.platform") version "2.16.0"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
        jetbrainsRuntime()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea(providers.gradleProperty("platformVersion")) { useInstaller = false }
        bundledPlugins("org.jetbrains.kotlin", "com.intellij.java")
        jetbrainsRuntime()
        pluginVerifier()
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Java)
    }
    testImplementation("junit:junit:4.13.2")
}

kotlin {
    jvmToolchain(21)
}

intellijPlatform {
    pluginConfiguration {
        name = providers.gradleProperty("pluginName")
        ideaVersion {
            sinceBuild = providers.gradleProperty("platformVersion")
            untilBuild = provider { null }
        }
    }
    pluginVerification {
        ides {
            create(IntelliJPlatformType.IntellijIdea, providers.gradleProperty("platformVersion").get())
//            recommended()
        }
    }

}

intellijPlatformTesting {
    runIde {
        register("runIdeForPerformance") {

            plugins {
                plugin("com.google.ide-perf:1.3.2")
            }

        }
    }
}

sourceSets {
    main {
        resources.srcDir("resources")
    }
}

tasks {

    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }

    withType<KotlinCompile> {
        compilerOptions {
            freeCompilerArgs.add("-Xcontext-receivers")
        }
    }

    named<KotlinCompile>("compileTestKotlin") {
        compilerOptions {
            freeCompilerArgs.add("-Xcontext-receivers")
        }
    }

    runIde {
        maxHeapSize = "8g"
        systemProperties = mapOf(
            "idea.ProcessCanceledException" to "disabled",
            "idea.is.internal" to "true",
            "idea.kotlin.plugin.use.k1" to "false"
        )
    }

    test {
        environment("NO_FS_ROOTS_ACCESS_CHECK", "1")
        jvmArgumentProviders += CommandLineArgumentProvider {
            listOf("-Didea.kotlin.plugin.use.k1=false")
        }

    }

}
