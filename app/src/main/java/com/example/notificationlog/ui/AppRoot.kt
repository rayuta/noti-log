package com.example.notificationlog.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.notificationlog.NotificationLogApp
import com.example.notificationlog.data.repository.SettingsRepository
import com.example.notificationlog.service.NotificationAccess
import com.example.notificationlog.ui.detail.DetailScreen
import com.example.notificationlog.ui.filter.FilterListScreen
import com.example.notificationlog.ui.filter.FilterResultScreen
import com.example.notificationlog.ui.home.HomeScreen
import com.example.notificationlog.ui.search.SearchScreen
import com.example.notificationlog.ui.settings.AppSelectionScreen
import com.example.notificationlog.ui.settings.SettingsScreen
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val FILTERS = "filters"
    const val SETTINGS = "settings"
    const val APP_SELECTION = "app_selection"
    const val DETAIL = "detail/{id}"
    const val FILTER_RESULT = "filter_result/{id}"

    fun detail(id: Long) = "detail/$id"
    fun filterResult(id: Long) = "filter_result/$id"
}

private data class TopLevelDestination(val route: String, val label: String, val icon: ImageVector)

private val topLevelDestinations = listOf(
    TopLevelDestination(Routes.HOME, "알림", Icons.Filled.Home),
    TopLevelDestination(Routes.SEARCH, "검색", Icons.Filled.Search),
    TopLevelDestination(Routes.FILTERS, "필터", Icons.AutoMirrored.Filled.List),
    TopLevelDestination(Routes.SETTINGS, "설정", Icons.Filled.Settings),
)

/** 첫 실행 개인정보 안내 표시 여부 */
class AppRootViewModel(private val settings: SettingsRepository) : ViewModel() {
    /** null: 아직 불러오는 중 */
    val onboardingDone: StateFlow<Boolean?> = settings.onboardingDone
        .map<Boolean, Boolean?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun completeOnboarding() {
        viewModelScope.launch { settings.setOnboardingDone() }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as NotificationLogApp
                AppRootViewModel(app.container.settingsRepository)
            }
        }
    }
}

@Composable
fun AppRoot(rootViewModel: AppRootViewModel = viewModel(factory = AppRootViewModel.Factory)) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val accessGranted by NotificationAccess.granted.collectAsStateWithLifecycle()
    val onboardingDone by rootViewModel.onboardingDone.collectAsStateWithLifecycle()
    var showPrivacy by remember { mutableStateOf(false) }

    // 설정 화면에서 돌아올 때마다 권한 상태를 다시 확인해 UI 를 자동 갱신한다.
    LifecycleResumeEffect(Unit) {
        NotificationAccess.refresh(context)
        NotificationAccess.requestRebindIfNeeded(context)
        onPauseOrDispose { }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = topLevelDestinations.any { dest ->
        currentDestination?.hierarchy?.any { it.route == dest.route } == true
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topLevelDestinations.forEach { dest ->
                        val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = null) },
                            label = { Text(dest.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    accessGranted = accessGranted,
                    onOpenDetail = { navController.navigate(Routes.detail(it)) },
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(onOpenDetail = { navController.navigate(Routes.detail(it)) })
            }
            composable(Routes.FILTERS) {
                FilterListScreen(onOpenFilter = { navController.navigate(Routes.filterResult(it)) })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    accessGranted = accessGranted,
                    onOpenAppSelection = { navController.navigate(Routes.APP_SELECTION) },
                    onShowPrivacy = { showPrivacy = true },
                )
            }
            composable(Routes.APP_SELECTION) {
                AppSelectionScreen(onBack = { navController.popBackStack() })
            }
            composable(
                Routes.DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: return@composable
                DetailScreen(id = id, onBack = { navController.popBackStack() })
            }
            composable(
                Routes.FILTER_RESULT,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: return@composable
                FilterResultScreen(
                    filterId = id,
                    onBack = { navController.popBackStack() },
                    onOpenDetail = { navController.navigate(Routes.detail(it)) },
                )
            }
        }
    }

    // 첫 실행 시(또는 설정에서 다시 보기) 개인정보 안내
    if (onboardingDone == false || showPrivacy) {
        PrivacyDialog(
            accessGranted = accessGranted,
            onOpenSettings = {
                rootViewModel.completeOnboarding()
                showPrivacy = false
                NotificationAccess.openSettings(context)
            },
            onClose = {
                rootViewModel.completeOnboarding()
                showPrivacy = false
            },
        )
    }
}

@Composable
private fun PrivacyDialog(accessGranted: Boolean, onOpenSettings: () -> Unit, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("알림 기록을 시작하기 전에") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("이 앱은 기기에 표시된 알림만 저장합니다.", style = MaterialTheme.typography.bodyLarge)
                Text("• 알림은 기기 내부에만 저장됩니다.")
                Text("• 서버로 전송되지 않습니다. (인터넷 권한 없음)")
                Text("• 사용자가 명시적으로 내보내기 전까지 외부로 나가지 않습니다.")
                Text("• 금융·인증번호 등 민감한 알림이 포함될 수 있으니 내보낸 파일 관리에 주의하세요.")
                if (!accessGranted) {
                    Text(
                        "알림 접근 권한이 필요합니다. 다음 화면에서 '알림 기록'을 찾아 허용해 주세요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        confirmButton = {
            if (accessGranted) {
                TextButton(onClick = onClose) { Text("확인") }
            } else {
                TextButton(onClick = onOpenSettings) { Text("권한 설정으로 이동") }
            }
        },
        dismissButton = {
            if (!accessGranted) TextButton(onClick = onClose) { Text("나중에") }
        },
    )
}
