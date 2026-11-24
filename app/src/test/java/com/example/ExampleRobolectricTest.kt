package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.core.E2eeCryptoManager
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ProfileSetupScreen
import com.example.ui.theme.SmileTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `app_name string resource is SMILE`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("SMILE", context.getString(R.string.app_name))
    }

    @Test
    fun `ECDH and AES-256-GCM roundtrip encrypts and decrypts between two users`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val crypto = E2eeCryptoManager(context)
        val pubA = crypto.getPublicKeyBase64("alice_test_uid")
        val pubB = crypto.getPublicKeyBase64("bob_test_uid")

        val aesA = crypto.deriveSharedAesKey("alice_test_uid", pubB, "chat_ab")
        val aesB = crypto.deriveSharedAesKey("bob_test_uid", pubA, "chat_ab")

        val (ciphertext, iv) = crypto.encryptText("Namaste SMILE! बात करें, मुस्कुराएं!", aesA)
        val decrypted = crypto.decryptText(ciphertext, iv, aesB)
        assertEquals("Namaste SMILE! बात करें, मुस्कुराएं!", decrypted)

        // Tampered ciphertext fails GCM authentication tag verification
        val tampered = crypto.decryptText("AAAA$ciphertext", iv, aesB)
        assertNull(tampered)

        // Symmetric safety number
        assertEquals(
            crypto.computeSafetyNumber(pubA, pubB),
            crypto.computeSafetyNumber(pubB, pubA)
        )
    }

    @Test
    fun `group AES key encrypts and decrypts cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val crypto = E2eeCryptoManager(context)
        val key = crypto.deriveGroupAesKey("group_123")
        val (cipher, iv) = crypto.encryptText("Group announcement", key)
        val plain = crypto.decryptText(cipher, iv, key)
        assertEquals("Group announcement", plain)
        assertNotNull(cipher)
    }

    @Test
    fun `AuthScreen renders SMILE branding and Google Sign-In button`() {
        composeTestRule.setContent {
            SmileTheme {
                AuthScreen(onAuthSuccess = {})
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("SMILE").assertIsDisplayed()
        composeTestRule.onNodeWithText("बात करें, मुस्कुराएं!").assertIsDisplayed()
        composeTestRule.onNodeWithTag("google_sign_in_button").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `ProfileSetupScreen validates 10-digit India phone number and submits valid profile`() {
        var savedPhone = ""
        var savedName = ""
        var savedId = ""
        composeTestRule.setContent {
            SmileTheme {
                ProfileSetupScreen(
                    initialName = "Aarav",
                    isSaving = false,
                    errorText = null,
                    onSaveProfile = { phone, name, smileId, _, _ ->
                        savedPhone = phone
                        savedName = name
                        savedId = smileId
                    }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Invalid short phone shows validation error and does not invoke callback
        composeTestRule.onNodeWithTag("setup_phone_input").performScrollTo().performTextInput("98765")
        composeTestRule.onNodeWithTag("setup_save_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Please enter a valid 10-digit India mobile number.").performScrollTo().assertIsDisplayed()
        assertTrue(savedPhone.isEmpty())

        // Complete 10-digit phone submits +91 E.164 format
        composeTestRule.onNodeWithTag("setup_phone_input").performScrollTo().performTextInput("43210")
        composeTestRule.onNodeWithTag("setup_save_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals("+919876543210", savedPhone)
        assertEquals("Aarav", savedName)
        assertEquals("aarav", savedId)
    }
}
