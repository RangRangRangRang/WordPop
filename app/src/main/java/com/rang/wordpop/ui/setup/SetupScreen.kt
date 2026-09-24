package com.rang.wordpop.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val ALL_LEVELS = listOf("A1", "A2", "B1", "B2", "C1", "C2")

/**
 * Màn hình setup lần đầu — chỉ hiện 1 lần, sau khi cấp đủ quyền và trước HomeScreen.
 * Khác SettingsScreen:
 *  - UI tối giản, có nút "Hoàn tất" ở cuối.
 *  - Không có phần chọn khung giờ (người dùng vào Settings sau để thêm).
 *  - State được truyền vào (stateless), chỉ commit khi bấm "Hoàn tất".
 */
@Composable
fun SetupScreen(
    selectedLevels: Set<String>,
    idiomEnabled: Boolean,
    onSelectedLevelsChange: (Set<String>) -> Unit,
    onIdiomEnabledChange: (Boolean) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Thiết lập ban đầu",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Chọn cấp độ từ vựng bạn muốn học và có muốn bật nhắc thành ngữ không. " +
                    "Bạn có thể đổi lại bất cứ lúc nào trong phần Cài đặt.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(24.dp))
        Text(
            text = "Cấp độ CEFR:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))

        ALL_LEVELS.forEach { level ->
            val checked = selectedLevels.contains(level)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { isChecked ->
                        val updated =
                            if (isChecked) selectedLevels + level
                            else selectedLevels - level
                        onSelectedLevelsChange(updated)
                    }
                )
                Text(text = level, modifier = Modifier.padding(start = 4.dp))
            }
        }

        Spacer(Modifier.height(24.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Nhắc thành ngữ theo giờ",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Bật để WordPop hiện thành ngữ vào các khung giờ bạn chọn.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = idiomEnabled,
                    onCheckedChange = onIdiomEnabledChange
                )
            }
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Hoàn tất")
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "Bạn có thể chỉnh lại mọi thứ trong Cài đặt.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
    }
}