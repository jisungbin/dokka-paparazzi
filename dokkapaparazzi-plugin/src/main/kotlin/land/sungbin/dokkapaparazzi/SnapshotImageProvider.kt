/*
 * Developed by Ji Sungbin 2024.
 *
 * Licensed under the MIT.
 * Please see full license: https://github.com/jisungbin/dokka-paparazzi/blob/main/LICENSE
 */

package land.sungbin.dokkapaparazzi

import okio.FileSystem
import okio.Path
import org.jetbrains.dokka.base.renderers.isImage
import org.jetbrains.dokka.links.DRI

class SnapshotImageProvider(
  private val path: Path,
  private val fs: FileSystem = FileSystem.SYSTEM,
) {
  private var pathCaches: List<Path>? = null

  init {
    require(fs.exists(path)) { "Snapshot directory '$path' does not exist" }
    require(fs.metadata(path).isDirectory) { "Snapshot directory '$path' is not a directory" }
  }

  fun getPath(names: List<String>): Path? {
    pathCaches?.let { pathCaches -> return pathCaches.findPath(names) }
    return fs.listRecursively(path).toList().also { pathCaches = it }.findPath(names)
  }

  private fun List<Path>.findPath(names: List<String>): Path? {
    for (i in indices) {
      val path = this[i]
      if (path.name.isImage() && names.all { name -> path.name.contains(name, ignoreCase = true) }) {
        return path
      }
    }
    return null
  }

  companion object {
    const val CONFIGURATION_PATH_KEY = "snapshotDir"

    /** @snapshotname a,b,c */
    const val TAG_NAME_ANNOTATION = "snapshotname"

    /** @snapshotsize 100,100 */
    // width,height
    const val TAG_SIZE_ANNOTATION = "snapshotsize"

    fun Path.dri(anchor: DRI) = DRI(
      packageName = anchor.packageName,
      classNames = anchor.classNames,
      extra = "snapshot@$name",
    )
  }
}
