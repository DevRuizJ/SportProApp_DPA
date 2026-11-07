package com.dpa.sportpro.data.repository

import android.content.Context
import com.dpa.sportpro.data.model.CommunityComment
import com.dpa.sportpro.data.model.CommunityPost
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class CommunityRepository private constructor(context: Context) {

    private val preferencesContext = context.applicationContext
    private val preferences =
        preferencesContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val _posts = MutableStateFlow(loadPosts())

    val posts: StateFlow<List<CommunityPost>> = _posts.asStateFlow()

    fun createPost(
        authorId: String,
        authorName: String,
        authorRole: String,
        isVerified: Boolean,
        text: String,
        photoUris: List<String>,
    ) {
        requireNotSuspended(authorId)
        require(isVerified) { "Solo los perfiles verificados pueden publicar." }
        require(authorId.isNotBlank()) { "Inicia sesión para publicar." }
        require(text.isNotBlank() || photoUris.isNotEmpty()) {
            "Escribe un mensaje o selecciona al menos una fotografía."
        }
        require(photoUris.size <= MAX_PHOTOS) { "Cada publicación admite hasta 3 fotografías." }

        val post =
            CommunityPost(
                id = UUID.randomUUID().toString(),
                authorId = authorId,
                authorName = authorName,
                authorRole = authorRole,
                text = text.trim(),
                photoUris = photoUris.distinct(),
                createdAtMillis = System.currentTimeMillis(),
            )
        publish(listOf(post) + _posts.value)
    }

    fun addComment(
        postId: String,
        authorId: String,
        authorName: String,
        authorRole: String,
        text: String,
    ) {
        requireNotSuspended(authorId)
        require(authorId.isNotBlank()) { "Inicia sesión para comentar." }
        require(text.isNotBlank()) { "Escribe un comentario antes de publicarlo." }
        val post =
            _posts.value.firstOrNull { it.id == postId }
                ?: throw IllegalArgumentException("No se encontró la publicación.")
        val comment =
            CommunityComment(
                id = UUID.randomUUID().toString(),
                authorId = authorId,
                authorName = authorName,
                authorRole = authorRole,
                text = text.trim(),
                createdAtMillis = System.currentTimeMillis(),
            )
        publish(
            _posts.value.map { item ->
                if (item.id == postId) post.copy(comments = post.comments + comment) else item
            }
        )
    }

    fun deletePost(postId: String, moderatorRole: String) {
        requireModerator(moderatorRole)
        require(_posts.value.any { it.id == postId }) { "No se encontró la publicación." }
        publish(_posts.value.filterNot { it.id == postId })
        ModerationRepository.getInstance(preferencesContext)
            .resolveTarget(com.dpa.sportpro.data.model.ReportTargetType.POST, postId, moderatorRole)
    }

    fun deleteComment(postId: String, commentId: String, moderatorRole: String) {
        requireModerator(moderatorRole)
        val post =
            _posts.value.firstOrNull { it.id == postId }
                ?: throw IllegalArgumentException("No se encontró la publicación.")
        require(post.comments.any { it.id == commentId }) { "No se encontró el comentario." }
        publish(
            _posts.value.map { item ->
                if (item.id == postId) {
                    item.copy(comments = item.comments.filterNot { it.id == commentId })
                } else {
                    item
                }
            }
        )
        ModerationRepository.getInstance(preferencesContext)
            .resolveTarget(
                com.dpa.sportpro.data.model.ReportTargetType.COMMENT,
                commentId,
                moderatorRole,
            )
    }

    private fun requireNotSuspended(userId: String) {
        require(!ModerationRepository.getInstance(preferencesContext).isUserSuspended(userId)) {
            "Esta cuenta está suspendida y no puede interactuar con la comunidad."
        }
    }

    private fun requireModerator(role: String) {
        require(role == "ADM") { "Solo un Administrador puede eliminar contenido." }
    }

    fun setReaction(postId: String, userId: String, reaction: String) {
        requireNotSuspended(userId)
        require(userId.isNotBlank()) { "Inicia sesión para reaccionar." }
        require(reaction in SUPPORTED_REACTIONS) { "La reacción seleccionada no está disponible." }
        val post =
            _posts.value.firstOrNull { it.id == postId }
                ?: throw IllegalArgumentException("No se encontró la publicación.")
        val updatedReactions =
            if (post.reactionsByUser[userId] == reaction) post.reactionsByUser - userId
            else post.reactionsByUser + (userId to reaction)
        publish(
            _posts.value.map { item ->
                if (item.id == postId) post.copy(reactionsByUser = updatedReactions) else item
            }
        )
    }

    private fun publish(posts: List<CommunityPost>) {
        _posts.value = posts
        val json = JSONArray()
        posts.forEach { post -> json.put(post.toJson()) }
        preferences.edit().putString(POSTS_KEY, json.toString()).apply()
    }

    private fun loadPosts(): List<CommunityPost> {
        val saved = preferences.getString(POSTS_KEY, null) ?: return emptyList()
        val json = JSONArray(saved)
        return List(json.length()) { index -> json.getJSONObject(index).toCommunityPost() }
    }

    private fun JSONObject.toCommunityPost(): CommunityPost {
        val photoArray = getJSONArray("photoUris")
        val reactionArray = getJSONArray("reactions")
        val commentArray = getJSONArray("comments")
        return CommunityPost(
            id = getString("id"),
            authorId = getString("authorId"),
            authorName = getString("authorName"),
            authorRole = getString("authorRole"),
            text = getString("text"),
            photoUris = List(photoArray.length()) { photoArray.getString(it) },
            createdAtMillis = getLong("createdAtMillis"),
            reactionsByUser =
                List(reactionArray.length()) {
                        val reaction = reactionArray.getJSONObject(it)
                        reaction.getString("userId") to reaction.getString("type")
                    }
                    .toMap(),
            comments =
                List(commentArray.length()) { commentArray.getJSONObject(it).toCommunityComment() },
        )
    }

    private fun JSONObject.toCommunityComment() =
        CommunityComment(
            id = getString("id"),
            authorId = getString("authorId"),
            authorName = getString("authorName"),
            authorRole = getString("authorRole"),
            text = getString("text"),
            createdAtMillis = getLong("createdAtMillis"),
        )

    private fun CommunityPost.toJson(): JSONObject =
        JSONObject()
            .put("id", id)
            .put("authorId", authorId)
            .put("authorName", authorName)
            .put("authorRole", authorRole)
            .put("text", text)
            .put("photoUris", JSONArray(photoUris))
            .put("createdAtMillis", createdAtMillis)
            .put(
                "reactions",
                JSONArray().apply {
                    reactionsByUser.forEach { (userId, type) ->
                        put(JSONObject().put("userId", userId).put("type", type))
                    }
                },
            )
            .put(
                "comments",
                JSONArray().apply {
                    comments.forEach { comment ->
                        put(
                            JSONObject()
                                .put("id", comment.id)
                                .put("authorId", comment.authorId)
                                .put("authorName", comment.authorName)
                                .put("authorRole", comment.authorRole)
                                .put("text", comment.text)
                                .put("createdAtMillis", comment.createdAtMillis)
                        )
                    }
                },
            )

    companion object {
        private const val PREFERENCES_NAME = "sportpro_community"
        private const val POSTS_KEY = "community_posts"
        const val MAX_PHOTOS = 3
        private val SUPPORTED_REACTIONS = setOf("LIKE", "CHEER")

        @Volatile private var instance: CommunityRepository? = null

        fun getInstance(context: Context): CommunityRepository =
            instance
                ?: synchronized(this) {
                    instance
                        ?: CommunityRepository(context.applicationContext).also { instance = it }
                }
    }
}
