package com.babysplit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.babysplit.app.core.ui.theme.BabySplitTheme
import com.babysplit.app.navigation.NavGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as BabySplitApplication

        setContent {
            BabySplitTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NavGraph(app = app)
                }
            }
        }
    }
}
