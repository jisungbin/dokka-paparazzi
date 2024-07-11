/*
 * Developed by Ji Sungbin 2024.
 *
 * Licensed under the MIT.
 * Please see full license: https://github.com/jisungbin/dokka-paparazzi/blob/main/LICENSE
 */

package land.sungbin.dokkapaparazzi

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.contracts.contract
import land.sungbin.dokkapaparazzi.SnapshotImageProvider.Companion.dri
import okio.Path
import okio.Path.Companion.toPath
import org.jetbrains.dokka.base.signatures.KotlinSignatureProvider
import org.jetbrains.dokka.base.signatures.KotlinSignatureUtils.annotations
import org.jetbrains.dokka.base.signatures.SignatureProvider
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.doc.CustomTagWrapper
import org.jetbrains.dokka.model.doc.Text
import org.jetbrains.dokka.model.firstMemberOfTypeOrNull
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.model.toDisplaySourceSets
import org.jetbrains.dokka.pages.ContentEmbeddedResource
import org.jetbrains.dokka.pages.ContentKind
import org.jetbrains.dokka.pages.ContentNode
import org.jetbrains.dokka.pages.DCI
import org.jetbrains.dokka.plugability.DokkaContext

class SnapshotAwareKotlinSignatureProvider(context: DokkaContext) : SignatureProvider {
  private val delegator = KotlinSignatureProvider(context)
  private val logger = context.logger
  private val snapshotImageProvider: SnapshotImageProvider

  init {
    val configuration = context.configuration.pluginsConfiguration.find { pluginsConfiguration ->
      pluginsConfiguration.fqPluginName == DokkaPaparazziPlugin.PLUGIN_NAME
    }
    checkNotNull(configuration) {
      "The 'land.sungbin.dokkapaparazzi.DokkaPaparazziPlugin' Dokka configuration is missing."
    }

    val values = Gson().fromJson<Map<String, String>>(configuration.values, object : TypeToken<Map<String, String>>() {}.type)
    val path = checkNotNull(values[SnapshotImageProvider.CONFIGURATION_PATH_KEY]) {
      "The 'snapshotDir' field in the 'land.sungbin.dokkapaparazzi.DokkaPaparazziPlugin' Dokka configuration is missing."
    }

    snapshotImageProvider = SnapshotImageProvider(path.toPath())
    logger.progress("The DokkaPaparazzi plugin has been successfully added with the snapshot path \"$path\".")
  }

  override fun signature(documentable: Documentable): List<ContentNode> {
    val original = delegator.signature(documentable)
    if (!isComposable(documentable)) return original
    logger.debug("Generating snapshot-aware signature for ${documentable.name}")

    var snapshotNames = listOf(documentable.name)
    var snapshotSize = emptyList<String>()

    documentable.documentation.values.flatMap { it.children }.forEach { tag ->
      if (tag is CustomTagWrapper) {
        fun CustomTagWrapper.text(): String = firstMemberOfTypeOrNull<Text>()?.body?.trimIndent().orEmpty()

        when (tag.name) {
          SnapshotImageProvider.TAG_NAME_ANNOTATION -> {
            tag.text().split(',').takeIf { it.isNotEmpty() }?.let { snapshotNames = it }
          }
          SnapshotImageProvider.TAG_SIZE_ANNOTATION -> {
            tag.text().split(',').takeIf { it.size == 2 }?.let { snapshotSize = it }
          }
        }
      }
    }

    if (snapshotNames.isNotEmpty()) logger.debug("Found snapshot names: ${snapshotNames.joinToString()}")
    if (snapshotSize.isNotEmpty()) logger.debug("Found snapshot size: ${snapshotSize.joinToString()}")

    val snapshotPath = snapshotImageProvider.getPath(snapshotNames)
    if (snapshotPath == null) {
      logger.debug("No snapshot found for $snapshotNames")
      return original
    }

    // TODO supports sourceSets for snapshot path
    val snapshotContentNode = ContentEmbeddedResource(
      address = dokkaSnapshotPathFor(snapshotPath),
      altText = snapshotPath.segments.last(),
      dci = DCI(setOf(snapshotPath.dri(anchor = documentable.dri)), ContentKind.Symbol),
      sourceSets = documentable.sourceSets.toDisplaySourceSets(),
      extra = PropertyContainer.empty<ContentNode>()
        .plus(SnapshotPathExtra(snapshotPath))
        .run { if (snapshotSize.isNotEmpty()) plus(SnapshotSizeExtra(width = snapshotSize[0], height = snapshotSize[1])) else this },
    )
    return original.toMutableList().apply { add(snapshotContentNode) }.also {
      logger.debug("Snapshot found for ${documentable.name}: $snapshotPath")
    }
  }

  private fun isComposable(documentable: Documentable): Boolean {
    contract { returns(true) implies (documentable is DFunction) }
    if (documentable !is DFunction) return false
    val found = documentable.annotations().values.flatten().find { annotation ->
      annotation.dri.packageName == COMPOSABLE_ANNOTATION.packageName &&
        annotation.dri.classNames == COMPOSABLE_ANNOTATION.classNames
    }
    return found != null
  }

  companion object {
    private val COMPOSABLE_ANNOTATION = DRI(packageName = "androidx.compose.runtime", classNames = "Composable")

    @Suppress("NOTHING_TO_INLINE")
    inline fun dokkaSnapshotPathFor(path: Path): String = "snapshots/${path.name}"
  }
}
