package com.example.ui.screens.upload

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.repository.CreatorRepository
import com.example.data.repository.SocialRepository
import com.example.data.storage.MediaUploadService
import com.example.ui.components.PrimeVideoPlayer
import com.example.ui.theme.PrimeBlue
import com.example.ui.theme.PrimeBorder
import com.example.ui.theme.PrimeCardBackground
import com.example.ui.theme.PrimeNavySurface
import com.example.ui.theme.PrimePurple
import com.example.ui.theme.PrimeRed
import com.example.ui.theme.PrimeTextMuted
import com.example.ui.theme.PrimeTextPrimary
import com.example.ui.theme.PrimeTextSecondary
import com.google.firebase.storage.UploadTask
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

enum class UploadType {
    MENU,
    POST,
    VIDEO,
    REEL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateContentSheet(
    socialRepository: SocialRepository,
    creatorRepository: CreatorRepository,
    currentUserId: String,
    initialMode: UploadType = UploadType.MENU,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onUploadSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val mediaUploadService = remember(context) { MediaUploadService(context) }

    var currentView by remember(initialMode) { mutableStateOf(initialMode) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PrimeNavySurface,
        dragHandle = null
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .padding(horizontal = 20.dp, vertical = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                when (currentView) {
                    UploadType.MENU -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Create & Upload Content",
                                    color = PrimeTextPrimary,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "Share with the PRIME ESPORT community",
                                    color = PrimeTextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                            IconButton(onClick = onDismiss, modifier = Modifier.testTag("upload_menu_close")) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = PrimeTextMuted)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        UploadOptionCard(
                            title = "Create Post",
                            description = "Share gaming clutch shots, tactical thoughts, and community announcements.",
                            icon = Icons.Default.DynamicFeed,
                            accentColor = PrimeBlue,
                            testTag = "upload_option_post",
                            onClick = { currentView = UploadType.POST }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        UploadOptionCard(
                            title = "Upload Video",
                            description = "Publish full match series, guides, and tournament replays to your channel.",
                            icon = Icons.Default.Movie,
                            accentColor = PrimePurple,
                            testTag = "upload_option_video",
                            onClick = { currentView = UploadType.VIDEO }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        UploadOptionCard(
                            title = "Upload Reel",
                            description = "Post fast-paced vertical clips, 1v4 highlights, and clutch plays.",
                            icon = Icons.Default.PlayCircle,
                            accentColor = PrimeRed,
                            testTag = "upload_option_reel",
                            onClick = { currentView = UploadType.REEL }
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    UploadType.POST -> {
                        CreatePostForm(
                            socialRepository = socialRepository,
                            mediaUploadService = mediaUploadService,
                            currentUserId = currentUserId,
                            onBack = {
                                if (initialMode == UploadType.MENU) currentView = UploadType.MENU else onDismiss()
                            },
                            onSuccess = {
                                Toast.makeText(context, "Post published to PRIME ESPORT!", Toast.LENGTH_SHORT).show()
                                onUploadSuccess()
                                onDismiss()
                            }
                        )
                    }

                    UploadType.VIDEO -> {
                        UploadVideoForm(
                            creatorRepository = creatorRepository,
                            mediaUploadService = mediaUploadService,
                            currentUserId = currentUserId,
                            onBack = {
                                if (initialMode == UploadType.MENU) currentView = UploadType.MENU else onDismiss()
                            },
                            onSuccess = {
                                Toast.makeText(context, "Video published to your profile channel!", Toast.LENGTH_SHORT).show()
                                onUploadSuccess()
                                onDismiss()
                            }
                        )
                    }

                    UploadType.REEL -> {
                        CreateReelForm(
                            socialRepository = socialRepository,
                            mediaUploadService = mediaUploadService,
                            currentUserId = currentUserId,
                            onBack = {
                                if (initialMode == UploadType.MENU) currentView = UploadType.MENU else onDismiss()
                            },
                            onSuccess = {
                                Toast.makeText(context, "Reel published to PRIME REELS!", Toast.LENGTH_SHORT).show()
                                onUploadSuccess()
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UploadOptionCard(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        color = PrimeCardBackground,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimeBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = PrimeTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = description, color = PrimeTextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
private fun CreatePostForm(
    socialRepository: SocialRepository,
    mediaUploadService: MediaUploadService,
    currentUserId: String,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var postContent by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf("BGMI") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadPercent by remember { mutableIntStateOf(0) }
    var uploadJob by remember { mutableStateOf<Job?>(null) }
    var activeTask by remember { mutableStateOf<UploadTask?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val tags = listOf("BGMI", "Minecraft", "Free Fire", "Valorant", "COD Mobile", "Clutch")

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                selectedImageUri = uri
                errorMessage = null
            }
        }
    )

    fun startPublish() {
        if (postContent.trim().isEmpty() && selectedImageUri == null) {
            errorMessage = "Please enter some text or attach an image."
            return
        }
        isUploading = true
        uploadPercent = 0
        errorMessage = null

        uploadJob = coroutineScope.launch {
            var uploadedImageUrl = ""
            var uploadedStoragePath = ""
            val imageUri = selectedImageUri
            if (imageUri != null) {
                val uploadResult = mediaUploadService.uploadMedia(
                    uri = imageUri,
                    folder = "posts",
                    userId = currentUserId,
                    onProgress = { progress -> uploadPercent = progress.percent },
                    onTaskCreated = { task -> activeTask = task }
                )
                activeTask = null
                if (uploadResult.isFailure) {
                    isUploading = false
                    errorMessage = uploadResult.exceptionOrNull()?.localizedMessage
                        ?: "Image upload failed. Please try again."
                    return@launch
                }
                val uploaded = uploadResult.getOrThrow()
                uploadedImageUrl = uploaded.downloadUrl
                uploadedStoragePath = uploaded.storagePath
            }

            val postResult = socialRepository.createPost(
                content = postContent.trim(),
                imageUrl = uploadedImageUrl,
                gameTag = selectedTag,
                storagePath = uploadedStoragePath
            )
            isUploading = false

            if (postResult.isSuccess) {
                onSuccess()
            } else {
                if (uploadedStoragePath.isNotBlank()) {
                    mediaUploadService.deleteMedia(uploadedStoragePath, uploadedImageUrl)
                }
                errorMessage = postResult.exceptionOrNull()?.localizedMessage ?: "Failed to publish post."
            }
        }
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                activeTask?.cancel()
                uploadJob?.cancel()
                onBack()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimeTextPrimary)
            }
            Text("Create Post", color = PrimeTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = postContent,
            onValueChange = { postContent = it },
            placeholder = { Text("What's happening in your game? Share clutches, strats, or match invites...", color = PrimeTextMuted, fontSize = 14.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .testTag("create_post_content_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = PrimeCardBackground,
                unfocusedContainerColor = PrimeCardBackground,
                focusedBorderColor = PrimeBlue,
                unfocusedBorderColor = PrimeBorder,
                focusedTextColor = PrimeTextPrimary,
                unfocusedTextColor = PrimeTextPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text("Select Game Tag:", color = PrimeTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tags) { tag ->
                FilterChip(
                    selected = selectedTag == tag,
                    onClick = { selectedTag = tag },
                    label = { Text("#$tag", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimeBlue,
                        selectedLabelColor = Color.White,
                        containerColor = PrimeCardBackground,
                        labelColor = PrimeTextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (selectedTag == tag) PrimeBlue else PrimeBorder,
                        selectedBorderColor = PrimeBlue,
                        enabled = true,
                        selected = selectedTag == tag
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (selectedImageUri != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, PrimeBorder, RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = "Selected Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                IconButton(
                    onClick = { selectedImageUri = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                        .size(32.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Remove image", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                enabled = !isUploading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimeBorder)
            ) {
                Text("Change Image", color = PrimeBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        } else {
            OutlinedButton(
                onClick = {
                    photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                enabled = !isUploading,
                modifier = Modifier.fillMaxWidth().testTag("post_pick_image_button"),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimeBlue.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimeBlue)
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Photo from Device", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (isUploading) {
            Spacer(modifier = Modifier.height(12.dp))
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = if (selectedImageUri != null) "Uploading photo ($uploadPercent%)..." else "Publishing post...",
                        color = PrimeBlue,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Cancel",
                        color = PrimeRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            activeTask?.cancel()
                            uploadJob?.cancel()
                            isUploading = false
                        }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { if (selectedImageUri != null) uploadPercent / 100f else 1f },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = PrimeBlue,
                    trackColor = PrimeCardBackground
                )
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(errorMessage ?: "", color = PrimeRed, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { startPublish() },
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimeBlue)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = PrimeBlue, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Retry", color = PrimeBlue, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = { startPublish() },
            enabled = !isUploading && (postContent.isNotBlank() || selectedImageUri != null),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("publish_post_button"),
            colors = ButtonDefaults.buttonColors(containerColor = PrimeBlue),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isUploading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("Publish Post", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun UploadVideoForm(
    creatorRepository: CreatorRepository,
    mediaUploadService: MediaUploadService,
    currentUserId: String,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var videoTitle by remember { mutableStateOf("") }
    var videoDescription by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Gameplay") }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedThumbnailUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadStatusText by remember { mutableStateOf("Uploading video...") }
    var uploadPercent by remember { mutableIntStateOf(0) }
    var uploadJob by remember { mutableStateOf<Job?>(null) }
    var activeTask by remember { mutableStateOf<UploadTask?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val categories = listOf("Gameplay", "Tournament", "Tutorial", "Highlights", "Minecraft", "BGMI")

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                selectedVideoUri = uri
                errorMessage = null
            }
        }
    )

    val thumbnailPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                selectedThumbnailUri = uri
                errorMessage = null
            }
        }
    )

    fun startUpload() {
        val videoUri = selectedVideoUri
        if (videoTitle.trim().isEmpty()) {
            errorMessage = "Please enter a video title."
            return
        }
        if (videoUri == null) {
            errorMessage = "Please select a video file to upload."
            return
        }

        isUploading = true
        uploadPercent = 0
        uploadStatusText = "Uploading video..."
        errorMessage = null

        uploadJob = coroutineScope.launch {
            val videoUploadRes = mediaUploadService.uploadMedia(
                uri = videoUri,
                folder = "videos",
                userId = currentUserId,
                onProgress = { progress -> uploadPercent = progress.percent },
                onTaskCreated = { task -> activeTask = task }
            )
            activeTask = null

            if (videoUploadRes.isFailure) {
                isUploading = false
                errorMessage = videoUploadRes.exceptionOrNull()?.localizedMessage
                    ?: "Video upload failed. Please try again."
                return@launch
            }

            val uploadedVideo = videoUploadRes.getOrThrow()

            var thumbnailUrl = ""
            var thumbnailStoragePath = ""
            val thumbUri = selectedThumbnailUri
            if (thumbUri != null) {
                uploadStatusText = "Uploading thumbnail..."
                uploadPercent = 0
                val thumbRes = mediaUploadService.uploadMedia(
                    uri = thumbUri,
                    folder = "thumbnails",
                    userId = currentUserId,
                    onProgress = { progress -> uploadPercent = progress.percent },
                    onTaskCreated = { task -> activeTask = task }
                )
                activeTask = null
                if (thumbRes.isFailure) {
                    mediaUploadService.deleteMedia(uploadedVideo.storagePath, uploadedVideo.downloadUrl)
                    isUploading = false
                    errorMessage = thumbRes.exceptionOrNull()?.localizedMessage
                        ?: "Thumbnail upload failed. Please retry."
                    return@launch
                }
                val uploadedThumb = thumbRes.getOrThrow()
                thumbnailUrl = uploadedThumb.downloadUrl
                thumbnailStoragePath = uploadedThumb.storagePath
            }

            uploadStatusText = "Publishing to your channel..."
            val publishRes = creatorRepository.uploadVideo(
                title = videoTitle.trim(),
                videoUrl = uploadedVideo.downloadUrl,
                description = videoDescription.trim(),
                category = selectedCategory,
                thumbnailUrl = thumbnailUrl,
                storagePath = uploadedVideo.storagePath,
                thumbnailStoragePath = thumbnailStoragePath
            )
            isUploading = false

            if (publishRes.isSuccess) {
                onSuccess()
            } else {
                mediaUploadService.deleteMedia(uploadedVideo.storagePath, uploadedVideo.downloadUrl)
                if (thumbnailStoragePath.isNotBlank()) {
                    mediaUploadService.deleteMedia(thumbnailStoragePath, thumbnailUrl)
                }
                errorMessage = publishRes.exceptionOrNull()?.localizedMessage ?: "Failed to save video record."
            }
        }
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                activeTask?.cancel()
                uploadJob?.cancel()
                onBack()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimeTextPrimary)
            }
            Text("Upload Video", color = PrimeTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = videoTitle,
            onValueChange = { videoTitle = it },
            placeholder = { Text("Video Title (e.g. BGMI Pro Qualifiers Grand Final 1v4)", color = PrimeTextMuted, fontSize = 14.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("upload_video_title_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = PrimeCardBackground,
                unfocusedContainerColor = PrimeCardBackground,
                focusedBorderColor = PrimePurple,
                unfocusedBorderColor = PrimeBorder,
                focusedTextColor = PrimeTextPrimary,
                unfocusedTextColor = PrimeTextPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = videoDescription,
            onValueChange = { videoDescription = it },
            placeholder = { Text("Video Description...", color = PrimeTextMuted, fontSize = 14.sp) },
            modifier = Modifier.fillMaxWidth().height(90.dp).testTag("upload_video_desc_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = PrimeCardBackground,
                unfocusedContainerColor = PrimeCardBackground,
                focusedBorderColor = PrimePurple,
                unfocusedBorderColor = PrimeBorder,
                focusedTextColor = PrimeTextPrimary,
                unfocusedTextColor = PrimeTextPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text("Category:", color = PrimeTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimePurple,
                        selectedLabelColor = Color.White,
                        containerColor = PrimeCardBackground,
                        labelColor = PrimeTextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (selectedCategory == category) PrimePurple else PrimeBorder,
                        selectedBorderColor = PrimePurple,
                        enabled = true,
                        selected = selectedCategory == category
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Video File Picker & Preview
        if (selectedVideoUri != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, PrimePurple, RoundedCornerShape(12.dp))
            ) {
                PrimeVideoPlayer(
                    videoUrl = selectedVideoUri.toString(),
                    autoPlay = false,
                    showController = true,
                    loop = false,
                    allowLocalUri = true,
                    modifier = Modifier.matchParentSize()
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    videoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                },
                enabled = !isUploading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimeBorder)
            ) {
                Text("Change Video File", color = PrimePurple, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Surface(
                color = PrimeCardBackground,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimeBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isUploading) {
                        videoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                    }
                    .testTag("video_pick_button")
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.VideoFile,
                        contentDescription = null,
                        tint = PrimePurple,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Select Video from Device",
                            color = PrimeTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Supports MP4, MKV, WebM",
                            color = PrimeTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Thumbnail Picker & Preview
        if (selectedThumbnailUri != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, PrimeBlue, RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = selectedThumbnailUri,
                    contentDescription = "Selected Thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                IconButton(
                    onClick = { selectedThumbnailUri = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                        .size(30.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Remove thumbnail", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedButton(
                onClick = {
                    thumbnailPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                enabled = !isUploading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimeBorder)
            ) {
                Text("Change Thumbnail", color = PrimeBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Surface(
                color = PrimeCardBackground,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimeBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isUploading) {
                        thumbnailPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        tint = PrimeTextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Attach Thumbnail (Optional)",
                            color = PrimeTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "16:9 ratio recommended",
                            color = PrimeTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        if (isUploading) {
            Spacer(modifier = Modifier.height(12.dp))
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("$uploadStatusText ($uploadPercent%)", color = PrimePurple, fontSize = 12.sp)
                    Text(
                        text = "Cancel",
                        color = PrimeRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            activeTask?.cancel()
                            uploadJob?.cancel()
                            isUploading = false
                        }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { uploadPercent / 100f },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = PrimePurple,
                    trackColor = PrimeCardBackground
                )
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(errorMessage ?: "", color = PrimeRed, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { startUpload() },
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimePurple)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = PrimePurple, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Retry", color = PrimePurple, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = { startUpload() },
            enabled = !isUploading && videoTitle.isNotBlank() && selectedVideoUri != null,
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("publish_video_button"),
            colors = ButtonDefaults.buttonColors(containerColor = PrimePurple),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isUploading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("Publish Video", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun CreateReelForm(
    socialRepository: SocialRepository,
    mediaUploadService: MediaUploadService,
    currentUserId: String,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var reelCaption by remember { mutableStateOf("") }
    var reelTags by remember { mutableStateOf("") }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadPercent by remember { mutableIntStateOf(0) }
    var uploadJob by remember { mutableStateOf<Job?>(null) }
    var activeTask by remember { mutableStateOf<UploadTask?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                selectedVideoUri = uri
                errorMessage = null
            }
        }
    )

    fun startUpload() {
        val videoUri = selectedVideoUri
        if (reelCaption.trim().isEmpty()) {
            errorMessage = "Please enter a caption for your Reel."
            return
        }
        if (videoUri == null) {
            errorMessage = "Please select a video clip."
            return
        }

        isUploading = true
        uploadPercent = 0
        errorMessage = null

        uploadJob = coroutineScope.launch {
            val uploadResult = mediaUploadService.uploadMedia(
                uri = videoUri,
                folder = "reels",
                userId = currentUserId,
                onProgress = { progress -> uploadPercent = progress.percent },
                onTaskCreated = { task -> activeTask = task }
            )
            activeTask = null

            if (uploadResult.isFailure) {
                isUploading = false
                errorMessage = uploadResult.exceptionOrNull()?.localizedMessage
                    ?: "Reel video upload failed. Please try again."
                return@launch
            }

            val uploaded = uploadResult.getOrThrow()
            val publishRes = socialRepository.createReel(
                videoUrl = uploaded.downloadUrl,
                caption = reelCaption.trim(),
                tags = reelTags.trim(),
                storagePath = uploaded.storagePath
            )
            isUploading = false

            if (publishRes.isSuccess) {
                onSuccess()
            } else {
                mediaUploadService.deleteMedia(uploaded.storagePath, uploaded.downloadUrl)
                errorMessage = publishRes.exceptionOrNull()?.localizedMessage ?: "Failed to save Reel."
            }
        }
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                activeTask?.cancel()
                uploadJob?.cancel()
                onBack()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimeTextPrimary)
            }
            Text("Upload Reel", color = PrimeTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = reelCaption,
            onValueChange = { reelCaption = it },
            placeholder = { Text("Reel caption (e.g. Cleanest 1v4 squad wipe)", color = PrimeTextMuted, fontSize = 14.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("upload_reel_caption_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = PrimeCardBackground,
                unfocusedContainerColor = PrimeCardBackground,
                focusedBorderColor = PrimeRed,
                unfocusedBorderColor = PrimeBorder,
                focusedTextColor = PrimeTextPrimary,
                unfocusedTextColor = PrimeTextPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = reelTags,
            onValueChange = { reelTags = it },
            placeholder = { Text("#Gaming #Clutch #Reels (optional)", color = PrimeTextMuted, fontSize = 14.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("upload_reel_tags_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = PrimeCardBackground,
                unfocusedContainerColor = PrimeCardBackground,
                focusedBorderColor = PrimeRed,
                unfocusedBorderColor = PrimeBorder,
                focusedTextColor = PrimeTextPrimary,
                unfocusedTextColor = PrimeTextPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedVideoUri != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, PrimeRed, RoundedCornerShape(12.dp))
            ) {
                PrimeVideoPlayer(
                    videoUrl = selectedVideoUri.toString(),
                    autoPlay = false,
                    showController = true,
                    loop = true,
                    allowLocalUri = true,
                    modifier = Modifier.matchParentSize()
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    videoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                },
                enabled = !isUploading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimeBorder)
            ) {
                Text("Change Video Clip", color = PrimeRed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Surface(
                color = PrimeCardBackground,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimeBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isUploading) {
                        videoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                    }
                    .testTag("reel_pick_video_button")
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = PrimeRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Select Reel Video from Device",
                            color = PrimeTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Short vertical or highlight clips",
                            color = PrimeTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        if (isUploading) {
            Spacer(modifier = Modifier.height(12.dp))
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Uploading reel clip ($uploadPercent%)...", color = PrimeRed, fontSize = 12.sp)
                    Text(
                        text = "Cancel",
                        color = PrimeRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            activeTask?.cancel()
                            uploadJob?.cancel()
                            isUploading = false
                        }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { uploadPercent / 100f },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = PrimeRed,
                    trackColor = PrimeCardBackground
                )
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(errorMessage ?: "", color = PrimeRed, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { startUpload() },
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimeRed)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = PrimeRed, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Retry", color = PrimeRed, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = { startUpload() },
            enabled = !isUploading && reelCaption.isNotBlank() && selectedVideoUri != null,
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("publish_reel_button"),
            colors = ButtonDefaults.buttonColors(containerColor = PrimeRed),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isUploading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("Publish Reel", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}
