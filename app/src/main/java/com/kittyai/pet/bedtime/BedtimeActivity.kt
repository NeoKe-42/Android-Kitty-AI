package com.kittyai.pet.bedtime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider

/**
 * Main launcher activity for the Kitty bedtime story app.
 * Uses Jetpack Compose for the UI.
 */
class BedtimeActivity : ComponentActivity() {

    private lateinit var viewModel: BedtimeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(this)[BedtimeViewModel::class.java]

        setContent {
            BedtimeScreen(viewModel = viewModel)
        }
    }
}
