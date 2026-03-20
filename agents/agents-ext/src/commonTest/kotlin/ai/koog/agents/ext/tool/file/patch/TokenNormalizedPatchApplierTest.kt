package ai.koog.agents.ext.tool.file.patch

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TokenNormalizedPatchApplierTest {

    // --- tokenize tests ---

    @Test
    fun `tokenize splits on whitespace`() {
        val tokens = tokenize("hello world")
        val contents = tokens.map { it.content }
        assertEquals(listOf("hello", " ", "world"), contents)
    }

    @Test
    fun `tokenize splits on newlines`() {
        val tokens = tokenize("line1\nline2")
        val contents = tokens.map { it.content }
        assertEquals(listOf("line1", "\n", "line2"), contents)
    }

    @Test
    fun `tokenize splits on punctuation`() {
        val tokens = tokenize("a(b)")
        val contents = tokens.map { it.content }
        assertEquals(listOf("a", "(", "b", ")"), contents)
    }

    @Test
    fun `tokenize handles empty string`() {
        val tokens = tokenize("")
        assertTrue(tokens.isEmpty())
    }

    @Test
    fun `tokenize preserves ranges correctly`() {
        val text = "ab cd"
        val tokens = tokenize(text)
        tokens.forEach { token ->
            assertEquals(token.content, text.substring(token.range))
        }
    }

    @Test
    fun `tokenize splits on multiple separator types`() {
        val tokens = tokenize("func(a, b)")
        val contents = tokens.map { it.content }
        assertEquals(listOf("func", "(", "a", ",", " ", "b", ")"), contents)
    }

    @Test
    fun `tokenize identifies whitespace tokens`() {
        val tokens = tokenize("a b")
        val spaceToken = tokens.find { it.content == " " }
        assertNotNull(spaceToken)
        assertTrue(spaceToken.isWhitespace)
    }

    @Test
    fun `tokenize identifies non-whitespace tokens`() {
        val tokens = tokenize("hello")
        assertEquals(1, tokens.size)
        assertFalse(tokens[0].isWhitespace)
    }

    // --- TokenList.find tests ---

    @Test
    fun `find locates matching subsequence`() {
        val content = TokenList(tokenize("hello world foo"))
        val search = TokenList(tokenize("world"))
        val range = content.find(search)
        assertNotNull(range)
    }

    @Test
    fun `find returns null when no match`() {
        val content = TokenList(tokenize("hello world"))
        val search = TokenList(tokenize("missing"))
        val range = content.find(search)
        assertNull(range)
    }

    @Test
    fun `find uses custom equality function`() {
        val content = TokenList(tokenize("Hello World"))
        val search = TokenList(tokenize("hello world"))
        val range = content.find(search) { fst, snd ->
            fst.content.equals(snd.content, ignoreCase = true)
        }
        assertNotNull(range)
    }

    @Test
    fun `find returns first occurrence`() {
        val content = TokenList(tokenize("aaa bbb aaa"))
        val search = TokenList(tokenize("aaa"))
        val range = content.find(search)
        assertNotNull(range)
        assertEquals(0, range.first)
    }

    // --- TokenList.replace tests ---

    @Test
    fun `replace substitutes tokens in range`() {
        val content = TokenList(tokenize("hello world"))
        val replacement = TokenList(tokenize("earth"))
        val search = content.find(TokenList(tokenize("world")))!!
        val result = content.replace(search, replacement)
        assertEquals("hello earth", result.text)
    }

    @Test
    fun `replace at start of token list`() {
        val content = TokenList(tokenize("old stuff"))
        val replacement = TokenList(tokenize("new"))
        val search = content.find(TokenList(tokenize("old")))!!
        val result = content.replace(search, replacement)
        assertEquals("new stuff", result.text)
    }

    @Test
    fun `replace at end of token list`() {
        val content = TokenList(tokenize("keep this"))
        val replacement = TokenList(tokenize("that"))
        val search = content.find(TokenList(tokenize("this")))!!
        val result = content.replace(search, replacement)
        assertEquals("keep that", result.text)
    }

    // --- applyTokenNormalizedPatch tests ---

    @Test
    fun `patch replaces matching text`() {
        val result = applyTokenNormalizedPatch(
            "hello world",
            FilePatch("hello", "goodbye")
        )
        assertIs<PatchApplyResult.Success>(result)
        assertEquals("goodbye world", result.updatedContent)
    }

    @Test
    fun `patch returns OriginalNotFound when no match`() {
        val result = applyTokenNormalizedPatch(
            "hello world",
            FilePatch("missing", "replacement")
        )
        assertIs<PatchApplyResult.Failure.OriginalNotFound>(result)
    }

    @Test
    fun `patch rewrites content when original is empty`() {
        val result = applyTokenNormalizedPatch(
            "old content",
            FilePatch("", "new content")
        )
        assertIs<PatchApplyResult.Success>(result)
        assertEquals("new content", result.updatedContent)
    }

    @Test
    fun `patch deletes matching text when replacement is empty`() {
        val result = applyTokenNormalizedPatch(
            "keep remove keep",
            FilePatch("remove ", "")
        )
        assertIs<PatchApplyResult.Success>(result)
        assertEquals("keep keep", result.updatedContent)
    }

    @Test
    fun `patch matches case-insensitively`() {
        val result = applyTokenNormalizedPatch(
            "Hello World",
            FilePatch("hello world", "Goodbye World")
        )
        assertIs<PatchApplyResult.Success>(result)
        assertEquals("Goodbye World", result.updatedContent)
    }

    @Test
    fun `patch matches with different whitespace`() {
        val result = applyTokenNormalizedPatch(
            "hello   world",
            FilePatch("hello world", "hello earth")
        )
        assertIs<PatchApplyResult.Success>(result)
        assertEquals("hello earth", result.updatedContent)
    }

    @Test
    fun `patch handles multiline replacement`() {
        val content = "line1\nline2\nline3"
        val result = applyTokenNormalizedPatch(
            content,
            FilePatch("line2", "replaced")
        )
        assertIs<PatchApplyResult.Success>(result)
        assertEquals("line1\nreplaced\nline3", result.updatedContent)
    }

    @Test
    fun `patch preserves content when both original and replacement are empty`() {
        val result = applyTokenNormalizedPatch(
            "content",
            FilePatch("", "")
        )
        assertIs<PatchApplyResult.Success>(result)
        assertEquals("content", result.updatedContent)
    }

    @Test
    fun `PatchApplyResult isSuccess returns true for Success`() {
        val result: PatchApplyResult = PatchApplyResult.Success("content")
        assertTrue(result.isSuccess())
    }

    @Test
    fun `PatchApplyResult isSuccess returns false for Failure`() {
        val result: PatchApplyResult = PatchApplyResult.Failure.OriginalNotFound
        assertFalse(result.isSuccess())
    }

    @Test
    fun `patch matches with tab vs space differences`() {
        val result = applyTokenNormalizedPatch(
            "hello\tworld",
            FilePatch("hello world", "hello earth")
        )
        assertIs<PatchApplyResult.Success>(result)
        assertEquals("hello earth", result.updatedContent)
    }

    @Test
    fun `patch replaces only first occurrence`() {
        val result = applyTokenNormalizedPatch(
            "aaa bbb aaa",
            FilePatch("aaa", "ccc")
        )
        assertIs<PatchApplyResult.Success>(result)
        assertEquals("ccc bbb aaa", result.updatedContent)
    }
}
