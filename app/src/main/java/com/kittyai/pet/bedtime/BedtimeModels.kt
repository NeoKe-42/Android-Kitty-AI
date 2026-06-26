package com.kittyai.pet.bedtime

import com.google.gson.annotations.SerializedName

/** Request body for POST /api/bedtime */
data class BedtimeRequest(
    @SerializedName("message")
    val message: String
)

/** Response from POST /api/bedtime */
data class BedtimeResponse(
    @SerializedName("success")
    val success: Boolean? = null,

    @SerializedName("story_id")
    val storyId: String? = null,

    @SerializedName("title")
    val title: String? = null,

    @SerializedName("text")
    val text: String? = null,

    @SerializedName("audio_url")
    val audioUrl: String? = null,

    @SerializedName("duration_minutes")
    val durationMinutes: Int? = null,

    @SerializedName("audience")
    val audience: String? = null,

    @SerializedName("error")
    val error: String? = null
)

/** Response from GET /api/health */
data class HealthResponse(
    @SerializedName("status")
    val status: String? = null
)
