package com.example.shipperincome

import androidx.compose.ui.text.AnnotatedString
import com.example.shipperincome.ui.components.ThousandsSeparatorTransformation
import com.example.shipperincome.utils.formatCurrency
import com.example.shipperincome.utils.formatDuration
import com.example.shipperincome.utils.formatDurationShort
import com.example.shipperincome.utils.groupDigits
import com.example.shipperincome.utils.minutesToHoursInput
import com.example.shipperincome.utils.parseHoursToMinutes
import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {

    @Test
    fun groupsDigitsByThousands() {
        assertEquals("", groupDigits(""))
        assertEquals("999", groupDigits("999"))
        assertEquals("1,000", groupDigits("1000"))
        assertEquals("12,345", groupDigits("12345"))
        assertEquals("1,234,567", groupDigits("1234567"))
    }

    @Test
    fun formatsCurrencyAndDuration() {
        assertEquals("12,346₩", formatCurrency(12_345.6))
        assertEquals("8 giờ 30 phút", formatDuration(8.5))
        assertEquals("2 giờ 0 phút", formatDuration(1.9999))
        assertEquals("8h30", formatDurationShort(8.5))
        assertEquals("3h", formatDurationShort(3.0))
    }

    @Test
    fun parsesTotalHoursInput() {
        assertEquals(480, parseHoursToMinutes("8"))
        assertEquals(510, parseHoursToMinutes("8.5"))
        assertEquals(510, parseHoursToMinutes("8,5"))
        assertEquals(510, parseHoursToMinutes("8:30"))
        assertEquals(510, parseHoursToMinutes("8h30"))
        assertEquals(480, parseHoursToMinutes("8h"))
        assertEquals(45, parseHoursToMinutes("45p"))
        assertEquals(null, parseHoursToMinutes(""))
        assertEquals(null, parseHoursToMinutes("8:75"))
        assertEquals(null, parseHoursToMinutes("abc"))
        assertEquals("8:30", minutesToHoursInput(510))
        assertEquals("8", minutesToHoursInput(480))
    }

    @Test
    fun separatorTransformationKeepsCursorInPlace() {
        val transformed = ThousandsSeparatorTransformation.filter(AnnotatedString("1234567"))
        assertEquals("1,234,567", transformed.text.text)
        val map = transformed.offsetMapping

        assertEquals(0, map.originalToTransformed(0))
        assertEquals(1, map.originalToTransformed(1)) // sau "1", truoc dau phay
        assertEquals(5, map.originalToTransformed(4)) // "1,234|"
        assertEquals(9, map.originalToTransformed(7)) // cuoi chuoi

        assertEquals(1, map.transformedToOriginal(2)) // "1,|" -> sau "1"
        assertEquals(4, map.transformedToOriginal(5))
        assertEquals(7, map.transformedToOriginal(9))

        // Kiem tra hai chieu khop nhau cho moi vi tri.
        for (i in 0..7) {
            assertEquals(i, map.transformedToOriginal(map.originalToTransformed(i)))
        }
    }
}
