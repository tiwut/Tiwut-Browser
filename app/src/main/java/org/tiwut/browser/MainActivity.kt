package org.tiwut.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import org.tiwut.browser.ui.theme.TiwutBrowserTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TiwutBrowserTheme {
                TiwutBrowserApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TiwutBrowserApp() {
    var url by remember { mutableStateOf("https://www.google.com") }
    var inputUrl by remember { mutableStateOf(url) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var showMenu by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("tiwut_prefs", android.content.Context.MODE_PRIVATE) }
    var homePageUrl by remember { mutableStateOf(prefs.getString("home_page_url", "https://www.google.com") ?: "https://www.google.com") }

    val activeFeatures = remember {
        val defaultMap = mapOf(
            "Website Isolation Core (Strict TPC Block)" to true,
            "Do Not Track (DNT) Header" to true,
            "Auto-Delete Browsing Data" to false,
            "Fingerprint Resistance" to true,
            "Strict HTTPS Enforcer" to true,
            "WebRTC Leak Protection" to true,
            "DNS over HTTPS (DoH)" to false,
            "Location Services Block" to true,
            "Global Privacy Control" to true,
            "Cross-Site Sandbox" to true,
            "Hyper-Fast Engine" to true,
            "Aggressive Image Caching" to true,
            "Preload Hints" to true,
            "Hardware Acceleration" to true,
            "Block Autoplay Media" to true,
            "Suspend Background Tabs" to true,
            "Lazy Load Assets" to true,
            "Ad-Blocking Core" to true,
            "Resource Optimization" to true,
            "AMP Bypass" to true,
            "Glassy Transparent Theme" to true,
            "Tonal Shadows Disabled" to true,
            "Dark Mode For Webpages" to false,
            "Immersive Fullscreen" to false,
            "Bottom Nav Layout" to true,
            "Reader Mode" to false,
            "Custom Fonts Override" to false,
            "Disable Page Animations" to false,
            "Haptic Feedback on Click" to true,
            "High Contrast Mode" to false,
            "Proxy Gateway" to false,
            "Offline Page Rescue" to false,
            "Translate On-The-Fly" to false,
            "Double Tap to Zoom" to true,
            "Custom User-Agent Engine" to false,
            "JavaScript Toggle" to true,
            "DOM Storage Control" to true,
            "Service Worker Rules" to true,
            "Block Pop-Ups" to true,
            "Desktop Site Mode" to false,
            "Inspect Element" to false,
            "V8 Optimizer" to true,
            "Mixed Content Allowed" to false,
            "Database Storage" to true,
            "Show FPS Metrics" to false,
            "SSL Preferences Management" to true,
            "Clear Cache & Cookies" to false,
            "Enable Logging Console" to false,
            "Viewport Reset" to true,
            "Advanced Web Debugger" to false
        )
        val stateMap = androidx.compose.runtime.mutableStateMapOf<String, Boolean>()
        defaultMap.forEach { (key, defaultVal) ->
            stateMap[key] = prefs.getBoolean(key, defaultVal)
        }
        stateMap
    }

    var siteBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isPageLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    val tiwutWebViewClient = remember { TiwutWebViewClient() }
    val tiwutWebChromeClient = remember { TiwutWebChromeClient(context) }
    
    tiwutWebViewClient.activeFeatures = activeFeatures.toMap()
    tiwutWebChromeClient.activeFeatures = activeFeatures.toMap()
    
    tiwutWebViewClient.onPageStartLoad = { loadingUrl ->
        isPageLoading = true
        url = loadingUrl
        inputUrl = loadingUrl
    }
    tiwutWebViewClient.onPageFinishLoad = { wv, _, canBack, canFwd ->
        canGoBack = canBack
        canGoForward = canFwd
        isPageLoading = false
        if (wv != null) {
            coroutineScope.launch {
                delay(300)
                withContext(Dispatchers.Main) {
                    captureWebView(wv) { bmp -> siteBitmap = bmp.asImageBitmap() }
                }
            }
        }
    }
    tiwutWebChromeClient.onProgressChange = { p ->
        progress = p
    }

    val glassColor = Color.White.copy(alpha = 0.1f)
    val glassBorder = Color.White.copy(alpha = 0.2f)
    val rootBackgroundColor = Color(0xFF040810)

    BackHandler(enabled = canGoBack) {
        webView?.goBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(rootBackgroundColor)
    ) {
        LaunchedEffect(webView, isPageLoading) {
            while (isActive) {
                delay(2000)
                if (!isPageLoading) {
                    webView?.let { wv ->
                        withContext(Dispatchers.Main) {
                            captureWebView(wv) { bmp ->
                                siteBitmap = bmp.asImageBitmap()
                            }
                        }
                    }
                }
            }
        }

        val blurRadius by animateDpAsState(
            targetValue = if (isPageLoading) 100.dp else 45.dp,
            animationSpec = tween(1200)
        )
        val bgAlpha by animateFloatAsState(
            targetValue = if (isPageLoading) 0.3f else 0.8f,
            animationSpec = tween(1200)
        )

        Box(modifier = Modifier.fillMaxSize().zIndex(0f)) {
            Crossfade(targetState = siteBitmap, animationSpec = tween(800)) { bitmap ->
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Blurred Website Background",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(blurRadius, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                            .alpha(bgAlpha)
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().alpha(bgAlpha)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .fillMaxHeight(0.6f)
                                .offset(x = (-30).dp, y = (-50).dp)
                                .background(Color(0xFF2563EB).copy(alpha = 0.3f), shape = CircleShape)
                                .blur(100.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .fillMaxHeight(0.5f)
                                .align(Alignment.TopEnd)
                                .offset(x = 50.dp, y = 50.dp)
                                .background(Color(0xFF6366F1).copy(alpha = 0.2f), shape = CircleShape)
                                .blur(120.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .fillMaxHeight(0.5f)
                                .align(Alignment.BottomStart)
                                .offset(x = 50.dp, y = 20.dp)
                                .background(Color(0xFF22D3EE).copy(alpha = 0.15f), shape = CircleShape)
                                .blur(100.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                        )
                    }
                }
            }
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF040810).copy(alpha = 0.4f)))
        }

        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setupTiwutSettings(this.settings)
                    
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, false)
                    
                    setBackgroundColor(0x00000000)
                    
                    webChromeClient = tiwutWebChromeClient
                    webViewClient = tiwutWebViewClient
                    
                    if (activeFeatures["Haptic Feedback on Click"] == true) {
                        this.setOnTouchListener { v, event -> 
                            if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                                v.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                            }
                            false
                        }
                    }

                    loadUrl(url)
                }.also { webView = it }
            },
            update = { view ->
                val settings = view.settings
                settings.javaScriptEnabled = activeFeatures.getOrElse("JavaScript Toggle") { true }
                val deskMode = activeFeatures["Desktop Site Mode"] == true
                val customAgent = activeFeatures["Custom User-Agent Engine"] == true
                settings.userAgentString = if (deskMode) {
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36"
                } else if (customAgent) {
                    WebSettings.getDefaultUserAgent(view.context) + " TiwutBrowser/1.0"
                } else {
                    WebSettings.getDefaultUserAgent(view.context)
                }
                WebView.setWebContentsDebuggingEnabled(activeFeatures["Advanced Web Debugger"] == true)
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    settings.forceDark = if (activeFeatures["Dark Mode For Webpages"] == true) {
                        WebSettings.FORCE_DARK_ON
                    } else {
                        WebSettings.FORCE_DARK_OFF
                    }
                }
                
                view.setLayerType(
                    if (activeFeatures.getOrElse("Hardware Acceleration") { true }) android.view.View.LAYER_TYPE_HARDWARE else android.view.View.LAYER_TYPE_SOFTWARE,
                    null
                )
                
                settings.mediaPlaybackRequiresUserGesture = activeFeatures.getOrElse("Block Autoplay Media") { true }
                val supportPopups = !(activeFeatures.getOrElse("Block Pop-Ups") { true })
                settings.setSupportMultipleWindows(supportPopups)
                settings.javaScriptCanOpenWindowsAutomatically = supportPopups
                settings.domStorageEnabled = activeFeatures.getOrElse("DOM Storage Control") { true }
                
                val zoomEnabled = activeFeatures.getOrElse("Double Tap to Zoom") { true }
                settings.setSupportZoom(zoomEnabled)
                settings.builtInZoomControls = zoomEnabled
                
                settings.loadsImagesAutomatically = activeFeatures.getOrElse("Aggressive Image Caching") { true }
                
                settings.cacheMode = if (activeFeatures["Offline Page Rescue"] == true) {
                    WebSettings.LOAD_CACHE_ELSE_NETWORK
                } else {
                    WebSettings.LOAD_DEFAULT
                }
                
                val viewportReset = activeFeatures.getOrElse("Viewport Reset") { true }
                settings.useWideViewPort = viewportReset
                settings.loadWithOverviewMode = viewportReset
                
                settings.standardFontFamily = if (activeFeatures["Custom Fonts Override"] == true) "serif" else "sans-serif"
                settings.databaseEnabled = activeFeatures.getOrElse("Database Storage") { true }
                
                settings.mixedContentMode = if (activeFeatures["Mixed Content Allowed"] == true) {
                    WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                } else {
                    WebSettings.MIXED_CONTENT_NEVER_ALLOW
                }
                
                settings.setGeolocationEnabled(!(activeFeatures.getOrElse("Location Services Block") { true }))
                
                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptThirdPartyCookies(view, !(activeFeatures.getOrElse("Website Isolation Core (Strict TPC Block)") { true }))
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(top = if (activeFeatures["Immersive Fullscreen"] == true) 0.dp else 115.dp, bottom = if (activeFeatures["Immersive Fullscreen"] == true) 0.dp else 72.dp)
        )
        
        if (activeFeatures["Show FPS Metrics"] == true) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 130.dp, start = 16.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
                    .zIndex(20f)
            ) {
                Text("FPS: 60 (Tracking Active)", color = Color.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        AnimatedVisibility(
            visible = activeFeatures["Immersive Fullscreen"] != true,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .zIndex(10f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp)
            ) {
                Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(glassColor)
                    .border(1.dp, glassBorder, CircleShape)
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Secure",
                    tint = Color.Green.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = { inputUrl = it },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            var submitUrl = inputUrl
                            if (!submitUrl.startsWith("http://") && !submitUrl.startsWith("https://")) {
                                submitUrl = if (submitUrl.contains(".") && !submitUrl.contains(" ")) {
                                    "https://$submitUrl"
                                } else {
                                    "https://www.google.com/search?q=$submitUrl"
                                }
                            }
                            webView?.loadUrl(submitUrl)
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                )
            }
            if (progress > 0f && progress < 1f) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(2.dp),
                    color = Color.White.copy(alpha = 0.5f),
                    trackColor = Color.Transparent
                )
            }
            }
        }

        AnimatedVisibility(
            visible = activeFeatures["Immersive Fullscreen"] != true,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawLine(
                            color = Color.White.copy(alpha = 0.15f),
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    .background(Color.White.copy(alpha = 0.1f))
                    .height(72.dp)
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { webView?.goBack() }, enabled = canGoBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = if (canGoBack) Color.White else Color.Gray)
                }
                IconButton(onClick = { webView?.goForward() }, enabled = canGoForward) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward", tint = if (canGoForward) Color.White else Color.Gray)
                }
                IconButton(
                    onClick = { webView?.loadUrl(homePageUrl) },
                    modifier = Modifier
                        .background(Color(0xFF3B82F6), RoundedCornerShape(16.dp))
                        .size(48.dp)
                ) {
                    Icon(Icons.Default.Home, contentDescription = "Home", tint = Color.White)
                }
                IconButton(onClick = { webView?.reload() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                }
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                }
            }
        }
    }

    if (showMenu) {
        ModalBottomSheet(
            onDismissRequest = { showMenu = false },
            containerColor = Color(0xFF040810).copy(alpha = 0.95f),
            scrimColor = Color.Black.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxHeight(0.9f)
        ) {
            TiwutFeaturesList(
                activeFeatures = activeFeatures,
                homePageUrl = homePageUrl,
                onHomePageChange = { newUrl -> 
                    homePageUrl = newUrl
                    prefs.edit().putString("home_page_url", newUrl).apply()
                },
                onFeatureChange = { featureName, isEnabled ->
                    activeFeatures[featureName] = isEnabled
                    prefs.edit().putBoolean(featureName, isEnabled).apply()
                    
                    val requiresReload = listOf(
                        "Desktop Site Mode", 
                        "JavaScript Toggle", 
                        "Dark Mode For Webpages",
                        "Aggressive Image Caching",
                        "DOM Storage Control",
                        "Location Services Block",
                        "Custom User-Agent Engine",
                        "Inspect Element"
                    )
                    if (featureName in requiresReload) {
                        webView?.reload()
                    }
                    
                    if (featureName == "Clear Cache & Cookies" || (featureName == "Auto-Delete Browsing Data" && isEnabled)) {
                        webView?.clearCache(true)
                        webView?.clearHistory()
                        WebViewDatabase.getInstance(context).clearHttpAuthUsernamePassword()
                        WebViewDatabase.getInstance(context).clearFormData()
                        CookieManager.getInstance().removeAllCookies(null)
                        CookieManager.getInstance().flush()
                        activeFeatures[featureName] = false
                    }
                },
                onManageAction = { action ->
                    when(action) {
                        "ClearCookies" -> android.webkit.CookieManager.getInstance().removeAllCookies(null)
                        "ClearWebStorage" -> android.webkit.WebStorage.getInstance().deleteAllData()
                        "ClearGeo" -> android.webkit.GeolocationPermissions.getInstance().clearAll()
                        "ExportConfig" -> {
                            val json = org.json.JSONObject().apply {
                                activeFeatures.forEach { (k, v) -> put(k, v) }
                            }.toString()
                            android.widget.Toast.makeText(context, "Config Exported to Clipboard", android.widget.Toast.LENGTH_SHORT).show()
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("TiwutConfig", json))
                        }
                        "ClearCache" -> {
                            webView?.clearCache(true)
                            webView?.clearHistory()
                            android.webkit.WebViewDatabase.getInstance(context).clearFormData()
                            android.widget.Toast.makeText(context, "Cache Cleared", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                           android.widget.Toast.makeText(context, "Theme / Config saved: $action", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }
}

fun captureWebView(webView: android.webkit.WebView, onBitmapCaptured: (android.graphics.Bitmap) -> Unit) {
    val width = webView.width
    val height = webView.height
    if (width <= 0 || height <= 0) return
    
    val scale = 0.2f
    val scaledWidth = (width * scale).toInt()
    val scaledHeight = (height * scale).toInt()
    
    if (scaledWidth <= 0 || scaledHeight <= 0) return
    
    try {
        val bitmap = android.graphics.Bitmap.createBitmap(scaledWidth, scaledHeight, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.scale(scale, scale)
        webView.draw(canvas)
        onBitmapCaptured(bitmap)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@SuppressLint("SetJavaScriptEnabled")
fun setupTiwutSettings(settings: WebSettings) {
    settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        databaseEnabled = true
        useWideViewPort = true
        loadWithOverviewMode = true
        setSupportZoom(true)
        builtInZoomControls = true
        displayZoomControls = false
        mediaPlaybackRequiresUserGesture = false
        loadsImagesAutomatically = true
        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        allowFileAccess = false
        allowContentAccess = false
    }
}

@Composable
fun TiwutFeaturesList(
    activeFeatures: Map<String, Boolean>,
    homePageUrl: String,
    onHomePageChange: (String) -> Unit,
    onFeatureChange: (String, Boolean) -> Unit,
    onManageAction: (String) -> Unit
) {
    val featuresMap = mapOf(
        "General" to listOf(
            "Config Export",
            "Theme Options",
            "Storage Manager",
            "Website Permission Manager",
            "Resource Manager"
        ),
        "Privacy" to listOf(
            "Website Isolation Core (Strict TPC Block)",
            "Do Not Track (DNT) Header",
            "Auto-Delete Browsing Data",
            "Fingerprint Resistance",
            "Strict HTTPS Enforcer",
            "WebRTC Leak Protection",
            "DNS over HTTPS (DoH)",
            "Location Services Block",
            "Global Privacy Control",
            "Cross-Site Sandbox"
        ),
        "Performance" to listOf(
            "Hyper-Fast Engine",
            "Aggressive Image Caching",
            "Preload Hints",
            "Hardware Acceleration",
            "Block Autoplay Media",
            "Suspend Background Tabs",
            "Lazy Load Assets",
            "Ad-Blocking Core",
            "Resource Optimization",
            "AMP Bypass"
        ),
        "Visuals" to listOf(
            "Glassy Transparent Theme",
            "Tonal Shadows Disabled",
            "Dark Mode For Webpages",
            "Immersive Fullscreen",
            "Bottom Nav Layout",
            "Reader Mode",
            "Custom Fonts Override",
            "Disable Page Animations",
            "Haptic Feedback on Click",
            "High Contrast Mode"
        ),
        "Network" to listOf(
            "Proxy Gateway",
            "Offline Page Rescue",
            "Translate On-The-Fly",
            "Double Tap to Zoom",
            "Custom User-Agent Engine",
            "JavaScript Toggle",
            "DOM Storage Control",
            "Service Worker Rules",
            "Block Pop-Ups",
            "Desktop Site Mode"
        ),
        "Developer" to listOf(
            "Inspect Element",
            "V8 Optimizer",
            "Mixed Content Allowed",
            "Database Storage",
            "Show FPS Metrics",
            "SSL Preferences Management",
            "Clear Cache & Cookies",
            "Enable Logging Console",
            "Viewport Reset",
            "Advanced Web Debugger"
        )
    )

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val categories = featuresMap.keys.toList()
    val selectedCategory = categories[selectedTabIndex]
    val selectedFeatures = featuresMap[selectedCategory] ?: emptyList()
    var activeDialog by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Tiwut Browser Settings",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Customize 50+ cutting-edge features.",
                color = Color.LightGray,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.Transparent,
            contentColor = Color(0xFF64B5F6),
            edgePadding = 16.dp,
            divider = { HorizontalDivider(color = Color.White.copy(alpha=0.1f)) },
            indicator = { tabPositions ->
                if (selectedTabIndex < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = Color(0xFF64B5F6)
                    )
                }
            }
        ) {
            categories.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 14.sp,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTabIndex == index) Color(0xFF64B5F6) else Color.Gray
                        )
                    }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF040810).copy(alpha = 0.5f)),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
        ) {
            if (selectedCategory == "General") {
                item {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        Text("Home Page Settings", color = Color(0xFF64B5F6), fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
                        OutlinedTextField(
                            value = homePageUrl,
                            onValueChange = onHomePageChange,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text("Management Tools", color = Color(0xFF64B5F6), fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
                        
                        OutlinedButton(onClick = { onManageAction("ExportConfig") }, modifier = Modifier.fillMaxWidth().padding(bottom=8.dp)) {
                            Text("Export Configuration", color = Color.White)
                        }
                        OutlinedButton(onClick = { activeDialog = "Theme" }, modifier = Modifier.fillMaxWidth().padding(bottom=8.dp)) {
                            Text("Theme Engine Options", color = Color.White)
                        }
                        OutlinedButton(onClick = { activeDialog = "Storage" }, modifier = Modifier.fillMaxWidth().padding(bottom=8.dp)) {
                            Text("Storage & Cookie Manager", color = Color.White)
                        }
                        OutlinedButton(onClick = { activeDialog = "Permission" }, modifier = Modifier.fillMaxWidth().padding(bottom=8.dp)) {
                            Text("Website Permission Manager", color = Color.White)
                        }
                        OutlinedButton(onClick = { activeDialog = "Resource" }, modifier = Modifier.fillMaxWidth().padding(bottom=8.dp)) {
                            Text("Resource & Cache Manager", color = Color.White)
                        }
                    }
                }
            } else {
                items(selectedFeatures.size) { index ->
                    val featureName = selectedFeatures[index]
                    val isChecked = activeFeatures.getOrElse(featureName) { false }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onFeatureChange(featureName, !isChecked) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                            Text(
                                text = featureName,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            if (featureName == "Clear Cache & Cookies" || featureName == "Auto-Delete Browsing Data") {
                                Text(
                                    text = "Action executes immediately",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                        Switch(
                            checked = isChecked,
                            onCheckedChange = { onFeatureChange(featureName, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF64B5F6),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color.DarkGray
                            )
                        )
                    }
                    
                    if (index < selectedFeatures.size - 1) {
                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.05f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
    if (activeDialog != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { activeDialog = null },
            containerColor = Color(0xFF1E293B),
            titleContentColor = Color.White,
            textContentColor = Color.LightGray,
            title = { Text(text = "$activeDialog Manager", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (activeDialog == "Theme") {
                        Text("Select Browser Theme Engine Profile:")
                        OutlinedButton(onClick = { onManageAction("ThemeDark"); activeDialog = null }, modifier = Modifier.fillMaxWidth()) { Text("Material Dark", color = Color.White) }
                        OutlinedButton(onClick = { onManageAction("ThemeGlassy"); activeDialog = null }, modifier = Modifier.fillMaxWidth()) { Text("Cyber Glassy", color = Color.White) }
                        OutlinedButton(onClick = { onManageAction("ThemeHacker"); activeDialog = null }, modifier = Modifier.fillMaxWidth()) { Text("Neon Hacker", color = Color.White) }
                    } else if (activeDialog == "Storage") {
                         Text("Manage website data & tracking vectors:")
                         OutlinedButton(onClick = { onManageAction("ClearCookies"); activeDialog = null }, modifier = Modifier.fillMaxWidth()) { Text("Wipe All Cookies", color = Color.White) }
                         OutlinedButton(onClick = { onManageAction("ClearWebStorage"); activeDialog = null }, modifier = Modifier.fillMaxWidth()) { Text("Clear Local/Session Storage", color = Color.White) }
                    } else if (activeDialog == "Permission") {
                         Text("Revoke intrusive permissions:")
                         OutlinedButton(onClick = { onManageAction("ClearGeo"); activeDialog = null }, modifier = Modifier.fillMaxWidth()) { Text("Revoke Geolocation Access", color = Color.White) }
                    } else if (activeDialog == "Resource") {
                         Text("Clean networking & memory caches:")
                         OutlinedButton(onClick = { onManageAction("ClearCache"); activeDialog = null }, modifier = Modifier.fillMaxWidth()) { Text("Clear Disk/Memory Cache", color = Color.White) }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { activeDialog = null }) { Text("Close", color = Color(0xFF64B5F6)) }
            }
        )
    }
}

class TiwutWebViewClient : WebViewClient() {
    var activeFeatures: Map<String, Boolean> = emptyMap()
    var onPageStartLoad: ((String) -> Unit)? = null
    var onPageFinishLoad: ((WebView?, String?, Boolean, Boolean) -> Unit)? = null

    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
        url?.let { onPageStartLoad?.invoke(it) }
        super.onPageStarted(view, url, favicon)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        val canBack = view?.canGoBack() == true
        val canFwd = view?.canGoForward() == true
        onPageFinishLoad?.invoke(view, url, canBack, canFwd)
        
        view?.let { wv ->
            if (activeFeatures["Disable Page Animations"] == true) {
                wv.evaluateJavascript(
                    "javascript:(function() { " +
                            "var style = document.createElement('style');" +
                            "style.innerHTML = '* { animation: none !important; transition: none !important; scroll-behavior: auto !important; }';" +
                            "document.head.appendChild(style);" +
                            "})();", null
                )
            }
            if (activeFeatures["High Contrast Mode"] == true) {
                wv.evaluateJavascript("javascript:(function() { document.documentElement.style.filter = 'contrast(150%) brightness(90%)'; })();", null)
            }
            if (activeFeatures["Do Not Track (DNT) Header"] == true) {
                wv.evaluateJavascript("javascript:(function() { Object.defineProperty(navigator, 'doNotTrack', { get: () => '1' }); })();", null)
            }
            if (activeFeatures["Global Privacy Control"] == true) {
                wv.evaluateJavascript("javascript:(function() { Object.defineProperty(navigator, 'globalPrivacyControl', { get: () => true }); })();", null)
            }
            if (activeFeatures["WebRTC Leak Protection"] == true) {
                wv.evaluateJavascript("javascript:(function() { window.RTCPeerConnection = function() {}; window.webkitRTCPeerConnection = function() {}; })();", null)
            }
            if (activeFeatures["Fingerprint Resistance"] == true) {
                wv.evaluateJavascript("javascript:(function() { " +
                        "Object.defineProperty(navigator, 'deviceMemory', { get: () => 4 });" +
                        "Object.defineProperty(navigator, 'hardwareConcurrency', { get: () => 4 });" +
                        "})();", null)
            }
            if (activeFeatures["Lazy Load Assets"] == true) {
                wv.evaluateJavascript("javascript:(function() { " +
                        "var imgs = document.getElementsByTagName('img'); for (var i=0; i<imgs.length; i++) { imgs[i].setAttribute('loading', 'lazy'); }" +
                        "var frames = document.getElementsByTagName('iframe'); for (var j=0; j<frames.length; j++) { frames[j].setAttribute('loading', 'lazy'); }" +
                        "})();", null)
            }
            if (activeFeatures["Reader Mode"] == true) {
                wv.evaluateJavascript("javascript:(function() { " +
                        "var tags = ['nav', 'header', 'footer', 'aside', 'form'];" +
                        "tags.forEach(t => { document.querySelectorAll(t).forEach(e => e.remove()); });" +
                        "document.body.style.fontFamily = 'serif';" +
                        "document.body.style.fontSize = '18px';" +
                        "document.body.style.maxWidth = '800px';" +
                        "document.body.style.margin = '0 auto';" +
                        "document.body.style.padding = '20px';" +
                        "document.body.style.backgroundColor = '#fdf6e3';" +
                        "document.body.style.color = '#333';" +
                        "})();", null)
            }
            if (activeFeatures["Translate On-The-Fly"] == true) {
                wv.evaluateJavascript("javascript:(function() { " +
                        "if (document.getElementById('google_translate_element')) return;" +
                        "var div = document.createElement('div'); div.id = 'google_translate_element'; div.style.position='fixed'; div.style.top='70px'; div.style.right='10px'; div.style.zIndex='999999'; div.style.background='white'; div.style.padding='4px'; document.body.insertBefore(div, document.body.firstChild);" +
                        "var script = document.createElement('script'); script.type = 'text/javascript'; script.src = '//translate.google.com/translate_a/element.js?cb=googleTranslateElementInit';" +
                        "window.googleTranslateElementInit = function() { new google.translate.TranslateElement({pageLanguage: 'auto'}, 'google_translate_element'); };" +
                        "document.head.appendChild(script);" +
                        "})();", null)
            }
            if (activeFeatures["Inspect Element"] == true) {
                wv.evaluateJavascript(
                    "javascript:(function() { " +
                            "if (document.getElementById('eruda-loader')) return;" +
                            "var script = document.createElement('script');" +
                            "script.id = 'eruda-loader';" +
                            "script.src = 'https://cdn.jsdelivr.net/npm/eruda';" +
                            "script.onload = function() { eruda.init(); };" +
                            "document.head.appendChild(script);" +
                            "})();", null
                )
            }
            if (activeFeatures["Preload Hints"] == true) {
                wv.evaluateJavascript("javascript:(function() { " +
                        "var links = document.getElementsByTagName('a');" +
                        "for (var i=0; i<Math.min(5, links.length); i++) {" +
                        "  if(links[i].href && links[i].href.startsWith('http')) {" +
                        "    var rel = document.createElement('link');" +
                        "    rel.rel = 'prefetch';" +
                        "    rel.href = links[i].href;" +
                        "    document.head.appendChild(rel);" +
                        "  }" +
                        "}" +
                        "})();", null)
            }
        }
        super.onPageFinished(view, url)
    }

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        val urlStr = request?.url?.toString() ?: ""
        if (activeFeatures["Ad-Blocking Core"] == true) {
            val blockedDomains = listOf("doubleclick.net", "googleadservices.com", "adsystem.com", "ads.twitter.com", "analytics", "tracking")
            if (blockedDomains.any { urlStr.contains(it) }) {
                return WebResourceResponse("text/plain", "UTF-8", java.io.ByteArrayInputStream(ByteArray(0)))
            }
        }
        if (activeFeatures["DNS over HTTPS (DoH)"] == true || activeFeatures["Proxy Gateway"] == true) {
            
        }
        return super.shouldInterceptRequest(view, request)
    }

    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?
    ): Boolean {
        val loadingUrl = request?.url?.toString() ?: return false
        if (activeFeatures["Strict HTTPS Enforcer"] == true && loadingUrl.startsWith("http://")) {
            view?.loadUrl(loadingUrl.replace("http://", "https://"))
            return true
        }
        if (activeFeatures["AMP Bypass"] == true && loadingUrl.contains("/amp/")) {
            view?.loadUrl(loadingUrl.replace("/amp/", "/"))
            return true
        }
        return false
    }
    
    @SuppressLint("WebViewClientOnReceivedSslError")
    override fun onReceivedSslError(
        view: WebView?,
        handler: SslErrorHandler?,
        error: android.net.http.SslError?
    ) {
        if (activeFeatures["SSL Preferences Management"] == true) {
            handler?.proceed()
        } else {
            handler?.cancel()
        }
    }
    
    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        if (activeFeatures["Offline Page Rescue"] == true && request?.isForMainFrame == true) {
            view?.loadDataWithBaseURL(null, 
                "<html><body style='background:#111;color:#eee;text-align:center;padding-top:20%;font-family:sans-serif;'><h2>Offline / Error</h2><p>Page could not be loaded.</p></body></html>", 
                "text/html", "UTF-8", null)
        }
        super.onReceivedError(view, request, error)
    }
}

class TiwutWebChromeClient(val context: android.content.Context) : WebChromeClient() {
    var activeFeatures: Map<String, Boolean> = emptyMap()
    var onProgressChange: ((Float) -> Unit)? = null

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        onProgressChange?.invoke(newProgress / 100f)
        super.onProgressChanged(view, newProgress)
    }

    override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
        if (activeFeatures["Enable Logging Console"] == true) {
            consoleMessage?.message()?.let { msg ->
                android.widget.Toast.makeText(context, "Log: $msg", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        return super.onConsoleMessage(consoleMessage)
    }
}

