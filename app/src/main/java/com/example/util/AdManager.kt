package com.example.util

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import java.util.Date

object AdManager {
    const val TAG = "AdManager"

    fun createAdRequest(): com.google.android.gms.ads.AdRequest {
        val extras = android.os.Bundle().apply {
            putString("disable_native_ad_validator", "true")
        }
        return com.google.android.gms.ads.AdRequest.Builder()
            .addNetworkExtrasBundle(com.google.ads.mediation.admob.AdMobAdapter::class.java, extras)
            .build()
    }
    
    // Direct Ad units provided by user from screenshots
    private const val APP_OPEN_AD_UNIT_ID = "ca-app-pub-6317800439853246/5207762089"
    private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-6317800439853246/4519999394"
    private const val NATIVE_AD_UNIT_ID = "ca-app-pub-6317800439853246/6220093872"
    const val BANNER_AD_UNIT_ID = "ca-app-pub-6317800439853246/8819466357"
    
    // Standard Google test Ad units as fallback or for debug testing
    private const val TEST_APP_OPEN_AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921"
    private const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"

    // App Open state
    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAppOpen = false
    private var appOpenLoadTime: Long = 0

    // Interstitial state
    private var interstitialAd: InterstitialAd? = null
    private var isLoadingInterstitial = false
    private var interstitialLoadTime: Long = 0

    fun initialize(context: Context, onInitComplete: () -> Unit = {}) {
        Log.d(TAG, "Initializing Google Mobile Ads SDK")
        MobileAds.initialize(context) { initializationStatus ->
            Log.d(TAG, "Google Mobile Ads init complete: $initializationStatus")
            onInitComplete()
            // Auto-load first App Open ad and Interstitial ad on startup
            loadAd(context)
            loadInterstitialAd(context)
        }
    }

    private fun wasLoadTimeLessThanNHoursAgo(loadTime: Long, numHours: Long): Boolean {
        val dateDifference: Long = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }

    // --- APP OPEN AD METHODS ---

    fun isAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(appOpenLoadTime, 4)
    }

    fun loadAd(context: Context, onAdLoaded: () -> Unit = {}, onAdFailed: (String) -> Unit = {}) {
        if (isLoadingAppOpen || isAdAvailable()) {
            onAdLoaded()
            return
        }

        isLoadingAppOpen = true
        val request = createAdRequest()
        
        Log.d(TAG, "Loading App Open Ad with ID: $APP_OPEN_AD_UNIT_ID")
        AppOpenAd.load(
            context,
            APP_OPEN_AD_UNIT_ID,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.d(TAG, "App Open Ad Loaded successfully")
                    appOpenAd = ad
                    isLoadingAppOpen = false
                    appOpenLoadTime = Date().time
                    onAdLoaded()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "App Open Ad failed to load: ${loadAdError.message} (Code: ${loadAdError.code})")
                    Log.d(TAG, "Attempting Fallback loading with Test App Open Ad Unit ID...")
                    AppOpenAd.load(
                        context,
                        TEST_APP_OPEN_AD_UNIT_ID,
                        request,
                        object : AppOpenAd.AppOpenAdLoadCallback() {
                            override fun onAdLoaded(testAd: AppOpenAd) {
                                Log.d(TAG, "Fallback Test App Open Ad Loaded successfully")
                                appOpenAd = testAd
                                isLoadingAppOpen = false
                                appOpenLoadTime = Date().time
                                onAdLoaded()
                            }

                            override fun onAdFailedToLoad(testError: LoadAdError) {
                                Log.e(TAG, "Fallback Test Ad failed too: ${testError.message}")
                                isLoadingAppOpen = false
                                onAdFailed("${loadAdError.message} / fallback: ${testError.message}")
                            }
                        }
                    )
                }
            }
        )
    }

    fun showAdIfAvailable(activity: Activity, onAdDismissed: () -> Unit = {}) {
        val prefs = activity.getSharedPreferences("AdMobPrefs", Context.MODE_PRIVATE)
        val lastShowTime = prefs.getLong("last_app_open_show_time", 0L)
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastShowTime < 30 * 60 * 1000) {
            Log.d(TAG, "App Open Ad skipped: 30 minutes have not passed since last presentation. ${(1800000 - (currentTime - lastShowTime)) / (60 * 1000)} minutes remaining.")
            onAdDismissed()
            return
        }

        if (!isAdAvailable()) {
            Log.d(TAG, "App Open Ad is not loaded yet. Prompting load...")
            loadAd(activity, 
                onAdLoaded = {
                    val ad = appOpenAd
                    if (ad != null) {
                        ad.fullScreenContentCallback = createAppOpenCallback(activity, onAdDismissed)
                        ad.show(activity)
                    } else {
                        onAdDismissed()
                    }
                },
                onAdFailed = {
                    onAdDismissed()
                }
            )
            return
        }

        val ad = appOpenAd
        if (ad != null) {
            ad.fullScreenContentCallback = createAppOpenCallback(activity, onAdDismissed)
            Log.d(TAG, "Showing App Open Ad.")
            ad.show(activity)
        } else {
            onAdDismissed()
        }
    }

    private fun createAppOpenCallback(activity: Activity, onAdDismissed: () -> Unit): FullScreenContentCallback {
        return object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "App Open Ad Dismissed.")
                appOpenAd = null
                isLoadingAppOpen = false
                loadAd(activity)
                onAdDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "App Open Ad failed to show: ${adError.message}")
                appOpenAd = null
                isLoadingAppOpen = false
                loadAd(activity)
                onAdDismissed()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "App Open Ad displayed successfully.")
                val prefs = activity.getSharedPreferences("AdMobPrefs", Context.MODE_PRIVATE)
                prefs.edit().putLong("last_app_open_show_time", System.currentTimeMillis()).apply()
            }
        }
    }

    // --- INTERSTITIAL AD METHODS ---

    fun isInterstitialAvailable(): Boolean {
        return interstitialAd != null && wasLoadTimeLessThanNHoursAgo(interstitialLoadTime, 2)
    }

    fun loadInterstitialAd(context: Context, onAdLoaded: () -> Unit = {}, onAdFailed: (String) -> Unit = {}) {
        if (isLoadingInterstitial || isInterstitialAvailable()) {
            onAdLoaded()
            return
        }

        isLoadingInterstitial = true
        val request = createAdRequest()

        Log.d(TAG, "Loading Interstitial Ad with ID: $INTERSTITIAL_AD_UNIT_ID")
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            request,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial Ad loaded successfully.")
                    interstitialAd = ad
                    isLoadingInterstitial = false
                    interstitialLoadTime = Date().time
                    onAdLoaded()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "Interstitial Ad failed to load: ${loadAdError.message}. Code: ${loadAdError.code}")
                    Log.d(TAG, "Attempting Fallback loading with Test Interstitial Ad Unit ID...")
                    InterstitialAd.load(
                        context,
                        TEST_INTERSTITIAL_AD_UNIT_ID,
                        request,
                        object : InterstitialAdLoadCallback() {
                            override fun onAdLoaded(testAd: InterstitialAd) {
                                Log.d(TAG, "Fallback Test Interstitial Ad loaded successfully.")
                                interstitialAd = testAd
                                isLoadingInterstitial = false
                                interstitialLoadTime = Date().time
                                onAdLoaded()
                            }

                            override fun onAdFailedToLoad(testError: LoadAdError) {
                                Log.e(TAG, "Fallback Test Interstitial failed too: ${testError.message}")
                                isLoadingInterstitial = false
                                onAdFailed("${loadAdError.message} / fallback: ${testError.message}")
                            }
                        }
                    )
                }
            }
        )
    }

    fun showInterstitialAd(activity: Activity, onAdDismissed: () -> Unit = {}) {
        val prefs = activity.getSharedPreferences("AdMobPrefs", Context.MODE_PRIVATE)
        val clickCount = prefs.getInt("interstitial_click_count", 0) + 1
        prefs.edit().putInt("interstitial_click_count", clickCount).apply()
        
        Log.d(TAG, "Interstitial click count: $clickCount/10")
        
        if (clickCount < 10) {
            Log.d(TAG, "Interstitial skipped: $clickCount/10 clicks registered.")
            onAdDismissed()
            return
        }
        
        // Reset counter upon reaching limit and preparing presentation
        prefs.edit().putInt("interstitial_click_count", 0).apply()

        if (!isInterstitialAvailable()) {
            Log.d(TAG, "Interstitial ad not loaded. Loading now and releasing back.")
            loadInterstitialAd(activity,
                onAdLoaded = {
                    val ad = interstitialAd
                    if (ad != null) {
                        ad.fullScreenContentCallback = createInterstitialCallback(activity, onAdDismissed)
                        ad.show(activity)
                    } else {
                        onAdDismissed()
                    }
                },
                onAdFailed = {
                    onAdDismissed()
                }
            )
            return
        }

        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = createInterstitialCallback(activity, onAdDismissed)
            Log.d(TAG, "Showing Interstitial Ad.")
            ad.show(activity)
        } else {
            onAdDismissed()
        }
    }

    private fun createInterstitialCallback(activity: Activity, onAdDismissed: () -> Unit): FullScreenContentCallback {
        return object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial Ad dismissed.")
                interstitialAd = null
                isLoadingInterstitial = false
                // Pre-load next interstitial
                loadInterstitialAd(activity)
                onAdDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Interstitial Ad failed to show: ${adError.message}")
                interstitialAd = null
                isLoadingInterstitial = false
                loadInterstitialAd(activity)
                onAdDismissed()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Interstitial Ad shown successfully.")
            }
        }
    }

    // --- NATIVE AD LOADER HELPER ---

    fun loadNativeAd(context: Context, onAdLoaded: (NativeAd) -> Unit, onAdFailed: (String) -> Unit) {
        val request = createAdRequest()
        Log.d(TAG, "Loading Native Ad with ID: $NATIVE_AD_UNIT_ID")

        val adLoader = AdLoader.Builder(context.applicationContext, NATIVE_AD_UNIT_ID)
            .forNativeAd { ad: NativeAd ->
                Log.d(TAG, "Native Ad loaded successfully.")
                onAdLoaded(ad)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "Native Ad failed to load: ${loadAdError.message}. Code: ${loadAdError.code}")
                    Log.d(TAG, "Attempting Fallback loading with Test Native Ad Unit ID...")
                    val fallbackAdLoader = AdLoader.Builder(context.applicationContext, TEST_NATIVE_AD_UNIT_ID)
                        .forNativeAd { testAd: NativeAd ->
                            Log.d(TAG, "Fallback Test Native Ad loaded successfully.")
                            onAdLoaded(testAd)
                        }
                        .withAdListener(object : AdListener() {
                            override fun onAdFailedToLoad(testError: LoadAdError) {
                                Log.e(TAG, "Fallback Test Native Ad failed too: ${testError.message}")
                                onAdFailed("${loadAdError.message} / fallback: ${testError.message}")
                            }
                        })
                        .withNativeAdOptions(NativeAdOptions.Builder().build())
                        .build()

                    fallbackAdLoader.loadAd(request)
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()

        adLoader.loadAd(request)
    }
}

@Composable
fun AdMobBanner(modifier: Modifier = Modifier, adSize: AdSize = AdSize.BANNER) {
    var adUnitState by remember { mutableStateOf(AdManager.BANNER_AD_UNIT_ID) }

    key(adUnitState) {
        AndroidView(
            modifier = modifier.fillMaxWidth(),
            factory = { context ->
                AdView(context).apply {
                    setAdSize(adSize)
                    adUnitId = adUnitState
                    adListener = object : AdListener() {
                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            Log.e(AdManager.TAG, "Banner ad failed to load: ${loadAdError.message} (Code: ${loadAdError.code})")
                            if (adUnitState == AdManager.BANNER_AD_UNIT_ID) {
                                Log.d(AdManager.TAG, "Attempting Fallback loading with Test Banner Ad Unit ID...")
                                adUnitState = AdManager.TEST_BANNER_AD_UNIT_ID
                            }
                        }
                    }
                    loadAd(AdManager.createAdRequest())
                }
            }
        )
    }
}

@Composable
fun AdMobNativeAd(
    modifier: Modifier = Modifier
) {
    var loadedNativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant.toArgb()
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val outlineColor = MaterialTheme.colorScheme.outline.toArgb()
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary.toArgb()

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(vertical = 8.dp),
        factory = { context ->
            // Construct a NativeAdView container programmatically
            val nativeAdView = NativeAdView(context)
            nativeAdView.apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                // Root vertical container matching M3 Card layout
                val cardLayout = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setPadding(32, 24, 32, 24)
                    
                    // Background & borders
                    val cardDrawable = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = 32f
                        setColor(surfaceColor)
                        setStroke(2, outlineColor)
                    }
                    background = cardDrawable
                }

                // Row for Icon and Header Text
                val headerRow = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                // Small Native ad icon placeholder
                val iconViewLocal = ImageView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(100, 100).apply {
                        setMargins(0, 0, 24, 0)
                    }
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    visibility = View.GONE
                }
                headerRow.addView(iconViewLocal)
                nativeAdView.iconView = iconViewLocal

                // Vertical stack for Headline & Advertiser/Sponsored marker
                val textStack = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                }

                val headlineViewLocal = TextView(context).apply {
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(onSurfaceColor)
                    textSize = 16f
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }
                textStack.addView(headlineViewLocal)
                nativeAdView.headlineView = headlineViewLocal

                val advertiserViewLocal = TextView(context).apply {
                    setTextColor(onSurfaceVariantColor)
                    textSize = 12f
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }
                textStack.addView(advertiserViewLocal)
                nativeAdView.advertiserView = advertiserViewLocal

                headerRow.addView(textStack)

                // Sponsored Badge
                val badgeView = TextView(context).apply {
                    text = "Ad"
                    setTextColor(onPrimaryColor)
                    textSize = 10f
                    setPadding(12, 4, 12, 4)
                    val badgeDrawable = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = 8f
                        setColor(primaryColor)
                    }
                    background = badgeDrawable
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(16, 0, 0, 0)
                    }
                }
                headerRow.addView(badgeView)

                cardLayout.addView(headerRow)

                // Ad Content Body
                val bodyViewLocal = TextView(context).apply {
                    setTextColor(onSurfaceVariantColor)
                    textSize = 14f
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 16, 0, 16)
                    }
                }
                cardLayout.addView(bodyViewLocal)
                nativeAdView.bodyView = bodyViewLocal

                // Call To Action Button
                val callToActionViewLocal = Button(context).apply {
                    textSize = 14f
                    setTextColor(onPrimaryColor)
                    isAllCaps = false
                    val btnDrawable = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = 32f
                        setColor(primaryColor)
                    }
                    background = btnDrawable
                    stateListAnimator = null
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }
                cardLayout.addView(callToActionViewLocal)
                nativeAdView.callToActionView = callToActionViewLocal

                addView(cardLayout)
            }

            // Start fetching actual native ad
            Log.d("AdMobNativeAd", "Initializing Native ad loading block.")
            AdManager.loadNativeAd(
                context,
                onAdLoaded = { ad ->
                    loadedNativeAd = ad
                    isLoading = false
                    
                    // Bind loaded native ad fields to programmatic views
                    (nativeAdView.headlineView as? TextView)?.text = ad.headline
                    (nativeAdView.bodyView as? TextView)?.text = ad.body
                    (nativeAdView.advertiserView as? TextView)?.text = ad.advertiser ?: "Sponsored"

                    if (ad.icon != null) {
                        nativeAdView.iconView?.visibility = View.VISIBLE
                        (nativeAdView.iconView as? ImageView)?.setImageDrawable(ad.icon?.drawable)
                    } else {
                        nativeAdView.iconView?.visibility = View.GONE
                    }

                    if (ad.callToAction != null) {
                        nativeAdView.callToActionView?.visibility = View.VISIBLE
                        (nativeAdView.callToActionView as? Button)?.text = ad.callToAction
                    } else {
                        nativeAdView.callToActionView?.visibility = View.GONE
                    }

                    nativeAdView.setNativeAd(ad)
                },
                onAdFailed = { err ->
                    loadError = err
                    isLoading = false
                }
            )

            nativeAdView
        },
        update = { nativeAdView ->
            // Re-apply current dynamically adapted theme colors on recomposition or theme toggle
            val cardLayout = nativeAdView.getChildAt(0) as? LinearLayout
            if (cardLayout != null) {
                val cardDrawable = cardLayout.background as? android.graphics.drawable.GradientDrawable
                cardDrawable?.setColor(surfaceColor)
                cardDrawable?.setStroke(2, outlineColor)
            }

            (nativeAdView.headlineView as? TextView)?.setTextColor(onSurfaceColor)
            (nativeAdView.bodyView as? TextView)?.setTextColor(onSurfaceVariantColor)
            (nativeAdView.advertiserView as? TextView)?.setTextColor(onSurfaceVariantColor)

            val ctaButton = nativeAdView.callToActionView as? Button
            if (ctaButton != null) {
                ctaButton.setTextColor(onPrimaryColor)
                val btnDrawable = ctaButton.background as? android.graphics.drawable.GradientDrawable
                btnDrawable?.setColor(primaryColor)
            }
        }
    )
}
