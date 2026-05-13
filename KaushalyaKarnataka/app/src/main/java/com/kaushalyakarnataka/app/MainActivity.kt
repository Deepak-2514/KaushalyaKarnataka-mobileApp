package com.kaushalyakarnataka.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.kaushalyakarnataka.app.data.KaushalyaRepository
import com.kaushalyakarnataka.app.data.UserProfile
import com.kaushalyakarnataka.app.data.UserRole
import com.kaushalyakarnataka.app.ui.AppLanguage
import com.kaushalyakarnataka.app.ui.MainViewModel
import com.kaushalyakarnataka.app.ui.Strings
import com.kaushalyakarnataka.app.ui.screens.ChatDetailScreen
import com.kaushalyakarnataka.app.ui.screens.ChatsScreen
import com.kaushalyakarnataka.app.ui.screens.DashboardScreen
import com.kaushalyakarnataka.app.ui.screens.HomeScreen
import com.kaushalyakarnataka.app.ui.screens.LoginScreen
import com.kaushalyakarnataka.app.ui.screens.ManageServicesScreen
import com.kaushalyakarnataka.app.ui.screens.MyHiresScreen
import com.kaushalyakarnataka.app.ui.screens.OnboardingScreen
import com.kaushalyakarnataka.app.ui.screens.ProfileScreen
import com.kaushalyakarnataka.app.ui.screens.RequestsScreen
import com.kaushalyakarnataka.app.ui.screens.WorkerProfileScreen
import com.kaushalyakarnataka.app.ui.theme.KaushalyaColors
import com.kaushalyakarnataka.app.ui.theme.KaushalyaTheme
import com.kaushalyakarnataka.app.ui.stringsFor
import kotlinx.coroutines.launch

private object Routes {
    const val HOME = "home"
    const val DASHBOARD = "dashboard"
    const val MY_HIRES = "my_hires"
    const val REQUESTS = "requests"
    const val CHATS = "chats"
    const val PROFILE = "profile"
    const val MANAGE_SERVICES = "manage_services"
    const val WORKER = "worker/{workerId}"
    const val CHAT = "chat/{chatId}"
}

private data class TabDest(val route: String, val label: String, val icon: ImageVector)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KaushalyaTheme {
                val repo = remember { KaushalyaRepository() }
                val vm: MainViewModel = viewModel(factory = MainViewModel.factory(application, repo))
                val session by vm.session.collectAsStateWithLifecycle()
                val language by vm.language.collectAsStateWithLifecycle()
                val strings = remember(language) { stringsFor(language) }

                val snackbar = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                fun toast(msg: String) {
                    scope.launch { snackbar.showSnackbar(msg) }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbar) },
                    containerColor = MaterialTheme.colorScheme.background
                ) { padding ->
                    Box(Modifier.padding(padding)) {
                        when {
                            session.loading -> {
                                LoadingState(strings.loading)
                            }

                            session.uid == null -> {
                                LoginScreen(
                                    strings = strings,
                                    repo = repo,
                                    onSignedIn = { },
                                    onMessage = ::toast,
                                )
                            }

                            session.profile == null -> {
                                val uid = session.uid ?: return@Box
                                val user = FirebaseAuth.getInstance().currentUser
                                OnboardingScreen(
                                    uid = uid,
                                    strings = strings,
                                    repo = repo,
                                    authDisplayName = user?.displayName,
                                    authPhotoUrl = user?.photoUrl?.toString(),
                                    onDone = { },
                                    onMessage = ::toast,
                                )
                            }

                            else -> {
                                MainShell(
                                    profile = session.profile!!,
                                    strings = strings,
                                    language = language,
                                    repo = repo,
                                    snackbar = snackbar,
                                    onToast = ::toast,
                                    onLanguageToggle = {
                                        vm.setLanguage(if (language == AppLanguage.EN) AppLanguage.KN else AppLanguage.EN)
                                    },
                                    onSignOut = { vm.signOut() },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState(label: String) {
    Box(
        Modifier
            .fillMaxSize()
            .background(KaushalyaColors.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = KaushalyaColors.Primary,
                strokeWidth = 2.dp
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = KaushalyaColors.TextSecondary
            )
        }
    }
}

@Composable
private fun MainShell(
    profile: UserProfile,
    strings: Strings,
    language: AppLanguage,
    repo: KaushalyaRepository,
    snackbar: SnackbarHostState,
    onToast: (String) -> Unit,
    onLanguageToggle: () -> Unit,
    onSignOut: () -> Unit,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route.orEmpty()
    val hideBottomNav = currentRoute.startsWith("worker/") || currentRoute.startsWith("chat/")

    val tabs = remember(profile.role, strings) {
        when (profile.role) {
            UserRole.customer -> listOf(
                TabDest(Routes.HOME, strings.home, Icons.Outlined.Home),
                TabDest(Routes.MY_HIRES, strings.myHires, Icons.AutoMirrored.Outlined.ListAlt),
                TabDest(Routes.CHATS, strings.chats, Icons.AutoMirrored.Outlined.Chat),
                TabDest(Routes.PROFILE, strings.profile, Icons.Outlined.Person),
            )

            UserRole.worker -> listOf(
                TabDest(Routes.DASHBOARD, strings.dashboard, Icons.Outlined.WorkOutline),
                TabDest(Routes.REQUESTS, strings.requests, Icons.AutoMirrored.Outlined.ListAlt),
                TabDest(Routes.CHATS, strings.chats, Icons.AutoMirrored.Outlined.Chat),
                TabDest(Routes.PROFILE, strings.profile, Icons.Outlined.Person),
            )
        }
    }

    val start = if (profile.role == UserRole.worker) Routes.DASHBOARD else Routes.HOME

    Scaffold(
        bottomBar = {
            if (!hideBottomNav) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding()
                ) {
                    Surface(
                        color = KaushalyaColors.NavBackground,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, KaushalyaColors.Border),
                        shadowElevation = 12.dp,
                        modifier = Modifier.height(72.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            tabs.forEach { tab ->
                                val selected = currentRoute == tab.route
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable {
                                            navController.navigate(tab.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                        .weight(1f)
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.label,
                                        tint = if (selected) KaushalyaColors.Primary else KaushalyaColors.UnselectedTab,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    if (selected) {
                                        Spacer(Modifier.height(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(12.dp)
                                                .height(3.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(KaushalyaColors.Primary)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = start,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    profile = profile,
                    strings = strings,
                    language = language,
                    onToggleLanguage = onLanguageToggle,
                    repo = repo,
                    onOpenWorker = { id -> navController.navigate("worker/$id") },
                )
            }
            composable(Routes.MY_HIRES) {
                MyHiresScreen(
                    profile = profile,
                    strings = strings,
                    repo = repo,
                    onOpenChat = { id -> navController.navigate("chat/$id") },
                    onMessage = onToast,
                )
            }
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    profile = profile,
                    strings = strings,
                    repo = repo,
                    onOpenRequests = { navController.navigate(Routes.REQUESTS) },
                    onOpenChat = { id -> navController.navigate("chat/$id") },
                    onMessage = onToast,
                )
            }
            composable(Routes.REQUESTS) {
                RequestsScreen(profile = profile, strings = strings, repo = repo, onMessage = onToast)
            }
            composable(Routes.CHATS) {
                ChatsScreen(
                    profile = profile,
                    strings = strings,
                    repo = repo,
                    onOpenChat = { id -> navController.navigate("chat/$id") },
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    profile = profile,
                    strings = strings,
                    language = language,
                    repo = repo,
                    onToggleLanguage = onLanguageToggle,
                    onSignOut = onSignOut,
                    onManageServices = { navController.navigate(Routes.MANAGE_SERVICES) },
                    onMessage = onToast,
                )
            }
            composable(Routes.MANAGE_SERVICES) {
                ManageServicesScreen(
                    repo = repo,
                    strings = strings,
                    onBack = { navController.popBackStack() },
                    onMessage = onToast
                )
            }
            composable(
                Routes.WORKER,
                arguments = listOf(navArgument("workerId") { type = NavType.StringType }),
            ) { entry ->
                val workerId = entry.arguments?.getString("workerId").orEmpty()
                WorkerProfileScreen(
                    workerId = workerId,
                    customer = profile,
                    strings = strings,
                    repo = repo,
                    onBack = { navController.popBackStack() },
                    onHired = {
                        navController.navigate(Routes.MY_HIRES) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onMessage = onToast,
                )
            }
            composable(
                Routes.CHAT,
                arguments = listOf(navArgument("chatId") { type = NavType.StringType }),
            ) { entry ->
                val chatId = entry.arguments?.getString("chatId").orEmpty()
                ChatDetailScreen(
                    chatId = chatId,
                    profile = profile,
                    strings = strings,
                    repo = repo,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
