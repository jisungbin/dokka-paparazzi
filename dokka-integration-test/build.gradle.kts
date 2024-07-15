/*
 * Developed by Ji Sungbin 2024.
 *
 * Licensed under the MIT.
 * Please see full license: https://github.com/jisungbin/dokka-paparazzi/blob/main/LICENSE
 */

plugins {
  kotlin("jvm")

  // TODO
  //  val dokkaVersion: String by rootProject.properties
  id("org.jetbrains.dokka") version "1.9.20"
  id("land.sungbin.dokka-paparazzi") version "0.1.0"
}

val removeOldOutputTask = tasks.create<Delete>("removeOldOutput") {
  delete(file("output"))
}

tasks.dokkaHtml {
  dependsOn(removeOldOutputTask)
  outputDirectory = projectDir.resolve("output")
}

dokkaPaparazzi {
  snapshotDir = projectDir.resolve("snapshots")
}
