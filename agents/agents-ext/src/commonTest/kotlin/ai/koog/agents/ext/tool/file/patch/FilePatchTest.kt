package ai.koog.agents.ext.tool.file.patch

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FilePatchTest {

    @Test
    fun `isDelete is true when original is non-empty and replacement is empty`() {
        val patch = FilePatch(original = "some text", replacement = "")
        assertTrue(patch.isDelete)
        assertFalse(patch.isReplace)
        assertFalse(patch.isRewrite)
    }

    @Test
    fun `isReplace is true when both are non-empty and different`() {
        val patch = FilePatch(original = "old", replacement = "new")
        assertTrue(patch.isReplace)
        assertFalse(patch.isDelete)
        assertFalse(patch.isRewrite)
    }

    @Test
    fun `isRewrite is true when original is empty and replacement is non-empty`() {
        val patch = FilePatch(original = "", replacement = "new content")
        assertTrue(patch.isRewrite)
        assertFalse(patch.isDelete)
        assertFalse(patch.isReplace)
    }

    @Test
    fun `isReplace is false when original and replacement are identical`() {
        val patch = FilePatch(original = "same", replacement = "same")
        assertFalse(patch.isReplace)
        assertFalse(patch.isDelete)
        assertFalse(patch.isRewrite)
    }

    @Test
    fun `all flags are false when both original and replacement are empty`() {
        val patch = FilePatch(original = "", replacement = "")
        assertFalse(patch.isDelete)
        assertFalse(patch.isReplace)
        assertFalse(patch.isRewrite)
    }
}
