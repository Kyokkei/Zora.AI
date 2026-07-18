package com.yozora.aichat.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.yozora.aichat.ui.theme.AppTextPrimary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoleplayFormatterTest {
    private val pink = Color(0xFFFF5D8F)
    private val accidentalBlack = Color(0xFF17131A)

    @Test
    fun semanticPaletteNeverUsesAccidentalBlackInEitherMode() {
        listOf(false, true).forEach { lightMode ->
            val formatted = formatRoleplayMessage(
                "Plain *action* \"dialogue\" (thought). 「legacy」",
                lightMode
            )

            assertFalse(formatted.spanStyles.any { it.item.color == accidentalBlack })
            assertRangeHasColor(formatted, "Plain ", AppTextPrimary)
            assertRangeHasColor(formatted, "(thought)", AppTextPrimary)
            assertRangeHasColor(formatted, "\"dialogue\"", pink)
            assertRangeHasColor(formatted, "「legacy」", pink)
            assertRangeHasStyle(formatted, "action") { style ->
                style.fontStyle == FontStyle.Italic && style.color != Color.Unspecified
            }
        }
    }

    @Test
    fun actionStarsAreHiddenAndActionIsItalic() {
        val formatted = formatRoleplayMessage("*walks away*", lightMode = false)

        assertEquals("walks away", formatted.text)
        assertRangeHasStyle(formatted, "walks away") { style ->
            style.fontStyle == FontStyle.Italic && style.color == Color(0xFF55B7FF)
        }
    }

    @Test
    fun doubleStarsRemainBoldInsteadOfBecomingAnAction() {
        val formatted = formatRoleplayMessage("**important**", lightMode = true)

        assertEquals("important", formatted.text)
        assertRangeHasStyle(formatted, "important") { it.fontWeight == FontWeight.Bold }
        assertTrue(formatted.spanStyles.none { it.item.fontStyle == FontStyle.Italic })
    }

    private fun assertRangeHasColor(text: AnnotatedString, needle: String, color: Color) {
        assertRangeHasStyle(text, needle) { it.color == color }
    }

    private fun assertRangeHasStyle(
        text: AnnotatedString,
        needle: String,
        predicate: (androidx.compose.ui.text.SpanStyle) -> Boolean
    ) {
        val start = text.text.indexOf(needle)
        assertTrue("Expected '$needle' in '${text.text}'", start >= 0)
        val end = start + needle.length
        assertTrue(
            "Expected a matching style over '$needle': ${text.spanStyles}",
            text.spanStyles.any { range -> range.start <= start && range.end >= end && predicate(range.item) }
        )
    }
}
