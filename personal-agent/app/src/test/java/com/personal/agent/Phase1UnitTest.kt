package com.personal.agent

import com.personal.agent.ai.ProviderConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase1UnitTest {
    @Test
    fun providerDefaults_haveFiveEntries() {
        val all = ProviderConfig.defaults()
        assertEquals(5, all.size)
        assertTrue(all.all { it.baseUrl.startsWith("https://") })
    }

    @Test
    fun projectNameSanitized() {
        val raw = "My App!@#"
        val safe = raw.trim().replace(Regex("[^A-Za-z0-9._-]"), "_")
        assertEquals("My_App___", safe)
    }
}
