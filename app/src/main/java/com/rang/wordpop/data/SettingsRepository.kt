package com.rang.wordpop.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "wordpop_settings")

private object SettingsKeys {
    val SELECTED_LEVELS = stringSetPreferencesKey("selected_levels")
    val IDIOM_FEATURE_ENABLED = booleanPreferencesKey("idiom_feature_enabled")
    val HAS_COMPLETED_SETUP = booleanPreferencesKey("has_completed_setup")
    val WORD_CARD_ENABLED = booleanPreferencesKey("word_card_enabled")
}

/**
 * Lưu cấp độ CEFR người dùng chọn học (mục 7 trong spec).
 * Mặc định lần đầu mở app: chỉ B2, đúng yêu cầu spec.
 */
class SettingsRepository(private val context: Context) {

    val selectedLevels: Flow<Set<String>> = context.settingsDataStore.data.map { prefs ->
        prefs[SettingsKeys.SELECTED_LEVELS] ?: setOf("B2")
    }

    suspend fun setSelectedLevels(levels: Set<String>) {
        // Bắt buộc chọn ít nhất 1 cấp độ (mục 7 spec)
        val safeLevels = levels.ifEmpty { setOf("B2") }
        context.settingsDataStore.edit { prefs ->
            prefs[SettingsKeys.SELECTED_LEVELS] = safeLevels
        }
    }

    /**
     * Bật/tắt tổng cho cả tính năng thành ngữ. Khi tắt, các mốc giờ vẫn được
     * giữ nguyên trong danh sách (không xóa) nhưng không bắn báo thức nữa.
     */
    val idiomFeatureEnabled: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[SettingsKeys.IDIOM_FEATURE_ENABLED] ?: false
    }

    suspend fun setIdiomFeatureEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[SettingsKeys.IDIOM_FEATURE_ENABLED] = enabled
        }
    }

    /**
     * Cờ đánh dấu người dùng đã hoàn tất màn hình Setup lần đầu chưa.
     * false (mặc định) → cần hiện SetupScreen sau khi cấp đủ quyền.
     * true → vào thẳng HomeScreen.
     */
    val hasCompletedSetup: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[SettingsKeys.HAS_COMPLETED_SETUP] ?: false
    }

    suspend fun setHasCompletedSetup(value: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[SettingsKeys.HAS_COMPLETED_SETUP] = value
        }
    }

    /**
     * Bật/tắt thẻ từ vựng hiện khi mở khóa máy.
     * Mặc định TRUE (bật) khi lần đầu cài đặt.
     *
     * Khi tắt: WordPopOverlayService vẫn chạy foreground (không cần khởi động lại),
     * nhưng bỏ qua broadcast SCREEN_ON / USER_PRESENT → không hiện thẻ nữa.
     * Hữu ích khi chơi game / cần mở máy liên tục mà không muốn bị phiền.
     *
     * Lưu ý: chỉ ảnh hưởng tới WORD card. Thành ngữ (idiom) theo giờ vẫn hiện
     * bình thường, vì đó là lịch do user chủ động đặt.
     */
    val wordCardEnabled: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[SettingsKeys.WORD_CARD_ENABLED] ?: true
    }

    suspend fun setWordCardEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[SettingsKeys.WORD_CARD_ENABLED] = enabled
        }
    }
}