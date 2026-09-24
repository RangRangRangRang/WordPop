package com.rang.wordpop.util

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Singleton TTS dùng chung cho cả app.
 * Khởi tạo 1 lần duy nhất (từ MainActivity), giữ nguyên suốt vòng đời app.
 *
 * Tránh việc mỗi màn hình (History/Notebook/Overlay) tự tạo/shutdown TTS riêng,
 * vì:
 *  - Mỗi lần tạo TTS mất 200-500ms, chạy trên main thread → gây đơ khi đổi tab.
 *  - HorizontalPager dispose tab khi rời đi → TTS bị shutdown → lần sau vào lại
 *    phải init lại từ đầu.
 *
 * Dùng chung 1 instance → chỉ init 1 lần khi mở app, sau đó speak() là tức thì.
 */
object TtsManager {

    private var tts: TextToSpeech? = null
    private var ready: Boolean = false

    /**
     * Gọi 1 lần trong MainActivity.onCreate() (hoặc Application.onCreate()).
     * Idempotent — gọi nhiều lần vô hại.
     */
    fun init(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val engine = tts ?: return@TextToSpeech
                engine.language = Locale.US
                // Chọn giọng en-US chất lượng cao nhất có sẵn trên máy
                val bestVoice = engine.voices
                    ?.filter { it.locale == Locale.US && !it.isNetworkConnectionRequired }
                    ?.maxByOrNull { it.quality }
                    ?: engine.voices?.filter { it.locale == Locale.US }?.maxByOrNull { it.quality }
                if (bestVoice != null) {
                    engine.voice = bestVoice
                }
                ready = true
            }
        }
    }

    /**
     * Đọc text bằng TTS. Nếu engine chưa sẵn sàng thì bỏ qua (im lặng),
     * tránh crash. Trên máy thật, engine sẵn sàng gần như ngay sau khi init().
     */
    fun speak(text: String) {
        if (!ready || text.isBlank()) return
        tts?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "tts_${text.hashCode()}"
        )
    }

    /**
     * Dừng đọc ngay lập tức.
     */
    fun stop() {
        tts?.stop()
    }

    /**
     * Chỉ gọi khi app thực sự thoát — không cần gọi trong từng Composable.
     */
    fun shutdown() {
        tts?.shutdown()
        tts = null
        ready = false
    }
}