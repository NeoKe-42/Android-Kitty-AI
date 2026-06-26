package com.kittyai.pet.bedtime

/**
 * Central configuration for the Kitty bedtime story API.
 * Change BASE_URL when deploying to a different server.
 */
object ApiConfig {
    /** Server base URL — must end with "/" for Retrofit */
    const val BASE_URL = "http://47.95.111.58:8000/"

    const val CHAT_ENDPOINT = "api/chat"
    const val BEDTIME_ENDPOINT = "api/bedtime"
    const val HEALTH_ENDPOINT = "api/health"

    /** Default message when user leaves input empty */
    const val DEFAULT_MESSAGE = "你好呀"
}
