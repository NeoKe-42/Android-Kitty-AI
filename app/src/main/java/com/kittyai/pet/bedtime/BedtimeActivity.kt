package com.kittyai.pet.bedtime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider

/**
 * Main launcher activity for the Kitty AI chat app.
 * Uses Jetpack Compose for the UI, supports both chat and bedtime stories.
 */
class BedtimeActivity : ComponentActivity() {

    private lateinit var viewModel: ChatViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(this)[ChatViewModel::class.java]

        setContent {
            ChatScreen(viewModel = viewModel)
        }
    }
}
