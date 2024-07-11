/*
 * Developed by Ji Sungbin 2024.
 *
 * Licensed under the MIT.
 * Please see full license: https://github.com/jisungbin/dokka-paparazzi/blob/main/LICENSE
 */

package land.sungbin.dokkapaparazzi

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement

@Suppress("unused")
class DokkaPaparazziPlugin : DokkaPlugin() {
  private val basePlugin by lazy { plugin<DokkaBase>() }

  val snapshotTagProvider by extending {
    (
      basePlugin.customTagContentProvider
        with SnapshotAwareTagProvider
        order { before(basePlugin.sinceKotlinTagContentProvider) }
      )
  }

  val signatureProvider by extending {
    (
      plugin<DokkaBase>().signatureProvider
        providing ::SnapshotAwareKotlinSignatureProvider
        override basePlugin.kotlinSignatureProvider
      )
  }

  val htmlRenderer by extending {
    (
      CoreExtensions.renderer
        providing ::SnapshotAwareHtmlRenderer
        override basePlugin.htmlRenderer
      )
  }

  override fun pluginApiPreviewAcknowledgement() = PluginApiPreviewAcknowledgement

  companion object {
    const val PLUGIN_NAME = "land.sungbin.dokkapaparazzi.DokkaPaparazziPlugin"
  }
}
