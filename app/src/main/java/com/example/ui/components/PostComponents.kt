package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Post
import com.example.data.model.UserProfile
import com.example.data.repository.SafetyRepository
import com.example.data.repository.SocialRepository
import com.example.ui.theme.PrimeBlue
import com.example.ui.theme.PrimeBorder
import com.example.ui.theme.PrimeCardBackground
import com.example.ui.theme.PrimeNavySurface
import com.example.ui.theme.PrimePurple
import com.example.ui.theme.PrimeRed
import com.example.ui.theme.PrimeTextMuted
import com.example.ui.theme.PrimeTextPrimary
import com.example.ui.theme.PrimeTextSecondary
import kotlinx.coroutines.launch

@Composable
fun PostCard(
    post: Post,
    authorProfile: UserProfile?,
    currentUserId: String,
    isLiked: Boolean,
    onLikeClick: () -> Unit,
    onCommentsClick: () -> Unit,
    onShareClick: () -> Unit,
    onReportClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onAuthorClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isOwner = post.authorId == currentUserId

    val displayName = authorProfile?.displayName?.ifBlank { authorProfile.username }
        ?: post.authorUsername.ifBlank { "Player" }
    val handle = authorProfile?.username?.ifBlank { null } ?: post.authorUsername
    val avatarUrl = authorProfile?.avatarUrl?.ifBlank { null } ?: post.authorAvatarUrl

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PrimeCardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimeBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onAuthorClick() }
                ) {
                    GamerAvatar(
                        avatarUrl = avatarUrl,
                        displayName = displayName,
                        size = 40
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = displayName,
                                color = PrimeTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            VerificationBadges(
                                isOwner = authorProfile?.isOwner == true,
                                isVerified = authorProfile?.isVerified == true
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (handle.isNotBlank()) {
                                Text(
                                    text = "@$handle",
                                    color = PrimeTextMuted,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = " • ",
                                    color = PrimeTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                text = formatRelativeTime(post.createdAt),
                                color = PrimeTextMuted,
                                fontSize = 11.sp
                            )
                            if (post.gameTag.isNotBlank()) {
                                Text(
                                    text = " • #${post.gameTag}",
                                    color = PrimePurple,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Post Options", tint = PrimeTextMuted)
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(PrimeNavySurface)
                    ) {
                        if (isOwner) {
                            DropdownMenuItem(
                                text = { Text("Delete Post", color = PrimeRed) },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteClick()
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = PrimeRed) }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Report Post", color = PrimeRed) },
                                onClick = {
                                    menuExpanded = false
                                    onReportClick()
                                },
                                leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null, tint = PrimeRed) }
                            )
                        }
                    }
                }
            }

            if (post.content.isNotBlank()) {
                Text(
                    text = post.content,
                    color = PrimeTextPrimary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 10.dp, bottom = 10.dp),
                    lineHeight = 20.sp
                )
            } else {
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (post.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = "Post Media",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onLikeClick() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like Post",
                        tint = if (isLiked) PrimeRed else PrimeTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${post.likesCount}",
                        color = if (isLiked) PrimeRed else PrimeTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onCommentsClick() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "Comments",
                        tint = PrimeTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${post.commentsCount}",
                        color = PrimeTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onShareClick() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Post",
                        tint = PrimeTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostCommentsSheet(
    post: Post,
    currentUserId: String,
    usersMap: Map<String, UserProfile>,
    socialRepository: SocialRepository,
    blockedUserIds: Set<String> = emptySet(),
    onOpenProfile: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val comments by socialRepository.observeComments(post.id).collectAsState(initial = emptyList())
    val visibleComments = remember(comments, blockedUserIds) {
        comments.filter { !blockedUserIds.contains(it.authorId) }
    }
    var commentText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PrimeCardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Comments (${visibleComments.size})",
                color = PrimeTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .height(320.dp)
            ) {
                if (visibleComments.isEmpty()) {
                    item {
                        Text(
                            text = "No comments yet. Start the conversation!",
                            color = PrimeTextMuted,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                } else {
                    items(visibleComments, key = { it.id }) { comment ->
                        val profile = usersMap[comment.authorId]
                        val name = profile?.username?.ifBlank { null } ?: comment.authorUsername
                        val avatar = profile?.avatarUrl?.ifBlank { null } ?: comment.authorAvatarUrl

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            GamerAvatar(
                                avatarUrl = avatar,
                                displayName = name,
                                size = 34,
                                onClick = {
                                    onDismiss()
                                    onOpenProfile(comment.authorId)
                                }
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        onDismiss()
                                        onOpenProfile(comment.authorId)
                                    }
                                ) {
                                    Text(
                                        text = name,
                                        color = PrimeTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    VerificationBadges(
                                        isOwner = profile?.isOwner == true,
                                        isVerified = profile?.isVerified == true
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = formatRelativeTime(comment.createdAt),
                                        color = PrimeTextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    text = comment.text,
                                    color = PrimeTextSecondary,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            if (comment.authorId == currentUserId) {
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            socialRepository.deleteComment(post.id, comment.id)
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete comment",
                                        tint = PrimeTextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("Write a comment...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimeTextPrimary,
                        unfocusedTextColor = PrimeTextPrimary,
                        focusedBorderColor = PrimeBlue,
                        unfocusedBorderColor = PrimeBorder
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (commentText.isNotBlank()) {
                            isSending = true
                            coroutineScope.launch {
                                val res = socialRepository.addComment(post.id, commentText)
                                isSending = false
                                if (res.isSuccess) {
                                    commentText = ""
                                } else {
                                    Toast.makeText(
                                        context,
                                        res.exceptionOrNull()?.localizedMessage ?: "Failed to post comment.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    },
                    enabled = commentText.isNotBlank() && !isSending,
                    modifier = Modifier.testTag("send_comment_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Comment", tint = PrimeBlue)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportContentSheet(
    title: String,
    targetType: String,
    targetId: String,
    safetyRepository: SafetyRepository,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var reason by remember { mutableStateOf("Spam or abusive content") }
    var isReporting by remember { mutableStateOf(false) }
    var reportSuccess by remember { mutableStateOf(false) }
    var reportError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PrimeCardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = title,
                color = PrimeRed,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Help us keep the PRIME ESPORT community safe and competitive.",
                color = PrimeTextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            val reasons = listOf(
                "Spam or abusive content",
                "Hate speech or harassment",
                "Scam or misleading claim",
                "Inappropriate media"
            )
            reasons.forEach { r ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { reason = r }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .border(2.dp, if (reason == r) PrimeRed else PrimeBorder, CircleShape)
                            .background(if (reason == r) PrimeRed else Color.Transparent)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(r, color = PrimeTextPrimary, fontSize = 14.sp)
                }
            }

            if (reportError != null) {
                Text(reportError ?: "", color = PrimeRed, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (reportSuccess) {
                Text(
                    text = "Thank you. Your report has been submitted for moderation review.",
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            } else {
                Button(
                    onClick = {
                        isReporting = true
                        reportError = null
                        coroutineScope.launch {
                            val res = safetyRepository.submitReport(
                                targetType = targetType,
                                targetId = targetId,
                                reason = reason
                            )
                            isReporting = false
                            if (res.isSuccess) {
                                reportSuccess = true
                            } else {
                                reportError = res.exceptionOrNull()?.localizedMessage ?: "Failed to submit report."
                            }
                        }
                    },
                    enabled = !isReporting,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimeRed),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isReporting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    } else {
                        Text("Submit Report", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    title: String,
    message: String,
    isDeleting: Boolean,
    errorMessage: String? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        containerColor = PrimeNavySurface,
        title = {
            Text(title, color = PrimeTextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(message, color = PrimeTextSecondary, fontSize = 13.sp)
                if (!errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage, color = PrimeRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isDeleting,
                colors = ButtonDefaults.buttonColors(containerColor = PrimeRed)
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) {
                Text("Cancel", color = PrimeTextSecondary)
            }
        }
    )
}
