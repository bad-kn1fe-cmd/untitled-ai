package com.example.util

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Manages background AI response generation:
 * 1. CPU WakeLock so generation and network calls do not get suspended when screen is locked or app is minimized.
 * 2. Background ongoing status notification during generation.
 * 3. High-priority rich notification when character finishes generating, allowing the user to tap and jump directly into the chat.
 */
object BackgroundGenerationManager {

    private const val CHANNEL_CHAT_RESPONSES = "ai_chat_responses_channel"
    private const val CHANNEL_GENERATION_PROGRESS = "ai_generation_progress_channel"

    private const val PROGRESS_NOTIFICATION_BASE_ID = 20000
    private const val RESPONSE_NOTIFICATION_BASE_ID = 10000

    private var appContext: Context? = null
    private var powerManager: PowerManager? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val isAppInForeground = AtomicBoolean(true)
    private val activeGenerations = ConcurrentHashMap<Long, String>() // chatId -> characterName
    private val notificationCounter = AtomicInteger(0)

    fun init(context: Context) {
        val app = context.applicationContext
        appContext = app
        powerManager = app.getSystemService(Context.POWER_SERVICE) as? PowerManager

        createNotificationChannels(app)
    }

    fun setAppForegroundState(inForeground: Boolean) {
        val wasForeground = isAppInForeground.getAndSet(inForeground)
        if (!inForeground && activeGenerations.isNotEmpty()) {
            // App was minimized while generation is in progress: show progress notification
            activeGenerations.forEach { (chatId, characterName) ->
                showProgressNotification(chatId, characterName)
            }
        } else if (inForeground) {
            // App came back to foreground: clear any lingering progress notifications
            activeGenerations.keys.forEach { chatId ->
                cancelProgressNotification(chatId)
            }
        }
    }

    fun getAppForegroundState(): Boolean {
        return isAppInForeground.get()
    }

    @Synchronized
    fun startGeneration(chatId: Long, characterName: String) {
        val ctx = appContext ?: return
        activeGenerations[chatId] = characterName

        // Start Foreground Service to maintain CPU and unrestricted network connectivity in background
        AiGenerationService.start(ctx, chatId, characterName)

        // Acquire WakeLock (max 3 minutes timeout safety)
        try {
            if (wakeLock == null) {
                wakeLock = powerManager?.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "AiRoleplay:GenerationWakeLock"
                )?.apply {
                    setReferenceCounted(true)
                }
            }
            wakeLock?.acquire(180_000L) // 3 minutes timeout
        } catch (_: Exception) {}

        // If app is currently backgrounded, immediately show progress notification
        if (!isAppInForeground.get()) {
            showProgressNotification(chatId, characterName)
        }
    }

    @Synchronized
    fun endGeneration(
        chatId: Long,
        characterName: String,
        responseText: String?,
        isError: Boolean = false,
        notificationsEnabled: Boolean = true
    ) {
        val ctx = appContext ?: return
        activeGenerations.remove(chatId)

        // Stop Foreground Service for this chat
        AiGenerationService.stop(ctx, chatId)

        // Cancel the ongoing progress notification
        cancelProgressNotification(chatId)

        // Release WakeLock safely
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}

        // If app is in background and answer was successfully generated, post notification
        if (!isAppInForeground.get() && !responseText.isNullOrBlank() && !isError && notificationsEnabled) {
            showResponseNotification(chatId, characterName, responseText)
        }
    }

    private fun showProgressNotification(chatId: Long, characterName: String) {
        val ctx = appContext ?: return
        try {
            if (!hasNotificationPermission(ctx)) return

            val intent = Intent(ctx, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("target_chat_id", chatId)
            }

            val pendingIntent = PendingIntent.getActivity(
                ctx,
                chatId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(ctx, CHANNEL_GENERATION_PROGRESS)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentTitle(characterName)
                .setContentText("Печатает ответ...")
                .setProgress(0, 0, true)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .setAutoCancel(false)
                .build()

            NotificationManagerCompat.from(ctx).notify(
                PROGRESS_NOTIFICATION_BASE_ID + (chatId % 1000).toInt(),
                notification
            )
        } catch (_: Exception) {}
    }

    private fun cancelProgressNotification(chatId: Long) {
        val ctx = appContext ?: return
        try {
            NotificationManagerCompat.from(ctx).cancel(
                PROGRESS_NOTIFICATION_BASE_ID + (chatId % 1000).toInt()
            )
        } catch (_: Exception) {}
    }

    private fun showResponseNotification(
        chatId: Long,
        characterName: String,
        responseText: String
    ) {
        val ctx = appContext ?: return
        try {
            if (!hasNotificationPermission(ctx)) return

            val intent = Intent(ctx, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("target_chat_id", chatId)
            }

            val pendingIntent = PendingIntent.getActivity(
                ctx,
                (chatId + 10000).toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Clean text preview (remove system asterisks/markdown tags for clean push notification readability)
            val cleanText = responseText
                .replace(Regex("""[*_~`#>]"""), "")
                .replace(Regex("""\s+"""), " ")
                .trim()

            val snippet = if (cleanText.length > 120) cleanText.take(120) + "..." else cleanText

            val notification = NotificationCompat.Builder(ctx, CHANNEL_CHAT_RESPONSES)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(characterName)
                .setContentText(snippet)
                .setStyle(NotificationCompat.BigTextStyle().bigText(cleanText).setSummaryText("Новое сообщение"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            val notifId = RESPONSE_NOTIFICATION_BASE_ID + (chatId % 1000).toInt()
            NotificationManagerCompat.from(ctx).notify(notifId, notification)
        } catch (_: Exception) {}
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    private fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            // 1. High priority responses channel
            val responsesChannel = NotificationChannel(
                CHANNEL_CHAT_RESPONSES,
                "Ответы персонажей",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о новых ответах персонажей, когда приложение свёрнуто"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 150, 80, 150)
            }

            // 2. Silent progress channel for ongoing generation
            val progressChannel = NotificationChannel(
                CHANNEL_GENERATION_PROGRESS,
                "Генерация ответа в фоне",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Индикация фоновой работы генерации AI"
                enableVibration(false)
                setShowBadge(false)
            }

            manager.createNotificationChannel(responsesChannel)
            manager.createNotificationChannel(progressChannel)
        }
    }
}
