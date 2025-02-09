/*
 * Developed by Ji Sungbin 2024.
 *
 * Licensed under the MIT.
 * Please see full license: https://github.com/jisungbin/dokka-paparazzi/blob/main/LICENSE
 */

package land.sungbin.dokkapaparazzi

import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.exists
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.prop
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.exists
import kotlin.io.path.walk
import kotlin.test.Test
import okio.Path.Companion.toPath
import org.jetbrains.dokka.model.withDescendants
import org.jetbrains.dokka.pages.ContentEmbeddedResource
import org.jetbrains.dokka.pages.MemberPageNode
import org.jetbrains.dokka.pages.RootPageNode
import org.jsoup.nodes.Element
import org.opentest4j.AssertionFailedError
import utils.TestOutputWriter

@OptIn(ExperimentalPathApi::class)
class DokkaPaparazziPluginTest : AbstractDokkaPaparazziTest() {
  @Test fun composableFunctionDocumentedWithSnapshot() = test(
    "src/main/kotlin/Composable.kt",
    """
    package androidx.compose.runtime
    annotation class Composable
    """,
    "src/main/kotlin/FakeComposable.kt",
    """
    package androidx.compose.runtime
    @Composable fun FakeSnapshot() = Unit
    """,
    cleanupOutput = false,
    verifyOnPagesGenerationStage = { page ->
      val embeddeds = page.embeddedResources()
      assertThat(embeddeds).hasSize(1)
      assertThat(embeddeds.first()).all {
        prop(ContentEmbeddedResource::address).isEqualTo("snapshots/FakeSnapshot.png")
        prop(ContentEmbeddedResource::altText).isEqualTo("FakeSnapshot.png")
        prop(ContentEmbeddedResource::extra)
          .transform { it.allOfType<SnapshotPathExtra>() }
          .containsOnly(SnapshotPathExtra("src/test/resources/FakeSnapshot.png".toPath()))
      }
    },
    verifyOnPostStage = { root ->
      val current = root.parent("-fake-snapshot.html")
      assertThat(current.resolve("snapshots/FakeSnapshot.png")).exists()
    },
  )

  @Test fun composableFunctionDocumentedWithCustomSizedSnapshot() = test(
    "src/main/kotlin/Composable.kt",
    """
    package androidx.compose.runtime
    annotation class Composable
    """,
    "src/main/kotlin/FakeComposable.kt",
    """
    package androidx.compose.runtime
    /** @snapshotsize 100,300 */
    @Composable fun FakeSnapshot() = Unit
    """,
    useTestWriter = true,
    verifyOnPagesGenerationStage = { page ->
      val embeddeds = page.embeddedResources()
      assertThat(embeddeds).hasSize(1)
      assertThat(embeddeds.first()).all {
        prop(ContentEmbeddedResource::address).isEqualTo("snapshots/FakeSnapshot.png")
        prop(ContentEmbeddedResource::altText).isEqualTo("FakeSnapshot.png")
        prop(ContentEmbeddedResource::extra)
          .transform { it.allOfType<SnapshotPathExtra>() }
          .containsOnly(SnapshotPathExtra("src/test/resources/FakeSnapshot.png".toPath()))
        prop(ContentEmbeddedResource::extra)
          .transform { it.allOfType<SnapshotSizeExtra>() }
          .containsOnly(SnapshotSizeExtra(width = "100", height = "300"))
      }
    },
    verifyOnRenderingStage = { writer, _, _ ->
      writer.validateFirstImage("-fake-snapshot.html") {
        attr("src").isEqualTo("snapshots/FakeSnapshot.png")
        attr("alt").isEqualTo("FakeSnapshot.png")
        attr("width").isEqualTo("100")
        attr("height").isEqualTo("300")
      }
    },
  )

  @Test fun composableFunctionDocumentedWithInvalidCustomSizedSnapshot() = test(
    "src/main/kotlin/Composable.kt",
    """
    package androidx.compose.runtime
    annotation class Composable
    """,
    "src/main/kotlin/FakeComposable.kt",
    """
    package androidx.compose.runtime
    /** @snapshotsize */
    @Composable fun FakeSnapshot() = Unit
    """,
    useTestWriter = true,
    verifyOnPagesGenerationStage = { page ->
      val embeddeds = page.embeddedResources()
      assertThat(embeddeds).hasSize(1)
      assertThat(embeddeds.first()).all {
        prop(ContentEmbeddedResource::address).isEqualTo("snapshots/FakeSnapshot.png")
        prop(ContentEmbeddedResource::altText).isEqualTo("FakeSnapshot.png")
        prop(ContentEmbeddedResource::extra)
          .transform { it.allOfType<SnapshotSizeExtra>() }
          .isEmpty()
        prop(ContentEmbeddedResource::extra)
          .transform { it.allOfType<SnapshotPathExtra>() }
          .containsOnly(SnapshotPathExtra("src/test/resources/FakeSnapshot.png".toPath()))
      }
    },
    verifyOnRenderingStage = { writer, _, _ ->
      writer.validateFirstImage("-fake-snapshot.html") {
        attr("src").isEqualTo("snapshots/FakeSnapshot.png")
        attr("alt").isEqualTo("FakeSnapshot.png")
        attr("width").isEqualTo("")
        attr("height").isEqualTo("")
      }
    },
  )

  @Test fun composableFunctionDocumentedWithMultiNamesSnapshot() = test(
    "src/main/kotlin/Composable.kt",
    """
    package androidx.compose.runtime
    annotation class Composable
    """,
    "src/main/kotlin/FakeComposable.kt",
    """
    package androidx.compose.runtime
    /** @snapshotname fakesnapshot,scaled */
    @Composable fun FakeSnapshot() = Unit
    """,
    useTestWriter = true,
    verifyOnPagesGenerationStage = { page ->
      val embeddeds = page.embeddedResources()
      assertThat(embeddeds).hasSize(1)
      assertThat(embeddeds.first()).all {
        prop(ContentEmbeddedResource::address).isEqualTo("snapshots/FakeSnapshotWithThreeScaled.png")
        prop(ContentEmbeddedResource::altText).isEqualTo("FakeSnapshotWithThreeScaled.png")
        prop(ContentEmbeddedResource::extra)
          .transform { it.allOfType<SnapshotPathExtra>() }
          .containsOnly(SnapshotPathExtra("src/test/resources/FakeSnapshotWithThreeScaled.png".toPath()))
      }
    },
    verifyOnRenderingStage = { writer, _, _ ->
      writer.validateFirstImage("-fake-snapshot.html") {
        attr("src").isEqualTo("snapshots/FakeSnapshotWithThreeScaled.png")
        attr("alt").isEqualTo("FakeSnapshotWithThreeScaled.png")
      }
    },
  )

  @Test fun composableFunctionDocumentedWithMultiNamesCustomSizedSnapshot() = test(
    "src/main/kotlin/Composable.kt",
    """
    package androidx.compose.runtime
    annotation class Composable
    """,
    "src/main/kotlin/FakeComposable.kt",
    """
    package androidx.compose.runtime
    /** 
     * @snapshotname fakesnapshot,scaled
     * @snapshotsize 100,300
     */
    @Composable fun FakeSnapshot() = Unit
    """,
    useTestWriter = true,
    verifyOnPagesGenerationStage = { page ->
      val embeddeds = page.embeddedResources()
      assertThat(embeddeds).hasSize(1)
      assertThat(embeddeds.first()).all {
        prop(ContentEmbeddedResource::address).isEqualTo("snapshots/FakeSnapshotWithThreeScaled.png")
        prop(ContentEmbeddedResource::altText).isEqualTo("FakeSnapshotWithThreeScaled.png")
        prop(ContentEmbeddedResource::extra)
          .transform { it.allOfType<SnapshotPathExtra>() }
          .containsOnly(SnapshotPathExtra("src/test/resources/FakeSnapshotWithThreeScaled.png".toPath()))
        prop(ContentEmbeddedResource::extra)
          .transform { it.allOfType<SnapshotSizeExtra>() }
          .containsOnly(SnapshotSizeExtra(width = "100", height = "300"))
      }
    },
    verifyOnRenderingStage = { writer, _, _ ->
      writer.validateFirstImage("-fake-snapshot.html") {
        attr("src").isEqualTo("snapshots/FakeSnapshotWithThreeScaled.png")
        attr("alt").isEqualTo("FakeSnapshotWithThreeScaled.png")
        attr("width").isEqualTo("100")
        attr("height").isEqualTo("300")
      }
    },
  )

  @Test fun composableFunctionDocumentedWithSnapshotButSnapshotDoesNotExists() = test(
    "src/main/kotlin/Composable.kt",
    """
    package androidx.compose.runtime
    annotation class Composable
    """,
    "src/main/kotlin/FakeComposable2.kt",
    """
    package androidx.compose.runtime
    @Composable fun FakeSnapshot2() = Unit
    """,
    verifyOnPagesGenerationStage = { page ->
      assertThat(page.embeddedResources()).isEmpty()
    },
    verifyOnPostStage = { root ->
      val current = root.parent("-fake-snapshot2.html")

      // TODO Assert<Path>.doesNotExist()
      //  when https://github.com/willowtreeapps/assertk/pull/542 is released
      assertThat(current.resolve("snapshots/FakeSnapshot2.png").exists()).isFalse()
    },
  )

  @Test fun regularFunctionDocumentedWithoutSnapshot() = test(
    "src/main/kotlin/Function.kt",
    "fun FakeSnapshot() = Unit",
    verifyOnPagesGenerationStage = { page ->
      assertThat(page.embeddedResources()).isEmpty()
    },
    verifyOnPostStage = { root ->
      val current = root.parent("-fake-snapshot.html")

      // TODO Assert<Path>.doesNotExist()
      //  when https://github.com/willowtreeapps/assertk/pull/542 is released
      assertThat(current.resolve("snapshots/FakeSnapshot.png").exists()).isFalse()
    },
  )

  private fun TestOutputWriter.validateFirstImage(
    path: String,
    scope: ImageAssertionScope.() -> Unit,
  ) {
    val img = findContent("-fake-snapshot.html").selectFirst("img")
      ?: throw AssertionFailedError("No <img> tag found in $path")
    ImageAssertionScope(img).run(scope)
  }

  private fun Path.parent(path: String): Path = walk().first { current -> current.endsWith(path) }.parent

  private fun RootPageNode.embeddedResources(): List<ContentEmbeddedResource> =
    withDescendants().filterIsInstance<MemberPageNode>()
      .flatMap { member -> member.content.withDescendants().filterIsInstance<ContentEmbeddedResource>() }
      .toList()
}

@TestDsl
private fun interface ImageAssertionScope {
  fun attr(name: String): Assert<String>

  companion object {
    operator fun invoke(element: Element) = ImageAssertionScope { attr ->
      assertThat(element).prop(attr) { it.attr(attr) }
    }
  }
}
