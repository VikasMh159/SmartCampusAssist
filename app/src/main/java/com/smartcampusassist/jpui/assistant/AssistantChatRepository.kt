package com.smartcampusassist.jpui.assistant

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

data class AssistantChatHistory(
    val messages: List<ChatMessage> = emptyList(),
    val updatedAt: Long = 0L
)

class AssistantChatRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun observeChatHistory(
        onChange: (AssistantChatHistory) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration? {
        val user = auth.currentUser ?: return null

        return firestore.collection("users")
            .document(user.uid)
            .collection("assistant")
            .document("history")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(Exception(error))
                    return@addSnapshotListener
                }

                val history = snapshot?.toObject(AssistantChatHistory::class.java)
                onChange(history ?: AssistantChatHistory())
            }
    }

    fun hasSignedInUser(): Boolean {
        return auth.currentUser != null
    }

    fun currentUserId(): String? = auth.currentUser?.uid

    suspend fun saveMessages(
        messages: List<ChatMessage>
    ) {
        val user = auth.currentUser ?: return
        val updatedAt = System.currentTimeMillis()

        firestore.collection("users")
            .document(user.uid)
            .collection("assistant")
            .document("history")
            .set(
                mapOf(
                    "messages" to messages,
                    "updatedAt" to updatedAt
                ),
                SetOptions.merge()
            )
            .await()
    }
}
