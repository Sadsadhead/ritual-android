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
import androidx.compose.material.icons.outlined.CalendarMonth
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
import ru.ritual.app.ui.screens.ScheduleScreen
import ru.ritual.app.ui.screens.HomeScreen
import ru.ritual.app.ui.screens.RunnerScreen
import ru.ritual.app.ui.screens.SettingsScreen
import ru.ritual.app.ui.screens.EditorScreen

private data class Destination(val route: String, val label: String, val icon: ImageVector)

private val bottomDestinations = listOf(
    Destination("home", "Главная", Icons.Outlined.Home),
    Destination("schedule", "Расписание", Icons.Outlined.CalendarMonth),
    Destination("ai", "Создать", Icons.Outlined.AutoAwesome),
    Destination("settings", "Настройки", Icons.Outlined.Settings),
)

@Composable
fun RitualApp(
    viewModel: AppViewModel,
    state: AppUiState,
    requestedAlgorithmId: String? = null,
    requestedDestination: String? = null,
    launchRequestToken: Long = 0L,
    onLaunchConsumed: () -> Unit = {},
) {
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

    LaunchedEffect(launchRequestToken, state.checklists) {
        when {
            requestedAlgorithmId != null && state.checklists.any { it.id == requestedAlgorithmId } -> {
                navController.navigate("runner/$requestedAlgorithmId") { launchSingleTop = true }
                onLaunchConsumed()
            }
            requestedDestination in bottomDestinations.map(Destination::route) -> {
                navController.navigateSingleTop(requestedDestination!!)
                onLaunchConsumed()
            }
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
                        checklists = viewModel.filteredChecklists(),
                        recentChecklistIds = viewModel.recentChecklistIds(),
                        onQueryChange = viewModel::updateQuery,
                        onChecklistClick = { navController.navigate("runner/$it") },
                        onChecklistEdit = { navController.navigate("editor/$it") },
                        onChecklistDelete = viewModel::deleteChecklist,
                        onChecklistFavorite = viewModel::toggleChecklistFavorite,
                        onChecklistDuplicate = viewModel::duplicateChecklist,
                        onActiveRunClick = { navController.navigate("runner/$it") },
                        onFinishAllRuns = viewModel::finishAllRuns,
                        onAiClick = { navController.navigateSingleTop("ai") },
                        onCreateClick = {
                            viewModel.clearGeneratedChecklist()
                            navController.navigate("editor")
                        },
                    )
                }
                composable("schedule") {
                    ScheduleScreen(
                        items = state.scheduleItems,
                        algorithms = state.checklists,
                        preferences = state.preferences,
                        isImprovingWithAi = state.isImprovingScheduleItem,
                        aiSuggestion = state.scheduleAiSuggestion,
                        aiError = state.scheduleAiError,
                        onSave = viewModel::saveScheduleItem,
                        onDelete = viewModel::deleteScheduleItem,
                        onRunAlgorithm = { navController.navigate("runner/$it") },
                        onImproveWithAi = viewModel::improveScheduleItem,
                        onClearAi = viewModel::clearScheduleAiSuggestion,
                    )
                }
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
                        calendarWeekStartsMonday = state.preferences.calendarWeekStartsMonday,
                        onCalendarWeekStartsMondayChange = viewModel::setCalendarWeekStartsMonday,
                        calendarDefaultView = state.preferences.calendarDefaultView,
                        onCalendarDefaultViewChange = viewModel::setCalendarDefaultView,
                        calendarShowNotes = state.preferences.calendarShowNotes,
                        onCalendarShowNotesChange = viewModel::setCalendarShowNotes,
                        calendarOfferSystemExport = state.preferences.calendarOfferSystemExport,
                        onCalendarOfferSystemExportChange = viewModel::setCalendarOfferSystemExport,
                        showActiveRunOnHome = state.preferences.showActiveRunOnHome,
                        onShowActiveRunOnHomeChange = viewModel::setShowActiveRunOnHome,
                        showProgressRange = state.preferences.showProgressRange,
                        onShowProgressRangeChange = viewModel::setShowProgressRange,
                        confirmBeforeStopping = state.preferences.confirmBeforeStopping,
                        onConfirmBeforeStoppingChange = viewModel::setConfirmBeforeStopping,
                        compactAlgorithmCards = state.preferences.compactAlgorithmCards,
                        onCompactAlgorithmCardsChange = viewModel::setCompactAlgorithmCards,
                        calendarShowWeekNumbers = state.preferences.calendarShowWeekNumbers,
                        onCalendarShowWeekNumbersChange = viewModel::setCalendarShowWeekNumbers,
                        calendarHighlightCurrentWeek = state.preferences.calendarHighlightCurrentWeek,
                        onCalendarHighlightCurrentWeekChange = viewModel::setCalendarHighlightCurrentWeek,
                    )
                }
                composable("runner/{id}") { entry ->
                    val id = entry.arguments?.getString("id")
                    val checklist = viewModel.checklists.firstOrNull { it.id == id }
                    if (checklist != null) {
                        RunnerScreen(
                            checklist = checklist,
                            initialStepIndex = state.activeRuns
                                .firstOrNull { it.algorithmId == checklist.id }
                                ?.currentStepIndex
                                ?: 0,
                            initialVisitedStepIds = state.activeRuns
                                .firstOrNull { it.algorithmId == checklist.id }
                                ?.visitedStepIds
                                .orEmpty(),
                            tapNavigation = state.preferences.tapNavigation,
                            keepScreenAwake = state.preferences.keepScreenAwake,
                            autoPlayVideoNotes = state.preferences.autoPlayVideoNotes,
                            showProgressRange = state.preferences.showProgressRange,
                            confirmBeforeStopping = state.preferences.confirmBeforeStopping,
                            suggestions = viewModel.recommendationsFor(checklist.id),
                            onSuggestionClick = { suggestedId ->
                                viewModel.finishRun(checklist.id)
                                navController.popBackStack()
                                navController.navigate("runner/$suggestedId")
                            },
                            onRunProgress = { index, visited -> viewModel.updateRun(checklist, index, visited) },
                            onNavigateHome = navController::popBackStack,
                            onClose = {
                                viewModel.finishRun(checklist.id)
                                navController.popBackStack()
                            },
                        )
                    }
                }
                composable("editor") {
                    EditorScreen(
                        initialChecklist = state.generatedChecklist,
                        isGeneratingMetadata = state.isGeneratingMetadata,
                        metadataTarget = state.metadataTarget,
                        metadataSuggestion = state.metadataSuggestion,
                        isImprovingAlgorithm = state.isImprovingAlgorithm,
                        improvementStage = state.improvementStage,
                        improvementOriginal = state.improvementOriginal,
                        improvementProposal = state.improvementProposal,
                        improvementError = state.improvementError,
                        onRequestMetadata = viewModel::requestMetadata,
                        onConsumeMetadataSuggestion = viewModel::consumeMetadataSuggestion,
                        onImprove = viewModel::improveChecklist,
                        onDiscardImprovement = viewModel::discardImprovement,
                        onAcceptImprovement = {
                            viewModel.acceptImprovement()
                            navController.popBackStack()
                        },
                        onSave = {
                            viewModel.saveChecklist(it)
                            navController.popBackStack()
                        },
                        onClose = {
                            viewModel.discardImprovement()
                            navController.popBackStack()
                        },
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
                            isImprovingAlgorithm = state.isImprovingAlgorithm,
                            improvementStage = state.improvementStage,
                            improvementOriginal = state.improvementOriginal,
                            improvementProposal = state.improvementProposal,
                            improvementError = state.improvementError,
                            onRequestMetadata = viewModel::requestMetadata,
                            onConsumeMetadataSuggestion = viewModel::consumeMetadataSuggestion,
                            onImprove = viewModel::improveChecklist,
                            onDiscardImprovement = viewModel::discardImprovement,
                            onAcceptImprovement = {
                                viewModel.acceptImprovement()
                                navController.popBackStack()
                            },
                            onSave = {
                                viewModel.saveChecklist(it)
                                navController.popBackStack()
                            },
                            onClose = {
                                viewModel.discardImprovement()
                                navController.popBackStack()
                            },
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
