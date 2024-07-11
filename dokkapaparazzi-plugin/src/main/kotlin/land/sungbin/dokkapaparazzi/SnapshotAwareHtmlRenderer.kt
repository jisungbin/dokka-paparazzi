/*
 * Developed by Ji Sungbin 2024.
 *
 * Licensed under the MIT.
 * Please see full license: https://github.com/jisungbin/dokka-paparazzi/blob/main/LICENSE
 */

package land.sungbin.dokkapaparazzi

import kotlinx.html.FlowContent
import kotlinx.html.br
import kotlinx.html.img
import land.sungbin.dokkapaparazzi.SnapshotAwareKotlinSignatureProvider.Companion.dokkaSnapshotPathFor
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import okio.Path.Companion.toPath
import org.jetbrains.dokka.base.renderers.html.HtmlRenderer
import org.jetbrains.dokka.base.renderers.isImage
import org.jetbrains.dokka.pages.ContentEmbeddedResource
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.plugability.DokkaContext

class SnapshotAwareHtmlRenderer(
  context: DokkaContext,
  private val fs: FileSystem = FileSystem.SYSTEM,
) : HtmlRenderer(context) {
  private val logger = context.logger
  private val outputPath = context.configuration.outputDir.toOkioPath()

  override fun FlowContent.buildResource(node: ContentEmbeddedResource, pageContext: ContentPage) {
    val providedSnapshot = node.extra.allOfType<SnapshotPathExtra>().firstOrNull()

    if (providedSnapshot != null) br()
    if (node.isImage()) {
      img(src = node.address, alt = node.altText) {
        val (width, height) = node.extra.allOfType<SnapshotSizeExtra>().firstOrNull() ?: return@img
        this.width = width
        this.height = height
      }
    } else {
      logger.error("Unrecognized resource address: ${node.address}")
    }

    if (providedSnapshot != null) {
      val destination = run {
        val current = locationProvider.resolve(pageContext, skipExtension = true) ?: run {
          logger.error("Failed to resolve the location for the current node: ${node.dci}")
          return
        }
        outputPath.resolve(current.toPath().parent!!).resolve(dokkaSnapshotPathFor(providedSnapshot.path))
      }
      fs.createDirectories(destination.parent!!)
      fs.copy(providedSnapshot.path, destination)
      logger.debug("Copied ${providedSnapshot.path} to $destination")
    }
  }
}
