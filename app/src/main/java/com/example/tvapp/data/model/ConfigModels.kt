package com.example.tvapp.data.model

import com.google.gson.annotations.SerializedName

/** TVBox 配置根结构（config.json） */
data class TvConfig(
    @SerializedName("sites") val sites: List<Site> = emptyList(),
    @SerializedName("rules") val rules: Rules? = null,
    @SerializedName("parses") val parses: List<Any> = emptyList(),
    @SerializedName("lives") val lives: List<Any> = emptyList()
)

data class Rules(
    @SerializedName("ua") val ua: String? = null,
    @SerializedName("host") val host: List<String>? = null,
    @SerializedName("cookies") val cookies: Map<String, String>? = null,
    @SerializedName("header") val header: Map<String, String>? = null
)

/** 单个影视源 */
data class Site(
    @SerializedName("key") val key: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("type") val type: Any? = null,
    @SerializedName("api") val api: String? = null,
    @SerializedName("searchable") val searchable: Int? = 1,
    @SerializedName("filterable") val filterable: Int? = 0,
    @SerializedName("quickSearch") val quickSearch: Int? = 0
) {
    /** 标准 CMS（苹果CMS 类）HTTP JSON 源 */
    val isCms: Boolean
        get() = when (type) {
            is Number -> type.toInt() in 1..3
            is String -> type.lowercase() in listOf(
                "cms", "json", "cms_json", "苹果cms", "maccms"
            )
            else -> false
        }

    /** drpy / spider（JS）源 */
    val isJs: Boolean
        get() = when (type) {
            is Number -> type.toInt() in listOf(0, 4)
            is String -> type.lowercase() in listOf(
                "js", "drpy", "drpys", "spider", "csp", "python"
            )
            else -> false
        }
}
