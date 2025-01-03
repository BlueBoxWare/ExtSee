import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.RunIdeTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.0.0"
    id("org.jetbrains.intellij.platform") version "2.5.0"
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
        // https://github.com/JetBrains/intellij-platform-gradle-plugin/issues/1638#issuecomment-2226880397
        intellijIdeaCommunity(providers.gradleProperty("platformVersion"), useInstaller = false)
        bundledPlugins(providers.gradleProperty("platformBundledPlugins").map { it.split(',') })
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
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = provider { null }
        }
    }
    pluginVerification {
        ides {
//            ide(IntelliJPlatformType.IntellijIdeaCommunity, providers.gradleProperty("platformVersion").get())
            recommended()
        }
    }
    buildSearchableOptions = false

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

    withType<RunIdeTask> {
        maxHeapSize = "8g"
        systemProperties = mapOf(
            "idea.ProcessCanceledException" to "disabled",
            "idea.is.internal" to "true",
            "idea.kotlin.plugin.use.k2" to "true"
        )
    }


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

    test {
        environment("NO_FS_ROOTS_ACCESS_CHECK", "1")
        systemProperties = mapOf(
            "idea.kotlin.plugin.use.k2" to "true"
        )
    }

}
