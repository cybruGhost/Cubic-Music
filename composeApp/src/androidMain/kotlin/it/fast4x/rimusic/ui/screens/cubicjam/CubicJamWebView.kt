package it.fast4x.rimusic.ui.screens.cubicjam

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import it.fast4x.rimusic.LocalPlayerAwareWindowInsets
import it.fast4x.rimusic.ui.components.themed.Title
import kotlinx.coroutines.launch
import timber.log.Timber
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import android.os.Build

private const val MOTOROLA = "motorola"
private const val SAMSUNG_USER_AGENT = "Mozilla/5.0 (Linux; Android 14; SM-S921U; Build/UP1A.231005.007) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Mobile Safari/537.36"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CubicJamWebView(
    navController: NavController,
    initialUrl: String = "https://swipes.lovable.app"
) {
    val scope = rememberCoroutineScope()
    var webView: WebView? = null

    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
    ) {
        // Header
        Title(
            title = "Cubic Jam",
            onClick = { navController.navigateUp() }
        )

        AndroidView(
            modifier = Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                .fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    
                    webViewClient = object : WebViewClient() {
                        
                        // For newer Android versions (API 24+)
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            request?.url?.let { uri ->
                                val url = uri.toString()
                                Timber.d("shouldOverrideUrlLoading (new): $url")
                                
                                // Handle intent URLs and convert them
                                val processedUrl = processUrlForWebView(url)
                                if (processedUrl != url) {
                                    // If URL was modified, load the processed version
                                    view?.loadUrl(processedUrl)
                                    return true
                                }
                                
                                // Check if it's an external URL that should open in browser
                                if (shouldOpenExternally(url, context)) {
                                    openUrlInBrowser(context, url)
                                    return true
                                }
                            }
                            return false
                        }
                        
                        // For older Android versions
                        @Deprecated("Deprecated in Java")
                        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                            url?.let {
                                Timber.d("shouldOverrideUrlLoading (old): $it")
                                
                                // Handle intent URLs and convert them
                                val processedUrl = processUrlForWebView(it)
                                if (processedUrl != it) {
                                    // If URL was modified, load the processed version
                                    view?.loadUrl(processedUrl)
                                    return true
                                }
                                
                                // Check if it's an external URL that should open in browser
                                if (shouldOpenExternally(it, context)) {
                                    openUrlInBrowser(context, it)
                                    return true
                                }
                            }
                            return false
                        }
                        
                        override fun onPageFinished(view: WebView, url: String) {
                            // Check if user is already logged in via token
                            val prefs = context.getSharedPreferences("cubic_jam_prefs", Context.MODE_PRIVATE)
                            val token = prefs.getString("bearer_token", null)
                            
                            if (token != null && url.contains("lovable.app")) {
                                // Inject token into localStorage if needed
                                val js = """
                                    localStorage.setItem('supabase.auth.token', JSON.stringify({
                                        access_token: '$token',
                                        token_type: 'bearer',
                                        expires_in: 3600,
                                        refresh_token: '',
                                        user: null
                                    }));
                                    console.log('Cubic Jam token injected');
                                """.trimIndent()
                                evaluateJavascript(js, null)
                            }
                            
                            // Inject JavaScript to fix sharing and external links
                            injectLinkFixer(view, context)
                        }
                        
                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                val errorMsg = error?.description ?: "Unknown error"
                                val url = request?.url?.toString() ?: "Unknown URL"
                                Timber.e("WebView Error: $errorMsg for URL: $url")
                            }
                        }
                        
                        @Deprecated("Deprecated in Java")
                        override fun onReceivedError(
                            view: WebView?,
                            errorCode: Int,
                            description: String?,
                            failingUrl: String?
                        ) {
                            super.onReceivedError(view, errorCode, description, failingUrl)
                            Timber.e("WebView Error: $description (Code: $errorCode) for URL: $failingUrl")
                        }
                    }
                    
                    webChromeClient = object : WebChromeClient() {
                        // Handle JavaScript dialogs
                        override fun onJsAlert(
                            view: WebView?,
                            url: String?,
                            message: String?,
                            result: JsResult?
                        ): Boolean {
                            return super.onJsAlert(view, url, message, result)
                        }
                        
                        // Handle file uploads
                        override fun onShowFileChooser(
                            webView: WebView?,
                            filePathCallback: ValueCallback<Array<Uri>>?,
                            fileChooserParams: FileChooserParams?
                        ): Boolean {
                            return super.onShowFileChooser(webView, filePathCallback, fileChooserParams)
                        }
                    }
                    
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true
                    settings.setSupportMultipleWindows(true)
                    settings.javaScriptCanOpenWindowsAutomatically = true
                    
                    // Enable safe browsing if available
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        settings.safeBrowsingEnabled = true
                    }
                    
                    if (android.os.Build.MANUFACTURER.equals(MOTOROLA, ignoreCase = true)) {
                        settings.userAgentString = SAMSUNG_USER_AGENT
                    }
                    
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)
                    
                    webView = this
                    
                    // Load Cubic Jam website
                    loadUrl(initialUrl)
                }
            }
        )

        BackHandler(enabled = webView?.canGoBack() == true) {
            webView?.goBack()
        }
    }
}

/**
 * Process URLs before they're loaded in WebView
 * Converts intent:// URLs to regular URLs
 */
private fun processUrlForWebView(url: String): String {
    Timber.d("Processing URL: $url")
    
    // Handle intent:// URLs - extract the real URL
    if (url.startsWith("intent://")) {
        try {
            // Parse the intent URL to extract parameters
            val uri = Uri.parse(url)
            val scheme = uri.getQueryParameter("scheme") ?: "https"
            val host = uri.host ?: ""
            val path = uri.path ?: ""
            val query = uri.query ?: ""
            
            // Extract fallback URL from intent parameters
            val intentParams = url.split("#Intent;")
            if (intentParams.size > 1) {
                val params = intentParams[1].split(";")
                for (param in params) {
                    if (param.startsWith("S.browser_fallback_url=")) {
                        val fallbackUrl = param.substring("S.browser_fallback_url=".length)
                        Timber.d("Found fallback URL: $fallbackUrl")
                        return fallbackUrl
                    }
                }
            }
            
            // If no fallback, construct URL from intent components
            val constructedUrl = when {
                host.isNotEmpty() && path.isNotEmpty() -> "$scheme://$host$path${if (query.isNotEmpty()) "?$query" else ""}"
                host.isNotEmpty() -> "$scheme://$host"
                else -> url // Return original if can't parse
            }
            
            Timber.d("Converted intent to: $constructedUrl")
            return constructedUrl
            
        } catch (e: Exception) {
            Timber.e("Error processing intent URL: ${e.message}")
            return url
        }
    }
    
    return url
}

/**
 * Check if a URL should open in external browser
 */
private fun shouldOpenExternally(url: String, context: Context): Boolean {
    // URLs that should stay in WebView
    val stayInWebView = listOf(
        "lovable.app",
        "swipes.lovable.app",
        "jam-wave-connect.lovable.app"
    )
    
    // Check if URL should stay in WebView
    stayInWebView.forEach { domain ->
        if (url.contains(domain)) {
            return false
        }
    }
    
    // URLs that should open externally
    return when {
        url.startsWith("market://") -> true
        url.startsWith("whatsapp://") -> true
        url.startsWith("fb://") -> true
        url.startsWith("twitter://") -> true
        url.startsWith("instagram://") -> true
        url.startsWith("spotify://") -> true
        url.startsWith("tel:") -> true
        url.startsWith("mailto:") -> true
        url.startsWith("sms:") -> true
        url.contains("youtube.com") || url.contains("youtu.be") -> true
        url.startsWith("http://") || url.startsWith("https://") -> true
        else -> false
    }
}

/**
 * Open URL in external browser
 */
private fun openUrlInBrowser(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        
        // Try to open in default browser
        context.startActivity(intent)
        Timber.d("Opened in browser: $url")
    } catch (e: Exception) {
        Timber.e("Failed to open URL in browser: ${e.message}")
    }
}

/**
 * Inject JavaScript to fix sharing and external links on the page
 */
private fun injectLinkFixer(webView: WebView, context: Context) {
    val js = """
        (function() {
            'use strict';
            
            console.log('Injecting link fixer...');
            
            // Function to fix intent URLs
            function fixIntentUrl(url) {
                if (url && url.startsWith('intent://')) {
                    try {
                        // Try to extract fallback URL from intent
                        const fallbackMatch = url.match(/S\.browser_fallback_url=([^;]+)/);
                        if (fallbackMatch && fallbackMatch[1]) {
                            return decodeURIComponent(fallbackMatch[1]);
                        }
                        
                        // Try to extract regular URL from intent
                        const urlMatch = url.match(/intent:\/\/([^#]+)/);
                        if (urlMatch && urlMatch[1]) {
                            return 'https://' + urlMatch[1];
                        }
                    } catch(e) {
                        console.error('Error fixing intent URL:', e);
                    }
                }
                return url;
            }
            
            // Fix all links on the page
            function fixAllLinks() {
                const links = document.querySelectorAll('a[href]');
                links.forEach(link => {
                    const originalHref = link.getAttribute('href');
                    if (originalHref && originalHref.startsWith('intent://')) {
                        const fixedUrl = fixIntentUrl(originalHref);
                        if (fixedUrl && fixedUrl !== originalHref) {
                            link.setAttribute('href', fixedUrl);
                            link.setAttribute('data-original-intent', originalHref);
                            console.log('Fixed link:', originalHref, '->', fixedUrl);
                        }
                    }
                });
            }
            
            // Fix onclick handlers
            function fixOnClickHandlers() {
                const elements = document.querySelectorAll('[onclick*="intent://"]');
                elements.forEach(el => {
                    const originalOnClick = el.getAttribute('onclick');
                    if (originalOnClick && originalOnClick.includes('intent://')) {
                        // Simple fix - replace intent URLs with https
                        const fixedOnClick = originalOnClick.replace(/intent:\/\/([^'"]+)/g, function(match) {
                            const fixed = fixIntentUrl(match);
                            return fixed || match;
                        });
                        el.setAttribute('onclick', fixedOnClick);
                        el.setAttribute('data-original-onclick', originalOnClick);
                        console.log('Fixed onclick handler');
                    }
                });
            }
            
            // Fix share buttons
            function fixShareButtons() {
                // Look for common share button patterns
                const shareSelectors = [
                    '[class*="share"]',
                    '[id*="share"]',
                    '[aria-label*="share"]',
                    'button:contains("Share")',
                    'a:contains("Share")'
                ];
                
                shareSelectors.forEach(selector => {
                    try {
                        const elements = document.querySelectorAll(selector);
                        elements.forEach(el => {
                            if (el.getAttribute('href') && el.getAttribute('href').startsWith('intent://')) {
                                const fixedUrl = fixIntentUrl(el.getAttribute('href'));
                                if (fixedUrl) {
                                    el.setAttribute('href', fixedUrl);
                                }
                            }
                            
                            // Also check for onclick
                            const onclick = el.getAttribute('onclick');
                            if (onclick && onclick.includes('intent://')) {
                                const fixedOnclick = onclick.replace(/intent:\/\/([^'"]+)/g, function(match) {
                                    return fixIntentUrl(match) || match;
                                });
                                el.setAttribute('onclick', fixedOnclick);
                            }
                        });
                    } catch(e) {
                        // Ignore selector errors
                    }
                });
            }
            
            // Run all fixes
            fixAllLinks();
            fixOnClickHandlers();
            fixShareButtons();
            
            // Also fix dynamically added content
            const observer = new MutationObserver(function(mutations) {
                mutations.forEach(function(mutation) {
                    if (mutation.addedNodes.length) {
                        setTimeout(function() {
                            fixAllLinks();
                            fixOnClickHandlers();
                            fixShareButtons();
                        }, 100);
                    }
                });
            });
            
            // Start observing the document for changes
            observer.observe(document.body, {
                childList: true,
                subtree: true
            });
            
            console.log('Link fixer injected successfully');
        })();
    """.trimIndent()
    
    // Inject the JavaScript
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        webView.evaluateJavascript(js, null)
    } else {
        webView.loadUrl("javascript:$js")
    }
}