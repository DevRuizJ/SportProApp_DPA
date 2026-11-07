package com.dpa.sportpro.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dpa.sportpro.data.model.UserProfile
import com.dpa.sportpro.ui.community.CommunityScreen
import com.dpa.sportpro.ui.theme.DarkBackground

@Composable
fun HomeScreen(userProfile: UserProfile) {
    Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground) {
        Column(modifier = Modifier.fillMaxSize()) {
            CommunityScreen(
                userProfile = userProfile,
                modifier = Modifier.fillMaxSize(),
                showHeader = false,
            )
        }
    }
}
