/*
 * Developed by Ji Sungbin 2024.
 *
 * Licensed under the MIT.
 * Please see full license: https://github.com/jisungbin/dokka-paparazzi/blob/main/LICENSE
 */

package land.sungbin.dokkapaparazzi

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem

class SnapshotImageProviderTest {
  private lateinit var fs: FakeFileSystem

  @BeforeTest fun prepare() {
    fs = FakeFileSystem()
  }

  @AfterTest fun tearDown() {
    fs.checkNoOpenFiles()
  }

  @Test fun snapshotPathShouldBeExist() {
    val path = "snapshot".toPath()

    assertFailure { SnapshotImageProvider(path, fs) }
      .hasMessage("Snapshot directory '$path' does not exist")
  }

  @Test fun snapshotPathShouldBeDirectory() {
    val path = "helloworld.txt".toPath().also { fs.write(it) {} }

    assertFailure { SnapshotImageProvider(path, fs) }
      .hasMessage("Snapshot directory '$path' is not a directory")
  }

  @Test fun takingSingleNameSnapshot() {
    val path = "a/b/c/d/e/f/g/h".toPath().also {
      fs.createDirectories(it)
      fs.write(it.resolve("Hello.png")) {}
    }
    val provider = SnapshotImageProvider(path, fs)
    val result = provider.getPath(listOf("hello"))

    assertThat(result).isEqualTo(path.resolve("Hello.png"))
  }

  @Test fun takingMultipleNamesSnapshot() {
    val path = "a/b/c/d/e/f/g/h".toPath().also {
      fs.createDirectories(it)
      fs.write(it.resolve("HelloWorldByeWorld.png")) {}
    }
    val provider = SnapshotImageProvider(path, fs)
    val result = provider.getPath(listOf("hello", "bye"))

    assertThat(result).isEqualTo(path.resolve("HelloWorldByeWorld.png"))
  }

  @Test fun returnsNullIfSnapshotDoesNotExist() {
    val provider = SnapshotImageProvider("/".toPath(), fs)
    val result = provider.getPath(listOf("world"))

    assertThat(result).isNull()
  }
}
