package land.sungbin.dokkapaparazzi.gradle

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.notExists
import kotlin.io.path.writeText
import kotlin.random.Random
import kotlin.test.Test
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach

class DokkaPaparazziGradlePluginTest {
  // @field:TempDir
  lateinit var projectDir: Path

  private lateinit var moduleDir: Path

  private lateinit var projectSettingsFile: Path
  private lateinit var projectBuildFile: Path
  private lateinit var moduleBuildFile: Path

  @BeforeEach fun setup() {
    projectDir = Path.of("/Users/jisungbin/IdeaProjects/test").resolve(Random.nextInt().toString())

    projectSettingsFile = projectDir.resolve("settings.gradle.kts").ensureFileCreated()
    projectBuildFile = projectDir.resolve("build.gradle.kts").ensureFileCreated()
    moduleDir = projectDir.resolve("test").createDirectory()
    moduleBuildFile = moduleDir.resolve("build.gradle.kts").ensureFileCreated()

    projectBuildFile.writeText(
      """
      plugins {
        kotlin("jvm") version "2.0.0"
      }
      """.trimIndent(),
    )
  }

  @Test fun withoutDokkaPluginThenErrors() {
    touchSettingsFile(projectName = "withoutDokkaPluginThenErrors")
    moduleBuildFile.writeText(
      """
      plugins {
        kotlin("jvm")
        ${dokkaPaparazziPlugin()}
      }
      """.trimIndent(),
    )

    val result = runner().withArguments(":test:dokkaHtml").buildAndFail()

    assertThat(result.output).all {
      contains("Failed to apply plugin 'land.sungbin.dokka-paparazzi'")
      contains("Dokka plugin is not applied")
    }
  }

  @Test fun snapshotDirShouldPresentWhenRunDokkaTask() {
    touchSettingsFile(projectName = "snapshotDirShouldPresentWhenRunDokkaTask")
    moduleBuildFile.writeText(
      """
      plugins {
        kotlin("jvm")
        ${dokkaPlugin()}
        ${dokkaPaparazziPlugin()}
      }
      
      dokkaPaparazzi {
        // Nothing to do  
      }
      """.trimIndent(),
    )

    runner().withArguments(":test:build").build()
    val result = runner().withArguments(":test:dokkaHtml").buildAndFail()

    assertThat(result.output).all {
      contains("Failed to calculate the value of task ':test:dokkaHtml' property 'pluginsMapConfiguration'")
      contains("'snapshotDir' is not set")
    }
  }

  @Test fun snapshotDirWithDokkaPluginThenWorksFine() {
    touchSettingsFile(projectName = "snapshotDirWithDokkaPluginThenBuildSuccess")
    moduleBuildFile.writeText(
      """
      plugins {
        kotlin("jvm")
        ${dokkaPlugin()}
        ${dokkaPaparazziPlugin()}
      }
      
      dokkaPaparazzi {
        snapshotDir = projectDir.resolve("snapshots")
      }
      """.trimIndent(),
    )

    moduleDir.resolve("snapshots").createDirectory()
    moduleDir
      .resolve("src/main/kotlin").also(Path::createDirectories)
      .resolve("Hello.kt").also(Path::createFile)
      .writeText(
        """
        /**
         * Hello, Dokka!
         */
        fun hello() = Unit
        """.trimIndent(),
      )

    val result = runner().withArguments(":test:dokkaHtml").build()
    println(result.output)
  }

  private fun runner(): GradleRunner =
    GradleRunner.create()
      .withProjectDir(projectDir.toFile())
      .withPluginClasspath()

  private fun dokkaPlugin() = """id("org.jetbrains.dokka") version "1.9.20""""
  private fun dokkaPaparazziPlugin() = """id("land.sungbin.dokka-paparazzi") version "0.1.0""""

  private fun touchSettingsFile(projectName: String) {
    val code = """
      rootProject.name = "$projectName"
      
      pluginManagement {
        repositories {
          mavenCentral()
          gradlePluginPortal()
          mavenLocal()
        }
      }

      dependencyResolutionManagement {
        repositories {
          mavenCentral()
          mavenLocal()
        }
      }
      
      include(":test")
    """.trimIndent()
    projectSettingsFile.writeText(code)
  }

  private fun Path.ensureFileCreated(): Path = apply {
    // TODO why Path.createParentDirectories() is not available?
    if (parent?.notExists() == true) parent!!.createDirectories()
    if (notExists()) createFile()
  }
}