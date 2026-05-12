package com.kaushalyakarnataka.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.snapshots
import com.kaushalyakarnataka.app.FirebaseBootstrap
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class KaushalyaRepository(
    private val auth: FirebaseAuth = FirebaseBootstrap.auth,
    private val db: com.google.firebase.firestore.FirebaseFirestore = FirebaseBootstrap.firestore,
) {

    val currentUserId: String?
        get() = auth.currentUser?.uid

    suspend fun signInWithGoogleIdToken(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).await()
    }

    fun signOut() {
        auth.signOut()
    }

    fun observeAuthUid(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { user ->
            trySend(user?.uid)
        }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser?.uid)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    fun observeProfile(uid: String): Flow<UserProfile?> =
        db.collection("users").document(uid).snapshots().map { snap ->
            if (!snap.exists()) null else snap.toUserProfile()
        }

    suspend fun mergeUserProfile(uid: String, fields: Map<String, Any?>) {
        val cleaned = fields.filterValues { it != null }.mapValues { (_, v) -> v!! }
        val withId = cleaned + mapOf("userId" to uid)
        db.collection("users").document(uid).set(withId, SetOptions.merge()).await()
    }

    suspend fun fetchWorkers(): List<UserProfile> {
        val snap = db.collection("users").whereEqualTo("role", UserRole.worker.name).get().await()
        return snap.documents.map { it.toUserProfile() }
    }

    suspend fun fetchUser(workerId: String): UserProfile? {
        val snap = db.collection("users").document(workerId).get().await()
        return if (snap.exists()) snap.toUserProfile() else null
    }

    suspend fun fetchReviewsForWorker(workerId: String, limit: Long = 5): List<ReviewEntry> {
        val snap = db.collection("reviews")
            .whereEqualTo("workerId", workerId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()
        return snap.documents.map { it.toReview() }
    }

    suspend fun fetchServices(workerId: String): List<ServiceItem> {
        val snap = db.collection("services").whereEqualTo("userId", workerId).get().await()
        return snap.documents.map { it.toServiceItem() }
    }

    suspend fun fetchPortfolio(workerId: String): List<PortfolioItem> {
        val snap = db.collection("portfolio").whereEqualTo("userId", workerId).get().await()
        return snap.documents.map { it.toPortfolioItem() }
    }

    suspend fun hireWorker(customerId: String, workerId: String): Result<Unit> = runCatching {
        val existing = db.collection("requests")
            .whereEqualTo("customerId", customerId)
            .whereEqualTo("workerId", workerId)
            .whereIn("status", listOf(RequestStatus.pending.name, RequestStatus.accepted.name))
            .get()
            .await()
        require(existing.isEmpty) { "duplicate_request" }
        db.collection("requests").add(
            mapOf(
                "customerId" to customerId,
                "workerId" to workerId,
                "status" to RequestStatus.pending.name,
                "createdAt" to FieldValue.serverTimestamp(),
                "chatEnabled" to false,
                "callEnabled" to false,
            ),
        ).await()
    }

    fun observeRequestsForWorker(workerId: String): Flow<List<JobRequest>> =
        db.collection("requests").whereEqualTo("workerId", workerId).snapshots().map { qs ->
            qs.documents.map { it.toJobRequest() }
                .sortedByDescending { it.createdAt.millisOrZero() }
        }

    fun observeAcceptedJobs(workerId: String): Flow<List<JobRequest>> =
        db.collection("requests")
            .whereEqualTo("workerId", workerId)
            .whereEqualTo("status", RequestStatus.accepted.name)
            .snapshots()
            .map { qs -> qs.documents.map { it.toJobRequest() } }

    fun observeRequestsForCustomer(customerId: String): Flow<List<JobRequest>> =
        db.collection("requests").whereEqualTo("customerId", customerId).snapshots().map { qs ->
            qs.documents.map { it.toJobRequest() }
                .sortedByDescending { it.createdAt.millisOrZero() }
        }

    fun observeReviewsByCustomer(customerId: String): Flow<List<ReviewEntry>> =
        db.collection("reviews").whereEqualTo("customerId", customerId).snapshots().map { qs ->
            qs.documents.map { it.toReview() }
        }

    suspend fun updateRequestStatus(
        requestId: String,
        status: RequestStatus,
        enableChatCall: Boolean = false,
    ) {
        val updates = mutableMapOf<String, Any>("status" to status.name)
        if (enableChatCall) {
            updates["chatEnabled"] = true
            updates["callEnabled"] = true
        }
        if (status == RequestStatus.completed) {
            updates["completedAt"] = FieldValue.serverTimestamp()
            updates["chatEnabled"] = false
            updates["callEnabled"] = false
        }
        db.collection("requests").document(requestId).update(updates).await()
    }

    suspend fun ensureChat(workerId: String, customerId: String) {
        val existing = db.collection("chats")
            .whereArrayContains("participants", workerId)
            .get()
            .await()
        val already = existing.documents.any { doc ->
            @Suppress("UNCHECKED_CAST")
            (doc.get("participants") as? List<String>)?.contains(customerId) == true
        }
        if (!already) {
            db.collection("chats").add(
                mapOf(
                    "participants" to listOf(workerId, customerId),
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
        }
    }

    suspend fun acceptRequest(requestId: String, workerId: String, customerId: String) {
        ensureChat(workerId, customerId)
        updateRequestStatus(requestId, RequestStatus.accepted, enableChatCall = true)
    }

    suspend fun rejectRequest(requestId: String) {
        updateRequestStatus(requestId, RequestStatus.rejected)
    }

    suspend fun completeJob(requestId: String, workerId: String) {
        db.collection("requests").document(requestId).update(
            mapOf(
                "status" to RequestStatus.completed.name,
                "completedAt" to FieldValue.serverTimestamp(),
                "chatEnabled" to false,
                "callEnabled" to false,
            ),
        ).await()
        db.collection("users").document(workerId).update(
            "jobsCompleted",
            FieldValue.increment(1),
        ).await()
    }

    suspend fun completeJobFromRequestsList(requestId: String, workerId: String) {
        db.collection("requests").document(requestId).update(
            mapOf(
                "status" to RequestStatus.completed.name,
                "completedAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
        db.collection("users").document(workerId).update(
            "jobsCompleted",
            FieldValue.increment(1),
        ).await()
    }

    fun observeChats(uid: String): Flow<List<ChatThread>> =
        db.collection("chats").whereArrayContains("participants", uid).snapshots().map { qs ->
            qs.documents.map { doc ->
                @Suppress("UNCHECKED_CAST")
                val parts = (doc.get("participants") as? List<String>).orEmpty()
                ChatThread(chatId = doc.id, participants = parts)
            }
        }

    fun observeMessages(chatId: String): Flow<List<ChatMessage>> =
        db.collection("chats").document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .snapshots()
            .map { qs ->
                qs.documents.map { it.toChatMessage(chatId) }
            }

    suspend fun sendMessage(chatId: String, senderId: String, text: String) {
        db.collection("chats").document(chatId).collection("messages").add(
            mapOf(
                "chatId" to chatId,
                "senderId" to senderId,
                "message" to text,
                "timestamp" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    suspend fun fetchChatParticipantIds(chatId: String): List<String> {
        val doc = db.collection("chats").document(chatId).get().await()
        @Suppress("UNCHECKED_CAST")
        return (doc.get("participants") as? List<String>).orEmpty()
    }

    suspend fun findChatIdBetween(uid: String, otherUid: String): String? {
        val snap = db.collection("chats").whereArrayContains("participants", uid).get().await()
        return snap.documents.firstOrNull { doc ->
            @Suppress("UNCHECKED_CAST")
            val parts = doc.get("participants") as? List<*>
            parts?.contains(otherUid) == true
        }?.id
    }

    suspend fun submitReview(
        workerId: String,
        customerId: String,
        rating: Int,
        comment: String,
    ) {
        val dup = db.collection("reviews")
            .whereEqualTo("workerId", workerId)
            .whereEqualTo("customerId", customerId)
            .get()
            .await()
        require(dup.isEmpty) { "already_rated" }

        db.collection("reviews").add(
            mapOf(
                "workerId" to workerId,
                "customerId" to customerId,
                "rating" to rating,
                "comment" to comment,
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()

        val workerRef = db.collection("users").document(workerId)
        val workerSnap = workerRef.get().await()
        val count = (workerSnap.getLong("ratingCount") ?: 0L) + 1
        val total = (workerSnap.getDouble("totalRatingValue") ?: 0.0) + rating
        val avg = String.format("%.1f", total / count).toDouble()

        workerRef.update(
            mapOf(
                "rating" to avg,
                "ratingCount" to FieldValue.increment(1),
                "totalRatingValue" to FieldValue.increment(rating.toDouble()),
            ),
        ).await()
    }

    fun observeServices(uid: String): Flow<List<ServiceItem>> =
        db.collection("services").whereEqualTo("userId", uid).snapshots().map { qs ->
            qs.documents.map { it.toServiceItem() }
        }

    fun observePortfolio(uid: String): Flow<List<PortfolioItem>> =
        db.collection("portfolio").whereEqualTo("userId", uid).snapshots().map { qs ->
            qs.documents.map { it.toPortfolioItem() }
        }

    suspend fun addService(uid: String, title: String, price: Double, description: String) {
        db.collection("services").add(
            mapOf(
                "userId" to uid,
                "title" to title,
                "price" to price,
                "description" to description,
            ),
        ).await()
    }

    suspend fun deleteService(serviceId: String) {
        db.collection("services").document(serviceId).delete().await()
    }

    suspend fun updateService(serviceId: String, data: Map<String, Any>) {
        db.collection("services").document(serviceId).update(data).await()
    }

    suspend fun addPortfolioItem(uid: String, imageUrl: String, description: String) {
        db.collection("portfolio").add(
            mapOf(
                "userId" to uid,
                "imageUrl" to imageUrl,
                "description" to description,
            ),
        ).await()
    }

    suspend fun deletePortfolio(portfolioId: String) {
        db.collection("portfolio").document(portfolioId).delete().await()
    }

    suspend fun uploadImage(bytes: ByteArray, pathPrefix: String): String {
        val ref = FirebaseBootstrap.storage.reference.child("$pathPrefix/${UUID.randomUUID()}.jpg")
        ref.putBytes(bytes).await()
        return ref.getDownloadUrl().await().toString()
    }

}
