package com.dpa.sportpro.data.storage

import android.content.ContentResolver
import android.net.Uri
import com.dpa.sportpro.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.tasks.await
import java.net.URI
import java.util.UUID

/** Only public academy crests. Never pass a player's private photo to this uploader. */
class SupabaseCrestStorage(private val resolver: ContentResolver) {
    suspend fun upload(uri: Uri): String {
        val url = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
        val key = BuildConfig.SUPABASE_PUBLISHABLE_KEY.trim()
        check(url.isNotBlank() && key.isNotBlank()) { "La subida de imágenes aún no está configurada." }
        val endpoint = URI(url)
        check(endpoint.scheme == "https" && endpoint.host?.endsWith(".supabase.co") == true &&
            endpoint.rawUserInfo == null && endpoint.port == -1 &&
            endpoint.rawQuery == null && endpoint.rawFragment == null && endpoint.path.isNullOrEmpty()) {
            "La dirección del almacenamiento no es válida."
        }
        check(key.startsWith("sb_publishable_")) { "Configura la clave publicable de Supabase." }
        val auth = FirebaseAuth.getInstance()
        val user = checkNotNull(auth.currentUser) { "Inicia sesión para subir el escudo." }
        val token = user.getIdToken(false).await()
        check(token.claims["role"] == "authenticated" && token.claims["app_role"] == "ADM") {
            "Tu cuenta aún no tiene permiso para subir escudos."
        }
        val bytes = CrestImageEncoder.encode(resolver, uri)
        check(auth.currentUser?.uid == user.uid) { "La sesión cambió. Vuelve a intentarlo." }
        val client = createSupabaseClient(url, key) {
            accessToken = {
                check(auth.currentUser?.uid == user.uid) { "La sesión cambió." }
                user.getIdToken(false).await().token
            }
            install(Storage)
        }
        return try {
            val path = "${user.uid}/${UUID.randomUUID()}.png"
            val bucket = client.storage.from("imagenes")
            bucket.upload(path, bytes) {
                upsert = false
                contentType = ContentType.Image.PNG
            }
            check(auth.currentUser?.uid == user.uid) { "La sesión cambió. Vuelve a iniciar sesión." }
            bucket.publicUrl(path)
        } finally {
            client.close()
        }
    }
}
