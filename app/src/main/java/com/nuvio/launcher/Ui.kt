package com.nuvio.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage

private val Bg = Color(0xFF0B0D12)
private val Panel = Color(0xFF161A24)
private val PanelSelected = Color(0xFF2A3350)

@Composable
fun LauncherApp(prefs: Prefs, registerHomeListener: (() -> Unit) -> Unit) {
    var tab by remember { mutableStateOf(0) }
    val tabs = listOf("Home", "Sports", "Settings")

    // Pressing the Home button while the launcher is open returns to the Home tab.
    LaunchedEffect(Unit) { registerHomeListener { tab = 0 } }
    BackHandler(enabled = true) { if (tab != 0) tab = 0 }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Bg)
                .padding(horizontal = 48.dp, vertical = 24.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                tabs.forEachIndexed { i, label ->
                    TvButton(label, selected = tab == i) { tab = i }
                }
            }
            Spacer(Modifier.height(24.dp))
            when (tab) {
                0 -> HomeScreen()
                1 -> SportsScreen(prefs)
                else -> SettingsScreen(prefs)
            }
        }
    }
}

@Composable
fun TvButton(label: String, selected: Boolean = false, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(50))
            .background(if (focused) Color.White else if (selected) PanelSelected else Panel)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 10.dp)
    ) {
        Text(label, color = if (focused) Color.Black else Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun FocusFrame(modifier: Modifier = Modifier, onClick: () -> Unit, content: @Composable () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(12.dp))
            .border(3.dp, if (focused) Color.White else Color.Transparent, RoundedCornerShape(12.dp))
            .background(Panel)
            .clickable(onClick = onClick)
    ) { content() }
}

// ---------------------------------------------------------------- Home

data class AppEntry(val label: String, val pkg: String, val image: Bitmap)

private fun loadApps(context: Context): List<AppEntry> {
    val pm = context.packageManager
    val leanback = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
    val regular = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val found = LinkedHashMap<String, AppEntry>()
    for (intent in listOf(leanback, regular)) {
        for (ri in pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)) {
            val pkg = ri.activityInfo.packageName
            if (pkg == context.packageName || pkg == NUVIO_PACKAGE || pkg in found) continue
            val drawable = ri.activityInfo.loadBanner(pm) ?: ri.loadIcon(pm)
            found[pkg] = AppEntry(ri.loadLabel(pm).toString(), pkg, drawable.toBitmap(320, 180))
        }
    }
    return found.values.sortedBy { it.label.lowercase() }
}

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val nuvioInstalled = remember { isInstalled(context, NUVIO_PACKAGE) }
    val apps = remember { loadApps(context) }
    val heroFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { heroFocus.requestFocus() } }

    Column(Modifier.fillMaxSize()) {
        FocusFrame(
            Modifier
                .focusRequester(heroFocus)
                .fillMaxWidth()
                .height(150.dp),
            onClick = { launchPackage(context, NUVIO_PACKAGE) }
        ) {
            Column(Modifier.padding(32.dp), verticalArrangement = Arrangement.Center) {
                Text("Nuvio", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    if (nuvioInstalled) "Press OK to open" else "Nuvio (com.nuvio.tv) isn't installed on this device",
                    color = Color(0xFFB0B7C8), fontSize = 16.sp
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Apps", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(200.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(apps, key = { it.pkg }) { app ->
                FocusFrame(Modifier.aspectRatio(16f / 9f), onClick = { launchPackage(context, app.pkg) }) {
                    Image(
                        bitmap = app.image.asImageBitmap(),
                        contentDescription = app.label,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(8.dp)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Sports

@Composable
fun SportsScreen(prefs: Prefs) {
    val context = LocalContext.current
    var rows by remember { mutableStateOf<List<SportsRow>?>(null) }
    var reload by remember { mutableStateOf(0) }
    val addons = remember { prefs.addons() }

    LaunchedEffect(reload) {
        rows = null
        rows = SportsRepo.load(addons)
    }

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Sports", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            TvButton("Refresh") { reload++ }
        }
        Spacer(Modifier.height(16.dp))
        val current = rows
        when {
            addons.isEmpty() -> Text(
                "No sports add-ons yet. Open Settings and add the manifest URL of each sports add-on.",
                color = Color(0xFFB0B7C8), fontSize = 18.sp
            )
            current == null -> Text("Loading…", color = Color(0xFFB0B7C8), fontSize = 18.sp)
            current.isEmpty() -> Text(
                "Nothing came back from your add-ons. Check the URLs in Settings, then Refresh.",
                color = Color(0xFFB0B7C8), fontSize = 18.sp
            )
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                items(current) { row ->
                    Text(row.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(row.items) { item ->
                            FocusFrame(Modifier.width(220.dp), onClick = { openInNuvio(context, item) }) {
                                Column {
                                    AsyncImage(
                                        model = item.poster,
                                        contentDescription = item.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(PanelSelected)
                                    )
                                    Text(
                                        item.name,
                                        color = Color.White, fontSize = 14.sp,
                                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Settings

@Composable
fun SettingsScreen(prefs: Prefs) {
    val context = LocalContext.current
    val addons = remember { mutableStateListOf<String>().apply { addAll(prefs.addons()) } }
    var input by remember { mutableStateOf("") }

    fun add(url: String) {
        val clean = url.trim()
        if (clean.isNotEmpty() && clean !in addons) {
            addons += clean
            prefs.setAddons(addons.toList())
        }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Sports add-ons", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Paste the manifest URL of each sports add-on you use in Nuvio. They're shown only in the Sports tab here.",
                color = Color(0xFFB0B7C8), fontSize = 16.sp
            )
        }
        item {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("Add-on manifest URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TvButton("Add") { add(input); input = "" }
                TvButton("Paste from clipboard") {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val text = cm.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
                    if (text.isNotBlank()) input = text
                }
            }
        }
        items(addons.toList()) { url ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(url, color = Color.White, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                TvButton("Remove") {
                    addons.remove(url)
                    prefs.setAddons(addons.toList())
                }
            }
        }
        item {
            Spacer(Modifier.height(16.dp))
            Text("About the default launcher", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "To make this the Home app that survives power-off, run once from a computer:\n" +
                    "adb shell cmd package set-home-activity com.nuvio.launcher/.MainActivity",
                color = Color(0xFFB0B7C8), fontSize = 15.sp
            )
        }
    }
}
