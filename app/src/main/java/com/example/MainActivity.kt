package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.fragment.app.FragmentActivity
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MainAppScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.TailorViewModel
import com.example.util.AdManager
import kotlinx.coroutines.delay

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Programmatically initialize Firebase in case the auto initialization was skipped or missing
        try {
            var isInitialized = false
            try {
                if (com.google.firebase.FirebaseApp.getApps(this).isNotEmpty()) {
                    isInitialized = true
                }
            } catch (e: Exception) {}
            if (!isInitialized) {
                com.google.firebase.FirebaseApp.initializeApp(this)
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Explicit FirebaseApp init failed", e)
        }
        
        // Initialize Google Mobile Ads and attempt to load/play the App Open Ad on launch
        AdManager.initialize(this) {
            runOnUiThread {
                AdManager.showAdIfAvailable(this)
            }
        }
        
        // AndroidViewModel default provider instantiation
        val viewModel = ViewModelProvider(this)[TailorViewModel::class.java]

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    var showSplash by remember { mutableStateOf(true) }

                    Crossfade(
                        targetState = showSplash,
                        animationSpec = tween(durationMillis = 600)
                    ) { isSplash ->
                        if (isSplash) {
                            SplashScreen(
                                onSplashFinished = { showSplash = false }
                            )
                        } else {
                            MainAppScreen(
                                viewModel = viewModel,
                                onSignOut = {
                                    viewModel.signOut()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 900)
    )
    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.85f,
        animationSpec = tween(durationMillis = 1000)
    )

    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(2200) // Elegant launcher display time
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer(
                alpha = alphaAnim,
                scaleX = scaleAnim,
                scaleY = scaleAnim
            )
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_tailor_app_icon),
                contentDescription = "Tailor Shop Manager Logo",
                modifier = Modifier
                    .size(180.dp)
                    .clip(RoundedCornerShape(32.dp))
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "TailorShopManager",
                color = Color(0xFFE5A93B), // Rich golden typography accent
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }
    }
}
