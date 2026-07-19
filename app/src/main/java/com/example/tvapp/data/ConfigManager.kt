package com.example.tvapp.data

import android.content.Context
import android.content.Context.MODE_PRIVATE
import com.example.tvapp.data.model.Site
import com.example.tvapp.data.model.TvConfig
import com.example.tvapp.data.remote.CmsSpider
import com.example.tvapp.data.remote.JsSpider
import com.example.tvapp.data.remote.RetrofitClient
import com.example.tvapp.data.Spider
import com.google.gson.Gson

/** 配置与当前源的全局管理（含持久化）。 */
object ConfigManager {
    private const val SP_NAME = "tvbox_pref"
    private const val KEY_CONFIG = "cfg_json"
    private const val KEY_SITE = "active_site"

    private lateinit var appContext: Context
    private val gson = Gson()
    private val prefs by lazy { appContext.getSharedPreferences(SP_NAME, MODE_PRIVATE) }

    var config: TvConfig? = null
    var activeSiteKey: String? = null

    fun init(ctx: Context) {
        appContext = ctx.applicationContext
    }

    /** 从 URL 拉取并解析 config.json，写入本地。 */
    suspend fun loadFromUrl(url: String) {
        val json = RetrofitClient.api.getString(url.trim())
        val cfg = gson.fromJson(json, TvConfig::class.java)
            ?: throw IllegalStateException("配置解析失败")
        config = cfg
        prefs.edit().putString(KEY_CONFIG, json).apply()
        if (activeSiteKey == null || cfg.sites.none { it.key == activeSiteKey }) {
            activeSiteKey = cfg.sites.firstOrNull()?.key
        }
        prefs.edit().putString(KEY_SITE, activeSiteKey).apply()
    }

    /** 启动时从本地恢复。 */
    fun restore() {
        val json = prefs.getString(KEY_CONFIG, null)
        if (json != null) {
            config = try {
                gson.fromJson(json, TvConfig::class.java)
            } catch (_: Exception) {
                null
            }
            activeSiteKey = prefs.getString(KEY_SITE, config?.sites?.firstOrNull()?.key)
        }
    }

    fun setActiveSite(key: String?) {
        activeSiteKey = key
        prefs.edit().putString(KEY_SITE, key).apply()
    }

    fun activeSite(): Site? =
        config?.sites?.firstOrNull { it.key == activeSiteKey }
            ?: config?.sites?.firstOrNull()

    /** 取当前源的 Spider 实现：CMS 走 CmsSpider，JS/drpy 走 JsSpider。不支持返回 null。 */
    fun spider(): Spider? {
        val site = activeSite() ?: return null
        return when {
            site.isCms -> CmsSpider(site, RetrofitClient.api)
            site.isJs || site.api.orEmpty().endsWith(".js", ignoreCase = true) -> JsSpider(site)
            else -> null
        }
    }

    fun hasConfig(): Boolean = config != null && config!!.sites.isNotEmpty()
}
