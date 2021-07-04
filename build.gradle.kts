import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun properties(key: String) = project.findProperty(key).toString()

plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "1.5.10"
  id("org.jetbrains.intellij") version "1.1.2"
  id("io.gitlab.arturbosch.detekt") version "1.17.1"
}

group = properties("pluginGroup")
version = properties("pluginVersion")

repositories {
  mavenCentral()
}

intellij {
  pluginName.set(properties("pluginName"))
  version.set(properties("platformVersion"))
  type.set(properties("platformType"))
  downloadSources.set(properties("platformDownloadSources").toBoolean())
  updateSinceUntilBuild.set(true)

  plugins.set(properties("platformPlugins").split(',').map(String::trim).filter(String::isNotEmpty))
}

detekt {
  config = files("./detekt-config.yml")
  buildUponDefaultConfig = true

  reports {
    html.enabled = true
    xml.enabled = false
    txt.enabled = false
  }
}

tasks {

  withType<JavaCompile> {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
  }
  withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
  }

  withType<Detekt> {
    jvmTarget = "1.8"
  }

  patchPluginXml {
    version.set(properties("pluginVersion"))
    sinceBuild.set(properties("pluginSinceBuild"))
    untilBuild.set(properties("pluginUntilBuild"))
  }

  runPluginVerifier {
    ideVersions.set(properties("pluginVerifierIdeVersions").split(',').map(String::trim).filter(String::isNotEmpty))
  }
}
