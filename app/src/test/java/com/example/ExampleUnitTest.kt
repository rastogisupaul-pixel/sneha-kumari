package com.example

import com.example.core.E2eeCryptoManager
import com.example.core.EnglishStrings
import com.example.core.HindiStrings
import com.example.core.MediaUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `normalizeIndianPhone handles 10-digit, 91-prefixed, and +91 formats`() {
        assertEquals("+919876543210", E2eeCryptoManager.normalizeIndianPhone("9876543210"))
        assertEquals("+919876543210", E2eeCryptoManager.normalizeIndianPhone("919876543210"))
        assertEquals("+919876543210", E2eeCryptoManager.normalizeIndianPhone("+91 98765 43210"))
        assertEquals("", E2eeCryptoManager.normalizeIndianPhone("   "))
    }

    @Test
    fun `hashPhoneE164 produces deterministic 32-char hex digest`() {
        val h1 = E2eeCryptoManager.hashPhoneE164("+919876543210")
        val h2 = E2eeCryptoManager.hashPhoneE164("+919876543210")
        val h3 = E2eeCryptoManager.hashPhoneE164("+919876543211")
        assertEquals(32, h1.length)
        assertEquals(h1, h2)
        assertNotEquals(h1, h3)
    }

    @Test
    fun `salted PIN hash verifies correct PIN and rejects wrong PIN`() {
        val hash1 = E2eeCryptoManager.hashPin("1234")
        val hash2 = E2eeCryptoManager.hashPin("1234")
        assertNotEquals(hash1, hash2) // unique salt per hash
        assertTrue(E2eeCryptoManager.verifyPin("1234", hash1))
        assertFalse(E2eeCryptoManager.verifyPin("4321", hash1))
        assertFalse(E2eeCryptoManager.verifyPin("1234", "invalid_format"))
    }

    @Test
    fun `QR matrix generation is deterministic with valid finder corners`() {
        val m1 = MediaUtils.generateQrMatrix("smile://user/aarav_01", 21)
        val m2 = MediaUtils.generateQrMatrix("smile://user/aarav_01", 21)
        assertEquals(21, m1.size)
        for (r in 0 until 21) {
            assertTrue(m1[r].contentEquals(m2[r]))
        }
        // Top-left finder corner
        assertTrue(m1[0][0])
        assertTrue(m1[3][3])
        assertFalse(m1[1][1])
    }

    @Test
    fun `bilingual strings define non-empty English and Hindi translations`() {
        assertEquals("SMILE", EnglishStrings.appName)
        assertEquals("SMILE", HindiStrings.appName)
        assertEquals("बात करें, मुस्कुराएं!", EnglishStrings.tagline)
        assertEquals("बात करें, मुस्कुराएं!", HindiStrings.tagline)
        assertNotEquals(EnglishStrings.tabChats, HindiStrings.tabChats)
    }
}
