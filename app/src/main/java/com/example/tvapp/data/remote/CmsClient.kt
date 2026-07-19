package com.example.tvapp.data.remote

import com.example.tvapp.data.Spider
import com.example.tvapp.data.model.*
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/** 全局 Retrofit 单例。使用 @Url 全路径请求，baseUrl 仅为占位。 */
object RetrofitClient {
    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://localhost/")
        .client(okHttp)
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: ApiService = retrofit.create(ApiService::class.java)
}

/**
 * 标准苹果 CMS（TVBox type 1~3）HTTP JSON 源实现。
 * 覆盖：分类(ac=list)、列表(ac=videolist)、详情(ac=detail)、搜索(wd)。
 */
class CmsSpider(private val site: Site, private val api: ApiService) : Spider {

    private val base: String
        get() = (site.api ?: "").trim().removeSuffix("/")

    private fun buildUrl(vararg pairs: Pair<String, String>): String {
        val q = pairs.joinToString("&") {
            "${it.first}=${URLEncoder.encode(it.second, "UTF-8")}"
        }
        val sep = if (base.contains("?")) "&" else "?"
        return "$base$sep$q"
    }

    override suspend fun categories(): List<VodClass> =
        api.getVod(buildUrl("ac" to "list")).classes

    override suspend fun latest(page: Int): Pair<List<VideoItem>, Boolean> =
        api.getVod(buildUrl("ac" to "videolist", "pg" to page.toString()))
            .let { resp -> resp.list.map { it.toItem() } to hasMore(resp, page) }

    override suspend fun listByCategory(typeId: String, page: Int): Pair<List<VideoItem>, Boolean> =
        api.getVod(buildUrl("ac" to "videolist", "t" to typeId, "pg" to page.toString()))
            .let { resp -> resp.list.map { it.toItem() } to hasMore(resp, page) }

    override suspend fun search(keyword: String, page: Int): Pair<List<VideoItem>, Boolean> =
        api.getVod(buildUrl("ac" to "videolist", "wd" to keyword, "pg" to page.toString()))
            .let { resp -> resp.list.map { it.toItem() } to hasMore(resp, page) }

    override suspend fun detail(vodId: String): VideoDetail {
        val v = api.getVod(buildUrl("ac" to "detail", "ids" to vodId))
            .list.firstOrNull() ?: throw IllegalStateException("未找到该影视详情")
        return v.toDetail()
    }

    private fun hasMore(resp: VodListResponse, page: Int): Boolean {
        val pagecount = resp.page?.pagecount ?: 0
        return if (pagecount > 0) page < pagecount else resp.list.isNotEmpty()
    }
}

private fun Vod.toItem() = VideoItem(
    id = vodId ?: "",
    name = vodName ?: "未知",
    pic = vodPic,
    remarks = vodRemarks
)

private fun Vod.toDetail(): VideoDetail {
    return VideoDetail(
        id = vodId ?: "",
        name = vodName ?: "未知",
        pic = vodPic,
        remarks = vodRemarks,
        year = vodYear,
        area = vodArea,
        actor = vodActor,
        director = vodDirector,
        content = vodContent,
        sources = parseSources(vodPlayFrom, vodPlayUrl)
    )
}

/**
 * 解析 vod_play_from / vod_play_url。
 * 来源以 $$$ 分隔，来源内剧集以 # 分隔，单集格式为 "名称$地址"。
 */
private fun parseSources(from: String?, url: String?): List<PlaySource> {
    if (from.isNullOrBlank() || url.isNullOrBlank()) return emptyList()
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
