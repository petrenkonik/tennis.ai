package com.rebuild.tennisai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rebuild.tennisai.core.model.StrokeType
import com.rebuild.tennisai.ui.analyze.AnalyzeScreen
import com.rebuild.tennisai.ui.axel.AxelScreen
import com.rebuild.tennisai.ui.common.BottomNavBar
import com.rebuild.tennisai.ui.dashboard.CoachDashboardScreen
import com.rebuild.tennisai.ui.dashboard.PlayerDashboardScreen
import com.rebuild.tennisai.ui.dashboard.SelectDashboardScreen
import com.rebuild.tennisai.ui.home.HomeScreen
import com.rebuild.tennisai.ui.library.AnalysesScreen
import com.rebuild.tennisai.ui.library.VideosScreen
import com.rebuild.tennisai.ui.live.LiveAnalysisScreen
import com.rebuild.tennisai.ui.profile.EditProfileScreen
import com.rebuild.tennisai.ui.profile.ProfileScreen
import com.rebuild.tennisai.ui.results.ResultsScreen
import com.rebuild.tennisai.ui.theme.TennisTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TennisTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    App()
                }
            }
        }
    }
}

@Composable
fun App(state: AppState = viewModel()) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route

    // Tabs with the bottom navigation bar (CLONE_SPEC §3): select/edit/axel/analyze/live/results are fullscreen
    val showBottomBar = route in setOf(
        AppState.ROUTE_HOME,
        AppState.ROUTE_VIDEOS,
        AppState.ROUTE_ANALYSES,
        AppState.ROUTE_DASHBOARD_PLAYER,
        AppState.ROUTE_DASHBOARD_COACH,
        AppState.ROUTE_PROFILE,
    )

    fun goTab(target: String) {
        // the grid tab leads to select_dashboard, the rest go to their tab roots
        val dest = if (target == AppState.ROUTE_DASHBOARD) AppState.ROUTE_SELECT_DASHBOARD else target
        nav.navigate(dest) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color(0xFF050505),
        bottomBar = { if (showBottomBar) BottomNavBar(currentRoute = route) { goTab(it) } },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = AppState.ROUTE_HOME,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(AppState.ROUTE_HOME) {
                HomeScreen(
                    state = state,
                    onLive = { nav.navigate(AppState.ROUTE_LIVE) },
                    onAnalyze = { stroke: StrokeType? ->
                        stroke?.let { state.stroke = it }
                        nav.navigate(AppState.ROUTE_ANALYZE)
                    },
                    onAxel = { nav.navigate(AppState.ROUTE_AXEL) },
                )
            }
            composable(AppState.ROUTE_VIDEOS) {
                VideosScreen(state = state, onAxel = { nav.navigate(AppState.ROUTE_AXEL) })
            }
            composable(AppState.ROUTE_ANALYSES) {
                AnalysesScreen(state = state, onAxel = { nav.navigate(AppState.ROUTE_AXEL) })
            }
            composable(AppState.ROUTE_DASHBOARD_PLAYER) {
                PlayerDashboardScreen(state = state, onAxel = { nav.navigate(AppState.ROUTE_AXEL) })
            }
            composable(AppState.ROUTE_DASHBOARD_COACH) {
                CoachDashboardScreen(state = state, onAxel = { nav.navigate(AppState.ROUTE_AXEL) })
            }
            composable(AppState.ROUTE_PROFILE) {
                ProfileScreen(
                    state = state,
                    onEdit = { nav.navigate(AppState.ROUTE_PROFILE_EDIT) },
                    onAxel = { nav.navigate(AppState.ROUTE_AXEL) },
                )
            }
            // Fullscreen screens
            composable(AppState.ROUTE_SELECT_DASHBOARD) {
                SelectDashboardScreen(
                    lang = state.lang,
                    onPlayer = {
                        nav.navigate(AppState.ROUTE_DASHBOARD_PLAYER) { launchSingleTop = true }
                    },
                    onCoach = {
                        nav.navigate(AppState.ROUTE_DASHBOARD_COACH) { launchSingleTop = true }
                    },
                    onBack = { nav.popBackStack() },
                )
            }
            composable(AppState.ROUTE_PROFILE_EDIT) {
                EditProfileScreen(state = state, onBack = { nav.popBackStack() })
            }
            composable(AppState.ROUTE_LIVE) {
                LiveAnalysisScreen(state = state, onBack = { nav.popBackStack() })
            }
            composable(AppState.ROUTE_ANALYZE) {
                AnalyzeScreen(
                    state = state,
                    onDone = { state.onAnalysisCompleted(); nav.navigate(AppState.ROUTE_RESULTS) },
                )
            }
            composable(AppState.ROUTE_RESULTS) {
                ResultsScreen(state = state, onAgain = { nav.popBackStack(AppState.ROUTE_HOME, inclusive = false) })
            }
            composable(AppState.ROUTE_AXEL) {
                AxelScreen(
                    state = state,
                    onBack = { nav.popBackStack() },
                    onRecord = {
                        nav.popBackStack()
                        nav.navigate(AppState.ROUTE_ANALYZE)
                    },
                    onUpload = {
                        nav.popBackStack()
                        nav.navigate(AppState.ROUTE_ANALYZE)
                    },
                )
            }
        }
    }
}
