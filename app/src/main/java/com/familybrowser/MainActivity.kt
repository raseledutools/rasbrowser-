package com.familybrowser

/**
 * ============================================================
 * FamilyBrowser — Production-Ready Android Browser
 * ============================================================
 *
 * REQUIRED PERMISSIONS (AndroidManifest.xml):
 * ─────────────────────────────────────────────
 * <uses-permission android:name="android.permission.INTERNET"/>
 * <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
 * <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
 *     android:maxSdkVersion="28"/>
 * <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
 *     android:maxSdkVersion="32"/>
 * <uses-permission android:name="android.permission.READ_MEDIA_IMAGES"/>
 * <uses-permission android:name="android.permission.READ_MEDIA_VIDEO"/>
 * <uses-permission android:name="android.permission.DOWNLOAD_WITHOUT_NOTIFICATION"/>
 * <uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
 * <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
 * <uses-permission android:name="android.permission.VIBRATE"/>
 * <uses-permission android:name="android.permission.CAMERA"/>
 * <uses-permission android:name="android.permission.RECORD_AUDIO"/>
 * <uses-permission android:name="android.permission.PICTURE_IN_PICTURE"/>
 *
 * REQUIRED MANIFEST ATTRIBUTES:
 * ─────────────────────────────────────────────
 * <activity
 *   android:name=".MainActivity"
 *   android:exported="true"
 *   android:configChanges="keyboard|keyboardHidden|orientation|screenLayout|screenSize|smallestScreenSize"
 *   android:hardwareAccelerated="true"
 *   android:launchMode="singleTask"
 *   android:supportsPictureInPicture="true"
 *   android:windowSoftInputMode="adjustResize">
 *   <intent-filter>
 *     <action android:name="android.intent.action.MAIN"/>
 *     <category android:name="android.intent.category.LAUNCHER"/>
 *   </intent-filter>
 *   <intent-filter>
 *     <action android:name="android.intent.action.VIEW"/>
 *     <data android:scheme="http"/>
 *     <data android:scheme="https"/>
 *   </intent-filter>
 * </activity>
 *
 * REQUIRED PROVIDER (for file sharing / downloads):
 * <provider
 *   android:name="androidx.core.content.FileProvider"
 *   android:authorities="${applicationId}.fileprovider"
 *   android:grantUriPermissions="true"
 *   android:exported="false">
 *   <meta-data
 *     android:name="android.support.FILE_PROVIDER_PATHS"
 *     android:resource="@xml/file_paths"/>
 * </provider>
 *
 * PROGUARD RULES (proguard-rules.pro):
 * ─────────────────────────────────────────────
 * -keep class com.familybrowser.** { *; }
 * -keepclassmembers class * extends android.webkit.WebViewClient { *; }
 * -keepclassmembers class * extends android.webkit.WebChromeClient { *; }
 * -keepattributes JavascriptInterface
 * -keepclassmembers class * {
 *     @android.webkit.JavascriptInterface <methods>;
 * }
 *
 * BUILD.GRADLE (app-level) — KEY DEPENDENCIES:
 * ─────────────────────────────────────────────
 * implementation("androidx.compose.ui:ui:1.6.0")
 * implementation("androidx.compose.material3:material3:1.2.0")
 * implementation("androidx.compose.ui:ui-tooling-preview:1.6.0")
 * implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
 * implementation("androidx.activity:activity-compose:1.8.2")
 * implementation("androidx.security:security-crypto:1.1.0-alpha06")
 * implementation("androidx.core:core-ktx:1.12.0")
 * implementation("androidx.webkit:webkit:1.10.0")
 * implementation("com.google.accompanist:accompanist-systemuicontroller:0.32.0")
 *
 * VERSION INFO:
 * versionCode = 1
 * versionName = "1.0.0"
 * minSdk = 26 (Android 8.0)
 * targetSdk = 34 (Android 14)
 * compileSdk = 34
 */

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

// ─── Color Palette ────────────────────────────────────────────────────────────
private val BrandBlue = Color(0xFF2563EB)
private val BrandBlueDark = Color(0xFF1D4ED8)
private val SurfaceDark = Color(0xFF1C1C1E)
private val SurfaceLight = Color(0xFFF2F2F7)
private val OnSurfaceDark = Color(0xFFECECEC)

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Handle results if needed */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable hardware acceleration
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )

        requestRuntimePermissions()

        setContent {
            BrowserApp()
        }
    }

    private fun requestRuntimePermissions() {
        val permissions = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
                add(Manifest.permission.READ_MEDIA_IMAGES)
                add(Manifest.permission.READ_MEDIA_VIDEO)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }
        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) requestPermissionLauncher.launch(needed.toTypedArray())
    }

    override fun onPictureInPictureModeChanged(isInPicture: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPicture, newConfig)
        // WebView continues playing — handled by overriding onPause in WebView
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// ROOT COMPOSABLE
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserApp(vm: BrowserViewModel = viewModel()) {
    val isDark = vm.profileManager.activeProfile.value?.darkModeEnabled ?: false

    MaterialTheme(
        colorScheme = if (isDark) darkColorScheme(
            primary = BrandBlue,
            surface = SurfaceDark,
            background = Color(0xFF000000)
        ) else lightColorScheme(
            primary = BrandBlue,
            surface = Color.White,
            background = SurfaceLight
        )
    ) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            BrowserScaffold(vm)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// MAIN SCAFFOLD
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScaffold(vm: BrowserViewModel) {
    val tabs = vm.tabManager.tabs
    val activeTab = vm.tabManager.activeTab

    BackHandler(enabled = vm.showTabSwitcher || vm.showMenu || vm.isFindInPage || vm.isAddressBarFocused) {
        when {
            vm.showTabSwitcher -> vm.showTabSwitcher = false
            vm.showMenu -> vm.showMenu = false
            vm.isFindInPage -> vm.toggleFindInPage()
            vm.isAddressBarFocused -> vm.onAddressBarDismissed()
        }
    }

    BackHandler(enabled = !vm.showTabSwitcher && !vm.showMenu) {
        if (vm.activeWebView?.canGoBack() == true) {
            vm.goBack()
        }
        // else: allow system back
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Top Bar ───────────────────────────────────────────────────
        if (!vm.isFullscreen) {
            TopBrowserBar(vm)
        }

        // ── Find In Page Bar ──────────────────────────────────────────
        AnimatedVisibility(visible = vm.isFindInPage) {
            FindInPageBar(vm)
        }

        // ── Progress Bar ──────────────────────────────────────────────
        AnimatedVisibility(visible = vm.isLoading) {
            LinearProgressIndicator(
                progress = { vm.loadProgress / 100f },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = BrandBlue,
                trackColor = Color.Transparent
            )
        }

        // ── WebView Area ──────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            tabs.forEachIndexed { index, tab ->
                val isActive = index == vm.tabManager.activeTabIndex.value
                BrowserWebView(
                    tab = tab,
                    vm = vm,
                    modifier = Modifier.fillMaxSize().then(
                        if (!isActive) Modifier.requiredSize(1.dp) else Modifier
                    )
                )
            }

            // ── Overlays ──────────────────────────────────────────────
            if (vm.isAddressBarFocused) {
                AddressBarSuggestions(vm)
            }

            // ── Tab Switcher ──────────────────────────────────────────
            AnimatedVisibility(
                visible = vm.showTabSwitcher,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                TabSwitcherScreen(vm)
            }

            // ── Bookmark saved snack ──────────────────────────────────
            AnimatedVisibility(
                visible = vm.showBookmarkDialog,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF1C1C1E),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Bookmark, null, tint = Color(0xFFFFBF00), modifier = Modifier.size(18.dp))
                        Text("Bookmark saved", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // ── Bottom Navigation ─────────────────────────────────────────
        if (!vm.isFullscreen) {
            BottomNavigationBar(vm)
        }
    }

    // ── Floating Menu ─────────────────────────────────────────────────────────
    if (vm.showMenu) {
        BrowserMenu(vm) { vm.showMenu = false }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TOP ADDRESS BAR
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBrowserBar(vm: BrowserViewModel) {
    val profile = vm.profileManager.activeProfile.value
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Profile avatar ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(BrandBlue.copy(alpha = 0.12f))
                    .clickable { vm.showProfileSwitcher = true },
                contentAlignment = Alignment.Center
            ) {
                Text(profile?.avatar ?: "😊", fontSize = 18.sp)
            }

            // ── Address Field ──────────────────────────────────────────
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // SSL icon
                    Icon(
                        if (vm.isSecureConnection) Icons.Filled.Lock else Icons.Filled.LockOpen,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (vm.isSecureConnection) Color(0xFF22C55E) else Color(0xFFEF4444)
                    )
                    Spacer(Modifier.width(6.dp))
                    if (vm.isAddressBarFocused) {
                        TextField(
                            value = vm.addressBarText,
                            onValueChange = { vm.onAddressBarTextChanged(it) },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Go
                            ),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    keyboardController?.hide()
                                    vm.onAddressBarSubmitted()
                                }
                            ),
                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                        )
                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                        }
                    } else {
                        Text(
                            text = vm.pageTitle.ifEmpty {
                                vm.currentUrl.removePrefix("https://").removePrefix("http://").take(40)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .clickable { vm.onAddressBarFocused() },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 14.sp
                        )
                    }
                    // Reader mode icon
                    if (!vm.isAddressBarFocused) {
                        IconButton(
                            onClick = { vm.toggleReaderMode() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Article, null,
                                modifier = Modifier.size(18.dp),
                                tint = if (vm.isReaderMode) BrandBlue else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── Reload / Stop ──────────────────────────────────────────
            IconButton(
                onClick = { if (vm.isLoading) vm.stopLoading() else vm.reload() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    if (vm.isLoading) Icons.Filled.Close else Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    // Profile Switcher Dialog
    if (vm.showProfileSwitcher) {
        ProfileSwitcherDialog(vm) { vm.showProfileSwitcher = false }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// WEBVIEW COMPOSABLE
// ═══════════════════════════════════════════════════════════════════════════════

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserWebView(
    tab: BrowserTab,
    vm: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profile = vm.profileManager.activeProfile.value

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // ── Hardware Acceleration ──────────────────────────────────
                setLayerType(View.LAYER_TYPE_HARDWARE, null)

                // ── WebSettings ────────────────────────────────────────────
                settings.apply {
                    javaScriptEnabled = profile?.javaScriptEnabled ?: true
                    domStorageEnabled = true
                    databaseEnabled = true
                    allowFileAccess = false
                    allowContentAccess = false
                    loadsImagesAutomatically = true
                    mediaPlaybackRequiresUserGesture = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    setSaveFormData(!tab.isIncognito)
                    setSavePassword(!tab.isIncognito)

                    // Desktop / Mobile UA
                    userAgentString = if (profile?.desktopModeEnabled == true) {
                        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                    } else {
                        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 FamilyBrowser/1.0"
                    }
                }

                // ── Force Dark Mode (Android 10+) ──────────────────────────
                if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING) &&
                    profile?.darkModeEnabled == true
                ) {
                    WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, true)
                }

                // ── WebViewClient ──────────────────────────────────────────
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest
                    ): WebResourceResponse? {
                        return vm.adBlocker.shouldBlock(
                            request,
                            isKidsMode = profile?.isKids ?: false,
                            kidsWhitelist = profile?.kidsWhitelist ?: emptySet()
                        ) ?: super.shouldInterceptRequest(view, request)
                    }

                    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        vm.onPageStarted(tab.id, url)
                    }

                    override fun onPageFinished(view: WebView, url: String) {
                        super.onPageFinished(view, url)
                        vm.onPageFinished(tab.id, url)
                        vm.onNavStateChanged(tab.id, view.canGoBack(), view.canGoForward())
                        // Capture thumbnail
                        view.post {
                            val bm = Bitmap.createBitmap(view.width.coerceAtLeast(1), view.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                            val canvas = android.graphics.Canvas(bm)
                            view.draw(canvas)
                            vm.tabManager.updateTabThumbnail(tab.id, bm)
                        }
                    }

                    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                        // Show SSL error instead of proceeding
                        handler.cancel()
                        val errorHtml = """<html><body style="font-family:sans-serif;padding:24px;text-align:center;">
                            <h2>⚠️ Connection Not Secure</h2>
                            <p>This site has an SSL certificate error.<br>Your connection may not be private.</p>
                            <button onclick="history.back()">Go Back</button></body></html>"""
                        view.loadData(errorHtml, "text/html", "UTF-8")
                    }

                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        val url = request.url.toString()
                        // Handle intent:// and other special schemes
                        if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("data:")) {
                            try {
                                val intent = android.content.Intent.parseUri(url, android.content.Intent.URI_INTENT_SCHEME)
                                context.startActivity(intent)
                                return true
                            } catch (e: Exception) {
                                return true
                            }
                        }
                        return false
                    }
                }

                // ── WebChromeClient ────────────────────────────────────────
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView, newProgress: Int) {
                        vm.onProgressChanged(tab.id, newProgress)
                    }

                    override fun onReceivedTitle(view: WebView, title: String) {
                        vm.onPageTitleChanged(tab.id, title)
                    }

                    override fun onReceivedIcon(view: WebView, icon: Bitmap) {
                        vm.tabManager.updateTabFavicon(tab.id, icon)
                    }

                    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                        vm.isFullscreen = true
                        (context as? Activity)?.window?.decorView?.let {
                            (it as? ViewGroup)?.addView(view)
                        }
                    }

                    override fun onHideCustomView() {
                        vm.isFullscreen = false
                    }

                    override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
                        result.confirm()
                        return true
                    }

                    override fun onPermissionRequest(request: PermissionRequest) {
                        // Only grant camera/mic per-request — don't auto-grant
                        request.deny()
                    }
                }

                // ── Download listener ──────────────────────────────────────
                setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
                    val fileName = URLUtil.guessFileName(url, contentDisposition, mimetype)
                    vm.downloadManager.startDownload(
                        url = url,
                        fileName = fileName,
                        mimeType = mimetype,
                        userAgent = userAgent
                    )
                }

                // ── Register with ViewModel ────────────────────────────────
                vm.registerWebView(tab.id, this)

                // ── Load initial URL ───────────────────────────────────────
                if (tab.url != "about:blank") {
                    loadUrl(tab.url)
                } else {
                    loadDataWithBaseURL(null, buildNewTabPage(), "text/html", "UTF-8", null)
                }
            }
        },
        update = { webView ->
            // Called when composition re-runs; don't reload unnecessarily
        },
        onRelease = { webView ->
            vm.unregisterWebView(tab.id)
            webView.stopLoading()
            webView.clearCache(false)
            webView.destroy()
        },
        modifier = modifier
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// BOTTOM NAV BAR
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun BottomNavigationBar(vm: BrowserViewModel) {
    val activeTab = vm.tabManager.activeTab
    val canGoBack = activeTab?.canGoBack ?: false
    val canGoForward = activeTab?.canGoForward ?: false

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back
            NavButton(
                icon = Icons.Default.ArrowBack,
                enabled = canGoBack,
                onClick = { vm.goBack() }
            )
            // Forward
            NavButton(
                icon = Icons.Default.ArrowForward,
                enabled = canGoForward,
                onClick = { vm.goForward() }
            )
            // Home
            NavButton(
                icon = Icons.Default.Home,
                onClick = { vm.goHome() }
            )
            // Tab Switcher
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { vm.showTabSwitcher = !vm.showTabSwitcher }
                    .border(2.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = vm.tabManager.tabCount.toString(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            // Menu
            NavButton(
                icon = Icons.Default.MoreVert,
                onClick = { vm.showMenu = !vm.showMenu }
            )
        }
    }
}

@Composable
fun NavButton(icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean = true, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            icon, null,
            modifier = Modifier.size(24.dp),
            tint = if (enabled) MaterialTheme.colorScheme.onSurface
                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// ADDRESS BAR SUGGESTIONS
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun AddressBarSuggestions(vm: BrowserViewModel) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn {
            items(vm.suggestions) { suggestion ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            when (suggestion.type) {
                                SuggestionType.SEARCH -> vm.navigate(vm.profileManager.buildSearchUrl(suggestion.text))
                                else -> vm.navigate(suggestion.text)
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        when (suggestion.type) {
                            SuggestionType.SEARCH -> Icons.Default.Search
                            SuggestionType.URL -> Icons.Default.Link
                            SuggestionType.HISTORY -> Icons.Default.History
                            SuggestionType.BOOKMARK -> Icons.Default.Bookmark
                        },
                        null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column {
                        Text(suggestion.text, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (suggestion.type != SuggestionType.SEARCH) {
                            Text(
                                suggestion.type.name.lowercase(),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                HorizontalDivider(thickness = 0.5.dp)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TAB SWITCHER
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun TabSwitcherScreen(vm: BrowserViewModel) {
    val tabs = vm.tabManager.tabs

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${tabs.size} Tab${if (tabs.size != 1) "s" else ""}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { vm.tabManager.closeAllTabs() }) {
                        Text("Close All")
                    }
                    Button(onClick = {
                        vm.openNewTab()
                        vm.showTabSwitcher = false
                    }) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New")
                    }
                }
            }

            // Incognito toggle
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = true,
                    onClick = { },
                    label = { Text("All (${vm.tabManager.regularTabCount})") }
                )
                FilterChip(
                    selected = false,
                    onClick = { vm.openNewTab(incognito = true); vm.showTabSwitcher = false },
                    label = { Text("+ Incognito") },
                    leadingIcon = { Icon(Icons.Default.VisibilityOff, null, modifier = Modifier.size(16.dp)) }
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(tabs, key = { it.id }) { tab ->
                    TabCard(
                        tab = tab,
                        isActive = tab.id == vm.tabManager.activeTab?.id,
                        onSelect = { vm.switchToTab(tab.id) },
                        onClose = { vm.closeTab(tab.id) }
                    )
                }
            }

            // Restore closed tab
            if (vm.tabManager.closedTabs.isNotEmpty()) {
                TextButton(
                    onClick = { vm.tabManager.restoreClosedTab() },
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                ) {
                    Icon(Icons.Default.RestoreFromTrash, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Restore Last Closed Tab")
                }
            }
        }
    }
}

@Composable
fun TabCard(tab: BrowserTab, isActive: Boolean, onSelect: () -> Unit, onClose: () -> Unit) {
    val borderColor = if (isActive) BrandBlue else Color.Transparent
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box {
            // Thumbnail
            tab.thumbnail?.let { bm ->
                Image(
                    bitmap = androidx.compose.ui.graphics.ImageBitmap.Companion.imageResource(
                        androidx.compose.ui.res.imageResource(0)
                    ),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            } ?: Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(tab.title.take(1).uppercase(), fontSize = 32.sp, fontWeight = FontWeight.Bold,
                     color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
            }

            // Title overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                    .padding(8.dp)
            ) {
                Text(
                    tab.title.ifEmpty { "New Tab" },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (tab.isIncognito) {
                    Text("Incognito", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                }
            }

            // Close button
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(28.dp)
                    .padding(4.dp)
            ) {
                Icon(
                    Icons.Default.Close, null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Active indicator
            if (isActive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(BrandBlue)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// BROWSER MENU
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun BrowserMenu(vm: BrowserViewModel, onDismiss: () -> Unit) {
    val profile = vm.profileManager.activeProfile.value

    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                // Privacy stats
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        PrivacyStat("🛡️ Ads", vm.adsBlockedCount.toString())
                        PrivacyStat("👁️ Trackers", vm.trackersBlockedCount.toString())
                    }
                }
                Spacer(Modifier.height(8.dp))

                val menuItems = listOf(
                    Triple(Icons.Outlined.Bookmark, "Bookmarks") { vm.currentScreen = BrowserScreen.BOOKMARKS; onDismiss() },
                    Triple(Icons.Outlined.History, "History") { vm.currentScreen = BrowserScreen.HISTORY; onDismiss() },
                    Triple(Icons.Outlined.Download, "Downloads") { vm.showDownloads = true; onDismiss() },
                    Triple(Icons.Outlined.FindInPage, "Find in Page") { vm.toggleFindInPage(); onDismiss() },
                    Triple(Icons.Outlined.Share, "Share Page") { vm.shareCurrentPage(); onDismiss() },
                    Triple(Icons.Outlined.OpenInBrowser, "Desktop Mode") { vm.toggleDesktopMode(); onDismiss() },
                    Triple(Icons.Outlined.DarkMode, "Dark Mode") { vm.toggleDarkMode(); onDismiss() },
                    Triple(Icons.Outlined.Settings, "Settings") { vm.currentScreen = BrowserScreen.SETTINGS; onDismiss() }
                )
                menuItems.forEach { (icon, label, action) ->
                    MenuRow(icon = icon, label = label, onClick = action)
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
fun MenuRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
        Text(label, fontSize = 15.sp)
    }
}

@Composable
fun PrivacyStat(label: String, count: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// FIND IN PAGE BAR
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun FindInPageBar(vm: BrowserViewModel) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = vm.findQuery,
                onValueChange = { vm.findNext(it) },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Find in page...") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.findNext(vm.findQuery) })
            )
            IconButton(onClick = { vm.findPrevious() }) {
                Icon(Icons.Default.KeyboardArrowUp, null)
            }
            IconButton(onClick = { vm.findNext(vm.findQuery) }) {
                Icon(Icons.Default.KeyboardArrowDown, null)
            }
            IconButton(onClick = { vm.toggleFindInPage() }) {
                Icon(Icons.Default.Close, null)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// PROFILE SWITCHER DIALOG
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun ProfileSwitcherDialog(vm: BrowserViewModel, onDismiss: () -> Unit) {
    val profiles = vm.profileManager.profiles

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Switch Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                profiles.forEach { profile ->
                    val isActive = profile.id == vm.profileManager.activeProfile.value?.id
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                vm.profileManager.switchProfile(profile.id)
                                vm.syncAdBlockerWithProfile()
                                onDismiss()
                            }
                            .border(
                                if (isActive) 2.dp else 0.dp,
                                if (isActive) BrandBlue else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            ),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(profile.avatar, fontSize = 28.sp)
                            Column {
                                Text(profile.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    when (profile.type) {
                                        ProfileType.KIDS -> "👶 Kids Mode"
                                        ProfileType.GUEST -> "👤 Guest Mode"
                                        ProfileType.STANDARD -> if (isActive) "Active" else "Standard"
                                    },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isActive) {
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Default.CheckCircle, null, tint = BrandBlue, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                if (profiles.size < UserProfileManager.MAX_PROFILES) {
                    OutlinedButton(
                        onClick = {
                            vm.profileManager.createProfile("New User", "🙂")
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add Profile")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// HELPERS
// ═══════════════════════════════════════════════════════════════════════════════

fun buildNewTabPage(): String = """
    <!DOCTYPE html><html><head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>New Tab</title>
    <style>
      body { background:#f0f4ff; font-family:-apple-system,sans-serif; margin:0;
             display:flex; align-items:center; justify-content:center; height:100vh; }
      h2 { color:#4a5568; font-weight:300; font-size:24px; }
    </style></head>
    <body><h2>🌐 Family Browser</h2></body></html>
""".trimIndent()
