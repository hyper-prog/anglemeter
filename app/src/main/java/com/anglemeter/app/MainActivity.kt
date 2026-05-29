package com.anglemeter.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.anglemeter.app.ui.AngleMeterScreen

class MainActivity : ComponentActivity() {

    private val viewModel: AngleMeterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme {
                Surface {
                    AngleMeterScreen(viewModel = viewModel)
                }
            }
        }
    }
}
