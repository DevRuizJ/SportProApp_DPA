package com.dpa.sportpro.data.model

data class CommunityPost(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorRole: String,
    val text: String,
    val photoUris: List<String>,
    val createdAtMillis: Long,
    val reactionsByUser: Map<String, String> = emptyMap(),
    val comments: List<CommunityComment> = emptyList(),
)

data class CommunityComment(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorRole: String,
    val text: String,
    val createdAtMillis: Long,
)
