package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.EqProfile
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
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Vivad Sound Equalizer", appName)
  }

  @Test
  fun `verify automated gain control property in EqProfile`() {
    val defaultProfile = EqProfile.FLAT
    assertFalse(defaultProfile.automatedGainControlEnabled)
    
    val agcEnabledProfile = defaultProfile.copy(automatedGainControlEnabled = true)
    assertTrue(agcEnabledProfile.automatedGainControlEnabled)
  }

  @Test
  fun `verify master normalization property in EqProfile`() {
    val defaultProfile = EqProfile.FLAT
    assertFalse(defaultProfile.masterNormalizationEnabled)
    
    val normEnabledProfile = defaultProfile.copy(masterNormalizationEnabled = true)
    assertTrue(normEnabledProfile.masterNormalizationEnabled)
  }

  @Test
  fun `verify manual gain and normalization reset values`() {
    val nonDefaultProfile = EqProfile.FLAT.copy(
        automatedGainControlEnabled = true,
        masterNormalizationEnabled = true,
        autoAttenuationEnabled = false,
        manualAttenuationDb = -6.0f,
        channelBalance = -0.5f,
        limiterEnabled = true,
        limiterThresholdDb = -10.0f,
        limiterRatio = 8.0f,
        limiterAttackMs = 1.0f,
        limiterReleaseMs = 250.0f
    )
    
    val resetProfile = nonDefaultProfile.copy(
        automatedGainControlEnabled = false,
        masterNormalizationEnabled = false,
        autoAttenuationEnabled = true,
        manualAttenuationDb = 0f,
        channelBalance = 0f,
        limiterEnabled = false,
        limiterThresholdDb = -3.0f,
        limiterRatio = 2.0f,
        limiterAttackMs = 5.0f,
        limiterReleaseMs = 50.0f
    )
    
    assertFalse(resetProfile.automatedGainControlEnabled)
    assertFalse(resetProfile.masterNormalizationEnabled)
    assertTrue(resetProfile.autoAttenuationEnabled)
    assertEquals(0f, resetProfile.manualAttenuationDb)
    assertEquals(0f, resetProfile.channelBalance)
    assertFalse(resetProfile.limiterEnabled)
    assertEquals(-3.0f, resetProfile.limiterThresholdDb)
    assertEquals(2.0f, resetProfile.limiterRatio)
    assertEquals(5.0f, resetProfile.limiterAttackMs)
    assertEquals(50.0f, resetProfile.limiterReleaseMs)
  }
}
