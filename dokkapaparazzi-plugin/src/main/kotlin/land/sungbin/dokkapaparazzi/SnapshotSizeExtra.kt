/*
 * Developed by Ji Sungbin 2024.
 *
 * Licensed under the MIT.
 * Please see full license: https://github.com/jisungbin/dokka-paparazzi/blob/main/LICENSE
 */

package land.sungbin.dokkapaparazzi

import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.pages.ContentNode

data class SnapshotSizeExtra(val width: String, val height: String) : ExtraProperty<ContentNode> {
  override val key: ExtraProperty.Key<ContentNode, *> get() = Key

  companion object Key : ExtraProperty.Key<ContentNode, SnapshotPathExtra>
}
