package com.rang.wordpop.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.rang.wordpop.MainActivity
import com.rang.wordpop.data.AppDatabase
import com.rang.wordpop.data.HistoryToday
import com.rang.wordpop.data.IdiomBank
import com.rang.wordpop.data.SettingsRepository
import com.rang.wordpop.data.VocabularySaved
import com.rang.wordpop.data.WordBank
import com.rang.wordpop.ui.overlay.WordCardOverlay
import com.rang.wordpop.ui.theme.WordPopTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class WordPopOverlayService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private var overlayLifecycleOwner: OverlayLifecycleOwner? = null
    private var tts: TextToSpeech? = null

    private var isOverlayShowing = false

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> showOverlay()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                selectBestAvailableVoice()
            }
        }.apply { language = Locale.US }

        startForeground(NOTIFICATION_ID, buildNotification())

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenReceiver, filter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_SHOW_IDIOM) {
            showIdiomOverlay()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: IllegalArgumentException) {
            // receiver đã được gỡ trước đó, bỏ qua
        }
        removeOverlay()
        tts?.shutdown()
        serviceJob.cancel()
    }

    // ----- Từ vựng -----

    private fun showOverlay() {
        if (isOverlayShowing) return
        isOverlayShowing = true

        serviceScope.launch {
            // Kiểm tra cờ bật/tắt word card — nếu user đã tắt thì bỏ qua hoàn toàn
            val settingsRepo = SettingsRepository(applicationContext)
            val enabled = settingsRepo.wordCardEnabled.first()
            if (!enabled) {
                isOverlayShowing = false
                return@launch
            }

            val db = AppDatabase.getInstance(applicationContext)
            val selectedLevels = settingsRepo.selectedLevels.first().toList()
            val startOfDay = startOfDayMillis()
            var word = db.wordBankDao().getRandomUnseenWord(selectedLevels, startOfDay)
            if (word == null) {
                word = db.wordBankDao().getRandomWordAnyState(selectedLevels)
            }
            if (word != null) addOverlayView(word) else isOverlayShowing = false
        }
    }

    private fun addOverlayView(word: WordBank) {
        val owner = OverlayLifecycleOwner().also { it.onCreate() }
        overlayLifecycleOwner = owner
        var isSaved by mutableStateOf(false)

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                WordPopTheme {
                    WordCardOverlay(
                        word = word.word,
                        pronunciation = word.pronunciation,
                        meanings = word.meanings,
                        onSpeak = { tts?.speak(word.word, TextToSpeech.QUEUE_FLUSH, null, null) },
                        onDone = { onWordDone(word) },
                        onSave = {
                            isSaved = true
                            saveWordToNotebook(word)
                        },
                        isSaved = isSaved
                    )
                }
            }
        }

        windowManager.addView(composeView, buildOverlayLayoutParams())
        overlayView = composeView
    }

    private fun saveWordToNotebook(word: WordBank) {
        serviceScope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            db.vocabularySavedDao().insert(
                VocabularySaved(
                    type = "word",
                    text = word.word,
                    pronunciation = word.pronunciation,
                    meanings = word.meanings,
                    level = word.level,
                    savedAt = System.currentTimeMillis()
                )
            )
        }
    }

    private fun buildOverlayLayoutParams(): WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        overlayWindowType(),
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
        PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.CENTER }

    // ----- Thành ngữ -----

    private fun showIdiomOverlay() {
        if (isOverlayShowing) return
        isOverlayShowing = true

        serviceScope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            val settingsRepo = SettingsRepository(applicationContext)
            val selectedLevels = settingsRepo.selectedLevels.first().toList()
            val startOfDay = startOfDayMillis()
            var idiom = db.idiomBankDao().getRandomUnseenIdiom(selectedLevels, startOfDay)
            if (idiom == null) {
                idiom = db.idiomBankDao().getRandomIdiomAnyState()
            }
            if (idiom != null) addIdiomOverlayView(idiom) else isOverlayShowing = false
        }
    }

    private fun addIdiomOverlayView(idiom: IdiomBank) {
        val owner = OverlayLifecycleOwner().also { it.onCreate() }
        overlayLifecycleOwner = owner
        var isSaved by mutableStateOf(false)

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                WordPopTheme {
                    WordCardOverlay(
                        word = idiom.idiom,
                        pronunciation = "",
                        meanings = listOf(idiom.meaningVi),
                        literalHint = idiom.literalHint,
                        example = idiom.exampleEn,
                        exampleTranslation = idiom.exampleVi,
                        onSpeak = { tts?.speak(idiom.idiom, TextToSpeech.QUEUE_FLUSH, null, null) },
                        onDone = { onIdiomDone(idiom) },
                        onSave = {
                            isSaved = true
                            saveIdiomToNotebook(idiom)
                        },
                        isSaved = isSaved
                    )
                }
            }
        }

        windowManager.addView(composeView, buildOverlayLayoutParams())
        overlayView = composeView
    }

    private fun saveIdiomToNotebook(idiom: IdiomBank) {
        serviceScope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            db.vocabularySavedDao().insert(
                VocabularySaved(
                    type = "idiom",
                    text = idiom.idiom,
                    pronunciation = "",
                    meanings = listOf(idiom.meaningVi),
                    example = "${idiom.exampleEn} — ${idiom.exampleVi}",
                    level = idiom.level,
                    savedAt = System.currentTimeMillis()
                )
            )
        }
    }

    private fun onIdiomDone(idiom: IdiomBank) {
        serviceScope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            db.historyTodayDao().insert(
                HistoryToday(
                    type = "idiom",
                    itemId = idiom.id,
                    text = idiom.idiom,
                    pronunciation = "",
                    meanings = listOf(idiom.meaningVi),
                    example = "${idiom.exampleEn} — ${idiom.exampleVi}",
                    level = idiom.level,
                    viewedAt = System.currentTimeMillis()
                )
            )
            removeOverlay()
        }
    }

    private fun onWordDone(word: WordBank) {
        serviceScope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            db.historyTodayDao().insert(
                HistoryToday(
                    type = "word",
                    itemId = word.id,
                    text = word.word,
                    pronunciation = word.pronunciation,
                    meanings = word.meanings,
                    level = word.level,
                    viewedAt = System.currentTimeMillis()
                )
            )
            removeOverlay()
        }
    }

    private fun removeOverlay() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: IllegalArgumentException) {
                // view đã bị gỡ trước đó, bỏ qua
            }
        }
        overlayLifecycleOwner?.onDestroy()
        overlayView = null
        overlayLifecycleOwner = null
        isOverlayShowing = false
    }

    private fun overlayWindowType(): Int = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

    private fun selectBestAvailableVoice() {
        val engine = tts ?: return
        val bestVoice = engine.voices
            ?.filter { it.locale == Locale.US && !it.isNetworkConnectionRequired }
            ?.maxByOrNull { it.quality }
            ?: engine.voices?.filter { it.locale == Locale.US }?.maxByOrNull { it.quality }

        if (bestVoice != null) {
            engine.voice = bestVoice
        }
    }

    private fun startOfDayMillis(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    private fun buildNotification(): Notification {
        val channelId = "wordpop_overlay_channel"
        val channel = NotificationChannel(
            channelId,
            "WordPop đang chạy nền",
            NotificationManager.IMPORTANCE_MIN
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("WordPop")
            .setContentText("Đang tự động hiện thẻ từ vựng khi mở khóa máy")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        const val ACTION_SHOW_IDIOM = "com.rang.wordpop.action.SHOW_IDIOM"

        @Volatile
        var isRunning: Boolean = false
            private set
    }
}