package com.pause.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import com.pause.app.MainActivity
import com.pause.app.R
import com.pause.app.core.Constants
import com.pause.app.domain.model.DetectionMode
import com.pause.app.domain.repository.SettingsRepository
import com.pause.app.overlay.OverlayController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Usage-Access foreground detection (the default, Play-friendly mode). A small foreground service
 * polls [UsageStatsManager] every second for the current foreground app and forwards changes to the
 * shared [SessionController]. It runs only while the user's chosen mode is [DetectionMode.USAGE_ACCESS];
 * if the mode changes away (or monitoring is no longer possible) it stops itself.
 */
@AndroidEntryPoint
class UsageAccessMonitorService : Service() {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var overlayController: OverlayController

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var controller: SessionController
    private val usageStatsManager by lazy { getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager }
    private val ignoredPackages: Set<String> by lazy { buildIgnoredPackages() }

    private var pollJob: Job? = null
    private var lastQueryEnd = 0L
    private var started = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        controller = SessionController(
            context = applicationContext,
            scope = scope,
            overlayController = overlayController,
            ownPackage = packageName,
            ignoredPackages = ignoredPackages,
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!started) {
            started = true
            promoteToForeground()
            observeSettings()
            startPolling()
        }
        return START_STICKY
    }

    private fun observeSettings() {
        settingsRepository.settings
            .onEach { updated ->
                if (updated.detectionMode != DetectionMode.USAGE_ACCESS) {
                    controller.stop()
                    stopSelf()
                    return@onEach
                }
                controller.onSettings(updated)
            }
            .launchIn(scope)
    }

    private fun startPolling() {
        pollJob?.cancel()
        lastQueryEnd = System.currentTimeMillis() - INITIAL_LOOKBACK_MS
        pollJob = scope.launch {
            while (isActive) {
                pollForeground()
                delay(Constants.USAGE_POLL_INTERVAL_MS)
            }
        }
    }

    /** Find the most recent foreground app since the last poll and forward it (controller dedupes). */
    @Suppress("DEPRECATION") // MOVE_TO_FOREGROUND is still emitted and works on minSdk 26.
    private fun pollForeground() {
        val now = System.currentTimeMillis()
        val begin = (lastQueryEnd - WINDOW_OVERLAP_MS).coerceAtLeast(0)
        var latestPackage: String? = null
        var latestTime = 0L
        runCatching {
            val events = usageStatsManager.queryEvents(begin, now)
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND && event.timeStamp >= latestTime) {
                    latestTime = event.timeStamp
                    latestPackage = event.packageName
                }
            }
        }
        lastQueryEnd = now
        latestPackage?.let { controller.onForegroundChanged(it) }
    }

    private fun promoteToForeground() {
        val channelId = Constants.MONITOR_CHANNEL_ID
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existing = manager.getNotificationChannel(channelId)
            if (existing == null) {
                manager.createNotificationChannel(
                    NotificationChannel(channelId, "Pause monitoring", NotificationManager.IMPORTANCE_LOW).apply {
                        description = "Lets Pause notice long scrolling sessions in the background."
                        setShowBadge(false)
                    },
                )
            }
        }

        val tapIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification: Notification = Notification.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle("Pause is on")
            .setContentText("Watching for long scrolling sessions.")
            .setOngoing(true)
            .setContentIntent(tapIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(Constants.MONITOR_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(Constants.MONITOR_NOTIFICATION_ID, notification)
        }
    }

    private fun buildIgnoredPackages(): Set<String> {
        val ignored = mutableSetOf("com.android.systemui")
        runCatching {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.enabledInputMethodList.forEach { info -> info.packageName?.let(ignored::add) }
        }
        return ignored
    }

    override fun onDestroy() {
        pollJob?.cancel()
        if (::controller.isInitialized) controller.stop()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val INITIAL_LOOKBACK_MS = 60_000L
        private const val WINDOW_OVERLAP_MS = 1_500L

        fun start(context: Context) {
            // Guard against the rare background-start case (Android 12+ throws if not allowed).
            runCatching {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, UsageAccessMonitorService::class.java),
                )
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, UsageAccessMonitorService::class.java))
        }
    }
}
