/*
 * Developed by Ji Sungbin 2024.
 *
 * Licensed under the MIT.
 * Please see full license: https://github.com/jisungbin/dokka-paparazzi/blob/main/LICENSE
 */

package land.sungbin.dokkapaparazzi

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.transformers.pages.tags.CustomTagContentProvider
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder.DocumentableContentBuilder
import org.jetbrains.dokka.model.doc.CustomTagWrapper
import org.jetbrains.dokka.model.doc.Text
import org.jetbrains.dokka.model.firstMemberOfTypeOrNull
import org.jetbrains.dokka.pages.TextStyle

object SnapshotAwareTagProvider : CustomTagContentProvider {
  override fun isApplicable(customTag: CustomTagWrapper): Boolean =
    customTag.name == SnapshotImageProvider.TAG_NAME_ANNOTATION ||
      customTag.name == SnapshotImageProvider.TAG_SIZE_ANNOTATION

  override fun DocumentableContentBuilder.contentForDescription(
    sourceSet: DokkaConfiguration.DokkaSourceSet,
    customTag: CustomTagWrapper,
  ) {
    if (customTag.name == SnapshotImageProvider.TAG_NAME_ANNOTATION) {
      val tagContent = customTag.firstMemberOfTypeOrNull<Text>()?.body?.trimIndent() ?: return

      breakLine()
      text("Snapshot names", styles = setOf(TextStyle.Bold))
      unorderedList {
        tagContent.split(',').forEach { name ->
          item { text(name) }
        }
      }
    }
  }
}
