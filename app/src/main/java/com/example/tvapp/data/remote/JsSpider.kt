package com.example.tvapp.data.remote

import android.util.Base64
import com.example.tvapp.data.Spider
import com.example.tvapp.data.model.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.Undefined
import java.util.concurrent.TimeUnit

/**
 * drpy / spider（JS）源实现：用 Rhino 运行 TVBox 的 JS spider 脚本。
 *
 * 说明：这是**尽力而为**的运行时，覆盖 `request / base64 / pdfh / pdfa` 等常用 API，
 * 足以驱动以 JSON 接口为主的 drpy 源；对重度依赖 HTML 选择器或特殊语法的 spider 可能不完全兼容。
 * 任何一步失败都会抛出清晰异常，由上层提示「该源暂未完全支持」。
 */
class JsSpider(private val site: Site) : Spider {

    private val gson = Gson()
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    @Volatile private var built = false
    private lateinit var scope: Scriptable

    @Synchronized
    private fun ensureScope() {
        if (built) return
        val cx = Context.enter()
        try {
            cx.optimizationLevel = -1 // Android 必须关闭 JIT 优化
            val sc = cx.initStandardObjects()
            sc.put("__bridge", sc, JsBridge(http))
            cx.evaluateString(sc, SHIM, "shim.js", 1, null)
            val js = loadScript()
            cx.evaluateString(sc, js, "spider.js", 1, null)
            scope = sc
            built = true
        } finally {
            Context.exit()
        }
    }

    private fun loadScript(): String {
        val api = site.api ?: throw IllegalStateException("源缺少 api 地址")
        val txt = JsBridge(http).request(api, "GET", null, null, true)
        if (txt.contains("homeContent") || txt.contains("categoryContent") || txt.contains("var spider"))
            return txt
        throw IllegalStateException("无法识别 spider 脚本（内容非 drpy 脚本）")
    }

    @Synchronized
    private fun call(shimFn: String, vararg args: Any?): String {
        ensureScope()
        val cx = Context.enter()
        try {
            val fn = scope.get(shimFn, scope)
            if (fn !is Function) throw IllegalStateException("脚本缺少方法 $shimFn")
            val r = fn.call(cx, scope, scope, args)
            return if (r is Undefined) "" else Context.toString(r)
        } finally {
            Context.exit()
        }
    }

    override suspend fun categories(): List<VodClass> = withContext(Dispatchers.IO) {
        ensureScope()
        val json = call("__home")
        val root = runCatching { gson.fromJson(json, JsonObject::class.java) }.getOrNull() ?: return@withContext emptyList()
        val classes = root.getAsJsonArray("classes") ?: return@withContext emptyList()
        classes.mapNotNull { el ->
            if (el !is JsonObject) null
            else VodClass(optString(el, "type_id"), optString(el, "type_name"))
        }
    }

    override suspend fun latest(page: Int): Pair<List<VideoItem>, Boolean> = withContext(Dispatchers.IO) {
        ensureScope()
        parseList(call("__home"))
    }

    override suspend fun listByCategory(typeId: String, page: Int): Pair<List<VideoItem>, Boolean> =
        withContext(Dispatchers.IO) {
            ensureScope()
            parseList(call("__cat", typeId, page, false, emptyMap<String, String>()))
        }

    override suspend fun search(keyword: String, page: Int): Pair<List<VideoItem>, Boolean> =
        withContext(Dispatchers.IO) {
            ensureScope()
            parseList(call("__search", keyword, false))
        }

    override suspend fun detail(vodId: String): VideoDetail = withContext(Dispatchers.IO) {
        ensureScope()
        val json = call("__detail", "[\"$vodId\"]")
        val root = gson.fromJson(json, JsonObject::class.java)
            ?: throw IllegalStateException("详情返回为空")
        val arr = root.getAsJsonArray("list")
        val el = (arr?.firstOrNull() as? JsonObject)
            ?: throw IllegalStateException("未找到该影视详情")
        mapDetail(el)
    }

    // ---- 解析 drpy 返回 ----
    private fun parseList(json: String): Pair<List<VideoItem>, Boolean> {
        val root = runCatching { gson.fromJson(json, JsonObject::class.java) }.getOrNull()
            ?: return emptyList<VideoItem>() to false
        val arr = root.getAsJsonArray("list") ?: return emptyList<VideoItem>() to false
        val items = arr.mapNotNull { el -> if (el is JsonObject) mapItem(el) else null }
            .filter { it.id.isNotEmpty() }
        val pageObj = root.getAsJsonObject("page")
        val pagecount = pageObj?.get("pagecount")?.asInt ?: 0
        val page = pageObj?.get("page")?.asInt ?: 1
        val hasMore = if (pagecount > 0) page < pagecount else items.isNotEmpty()
        return items to hasMore
    }

    private fun mapItem(el: JsonObject): VideoItem = VideoItem(
        id = optString(el, "vod_id"),
        name = optString(el, "vod_name"),
        pic = optString(el, "vod_pic").ifEmpty { null },
        remarks = optString(el, "vod_remarks").ifEmpty { null }
    )

    private fun mapDetail(el: JsonObject): VideoDetail = VideoDetail(
        id = optString(el, "vod_id"),
        name = optString(el, "vod_name"),
        pic = optString(el, "vod_pic").ifEmpty { null },
        remarks = optString(el, "vod_remarks").ifEmpty { null },
        year = optString(el, "vod_year").ifEmpty { null },
        area = optString(el, "vod_area").ifEmpty { null },
        actor = optString(el, "vod_actor").ifEmpty { null },
        director = optString(el, "vod_director").ifEmpty { null },
        content = optString(el, "vod_content").ifEmpty { null },
        sources = parseSources(optString(el, "vod_play_from"), optString(el, "vod_play_url"))
    )

    private fun parseSources(from: String?, url: String?): List<PlaySource> {
        if (from.isBlank() || url.isBlank()) return emptyList()
        val fromList = from.split("$$$")
        val urlList = url.split("$$$")
        val size = minOf(fromList.size, urlList.size)
        val result = mutableListOf<PlaySource>()
        for (i in 0 until size) {
            val fromName = fromList[i].ifBlank { "线路${i + 1}" }
            val episodes = urlList[i].split("#").mapNotNull { ep ->
                val seg = ep.split("$", limit = 2)
                val name = seg.getOrNull(0)?.trim() ?: return@mapNotNull null
                val u = seg.getOrNull(1)?.trim() ?: ""
                if (u.isBlank()) null else Episode(name, u)
            }
            if (episodes.isNotEmpty()) result.add(PlaySource(fromName, episodes))
        }
        return result
    }

    private fun optString(el: JsonObject, key: String): String {
        val e = el.get(key) ?: return ""
        return if (e.isJsonNull) "" else e.asString
    }

    companion object {
        private const val SHIM = """
        function request(u,m,h,d,r){return __bridge.request(u, m||'GET', h||null, d||null, r!==false);}
        function base64Encode(s){return __bridge.b64e(s);}
        function base64Decode(s){return __bridge.b64d(s);}
        function pdfh(h,r){return __bridge.pdfh(h,r);}
        function pdfa(h,r){return __bridge.pdfa(h,r);}
        var PC_UA='Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36';
        var MOBILE_UA='Mozilla/5.0 (Linux; Android 11) AppleWebKit/537.36';
        function __home(){return spider.homeContent();}
        function __cat(tid,pg,filter,extend){return spider.categoryContent(tid, pg, !!filter, extend||{});}
        function __detail(idsJson){return spider.detailContent(JSON.parse(idsJson));}
        function __search(key,quick){return spider.searchContent(key, !!quick);}
        """
    }
}

/** 暴露给 JS 的 Java 桥：网络请求与编解码。Rhino 通过反射调用其 public 方法。 */
class JsBridge(private val http: OkHttpClient) {
    fun request(url: String, method: String?, headers: Any?, data: Any?, redirect: Boolean): String {
        val m = (method ?: "GET").uppercase()
        val builder = Request.Builder().url(url)
        (headers as? Map<*, *>)?.forEach { (k, v) ->
            builder.addHeader(k.toString(), v?.toString() ?: "")
        }
        if (!redirect) builder.header("Redirect", "false")
        val body = if (m == "POST" && data != null) {
            data.toString().toRequestBody("application/x-www-form-urlencoded".toMediaType())
        } else null
        val req = builder.method(m, body).build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw RuntimeException("请求失败 ${resp.code}: $url")
            return resp.body?.string() ?: ""
        }
    }

    fun b64e(s: String): String = Base64.encodeToString(s.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    fun b64d(s: String): String = String(Base64.decode(s, Base64.NO_WRAP), Charsets.UTF_8)

    /** 尽力而为的 pdfh：支持 JSON 路径($.x.y[0]) 与正则(##后为正则)。 */
    fun pdfh(html: String, rule: String): String {
        if (rule.startsWith("$")) return jsonPath(html, rule)
        if (rule.contains("##")) {
            val regex = rule.substringAfter("##")
            val re = Regex(regex, RegexOption.DOT_MATCHES_ALL)
            val m = re.find(html) ?: return ""
            return m.groupValues.getOrNull(1) ?: m.value
        }
        return ""
    }

    fun pdfa(html: String, rule: String): String {
        if (rule.startsWith("$")) {
            val arr = jsonPathArray(html, rule)
            return org.mozilla.javascript.Context.toString(arr)
        }
        return "[]"
    }

    private fun jsonPath(json: String, path: String): String {
        val root = Gson().fromJson(json, JsonObject::class.java) ?: return ""
        var cur: com.google.gson.JsonElement = root
        for (tok in path.removePrefix("$.").split(".")) {
            val t = tok.trim()
            if (t.isEmpty()) continue
            if (cur is JsonObject) {
                cur = cur.get(t) ?: return ""
            } else if (cur is com.google.gson.JsonArray) {
                val idx = t.removeSuffix("]").removePrefix("[").toIntOrNull() ?: return ""
                cur = cur.get(idx) ?: return ""
            } else return ""
        }
        return if (cur.isJsonPrimitive) cur.asString else cur.toString()
    }

    private fun jsonPathArray(json: String, path: String): com.google.gson.JsonArray {
        val root = Gson().fromJson(json, JsonObject::class.java) ?: return com.google.gson.JsonArray()
        var cur: com.google.gson.JsonElement = root
        for (tok in path.removePrefix("$.").split(".")) {
            val t = tok.trim()
            if (t.isEmpty()) continue
            if (cur is JsonObject) cur = cur.get(t) ?: return com.google.gson.JsonArray()
            else if (cur is com.google.gson.JsonArray) {
                val idx = t.removeSuffix("]").removePrefix("[").toIntOrNull() ?: return com.google.gson.JsonArray()
                cur = cur.get(idx) ?: return com.google.gson.JsonArray()
            } else return com.google.gson.JsonArray()
        }
        return if (cur is com.google.gson.JsonArray) cur else com.google.gson.JsonArray()
    }
}
