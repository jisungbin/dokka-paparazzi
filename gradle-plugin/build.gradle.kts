/*
 * Developed by Ji Sungbin 2024.
 *
 * Licensed under the MIT.
 * Please see full license: https://github.com/jisungbin/dokka-paparazzi/blob/main/LICENSE
 */
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm")
  `kotlin-dsl`
  `java-gradle-plugin`
  id("com.vanniktech.maven.publish")
}

val functionalTest: SourceSet by sourceSets.creating
val functionalTestTask = tasks.register<Test>(functionalTest.name) {
  group = LifecycleBasePlugin.VERIFICATION_GROUP
  testClassesDirs = functionalTest.output.classesDirs
  classpath = functionalTest.runtimeClasspath
  mustRunAfter(tasks.test)
}

gradlePlugin {
  testSourceSets(functionalTest)
  plugins {
    create("dokkaPaparazziGradle") {
      id = "land.sungbin.dokkapaparazzi"
      implementationClass = "land.sungbin.dokkapaparazzi.gradle.DokkaPaparazziGradlePlugin"
    }
  }
}

tasks.check {
  dependsOn(functionalTestTask)
}

val updatePluginVersion = tasks.register<UpdatePluginVersionTask>("updatePluginVersion") {
  version.set(rootProject.properties.getValue("VERSION_NAME") as String)
  destination.set(projectDir.walk().first { path -> path.endsWith("VERSION.kt") })
}

tasks
  .matching { task -> task.name == "sourcesJar" || task.name == "spotlessKotlin" }
  .configureEach { dependsOn(updatePluginVersion) }

tasks.withType<KotlinCompile>().configureEach {
  dependsOn(updatePluginVersion)
}

val dokkaVersion: String by rootProject.properties

dependencies {
  compileOnly(gradleApi())
  compileOnly(projects.dokkapaparazziPlugin)
  compileOnly("org.jetbrains.dokka:dokka-gradle-plugin:$dokkaVersion")
  implementation("com.google.code.gson:gson:2.11.0")

  "functionalTestImplementation"(kotlin("test-junit5"))
  "functionalTestImplementation"(gradleTestKit())
}

abstract class UpdatePluginVersionTask : DefaultTask() {
  @get:Input abstract val version: Property<String>

  @get:InputFile abstract val destination: RegularFileProperty

  @TaskAction fun run() {
    val packageLine = destination.get().asFile.useLines { it.first { line -> line.startsWith("package") } }
    destination.get().asFile.writeText(
      """
      $packageLine

      const val PLUGIN_VERSION = "${version.get()}"
      """.trimIndent(),
    )
  }
}
