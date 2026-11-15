package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class GameHandle(
    val platform: String = "",
    val handle: String = ""
)

@IgnoreExtraProperties
data class RankHistoryEntry(
    val game: String = "",
    val season: String = "",
    val rank: String = "",
    val peakRank: String = "",
    val notes: String = ""
)

@IgnoreExtraProperties
data class SocialLink(
    val platform: String = "",
    val handleOrUrl: String = ""
)

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val avatarUrl: String = "",
    val bannerUrl: String = "",
    val bio: String = "",
    val favoriteGames: String = "",
    val favoriteTitles: List<String> = emptyList(),
    val gameHandles: List<GameHandle> = emptyList(),
    val rankHistory: List<RankHistoryEntry> = emptyList(),
    val socialLinks: List<SocialLink> = emptyList(),
    val postsCount: Int = 0,
    val videosCount: Int = 0,
    val reelsCount: Int = 0,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val friendsCount: Int = 0,
    val isVerified: Boolean = false,
    val isOwner: Boolean = false,
    val isOnline: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

fun UserProfile.resolvedFavoriteTitles(): List<String> {
    return if (favoriteTitles.isNotEmpty()) {
        favoriteTitles.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    } else {
        favoriteGames.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    }
}

@IgnoreExtraProperties
data class PrivateUserInfo(
    @DocumentId val id: String = "",
    val email: String = "",
    val isPrivate: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FriendRequest(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderUsername: String = "",
    val senderAvatarUrl: String = "",
    val receiverId: String = "",
    val status: String = "pending", // pending, accepted, rejected, cancelled
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Friendship(
    @DocumentId val id: String = "",
    val user1: String = "",
    val user2: String = "",
    val users: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Follow(
    @DocumentId val id: String = "",
    val followerId: String = "",
    val followingId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastSenderId: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DirectMessage(
    @DocumentId val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val mediaUrl: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Post(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val authorAvatarUrl: String = "",
    val content: String = "",
    val imageUrl: String = "",
    val storagePath: String = "",
    val gameTag: String = "",
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PostComment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val authorAvatarUrl: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Reel(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val authorAvatarUrl: String = "",
    val videoUrl: String = "",
    val storagePath: String = "",
    val thumbnailUrl: String = "",
    val caption: String = "",
    val tags: String = "",
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val viewsCount: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ReelComment(
    @DocumentId val id: String = "",
    val reelId: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val authorAvatarUrl: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChannelVideo(
    @DocumentId val id: String = "",
    val channelId: String = "",
    val ownerId: String = "",
    val authorUsername: String = "",
    val authorAvatarUrl: String = "",
    val title: String = "",
    val description: String = "",
    val thumbnailUrl: String = "",
    val thumbnailStoragePath: String = "",
    val videoUrl: String = "",
    val storagePath: String = "",
    val viewsCount: Int = 0,
    val category: String = "",
    val status: String = "published", // published, draft
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Community(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val description: String = "",
    val iconUrl: String = "",
    val memberCount: Int = 0,
    val isPublic: Boolean = true,
    val rules: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CommunityMember(
    @DocumentId val id: String = "",
    val communityId: String = "",
    val userId: String = "",
    val username: String = "",
    val avatarUrl: String = "",
    val role: String = "member", // owner, admin, moderator, member
    @ServerTimestamp val joinedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CommunityChannel(
    @DocumentId val id: String = "",
    val communityId: String = "",
    val name: String = "",
    val topic: String = "",
    val type: String = "text",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CommunityMessage(
    @DocumentId val id: String = "",
    val communityId: String = "",
    val channelId: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val authorAvatarUrl: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Tournament(
    @DocumentId val id: String = "",
    val organizerId: String = "",
    val organizerName: String = "",
    val title: String = "",
    val game: String = "",
    val description: String = "",
    val prizePool: String = "",
    val teamSize: Int = 4,
    val maxTeams: Int = 32,
    val registeredTeamsCount: Int = 0,
    val startDate: String = "",
    val status: String = "upcoming", // upcoming, ongoing, completed
    val rules: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TournamentRegistration(
    @DocumentId val id: String = "",
    val tournamentId: String = "",
    val userId: String = "",
    val teamName: String = "",
    val playerUsernames: String = "",
    val status: String = "confirmed",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MinecraftServer(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val edition: String = "Crossplay", // Java, Bedrock, Crossplay
    val version: String = "1.21.1",
    val address: String = "",
    val port: Int = 25565,
    val description: String = "",
    val onlinePlayers: Int = 0,
    val maxPlayers: Int = 100,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class NotificationItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatarUrl: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "general",
    val targetId: String = "",
    val isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserReport(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val targetType: String = "",
    val targetId: String = "",
    val reason: String = "",
    val status: String = "pending",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BlockedUser(
    @DocumentId val id: String = "",
    val blockerId: String = "",
    val blockedId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
