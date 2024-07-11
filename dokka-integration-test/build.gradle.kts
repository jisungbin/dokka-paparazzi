/*
 * Developed by Ji Sungbin 2024.
 *
 * Licensed under the MIT.
 * Please see full license: https://github.com/jisungbin/dokka-paparazzi/blob/main/LICENSE
 */
plugins {
  kotlin("jvm")
  id("org.jetbrains.dokka") version "1.9.20"
}

val removeOldOutputTask = tasks.create<Delete>("removeOldOutput") {
  delete(file("output"))
}

tasks.dokkaHtml {
  dependsOn(removeOldOutputTask)
  outputDirectory = projectDir.resolve("output")
  pluginsMapConfiguration = mapOf(
    "land.sungbin.dokkapaparazzi.DokkaPaparazziPlugin" to """
      |{
      |  "snapshotImageDir": "${projectDir.resolve("snapshots")}"
      |}
    """.trimMargin(),
  )
}

dependencies {
  dokkaPlugin(projects.dokkapaparazziPlugin)
}
