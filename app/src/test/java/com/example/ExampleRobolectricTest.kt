package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.components.validatePlayableVideoSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    assertEquals("PRIME ESPORT", appName)
  }

  @Test
  fun `validatePlayableVideoSource accepts valid streams and rejects unplayable sources`() {
    assertNull(
      validatePlayableVideoSource(
        "https://firebasestorage.googleapis.com/v0/b/bucket/o/reels%2Fu1%2Fclip.mp4?alt=media&token=abc",
        allowLocalUri = false
      )
    )
    assertNull(
      validatePlayableVideoSource(
        "https://cdn.primeesport.com/live/stream.m3u8?token=123",
        allowLocalUri = false
      )
    )
    assertNull(
      validatePlayableVideoSource("content://media/picker/0/video/42", allowLocalUri = true)
    )
    assertNotNull(
      validatePlayableVideoSource("content://media/picker/0/video/42", allowLocalUri = false)
    )
    assertNotNull(
      validatePlayableVideoSource("https://www.youtube.com/watch?v=dQw4w9WgXcQ", allowLocalUri = false)
    )
    assertNotNull(
      validatePlayableVideoSource("https://cdn.primeesport.com/thumb.jpg", allowLocalUri = false)
    )
    assertNotNull(
      validatePlayableVideoSource("   ", allowLocalUri = true)
    )
  }
}
