package ru.ritual.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ru.ritual.app.ui.theme.Lime
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ru.ritual.app.ui.screens.AiScreen
import ru.ritual.app.ui.screens.HistoryScreen
import ru.ritual.app.ui.screens.HomeScreen
import ru.ritual.app.ui.screens.RunnerScreen
import ru.ritual.app.ui.screens.SettingsScreen
import ru.ritual.app.ui.screens.EditorScreen

private data class Destination(val route: String, val label: String, val icon: ImageVector)

private val bottomDestinations = listOf(
    Destination("home", "Главная", Icons.Outlined.Home),
    Destination("history", "История", Icons.Outlined.History),
    Destination("ai", "Создать", Icons.Outlined.AutoAwesome),
    Destination("settings", "Настройки", Icons.Outlined.Settings),
)

@Composable
fun RitualApp(viewModel: AppViewModel, state: AppUiState) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.keyMessage) {
        state.keyMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (currentRoute?.startsWith("runner") != true && currentRoute?.startsWith("editor") != true) {
                Surface(tonalElevation = 1.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding().height(56.dp).padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                    ) {
                    bottomDestinations.forEach { destination ->
                        val selected = currentRoute == destination.route
                        IconButton(onClick = { navController.navigateSingleTop(destination.route) }) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(if (selected) Lime else androidx.compose.ui.graphics.Color.Transparent),
                                contentAlignment = androidx.compose.ui.Alignment.Center,
                            ) {
                                Icon(destination.icon, contentDescription = destination.label, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            NavHost(navController = navController, startDestination = "home") {
                composable("home") {
                    HomeScreen(
                        state = state,
                        categories = viewModel.categories,
                        checklists = viewModel.filteredChecklists(),
                        onQueryChange = viewModel::updateQuery,
                        onCategoryClick = viewModel::selectCategory,
                        onChecklistClick = { navController.navigate("runner/$it") },
                        onChecklistEdit = { navController.navigate("editor/$it") },
                        onChecklistDelete = viewModel::deleteChecklist,
                        onAiClick = { navController.navigateSingleTop("ai") },
                        onCreateClick = {
                            viewModel.clearGeneratedChecklist()
                            navController.navigate("editor")
                        },
                    )
                }
                composable("history") { HistoryScreen(viewModel.history) }
                composable("ai") {
                    AiScreen(
                        hasYandexCredentials = state.hasYandexCredentials,
                        isSavingKey = state.isSavingKey,
                        onSaveCredentials = viewModel::saveYandexCredentials,
                        onOpenSettings = { navController.navigateSingleTop("settings") },
                        isGenerating = state.isGenerating,
                        generationStage = state.generationStage,
                        generatedChecklist = state.generatedChecklist,
                        generationError = state.generationError,
                        generationNotifications = state.preferences.generationNotifications,
                        onGenerate = viewModel::generateChecklist,
                        onEditDraft = { navController.navigate("editor") },
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        hasYandexCredentials = state.hasYandexCredentials,
                        onSaveCredentials = viewModel::saveYandexCredentials,
                        onDeleteCredentials = viewModel::deleteYandexCredentials,
                        tapNavigation = state.preferences.tapNavigation,
                        onTapNavigationChange = viewModel::setTapNavigation,
                        keepScreenAwake = state.preferences.keepScreenAwake,
                        onKeepScreenAwakeChange = viewModel::setKeepScreenAwake,
                        autoPlayVideoNotes = state.preferences.autoPlayVideoNotes,
                        onAutoPlayVideoNotesChange = viewModel::setAutoPlayVideoNotes,
                        generationNotifications = state.preferences.generationNotifications,
                        onGenerationNotificationsChange = viewModel::setGenerationNotifications,
                    )
                }
                composable("runner/{id}") { entry ->
                    val id = entry.arguments?.getString("id")
                    val checklist = viewModel.checklists.firstOrNull { it.id == id }
                    if (checklist != null) {
                        RunnerScreen(
                            checklist = checklist,
                            tapNavigation = state.preferences.tapNavigation,
                            keepScreenAwake = state.preferences.keepScreenAwake,
                            autoPlayVideoNotes = state.preferences.autoPlayVideoNotes,
                            onClose = navController::popBackStack,
                        )
                    }
                }
                composable("editor") {
                    EditorScreen(
                        initialChecklist = state.generatedChecklist,
                        isGeneratingMetadata = state.isGeneratingMetadata,
                        metadataTarget = state.metadataTarget,
                        metadataSuggestion = state.metadataSuggestion,
                        onRequestMetadata = viewModel::requestMetadata,
                        onConsumeMetadataSuggestion = viewModel::consumeMetadataSuggestion,
                        onSave = {
                            viewModel.saveChecklist(it)
                            navController.popBackStack()
                        },
                        onClose = navController::popBackStack,
                    )
                }
                composable("editor/{id}") { entry ->
                    val id = entry.arguments?.getString("id")
                    val checklist = viewModel.checklists.firstOrNull { it.id == id }
                    if (checklist != null) {
                        EditorScreen(
                            existingChecklist = checklist,
                            isGeneratingMetadata = state.isGeneratingMetadata,
                            metadataTarget = state.metadataTarget,
                            metadataSuggestion = state.metadataSuggestion,
                            onRequestMetadata = viewModel::requestMetadata,
                            onConsumeMetadataSuggestion = viewModel::consumeMetadataSuggestion,
                            onSave = {
                                viewModel.saveChecklist(it)
                                navController.popBackStack()
                            },
                            onClose = navController::popBackStack,
                        )
                    }
                }
            }
        }
    }
}

private fun NavHostController.navigateSingleTop(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
