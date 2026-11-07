package com.dpa.sportpro.ui.community

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dpa.sportpro.data.model.CommunityComment
import com.dpa.sportpro.data.model.CommunityPost
import com.dpa.sportpro.data.model.MatchChronicle
import com.dpa.sportpro.data.model.ReportReason
import com.dpa.sportpro.data.model.ReportTargetType
import com.dpa.sportpro.data.model.UserProfile
import com.dpa.sportpro.data.repository.CommunityRepository
import com.dpa.sportpro.data.repository.MatchChronicleRepository
import com.dpa.sportpro.data.repository.ModerationRepository
import com.dpa.sportpro.ui.moderation.ReportActionMenu
import com.dpa.sportpro.ui.theme.CardBackground
import com.dpa.sportpro.ui.theme.DarkBackground
import com.dpa.sportpro.ui.theme.ErrorRed
import com.dpa.sportpro.ui.theme.InputBorder
import com.dpa.sportpro.ui.theme.NeonGreen
import com.dpa.sportpro.ui.theme.TextMuted
import com.dpa.sportpro.ui.theme.TextWhite
import java.io.FileNotFoundException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val CheerBlue = Color(0xFF2196F3)
private val AvatarBlue = Color(0xFF168EF0)

@Composable
fun CommunityScreen(userProfile: UserProfile, onBackClick: () -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { CommunityRepository.getInstance(context) }
    val chronicleRepository = remember(context) { MatchChronicleRepository.getInstance(context) }
    val moderation = remember(context) { ModerationRepository.getInstance(context) }
    val posts by repository.posts.collectAsState()
    val chronicles by chronicleRepository.chronicles.collectAsState()
    val reports by moderation.reports.collectAsState()
    val suspendedUsers by moderation.suspendedUserIds.collectAsState()
    var postText by remember { mutableStateOf("") }
    var selectedPhotos by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var composerError by remember { mutableStateOf<String?>(null) }
    var commentError by remember { mutableStateOf<Pair<String, String>?>(null) }
    var reportFeedback by remember { mutableStateOf<String?>(null) }
    val commentDrafts = remember { mutableStateMapOf<String, String>() }
    val visibleToCommunity = userProfile.role.code in setOf("ADM", "DT", "JUG", "PAD")
    val isSuspended = userProfile.uid in suspendedUsers
    val visiblePosts =
        remember(posts, reports) {
            posts.filterNot { moderation.isPreventivelyHidden(ReportTargetType.POST, it.id) }
        }
    val publishedChronicles = remember(chronicles) {
        chronicles.filter { it.status == com.dpa.sportpro.data.model.ChronicleStatus.APPROVED }
    }
    val photoPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris.isNotEmpty()) {
                val remaining = CommunityRepository.MAX_PHOTOS - selectedPhotos.size
                val additions = uris.distinct().filterNot { it in selectedPhotos }.take(remaining)
                val accessiblePhotos =
                    additions.filter { uri ->
                        try {
                            context.contentResolver.takePersistableUriPermission(
                                uri,
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                            )
                            true
                        } catch (_: SecurityException) {
                            false
                        }
                    }
                if (accessiblePhotos.size < additions.size) {
                    composerError = "No se pudo conservar el acceso a una de las fotografías."
                }
                selectedPhotos =
                    (selectedPhotos + accessiblePhotos).take(CommunityRepository.MAX_PHOTOS)
                if (uris.size > remaining) {
                    composerError = "Cada publicación admite hasta 3 fotografías."
                }
            }
        }

    Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground) {
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = TextWhite,
                    )
                }
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    Text(
                        text = "Comunidad",
                        color = TextWhite,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text("La red de fútbol base", color = TextMuted, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (!visibleToCommunity) {
                EmptyCommunity(
                    "Esta sección está disponible para Administradores, DT, jugadores y padres."
                )
            } else {
                if (isSuspended) {
                    EmptyCommunity(
                        "Tu cuenta está suspendida. No puedes publicar, comentar ni reaccionar."
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                } else if (userProfile.isVerified) {
                    ComposerCard(
                        userProfile = userProfile,
                        text = postText,
                        onTextChange = {
                            postText = it
                            composerError = null
                        },
                        photos = selectedPhotos,
                        onAddPhotos = {
                            composerError = null
                            photoPicker.launch(arrayOf("image/*"))
                        },
                        onRemovePhoto = { selectedPhotos = selectedPhotos - it },
                        errorMessage = composerError,
                        onPublish = {
                            try {
                                repository.createPost(
                                    authorId = userProfile.uid,
                                    authorName = displayName(userProfile),
                                    authorRole = userProfile.role.displayName,
                                    isVerified = userProfile.isVerified,
                                    text = postText,
                                    photoUris = selectedPhotos.map(Uri::toString),
                                )
                                postText = ""
                                selectedPhotos = emptyList()
                                composerError = null
                            } catch (error: IllegalArgumentException) {
                                composerError = error.message ?: "No se pudo publicar el contenido."
                            }
                        },
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                } else {
                    EmptyCommunity(
                        "Tu perfil aún no está verificado. Podrás publicar cuando completes la verificación."
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                }

                reportFeedback?.let {
                    Text(
                        it,
                        color = NeonGreen,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                }

                publishedChronicles.forEach { chronicle ->
                    PublishedChronicleCard(chronicle)
                    Spacer(modifier = Modifier.height(14.dp))
                }

                if (visiblePosts.isEmpty() && publishedChronicles.isEmpty()) {
                    EmptyCommunity(
                        if (posts.isEmpty()) {
                            "Todavía no hay publicaciones. ¡Comparte el primer momento del equipo!"
                        } else {
                            "Las publicaciones reportadas están ocultas mientras se revisan."
                        }
                    )
                } else {
                    visiblePosts.forEach { post ->
                        CommunityPostCard(
                            post = post,
                            currentUserId = userProfile.uid,
                            enabled = !isSuspended,
                            commentDraft = commentDrafts[post.id].orEmpty(),
                            onCommentDraftChange = { commentDrafts[post.id] = it },
                            commentError = commentError?.takeIf { it.first == post.id }?.second,
                            onLike = { repository.setReaction(post.id, userProfile.uid, "LIKE") },
                            onCheer = { repository.setReaction(post.id, userProfile.uid, "CHEER") },
                            onComment = {
                                try {
                                    repository.addComment(
                                        postId = post.id,
                                        authorId = userProfile.uid,
                                        authorName = displayName(userProfile),
                                        authorRole = userProfile.role.displayName,
                                        text = commentDrafts[post.id].orEmpty(),
                                    )
                                    commentDrafts[post.id] = ""
                                    commentError = null
                                } catch (error: IllegalArgumentException) {
                                    commentError =
                                        post.id to (error.message ?: "Escribe un comentario.")
                                }
                            },
                            onReportPost = { reason ->
                                moderation.report(
                                    targetType = ReportTargetType.POST,
                                    targetId = post.id,
                                    targetOwnerId = post.authorId,
                                    targetTitle =
                                        post.text.ifBlank {
                                            "Publicación con ${post.photoUris.size} fotografías"
                                        },
                                    reporterId = userProfile.uid,
                                    reporterName = displayName(userProfile),
                                    reason = reason,
                                )
                                reportFeedback = "Reporte enviado al equipo de moderación."
                            },
                            onReportComment = { comment, reason ->
                                moderation.report(
                                    targetType = ReportTargetType.COMMENT,
                                    targetId = comment.id,
                                    targetParentId = post.id,
                                    targetOwnerId = comment.authorId,
                                    targetTitle = comment.text,
                                    reporterId = userProfile.uid,
                                    reporterName = displayName(userProfile),
                                    reason = reason,
                                )
                                reportFeedback = "Comentario reportado para revisión."
                            },
                            moderation = moderation,
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PublishedChronicleCard(chronicle: MatchChronicle) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .border(1.dp, NeonGreen.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                    .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Group, contentDescription = null, tint = NeonGreen)
                Text(
                    "Crónica oficial del partido",
                    color = NeonGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                )
                Text(
                    "DT",
                    color = DarkBackground,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier =
                        Modifier.clip(RoundedCornerShape(6.dp))
                            .background(NeonGreen)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            Text(
                "${chronicle.homeTeamName} ${chronicle.homeScore} - ${chronicle.awayScore} ${chronicle.awayTeamName}",
                color = TextWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                chronicle.narrative,
                color = TextWhite,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                modifier = Modifier.padding(top = 9.dp),
            )
            if (chronicle.technicalComment.isNotBlank()) {
                Text(
                    "Análisis del Director Técnico",
                    color = NeonGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    chronicle.technicalComment,
                    color = TextMuted,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun ComposerCard(
    userProfile: UserProfile,
    text: String,
    onTextChange: (String) -> Unit,
    photos: List<Uri>,
    onAddPhotos: () -> Unit,
    onRemovePhoto: (Uri) -> Unit,
    errorMessage: String?,
    onPublish: () -> Unit,
) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(17.dp))
                .background(CardBackground)
                .border(1.dp, InputBorder, RoundedCornerShape(17.dp))
                .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(
                displayName(userProfile),
                Modifier.size(40.dp),
                verified = userProfile.isVerified,
            )
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = {
                    Text("Compartir noticias con el club...", color = TextMuted, fontSize = 14.sp)
                },
                modifier = Modifier.weight(1f).padding(start = 10.dp),
                minLines = 1,
                maxLines = 4,
                shape = RoundedCornerShape(12.dp),
                colors = communityFieldColors(),
            )
        }
        if (photos.isNotEmpty()) {
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                photos.forEach { uri ->
                    Box {
                        CommunityPhoto(uri, Modifier.size(82.dp).clip(RoundedCornerShape(10.dp)))
                        Text(
                            "×",
                            color = TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier =
                                Modifier.align(Alignment.TopEnd)
                                    .padding(3.dp)
                                    .clip(CircleShape)
                                    .background(ErrorRed)
                                    .clickable { onRemovePhoto(uri) }
                                    .padding(horizontal = 6.dp),
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${photos.size}/3 fotos", color = TextMuted, fontSize = 12.sp)
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = onAddPhotos,
                enabled = photos.size < CommunityRepository.MAX_PHOTOS,
            ) {
                Icon(
                    Icons.Default.Image,
                    contentDescription = "Agregar fotografías",
                    tint = NeonGreen,
                )
            }
            Button(
                onClick = onPublish,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    tint = DarkBackground,
                )
                Text(
                    "Publicar",
                    color = DarkBackground,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 5.dp),
                )
            }
        }
        errorMessage?.let {
            Text(it, color = ErrorRed, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun CommunityPostCard(
    post: CommunityPost,
    currentUserId: String,
    enabled: Boolean,
    moderation: ModerationRepository,
    commentDraft: String,
    onCommentDraftChange: (String) -> Unit,
    commentError: String?,
    onLike: () -> Unit,
    onCheer: () -> Unit,
    onComment: () -> Unit,
    onReportPost: (ReportReason) -> Unit,
    onReportComment: (CommunityComment, ReportReason) -> Unit,
) {
    val currentReaction = post.reactionsByUser[currentUserId]
    val visibleComments =
        post.comments.filterNot { moderation.isPreventivelyHidden(ReportTargetType.COMMENT, it.id) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(post.authorName, Modifier.size(42.dp), verified = true)
                Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            post.authorName,
                            color = TextWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            post.authorRole,
                            color = NeonGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier =
                                Modifier.padding(start = 8.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(NeonGreen.copy(alpha = 0.12f))
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                        )
                    }
                    Text(relativeTime(post.createdAtMillis), color = TextMuted, fontSize = 12.sp)
                }
                ReportActionMenu(onSubmitReport = onReportPost)
            }

            if (post.text.isNotBlank()) {
                Text(
                    text = post.text,
                    color = TextWhite,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(top = 13.dp),
                )
            }

            if (post.photoUris.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    post.photoUris.take(CommunityRepository.MAX_PHOTOS).forEach { uri ->
                        CommunityPhoto(
                            Uri.parse(uri),
                            Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(11.dp)),
                        )
                    }
                }
            }

            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(top = 12.dp)
                        .border(width = 1.dp, color = InputBorder, shape = RoundedCornerShape(1.dp))
                        .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ReactionButton(
                    count = post.reactionsByUser.values.count { it == "LIKE" },
                    selected = currentReaction == "LIKE",
                    color = NeonGreen,
                    icon = { Icon(Icons.Default.ThumbUp, contentDescription = null, tint = it) },
                    onClick = onLike,
                    enabled = enabled,
                )
                Spacer(modifier = Modifier.size(12.dp))
                ReactionButton(
                    count = post.reactionsByUser.values.count { it == "CHEER" },
                    selected = currentReaction == "CHEER",
                    color = CheerBlue,
                    icon = { Icon(Icons.Default.Favorite, contentDescription = null, tint = it) },
                    onClick = onCheer,
                    enabled = enabled,
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    Icons.Default.Forum,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    "${visibleComments.size} comentarios",
                    color = TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 5.dp),
                )
            }

            visibleComments.takeLast(2).forEach { comment ->
                CommentRow(comment) {
                    ReportActionMenu { reason -> onReportComment(comment, reason) }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = commentDraft,
                    onValueChange = onCommentDraftChange,
                    enabled = enabled,
                    placeholder = {
                        Text("Escribe un comentario...", color = TextMuted, fontSize = 13.sp)
                    },
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    colors = communityFieldColors(),
                )
                IconButton(onClick = onComment, enabled = enabled) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar comentario",
                        tint = NeonGreen,
                    )
                }
            }
            commentError?.let {
                Text(
                    it,
                    color = ErrorRed,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun ReactionButton(
    count: Int,
    selected: Boolean,
    color: Color,
    icon: @Composable (Color) -> Unit,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    Row(
        modifier =
            Modifier.clip(RoundedCornerShape(8.dp))
                .clickable(enabled = enabled, onClick = onClick)
                .padding(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon(if (selected || count > 0) color else TextMuted)
        Text(
            "$count",
            color = if (selected || count > 0) color else TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun CommentRow(comment: CommunityComment, actions: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        UserAvatar(comment.authorName, Modifier.size(32.dp), verified = false)
        Column(modifier = Modifier.weight(1f).padding(start = 9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    comment.authorName,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f),
                )
                actions()
            }
            Text(comment.authorRole, color = TextMuted, fontSize = 11.sp)
            Text(comment.text, color = TextMuted, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun UserAvatar(name: String, modifier: Modifier, verified: Boolean) {
    Box(
        modifier = modifier.clip(CircleShape).background(AvatarBlue),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials(name), color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        if (verified) {
            Icon(
                Icons.Default.Verified,
                contentDescription = "Perfil verificado",
                tint = NeonGreen,
                modifier =
                    Modifier.align(Alignment.BottomEnd)
                        .size(15.dp)
                        .background(DarkBackground, CircleShape),
            )
        }
    }
}

@Composable
private fun CommunityPhoto(uri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap by
        produceState<Bitmap?>(initialValue = null, uri) {
            value = withContext(Dispatchers.IO) { loadCommunityBitmap(context, uri) }
        }
    if (bitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "Fotografía de la comunidad",
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        Box(modifier = modifier.background(InputBorder), contentAlignment = Alignment.Center) {
            Icon(
                Icons.Default.Image,
                contentDescription = "Fotografía no disponible",
                tint = TextMuted,
            )
        }
    }
}

private fun loadCommunityBitmap(context: Context, uri: Uri): Bitmap? =
    try {
        context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
    } catch (_: FileNotFoundException) {
        null
    } catch (_: SecurityException) {
        null
    }

@Composable
private fun EmptyCommunity(message: String) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBackground)
                .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Default.Group,
            contentDescription = null,
            tint = NeonGreen,
            modifier = Modifier.size(30.dp),
        )
        Text(message, color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
    }
}

@Composable
private fun communityFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextWhite,
        unfocusedTextColor = TextWhite,
        focusedBorderColor = NeonGreen,
        unfocusedBorderColor = InputBorder,
        cursorColor = NeonGreen,
        focusedContainerColor = DarkBackground,
        unfocusedContainerColor = DarkBackground,
    )

private fun displayName(profile: UserProfile) =
    listOf(profile.names, profile.lastNames).filter(String::isNotBlank).joinToString(" ")

private fun initials(name: String): String =
    name
        .trim()
        .split(Regex("\\s+"))
        .take(2)
        .mapNotNull { it.firstOrNull() }
        .joinToString("")
        .uppercase(Locale.getDefault())
        .ifBlank { "SP" }

private fun relativeTime(timestamp: Long): String {
    val elapsed = (System.currentTimeMillis() - timestamp).coerceAtLeast(0)
    val minutes = elapsed / 60_000
    return when {
        minutes < 1 -> "Ahora"
        minutes < 60 -> "Hace ${minutes} min"
        minutes < 1_440 -> "Hace ${minutes / 60} h"
        minutes < 2_880 -> "Ayer"
        else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}
