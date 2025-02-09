/*
 * Developed by Ji Sungbin 2024.
 *
 * Licensed under the MIT.
 * Please see full license: https://github.com/jisungbin/dokka-paparazzi/blob/main/LICENSE
 */

package land.sungbin.dokkapaparazzi

import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.absolutePathString
import kotlin.io.path.createParentDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.writeText
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaConfiguration.SerializationFormat
import org.jetbrains.dokka.PluginConfigurationImpl
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.base.testApi.testRunner.BaseTestBuilder
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.testApi.logger.TestLogger
import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.dokka.utilities.LoggingLevel
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import utils.TestOutputWriter
import utils.TestOutputWriterPlugin

abstract class AbstractDokkaPaparazziTest(
  logger: TestLogger = TestLogger(DokkaConsoleLogger(LoggingLevel.DEBUG)),
) : BaseAbstractTest(logger) {
  protected fun test(
    vararg pathAndContents: String,
    cleanupOutput: Boolean = true,
    useTestWriter: Boolean = false,
    verifyOnPostStage: (Path) -> Unit = {},
    verifyOnRenderingStage: (TestOutputWriter, RootPageNode, DokkaContext) -> Unit = { _, _, _ -> },
    verifyOnPagesGenerationStage: (RootPageNode) -> Unit,
  ) {
    require(pathAndContents.size % 2 == 0) { "pathAndContents should have even number of elements" }

    val testWriter by lazy { TestOutputWriterPlugin() }
    val configuration = dokkaConfiguration {
      sourceSets {
        sourceSet {
          sourceRoots = listOf("src/main/kotlin")
        }
      }
      pluginsConfigurations = mutableListOf(
        PluginConfigurationImpl(
          fqPluginName = DokkaPaparazziPlugin.PLUGIN_NAME,
          serializationFormat = SerializationFormat.JSON,
          values = """
          {
            "${SnapshotImageProvider.CONFIGURATION_PATH_KEY}": "src/test/resources"
          }
          """.trimIndent(),
        ),
      )
    }

    withTempDirectory(cleanUpAfterUse = cleanupOutput) { tempDir ->
      if (!cleanupOutput) logger.info("Output will be generated under: ${tempDir.absolutePathString()}")

      pathAndContents.asList().materializeFiles(tempDir.toAbsolutePath())

      val tempDirAsFile = tempDir.toFile()
      val newConfiguration = configuration.copy(
        outputDir = tempDir.toFile(),
        sourceSets = configuration.sourceSets.map { sourceSet ->
          sourceSet.copy(
            sourceRoots = sourceSet.sourceRoots.map { file -> tempDirAsFile.resolve(file) }.toSet(),
            suppressedFiles = sourceSet.suppressedFiles.map { file -> tempDirAsFile.resolve(file) }.toSet(),
            sourceLinks = sourceSet.sourceLinks.map { link ->
              link.copy(localDirectory = tempDirAsFile.resolve(link.localDirectory).absolutePath)
            }.toSet(),
            includes = sourceSet.includes.map { file -> tempDirAsFile.resolve(file) }.toSet(),
          )
        },
      )
      runTests(
        configuration = newConfiguration,
        pluginOverrides = listOf(DokkaPaparazziPlugin()).run {
          if (useTestWriter) plus(testWriter) else this
        },
        testLogger = logger,
      ) {
        documentablesCreationStage = { println("A: $it") }
        pagesGenerationStage = verifyOnPagesGenerationStage
        renderingStage = { root, context ->
          verifyOnRenderingStage(testWriter.writer, root, context)
        }
      }
      verifyOnPostStage(tempDir)
    }
  }

  protected fun TestOutputWriter.findContent(path: String): Element =
    contents.entries.single { (key, _) -> key.endsWith(path, ignoreCase = true) }.value
      .let(Jsoup::parse).select("#content")
      .single()

  private inline fun runTests(
    configuration: DokkaConfiguration,
    pluginOverrides: List<DokkaPlugin>,
    testLogger: DokkaLogger = logger,
    block: BaseTestBuilder.() -> Unit,
  ) {
    val testMethods = testBuilder().apply(block).build()
    dokkaTestGenerator(configuration, testLogger, testMethods, pluginOverrides).generate()
  }

  private fun List<String>.materializeFiles(root: Path) {
    for (index in indices step 2) {
      val path = get(index)
      val content = get(index + 1).trimIndent()
      val file = root.resolve(path.removePrefix("/"))

      file.createParentDirectories()
      file.writeText(content)
    }
  }

  @OptIn(ExperimentalPathApi::class)
  private inline fun withTempDirectory(cleanUpAfterUse: Boolean, block: (tempDirectory: Path) -> Unit) {
    val tempDir = createTempDirectory(prefix = "dokka-test")
    try {
      block(tempDir)
    } finally {
      if (cleanUpAfterUse) {
        tempDir.deleteRecursively()
      }
    }
  }
}
