package ru.ritual.app

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.ritual.app.ui.AppViewModel
import ru.ritual.app.ui.RitualApp
import ru.ritual.app.ui.theme.RitualTheme

class MainActivity : ComponentActivity() {
    private var requestedAlgorithmId by mutableStateOf<String?>(null)
    private var requestedDestination by mutableStateOf<String?>(null)
    private var launchRequestToken by mutableLongStateOf(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        acceptLaunchIntent(intent)
        enableEdgeToEdge()
        setContent {
            RitualTheme {
                val viewModel: AppViewModel = viewModel()
                val state by viewModel.state.collectAsStateWithLifecycle()
                RitualApp(
                    viewModel = viewModel,
                    state = state,
                    requestedAlgorithmId = requestedAlgorithmId,
                    requestedDestination = requestedDestination,
                    launchRequestToken = launchRequestToken,
                    onLaunchConsumed = {
                        requestedAlgorithmId = null
                        requestedDestination = null
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        acceptLaunchIntent(intent)
    }

    private fun acceptLaunchIntent(intent: Intent?) {
        requestedAlgorithmId = intent?.getStringExtra(EXTRA_ALGORITHM_ID)
        requestedDestination = intent?.getStringExtra(EXTRA_DESTINATION)
        if (requestedAlgorithmId != null || requestedDestination != null) launchRequestToken++
    }

    companion object {
        const val EXTRA_ALGORITHM_ID = "launch_algorithm_id"
        const val EXTRA_DESTINATION = "launch_destination"
        const val DESTINATION_SCHEDULE = "schedule"
        const val DESTINATION_AI = "ai"
    }
}
