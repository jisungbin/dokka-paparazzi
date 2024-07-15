/*
 * Developed by Ji Sungbin 2024.
 *
 * Licensed under the MIT.
 * Please see full license: https://github.com/jisungbin/dokka-paparazzi/blob/main/LICENSE
 */

package land.sungbin.dokkapaparazzi.gradle

import com.google.gson.Gson
import land.sungbin.dokkapaparazzi.DokkaPaparazziPlugin
import land.sungbin.dokkapaparazzi.SnapshotImageProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.gradle.AbstractDokkaTask

@Suppress("unused") // Used by reflection in Gradle.
class DokkaPaparazziGradlePlugin : Plugin<Project> {
  override fun apply(target: Project) {
    check(target.plugins.hasPlugin("org.jetbrains.dokka")) {
      "Dokka plugin is not applied."
    }

    val extension = target.extensions.create<DokkaPaparazziExtension>("dokkaPaparazzi")

    target.afterEvaluate {
      target.dependencies.add(
        "dokkaPlugin",
        target.dependencies.create("land.sungbin.dokkapaparazzi:dokka-paparazzi:$PLUGIN_VERSION"),
      )

      target.tasks.withType<AbstractDokkaTask> {
        pluginsMapConfiguration.put(
          DokkaPaparazziPlugin.PLUGIN_NAME,
          provider {
            check(extension.snapshotDir.isPresent) { "'snapshotDir' is not set." }
            val path = extension.snapshotDir.get().asFile.path
            val configuration = mapOf(SnapshotImageProvider.CONFIGURATION_PATH_KEY to path)
            gson.toJson(configuration)
          },
        )
      }
    }
  }

  companion object {
    private val gson = Gson()
  }
}
