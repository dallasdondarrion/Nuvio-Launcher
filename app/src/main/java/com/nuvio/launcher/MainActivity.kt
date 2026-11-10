package com.nuvio.launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

const val NUVIO_PACKAGE = "com.nuvio.tv"

class MainActivity : ComponentActivity() {

    private var homeSignal = 0
    private val homeListeners = mutableListOf<() -> Unit>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = Prefs(this)
        setContent {
            LauncherApp(prefs = prefs, registerHomeListener = { homeListeners += it })
        }
    }

    /** Pressing Home while the launcher is already open brings us back to the Home tab. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            homeSignal++
            homeListeners.forEach { it() }
        }
    }
}

fun launchPackage(context: Context, pkg: String): Boolean {
    val pm = context.packageManager
    val intent = pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg)
    return if (intent != null) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } else false
}

fun isInstalled(context: Context, pkg: String): Boolean =
    runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess

/**
 * Tries to open a specific title in Nuvio. Nuvio's deep-link format isn't documented,
 * so this tries the Stremio-style link first and falls back to simply opening Nuvio.
 */
fun openInNuvio(context: Context, item: SportsItem) {
    val link = Intent(Intent.ACTION_VIEW, Uri.parse("stremio:///detail/${item.type}/${item.id}"))
        .setPackage(NUVIO_PACKAGE)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val ok = runCatching { context.startActivity(link) }.isSuccess
    if (!ok) launchPackage(context, NUVIO_PACKAGE)
}
