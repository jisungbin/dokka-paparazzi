/*
 * Developed by Ji Sungbin 2024.
 *
 * Licensed under the MIT.
 * Please see full license: https://github.com/jisungbin/dokka-paparazzi/blob/main/LICENSE
 */

import com.diffplug.gradle.spotless.BaseKotlinExtension
import com.diffplug.gradle.spotless.SpotlessExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "2.0.0"
  id("com.diffplug.spotless") version "6.25.0"
  id("com.vanniktech.maven.publish") version "0.29.0" apply false
  idea
}

idea {
  module {
    excludeDirs = excludeDirs + allprojects.map { it.file(".kotlin") }
  }
}

allprojects {
  apply {
    plugin("com.diffplug.spotless")
  }

  extensions.configure<SpotlessExtension> {
    fun BaseKotlinExtension.useKtlint() {
      ktlint("1.3.1").editorConfigOverride(
        mapOf(
          "indent_size" to "2",
          "ktlint_standard_filename" to "disabled",
          "ktlint_standard_package-name" to "disabled",
          "ktlint_standard_function-naming" to "disabled",
          "ktlint_standard_property-naming" to "disabled",
          "ktlint_standard_backing-property-naming" to "disabled",
          "ktlint_standard_class-signature" to "disabled",
          "ktlint_standard_import-ordering" to "disabled",
          "ktlint_standard_max-line-length" to "disabled",
          "ktlint_standard_annotation" to "disabled",
          "ktlint_standard_multiline-if-else" to "disabled",
          "ktlint_standard_value-argument-comment" to "disabled",
          "ktlint_standard_value-parameter-comment" to "disabled",
          "ktlint_standard_comment-wrapping" to "disabled",
        ),
      )
    }

    kotlin {
      target("**/*.kt")
      targetExclude("**/build/**/*.kt", "spotless/*.kt")
      useKtlint()
      licenseHeaderFile(rootProject.file("spotless/copyright.kt"))
    }
    kotlinGradle {
      target("**/*.kts")
      targetExclude("**/build/**/*.kts", "spotless/*.kts")
      useKtlint()
      // Look for the first line that doesn't have a block comment (assumed to be the license)
      licenseHeaderFile(rootProject.file("spotless/copyright.kts"), "(^(?![\\/ ]\\*).*$)")
    }
    format("xml") {
      target("**/*.xml")
      targetExclude("**/build/**/*.xml", "spotless/*.xml")
      // Look for the first XML tag that isn't a comment (<!--) or the xml declaration (<?xml)
      licenseHeaderFile(rootProject.file("spotless/copyright.xml"), "(<[^!?])")
    }
  }

  tasks.withType<KotlinCompile> {
    compilerOptions {
      jvmTarget = JvmTarget.JVM_17
      optIn.addAll(
        "kotlin.OptIn",
        "kotlin.RequiresOptIn",
        "kotlin.contracts.ExperimentalContracts",
      )
    }
  }
}

subprojects {
  tasks.withType<Test> {
    useJUnitPlatform()
    outputs.upToDateWhen { false }
  }
}

tasks.register<Delete>("cleanAll") {
  delete(*allprojects.map { project -> project.layout.buildDirectory }.toTypedArray())
}
