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

    private suspend fun edit(path: Path, original: String, replacement: String): EditFileTool.Result =
        tool.execute(EditFileTool.Args(path.toString(), original, replacement), enabler)

    @Test
    fun `descriptor is configured correctly`() {
        val descriptor = tool.descriptor
        assertEquals("edit_file", descriptor.name)
        assertTrue(descriptor.description.isNotEmpty())
        assertEquals(listOf("path", "original", "replacement"), descriptor.requiredParameters.map { it.name })
        assertTrue(descriptor.optionalParameters.isEmpty())
    }

    @Test
    fun `Args are passed correctly`() {
        val args = EditFileTool.Args("/tmp/file.kt", "old text", "new text")
        assertEquals("/tmp/file.kt", args.path)
        assertEquals("old text", args.original)
        assertEquals("new text", args.replacement)
    }

    @Test
    fun `creates new file when it does not exist`() = runBlocking {
        val p = tempDir.resolve("new/file.txt")

        val result = edit(p, "", "hello world")

        assertTrue(result.applied)
        assertTrue(p.toFile().exists())
        assertEquals("hello world", p.readText())
    }

    @Test
    fun `creates new file in nested directories`() = runBlocking {
        val p = tempDir.resolve("a/b/c/nested.kt")

        val result = edit(p, "", "package foo")

        assertTrue(result.applied)
        assertEquals("package foo", p.readText())
    }

    @Test
    fun `edits existing file by replacing matched text`() = runBlocking {
        val p = tempDir.resolve("edit.txt")
        p.parent.createDirectories()
        p.writeText("hello world")

        val result = edit(p, "hello", "goodbye")

        assertTrue(result.applied)
        assertEquals("goodbye world", p.readText())
    }

    @Test
    fun `rewrites entire existing file when original is empty`() = runBlocking {
        val p = tempDir.resolve("rewrite.txt")
        p.parent.createDirectories()
        p.writeText("old content that will be replaced")

        val result = edit(p, "", "brand new content")

        assertTrue(result.applied)
        assertEquals("brand new content", p.readText())
    }

    @Test
    fun `applies case-insensitive match`() = runBlocking {
        val p = tempDir.resolve("case.txt")
        p.parent.createDirectories()
        p.writeText("Hello World")

        val result = edit(p, "hello", "Hi")

        assertTrue(result.applied)
        assertEquals("Hi World", p.readText())
    }

    @Test
    fun `applies whitespace-fuzzy match with different spacing`() = runBlocking {
        val p = tempDir.resolve("whitespace.txt")
        p.parent.createDirectories()
        p.writeText("fun foo()  {  return 1  }")

        val result = edit(p, "fun foo() { return 1 }", "fun foo() { return 42 }")

        assertTrue(result.applied)
        assertTrue(p.readText().contains("42"))
    }

    @Test
    fun `returns applied false when original text not found`() = runBlocking {
        val p = tempDir.resolve("notfound.txt")
        p.parent.createDirectories()
        p.writeText("actual content")

        val result = edit(p, "text that does not exist", "replacement")

        assertFalse(result.applied)
        assertEquals("actual content", p.readText())
    }

    @Test
    fun `file is not modified when patch fails`() = runBlocking {
        val p = tempDir.resolve("unchanged.txt")
        p.parent.createDirectories()
        p.writeText("original content")

        edit(p, "nonexistent text", "new value")

        assertEquals("original content", p.readText())
    }

    @Test
    fun `throws ValidationFailure when file is not a text file`() {
        val bin = tempDir.resolve("bin.dat").createFile().apply {
            writeBytes(byteArrayOf(0x00, 0xFF.toByte(), 0x00, 0xFF.toByte()))
        }
        assertThrows<ToolException.ValidationFailure> {
            runBlocking { edit(bin, "anything", "replacement") }
        }
    }

    @Test
    fun `textForLLM returns success message when patch is applied`() = runBlocking {
        val p = tempDir.resolve("success.txt")
        p.parent.createDirectories()
        p.writeText("foo bar")

        val result = edit(p, "foo", "baz")

        assertTrue(result.textForLLM().contains("Successfully"))
        assertTrue(result.textForLLM().contains("edited file"))
    }

    @Test
    fun `textForLLM returns failure message when patch is not applied`() = runBlocking {
        val p = tempDir.resolve("fail.txt")
        p.parent.createDirectories()
        p.writeText("content")

        val result = edit(p, "missing text", "replacement")

        assertTrue(result.textForLLM().contains("not"))
        assertTrue(result.textForLLM().contains("patch application failed"))
    }

    @Test
    fun `replaces multiline block in existing file`() = runBlocking {
        val p = tempDir.resolve("multiline.kt")
        p.parent.createDirectories()
        p.writeText(
            """
            fun add(a: Int, b: Int): Int {
                return a + b
            }
            """.trimIndent()
        )

        val result = edit(
            p,
            "return a + b",
            "return a + b + 0 // no-op"
        )

        assertTrue(result.applied)
        assertTrue(p.readText().contains("return a + b + 0 // no-op"))
    }
}