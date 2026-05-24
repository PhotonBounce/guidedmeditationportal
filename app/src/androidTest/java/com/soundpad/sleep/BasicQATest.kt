// This is a stub for emulator-based QA automation. Expand with UIAutomator/Espresso for real tests.
package com.soundpad.sleep

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BasicQATest {
    @Test
    fun appContext_isCorrect() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.soundpad.sleep", appContext.packageName)
    }
}
