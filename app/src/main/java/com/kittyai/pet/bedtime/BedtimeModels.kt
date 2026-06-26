package com.kittyai.pet.bedtime

import com.google.gson.annotations.SerializedName

// ---- Bedtime story models (backward compatible) ----

data class BedtimeRequest(
    @SerializedName("message")
    val message: String
)

data class BedtimeResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("story_id") val storyId: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("text") val text: String? = null,
    @SerializedName("audio_url") val audioUrl: String? = null,
    @SerializedName("duration_minutes") val durationMinutes: Int? = null,
    @SerializedName("audience") val audience: String? = null,
    @SerializedName("error") val error: String? = null
)

data class HealthResponse(
    @SerializedName("status") val status: String? = null
)

// ---- Unified chat models ----

data class ChatRequest(
    @SerializedName("message") val message: String,
    @SerializedName("conversation_id") val conversationId: String = "default",
    @SerializedName("enable_tts") val enableTts: Boolean = false
)

data class ChatResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("mode") val mode: String? = null,
    @SerializedName("reply") val reply: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("audio_url") val audioUrl: String? = null,
    @SerializedName("story_id") val storyId: String? = null,
    @SerializedName("conversation_id") val conversationId: String? = null,
    @SerializedName("error") val error: String? = null
)

// ---- UI message model ----

enum class MessageRole { USER, KITTY }

data class ChatMessage(
    val id: String,
    val role: MessageRole,
    val text: String,
    val title: String? = null,
    val mode: String = "chat",
    val audioUrl: String? = null,
    val storyId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
