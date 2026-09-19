package com.lisa.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.IntSize
import androidx.core.app.NotificationCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.lisa.app.MainActivity
import com.lisa.app.R
import com.lisa.app.data.ProfileRepository
import com.lisa.app.domain.CapturedContent
import com.lisa.app.domain.MockSummarizationEngine
import com.lisa.app.domain.SummarizationEngine
import com.lisa.app.overlay.BubbleOverlayContent
import com.lisa.app.overlay.BubbleUiState
import com.lisa.app.overlay.OverlayLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Héberge la bulle flottante (façon "chat head") par-dessus les autres apps.
 * Au tap, récupère le dernier contenu lu par [ScreenContentAccessibilityService],
 * propose de le résumer, puis affiche le résultat produit par [SummarizationEngine].
 */
class BubbleOverlayService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val overlayLifecycleOwner = OverlayLifecycleOwner()
    private val summarizationEngine: SummarizationEngine = MockSummarizationEngine()
    private lateinit var profileRepository: ProfileRepository

    private lateinit var windowManager: WindowManager
    private lateinit var composeView: ComposeView
    private lateinit var layoutParams: WindowManager.LayoutParams

    private val uiState = MutableStateFlow<BubbleUiState>(BubbleUiState.Collapsed)
    private var lastMeasuredSize: IntSize? = null

    override fun onCreate() {
        super.onCreate()
        profileRepository = ProfileRepository(applicationContext)
        startForegroundWithNotification()
        overlayLifecycleOwner.onCreate()
        setupOverlayWindow()
        overlayLifecycleOwner.onStart()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        if (::composeView.isInitialized && composeView.isAttachedToWindow) {
            windowManager.removeView(composeView)
        }
        overlayLifecycleOwner.onDestroy()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun setupOverlayWindow() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 300
        }

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(overlayLifecycleOwner)
            setViewTreeViewModelStoreOwner(overlayLifecycleOwner)
            setViewTreeSavedStateRegistryOwner(overlayLifecycleOwner)
            setContent {
                MaterialTheme {
                    val state by uiState.collectAsState()
                    Box(modifier = Modifier.onSizeChanged { size -> resizeWindowTo(size) }) {
                        BubbleOverlayContent(
                            state = state,
                            onBubbleClick = ::handleBubbleClick,
                            onBubbleDrag = ::handleBubbleDrag,
                            onYes = ::handleYes,
                            onNo = ::collapse,
                            onClose = ::collapse,
                        )
                    }
                }
            }
        }

        windowManager.addView(composeView, layoutParams)
    }

    /**
     * Un overlay WRAP_CONTENT ne se redimensionne pas tout seul quand son contenu Compose
     * change de taille (bulle repliée <-> carte de résumé) : il faut forcer une nouvelle
     * passe de mesure/layout en réappliquant les mêmes LayoutParams (toujours WRAP_CONTENT,
     * jamais figés à une taille en pixels, pour pouvoir grandir et rétrécir dans les deux sens).
     */
    private fun resizeWindowTo(size: IntSize) {
        if (size.width == 0 || size.height == 0 || size == lastMeasuredSize) return
        lastMeasuredSize = size
        if (composeView.isAttachedToWindow) {
            runCatching { windowManager.updateViewLayout(composeView, layoutParams) }
        }
    }

    private fun handleBubbleDrag(dx: Float, dy: Float) {
        layoutParams.x += dx.toInt()
        layoutParams.y += dy.toInt()
        runCatching { windowManager.updateViewLayout(composeView, layoutParams) }
    }

    private fun handleBubbleClick() {
        val content = CapturedScreenHolder.snapshot()
        uiState.value = if (content != null && content.isUsable) {
            BubbleUiState.Prompt(content)
        } else {
            BubbleUiState.NothingCaptured
        }
    }

    private fun handleYes() {
        val current = uiState.value
        val content = (current as? BubbleUiState.Prompt)?.content ?: return
        uiState.value = BubbleUiState.Loading(content)
        serviceScope.launch { runSummarization(content) }
    }

    private suspend fun runSummarization(content: CapturedContent) {
        val profile = profileRepository.profile.first()
        val result = summarizationEngine.summarize(content, profile)
        uiState.value = BubbleUiState.Result(content, result)
    }

    private fun collapse() {
        uiState.value = BubbleUiState.Collapsed
    }

    private fun startForegroundWithNotification() {
        val channelId = "lisa_bubble_channel"
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(channelId, getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_LOW)
        )

        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_bubble)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 42
    }
}
