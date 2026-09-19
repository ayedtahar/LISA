package com.lisa.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lisa.app.data.ProfileRepository
import com.lisa.app.data.UserProfile
import com.lisa.app.service.BubbleOverlayService
import com.lisa.app.ui.screens.HomeScreen
import com.lisa.app.ui.screens.OnboardingScreen
import com.lisa.app.ui.screens.ProfileScreen
import com.lisa.app.ui.theme.LisaTheme
import com.lisa.app.util.PermissionUtils
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LisaTheme {
                LisaApp()
            }
        }
    }
}

private const val ROUTE_HOME = "home"
private const val ROUTE_PROFILE = "profile"

@Composable
private fun LisaApp() {
    val context = LocalContext.current
    val profileRepository = remember { ProfileRepository(context.applicationContext) }

    var overlayGranted by remember { mutableStateOf(PermissionUtils.canDrawOverlays(context)) }
    var accessibilityGranted by remember { mutableStateOf(PermissionUtils.isAccessibilityServiceEnabled(context)) }
    var bubbleEnabled by rememberSaveable { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                overlayGranted = PermissionUtils.canDrawOverlays(context)
                accessibilityGranted = PermissionUtils.isAccessibilityServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionsReady = overlayGranted && accessibilityGranted

    if (!permissionsReady) {
        OnboardingScreen(
            overlayGranted = overlayGranted,
            accessibilityGranted = accessibilityGranted,
            onRequestOverlay = {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            },
            onRequestAccessibility = {
                context.startActivity(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            },
        )
        return
    }

    val navController = rememberNavController()
    Scaffold(bottomBar = { LisaBottomBar(navController) }) { padding ->
        NavHost(navController = navController, startDestination = ROUTE_HOME, modifier = Modifier.padding(padding)) {
            composable(ROUTE_HOME) {
                HomeScreen(
                    bubbleEnabled = bubbleEnabled,
                    permissionsReady = permissionsReady,
                    onBubbleToggle = { enabled ->
                        bubbleEnabled = enabled
                        val intent = Intent(context, BubbleOverlayService::class.java)
                        if (enabled) context.startForegroundService(intent) else context.stopService(intent)
                    },
                )
            }
            composable(ROUTE_PROFILE) {
                val profile by profileRepository.profile.collectAsState(initial = UserProfile.EMPTY)
                val scope = rememberCoroutineScope()
                ProfileScreen(
                    profile = profile,
                    onSave = { updated -> scope.launch { profileRepository.save(updated) } },
                )
            }
        }
    }
}

@Composable
private fun LisaBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == ROUTE_HOME,
            onClick = { navController.navigate(ROUTE_HOME) { launchSingleTop = true } },
            icon = {},
            label = { Text(stringResource(R.string.home_title)) },
        )
        NavigationBarItem(
            selected = currentRoute == ROUTE_PROFILE,
            onClick = { navController.navigate(ROUTE_PROFILE) { launchSingleTop = true } },
            icon = {},
            label = { Text(stringResource(R.string.profile_title)) },
        )
    }
}
