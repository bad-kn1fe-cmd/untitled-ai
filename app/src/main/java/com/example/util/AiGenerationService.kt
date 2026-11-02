package com.example.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import java.util.concurrent.ConcurrentHashMap

/**
 * Foreground Service that keeps CPU awake and keeps network connection unrestricted
 * by Android OS Doze mode / battery optimizations during background AI responses.
 */
class AiGenerationService : Service() {

    companion object {
        const val ACTION_START_GENERATION = "com.example.action.START_GENERATION"
        const val ACTION_STOP_GENERATION = "com.example.action.STOP_GENERATION"
        const val EXTRA_CHAT_ID = "extra_chat_id"
        const val EXTRA_CHARACTER_NAME = "extra_character_name"

        const val FOREGROUND_NOTIFICATION_ID = 40001
        const val CHANNEL_ID = "ai_generation_foreground_channel"

        private val activeChats = ConcurrentHashMap<Long, String>()

        fun start(context: Context, chatId: Long, characterName: String) {
            val intent = Intent(context, AiGenerationService::class.java).apply {
                action = ACTION_START_GENERATION
                putExtra(EXTRA_CHAT_ID, chatId)
                putExtra(EXTRA_CHARACTER_NAME, characterName)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {}
        }

        fun stop(context: Context, chatId: Long) {
            val intent = Intent(context, AiGenerationService::class.java).apply {
                action = ACTION_STOP_GENERATION
                putExtra(EXTRA_CHAT_ID, chatId)
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "AiRoleplay:GenerationServiceWakeLock"
            )?.apply {
                setReferenceCounted(true)
            }
            wakeLock?.acquire(180_000L) // 3 mins max safe timeout
        } catch (_: Exception) {}
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY
        val chatId = intent.getLongExtra(EXTRA_CHAT_ID, -1L)
        val charName = intent.getStringExtra(EXTRA_CHARACTER_NAME) ?: "Персонаж"

        when (action) {
            ACTION_START_GENERATION -> {
                if (chatId > 0) {
                    activeChats[chatId] = charName
                }
                val notification = buildForegroundNotification()
                val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                } else {
                    0
                }
                ServiceCompat.startForeground(
                    this,
                    FOREGROUND_NOTIFICATION_ID,
                    notification,
                    fgsType
                )
            }
            ACTION_STOP_GENERATION -> {
                if (chatId > 0) {
                    activeChats.remove(chatId)
                }
                if (activeChats.isEmpty()) {
                    stopForegroundCompat()
                    stopSelf()
                } else {
                    // Update notification with remaining generations
                    val notification = buildForegroundNotification()
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    nm?.notify(FOREGROUND_NOTIFICATION_ID, notification)
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun stopForegroundCompat() {
        try {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
        activeChats.clear()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Генерация ответов в фоне",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Отображает процесс генерации реплики нейросетью"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val firstEntry = activeChats.entries.firstOrNull()
        val title = if (activeChats.size > 1) {
            "Генерация ответов (${activeChats.size})"
        } else if (firstEntry != null) {
            firstEntry.value
        } else {
            "Нейросеть отвечает..."
        }

        val text = "Составляет ответ в фоне..."

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            firstEntry?.let { putExtra("target_chat_id", it.key) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            1005,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(title)
            .setContentText(text)
            .setProgress(0, 0, true)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .build()
    }
}
