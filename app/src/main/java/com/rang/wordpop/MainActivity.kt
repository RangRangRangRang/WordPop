package com.rang.wordpop

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.rang.wordpop.data.AppDatabase
import com.rang.wordpop.data.SettingsRepository
import com.rang.wordpop.service.HistoryCleanupScheduler
import com.rang.wordpop.service.WordPopOverlayService
import com.rang.wordpop.ui.home.HomeScreen
import com.rang.wordpop.ui.onboarding.OnboardingScreen
import com.rang.wordpop.ui.setup.SetupScreen
import com.rang.wordpop.ui.theme.WordPopTheme
import com.rang.wordpop.util.PermissionHelper
import com.rang.wordpop.util.PermissionStatus
import com.rang.wordpop.util.TtsManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var permissionStatus by mutableStateOf(
        PermissionStatus(hasOverlay = false, hasBatteryExemption = false, hasNotification = false)
    )

    private val settingsRepo by lazy { SettingsRepository(this) }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        permissionStatus = PermissionHelper.check(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Khởi tạo Room + seed dữ liệu
        AppDatabase.getInstance(this)
        HistoryCleanupScheduler.schedule(this)

        // Khởi tạo TTS singleton 1 lần duy nhất cho cả app.
        // Các màn hình (History/Notebook) sẽ gọi TtsManager.speak() thay vì
        // tự tạo TextToSpeech riêng — tránh lag khi chuyển tab.
        TtsManager.init(this)

        permissionStatus = PermissionHelper.check(this)
        maybeStartOverlayService()

        setContent {
            WordPopTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val hasCompletedSetup by settingsRepo.hasCompletedSetup
                        .collectAsState(initial = null)

                    when {
                        !permissionStatus.allGranted -> {
                            OnboardingScreen(
                                status = permissionStatus,
                                onRequestOverlay = {
                                    startActivity(PermissionHelper.overlaySettingsIntent(this))
                                },
                                onRequestBattery = {
                                    startActivity(PermissionHelper.batteryOptimizationIntent(this))
                                },
                                onRequestNotification = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(
                                            Manifest.permission.POST_NOTIFICATIONS
                                        )
                                    }
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }

                        hasCompletedSetup == null -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        hasCompletedSetup == false -> {
                            var setupLevels by remember { mutableStateOf(setOf("B2")) }
                            var setupIdiom by remember { mutableStateOf(false) }

                            LaunchedEffect(Unit) {
                                setupLevels = settingsRepo.selectedLevels.first()
                                setupIdiom = settingsRepo.idiomFeatureEnabled.first()
                            }

                            SetupScreen(
                                selectedLevels = setupLevels,
                                idiomEnabled = setupIdiom,
                                onSelectedLevelsChange = { setupLevels = it },
                                onIdiomEnabledChange = { setupIdiom = it },
                                onFinish = {
                                    lifecycleScope.launch {
                                        settingsRepo.setSelectedLevels(setupLevels)
                                        settingsRepo.setIdiomFeatureEnabled(setupIdiom)
                                        settingsRepo.setHasCompletedSetup(true)
                                    }
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }

                        else -> {
                            HomeScreen(modifier = Modifier.padding(innerPadding))
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        permissionStatus = PermissionHelper.check(this)
        maybeStartOverlayService()
    }

    private fun maybeStartOverlayService() {
        if (permissionStatus.allGranted) {
            ContextCompat.startForegroundService(
                this,
                Intent(this, WordPopOverlayService::class.java)
            )
        }
    }
}