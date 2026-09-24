package com.rang.wordpop.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rang.wordpop.util.PermissionStatus

@Composable
fun OnboardingScreen(
    status: PermissionStatus,
    onRequestOverlay: () -> Unit,
    onRequestBattery: () -> Unit,
    onRequestNotification: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Cấp quyền cho WordPop",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "WordPop cần các quyền sau để tự hiện thẻ từ vựng ngay trên " +
                    "màn hình khóa mỗi khi bạn mở điện thoại, không cần mở app.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(24.dp))

        PermissionRow(
            title = "Hiển thị đè lên ứng dụng khác",
            description = "Bắt buộc để vẽ thẻ từ vựng lên màn hình khóa / màn hình chính.",
            granted = status.hasOverlay,
            onClick = onRequestOverlay
        )
        Spacer(Modifier.height(16.dp))
        PermissionRow(
            title = "Bỏ qua tối ưu hóa pin",
            description = "Tránh hệ thống tắt dịch vụ nền, đảm bảo thẻ luôn hiện đúng lúc.",
            granted = status.hasBatteryExemption,
            onClick = onRequestBattery
        )
        Spacer(Modifier.height(16.dp))
        PermissionRow(
            title = "Thông báo",
            description = "Cần để chạy dịch vụ nền hiển thị thẻ từ vựng (Android 13 trở lên).",
            granted = status.hasNotification,
            onClick = onRequestNotification
        )

        Spacer(Modifier.height(24.dp))
        Text(
            text = "Sau khi cấp đủ quyền, màn hình này sẽ tự đóng.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    granted: Boolean,
    onClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (granted) "✓" else "○",
                style = MaterialTheme.typography.headlineSmall,
                color = if (granted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                }
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.SemiBold)
                Text(text = description, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.width(12.dp))
            if (!granted) {
                Button(onClick = onClick) {
                    Text("Cấp quyền")
                }
            }
        }
    }
}