package com.dpa.sportpro.data.storage

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlin.math.roundToInt

/** Bounded input and resized output: no full-resolution bitmap retained or EXIF uploaded. */
object CrestImageEncoder {
    private const val MAX_INPUT_BYTES = 10 * 1024 * 1024
    private const val MAX_OUTPUT_BYTES = 2 * 1024 * 1024

    suspend fun encode(resolver: ContentResolver, uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        require(uri.scheme == "content") { "Selecciona una imagen desde la galería." }
        val bytes = resolver.openInputStream(uri)?.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var count = input.read(buffer)
            while (count != -1) {
                require(output.size() + count <= MAX_INPUT_BYTES) { "La imagen supera los 10 MB." }
                output.write(buffer, 0, count)
                count = input.read(buffer)
            }
            output.toByteArray()
        } ?: error("No se pudo leer la imagen.")
        encodeBytes(bytes)
    }

    internal fun encodeBytes(bytes: ByteArray): ByteArray {
        require(bytes.isNotEmpty() && bytes.size <= MAX_INPUT_BYTES) { "La imagen está vacía o supera los 10 MB." }
        val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(bytes))) { decoder, info, _ ->
            val scale = minOf(1.0, 1024.0 / maxOf(info.size.width, info.size.height))
            decoder.setTargetSize(
                (info.size.width * scale).roundToInt().coerceAtLeast(1),
                (info.size.height * scale).roundToInt().coerceAtLeast(1),
            )
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        return try {
            val output = ByteArrayOutputStream()
            // PNG preserves the transparent background often used in academy crests.
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) { "No se pudo preparar la imagen." }
            output.toByteArray().also {
                require(it.size <= MAX_OUTPUT_BYTES) { "El escudo es demasiado grande. Selecciona una imagen más sencilla." }
            }
        } finally {
            bitmap.recycle()
        }
    }
}
