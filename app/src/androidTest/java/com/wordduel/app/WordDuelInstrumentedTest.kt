package com.wordduel.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.runner.RunWith
import org.junit.Test

@RunWith(AndroidJUnit4::class)
class WordDuelInstrumentedTest {
    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertThat(appContext.packageName).contains("com.wordduel.app")
    }
}
