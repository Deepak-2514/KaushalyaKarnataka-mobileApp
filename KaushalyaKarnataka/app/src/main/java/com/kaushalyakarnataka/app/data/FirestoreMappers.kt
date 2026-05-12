package com.kaushalyakarnataka.app.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

private fun String?.toUserRole(): UserRole =
    runCatching { UserRole.valueOf(this ?: "") }.getOrDefault(UserRole.customer)

private fun String?.toRequestStatus(): RequestStatus =
    runCatching { RequestStatus.valueOf(this ?: "") }.getOrDefault(RequestStatus.pending)

fun DocumentSnapshot.toUserProfile(): UserProfile {
    val id = id
    return UserProfile(
        userId = getString("userId").takeUnless { it.isNullOrBlank() } ?: id,
        name = getString("name").orEmpty(),
        phone = getString("phone").orEmpty(),
        role = getString("role").toUserRole(),
        category = getString("category"),
        location = getString("location").orEmpty(),
        profileImage = getString("profileImage"),
        rating = getDouble("rating") ?: 0.0,
        ratingCount = getLong("ratingCount"),
        totalRatingValue = getDouble("totalRatingValue"),
        jobsCompleted = getLong("jobsCompleted") ?: 0,
        bio = getString("bio"),
        fcmToken = getString("fcmToken"),
    )
}

fun DocumentSnapshot.toJobRequest(): JobRequest =
    JobRequest(
        requestId = id,
        workerId = getString("workerId").orEmpty(),
        customerId = getString("customerId").orEmpty(),
        status = getString("status").toRequestStatus(),
        chatEnabled = getBoolean("chatEnabled") ?: false,
        callEnabled = getBoolean("callEnabled") ?: false,
        createdAt = getTimestamp("createdAt"),
        completedAt = getTimestamp("completedAt"),
    )

fun DocumentSnapshot.toServiceItem(): ServiceItem =
    ServiceItem(
        serviceId = id,
        userId = getString("userId").orEmpty(),
        title = getString("title").orEmpty(),
        price = getDouble("price") ?: 0.0,
        description = getString("description").orEmpty(),
    )

fun DocumentSnapshot.toPortfolioItem(): PortfolioItem =
    PortfolioItem(
        portfolioId = id,
        userId = getString("userId").orEmpty(),
        imageUrl = getString("imageUrl").orEmpty(),
        description = getString("description").orEmpty(),
    )

fun DocumentSnapshot.toChatMessage(chatId: String): ChatMessage =
    ChatMessage(
        messageId = id,
        chatId = chatId,
        senderId = getString("senderId").orEmpty(),
        message = getString("message").orEmpty(),
        timestamp = getTimestamp("timestamp"),
    )

fun DocumentSnapshot.toReview(): ReviewEntry =
    ReviewEntry(
        reviewId = id,
        workerId = getString("workerId").orEmpty(),
        customerId = getString("customerId").orEmpty(),
        rating = (getLong("rating") ?: 0L).toInt(),
        comment = getString("comment").orEmpty(),
        createdAt = getTimestamp("createdAt"),
    )

fun Timestamp?.millisOrZero(): Long = this?.toDate()?.time ?: 0L
