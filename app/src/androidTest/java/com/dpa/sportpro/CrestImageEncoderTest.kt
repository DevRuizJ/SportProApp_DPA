package com.dpa.sportpro

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dpa.sportpro.data.storage.CrestImageEncoder
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class CrestImageEncoderTest {
    @Test
    fun largeImageIsResizedToPngAndPreservesTransparency() {
        val original = Bitmap.createBitmap(2048, 512, Bitmap.Config.ARGB_8888)
        val source = ByteArrayOutputStream()
        original.compress(Bitmap.CompressFormat.PNG, 100, source)
        original.recycle()
        val output = CrestImageEncoder.encodeBytes(source.toByteArray())
        assertEquals(137, output[0].toInt() and 255)
        val decoded = BitmapFactory.decodeByteArray(output, 0, output.size)
        try {
            assertEquals(1024, decoded.width)
            assertEquals(256, decoded.height)
            assertEquals(0, decoded.getPixel(0, 0))
            assertTrue(output.size <= 2 * 1024 * 1024)
        } finally {
            decoded.recycle()
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun oversizedSourceIsRejectedBeforeDecoding() {
        CrestImageEncoder.encodeBytes(ByteArray(10 * 1024 * 1024 + 1))
    }

    @Test
    fun invalidImageIsRejected() {
        try {
            CrestImageEncoder.encodeBytes("not an image".toByteArray())
            fail("Invalid image must not be uploaded")
        } catch (_: android.graphics.ImageDecoder.DecodeException) {
            // Decoder rejected the data; renaming it to PNG is not sufficient.
        }
    }
}
