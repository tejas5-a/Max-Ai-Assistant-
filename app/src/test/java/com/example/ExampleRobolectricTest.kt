package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read app name string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("MAX AI ASSISTANT", appName)
  }

  @Test
  fun `test independent provider toggles behavior`() {
    // 1. Both enabled by default
    var settings = AppSettings()
    assertTrue(settings.isOpenRouterEnabled)
    assertTrue(settings.isPollinationsEnabled)

    // 2. Disable OpenRouter only -> Pollinations must remain enabled
    settings = settings.copy(isOpenRouterEnabled = false)
    assertFalse(settings.isOpenRouterEnabled)
    assertTrue(settings.isPollinationsEnabled)

    // 3. Enable OpenRouter and disable Pollinations -> OpenRouter must remain enabled
    settings = settings.copy(isOpenRouterEnabled = true, isPollinationsEnabled = false)
    assertTrue(settings.isOpenRouterEnabled)
    assertFalse(settings.isPollinationsEnabled)

    // 4. Both disabled
    settings = settings.copy(isOpenRouterEnabled = false, isPollinationsEnabled = false)
    assertFalse(settings.isOpenRouterEnabled)
    assertFalse(settings.isPollinationsEnabled)
  }

  @Test
  fun `test default OpenRouter model list formatting`() {
    val service = com.example.data.remote.OpenRouterService()
    val defaultModels = service.defaultModels
    assertTrue(defaultModels.isNotEmpty())
    
    // Ensure all default models have valid non-empty model IDs with provider prefixes
    defaultModels.forEach { model ->
      assertTrue(model.id.contains("/"))
      assertTrue(model.name.isNotBlank())
    }
  }

  @Test
  fun `test separate model ID and display name storage in AppSettings`() {
    val defaultSettings = AppSettings()
    assertEquals("meta-llama/llama-3.3-70b-instruct:free", defaultSettings.selectedOpenRouterModel)
    assertEquals("Llama 3.3 70B (Free)", defaultSettings.selectedOpenRouterModelName)

    val customSettings = defaultSettings.copy(
      selectedOpenRouterModel = "openai/gpt-4o-mini",
      selectedOpenRouterModelName = "GPT-4o Mini"
    )
    assertEquals("openai/gpt-4o-mini", customSettings.selectedOpenRouterModel)
    assertEquals("GPT-4o Mini", customSettings.selectedOpenRouterModelName)
  }
}
