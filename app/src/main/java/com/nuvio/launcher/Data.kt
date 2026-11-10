package com.nuvio.launcher

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Stores the list of sports add-on manifest URLs the user has added. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    fun addons(): List<String> {
        val raw = sp.getString("sports_addons", "[]") ?: "[]"
        val arr = JSONArray(raw)
        return List(arr.length()) { arr.getString(it) }
    }

    fun setAddons(list: List<String>) {
        sp.edit().putString("sports_addons", JSONArray(list).toString()).apply()
    }
}

data class SportsItem(
    val id: String,
    val type: String,
    val name: String,
    val poster: String?,
    val description: String?,
)

data class SportsRow(val title: String, val items: List<SportsItem>)

/**
 * Reads Stremio-style add-ons (the same protocol Nuvio add-ons use).
 * A manifest.json lists catalogs; each catalog is fetched from
 * {base}/catalog/{type}/{id}.json and returns {"metas":[...]}.
 */
object SportsRepo {

    /** Accepts stremio://, https://.../manifest.json or a bare base URL. */
    fun baseUrl(input: String): String {
        var u = input.trim()
        if (u.startsWith("stremio://")) u = "https://" + u.removePrefix("stremio://")
        u = u.removeSuffix("/")
        if (u.endsWith("/manifest.json")) u = u.removeSuffix("/manifest.json")
        return u
    }

    suspend fun load(addonUrls: List<String>): List<SportsRow> = coroutineScope {
        addonUrls.map { url ->
            async(Dispatchers.IO) {
                runCatching { loadAddon(baseUrl(url)) }.getOrDefault(emptyList())
            }
        }.awaitAll().flatten()
    }

    private fun loadAddon(base: String): List<SportsRow> {
        val manifest = JSONObject(get("$base/manifest.json"))
        val addonName = manifest.optString("name", "Sports")
        val catalogs = manifest.optJSONArray("catalogs") ?: return emptyList()
        val rows = mutableListOf<SportsRow>()
        for (i in 0 until catalogs.length()) {
            val cat = catalogs.getJSONObject(i)
            if (requiresExtra(cat)) continue
            val type = cat.getString("type")
            val id = cat.getString("id")
            val catName = cat.optString("name", id)
            val json = runCatching {
                JSONObject(get("$base/catalog/${enc(type)}/${enc(id)}.json"))
            }.getOrNull() ?: continue
            val metas = json.optJSONArray("metas") ?: continue
            val items = List(metas.length()) { j ->
                val m = metas.getJSONObject(j)
                SportsItem(
                    id = m.optString("id"),
                    type = m.optString("type", type),
                    name = m.optString("name"),
                    poster = m.optString("poster").takeIf { it.isNotBlank() }
                        ?: m.optString("background").takeIf { it.isNotBlank() },
                    description = m.optString("description").takeIf { it.isNotBlank() },
                )
            }
            if (items.isNotEmpty()) rows += SportsRow("$addonName · $catName", items)
        }
        return rows
    }

    private fun requiresExtra(cat: JSONObject): Boolean {
        val extra = cat.optJSONArray("extra") ?: return false
        for (i in 0 until extra.length()) {
            if (extra.getJSONObject(i).optBoolean("isRequired", false)) return true
        }
        return false
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    private fun get(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 15_000
        conn.setRequestProperty("User-Agent", "NuvioLauncher/1.0")
        return try {
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
