package com.dpa.sportpro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dpa.sportpro.ui.theme.ErrorRed
import com.dpa.sportpro.ui.theme.TextWhite

val ErrorBannerBackground = Color(0xFF2C1217)
val ErrorBannerBorder = Color(0xFF5C1B24)

@Composable
fun ErrorBanner(errorMessage: String, modifier: Modifier = Modifier) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(ErrorBannerBackground, RoundedCornerShape(12.dp))
                .border(1.dp, ErrorBannerBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = "Error",
            tint = ErrorRed,
            modifier = Modifier.size(22.dp).padding(end = 4.dp),
        )

        Text(
            text = errorMessage,
            color = TextWhite.copy(alpha = 0.9f),
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
