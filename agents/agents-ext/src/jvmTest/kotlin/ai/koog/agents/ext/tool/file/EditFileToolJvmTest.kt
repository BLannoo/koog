package ai.koog.agents.ext.tool.file

import ai.koog.agents.core.tools.DirectToolCallsEnabler
import ai.koog.agents.core.tools.ToolException
import ai.koog.agents.core.tools.annotations.InternalAgentToolsApi
import ai.koog.rag.base.files.JVMFileSystemProvider
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(InternalAgentToolsApi::class)
class EditFileToolJvmTest {

    private val fs = JVMFileSystemProvider.ReadWrite
    private val enabler = object : DirectToolCallsEnabler {}
    private val tool = EditFileTool(fs)

    @TempDir
    lateinit var tempDir: Path

    private fun createTestFile(name: String, content: String): Path =
        tempDir.resolve(name).also {
            it.parent.createDirectories()
            it.createFile()
            it.writeText(content)
        }

    private suspend fun edit(path: Path, original: String, replacement: String): EditFileTool.Result =
        tool.execute(EditFileTool.Args(path.toString(), original, replacement), enabler)

    @Test
    fun `descriptor is configured correctly`() {
        val descriptor = tool.descriptor
        assertEquals("edit_file", descriptor.name)
        assertTrue(descriptor.description.isNotEmpty())
        assertEquals(
            listOf("path", "original", "replacement"),
            descriptor.requiredParameters.map { it.name }
        )
        assertTrue(descriptor.optionalParameters.isEmpty())
    }

    @Test
    fun `Args are passed correctly`() {
        val args = EditFileTool.Args("/tmp/test.txt", "old", "new")
        assertEquals("/tmp/test.txt", args.path)
        assertEquals("old", args.original)
        assertEquals("new", args.replacement)
    }

    @Test
    fun `replaces text in existing file`() = runBlocking {
        val f = createTestFile("hello.txt", "hello world")

        val result = edit(f, "hello", "goodbye")

        assertTrue(result.applied)
        assertEquals("goodbye world", f.readText())
    }

    @Test
    fun `creates new file when original is empty (rewrite mode)`() = runBlocking {
        val f = tempDir.resolve("new_file.txt")

        val result = edit(f, "", "brand new content")

        assertTrue(result.applied)
        assertTrue(f.exists())
        assertEquals("brand new content", f.readText())
    }

    @Test
    fun `creates parent directories for new file`() = runBlocking {
        val f = tempDir.resolve("nested/deep/dir/file.txt")

        val result = edit(f, "", "content in nested file")

        assertTrue(result.applied)
        assertTrue(f.exists())
        assertEquals("content in nested file", f.readText())
    }

    @Test
    fun `deletes text when replacement is empty`() = runBlocking {
        val f = createTestFile("delete.txt", "keep this remove this keep that")

        val result = edit(f, "remove this ", "")

        assertTrue(result.applied)
        assertEquals("keep this keep that", f.readText())
    }

    @Test
    fun `rewrites entire file when original is empty on existing file`() = runBlocking {
        val f = createTestFile("rewrite.txt", "old content")

        val result = edit(f, "", "completely new content")

        assertTrue(result.applied)
        assertEquals("completely new content", f.readText())
    }

    @Test
    fun `returns failure when original text is not found`() = runBlocking {
        val f = createTestFile("notfound.txt", "actual content here")

        val result = edit(f, "nonexistent text", "replacement")

        assertFalse(result.applied)
        assertEquals("actual content here", f.readText())
    }

    @Test
    fun `throws ValidationFailure for non-text file`() {
        val bin = tempDir.resolve("binary.dat").also {
            it.parent.createDirectories()
            it.createFile()
            it.writeBytes(byteArrayOf(0x00, 0xFF.toByte(), 0x00, 0xFF.toByte()))
        }
        assertThrows<ToolException.ValidationFailure> {
            runBlocking { edit(bin, "something", "else") }
        }
    }

    @Test
    fun `matches original text case-insensitively`() = runBlocking {
        val f = createTestFile("case.txt", "Hello World")

        val result = edit(f, "hello world", "Goodbye World")

        assertTrue(result.applied)
        assertEquals("Goodbye World", f.readText())
    }

    @Test
    fun `matches original text with different whitespace`() = runBlocking {
        val f = createTestFile("whitespace.txt", "hello   world")

        val result = edit(f, "hello world", "hello earth")

        assertTrue(result.applied)
        assertEquals("hello earth", f.readText())
    }

    @Test
    fun `replaces multiline content`() = runBlocking {
        val content = "line1\nline2\nline3\nline4"
        val f = createTestFile("multi.txt", content)

        val result = edit(f, "line2\nline3", "replaced2\nreplaced3")

        assertTrue(result.applied)
        assertEquals("line1\nreplaced2\nreplaced3\nline4", f.readText())
    }

    @Test
    fun `preserves surrounding content during replacement`() = runBlocking {
        val content = "prefix==>target<==suffix"
        val f = createTestFile("surround.txt", content)

        val result = edit(f, "target", "REPLACED")

        assertTrue(result.applied)
        assertEquals("prefix==>REPLACED<==suffix", f.readText())
    }

    @Test
    fun `Result textForLLM shows success message`() = runBlocking {
        val f = createTestFile("success.txt", "old text")

        val result = edit(f, "old text", "new text")

        assertTrue(result.textForLLM().contains("Successfully"))
        assertTrue(result.textForLLM().contains("edited file"))
    }

    @Test
    fun `Result textForLLM shows failure message when original not found`() = runBlocking {
        val f = createTestFile("fail.txt", "actual content")

        val result = edit(f, "missing content", "replacement")

        assertTrue(result.textForLLM().contains("not"))
        assertTrue(result.textForLLM().contains("modified"))
    }

    @Test
    fun `no-op when both original and replacement are empty`() = runBlocking {
        val f = createTestFile("noop.txt", "content stays")

        val result = edit(f, "", "")

        // Empty original + empty replacement: patch is neither delete, replace, rewrite
        // so the content stays unchanged, but it returns Success with original content
        assertTrue(result.applied)
        assertEquals("content stays", f.readText())
    }

    @Test
    fun `replaces only the first occurrence`() = runBlocking {
        val f = createTestFile("first.txt", "aaa bbb aaa")

        val result = edit(f, "aaa", "ccc")

        assertTrue(result.applied)
        assertEquals("ccc bbb aaa", f.readText())
    }
}
