package com.example.tvapp.data.model

import com.google.gson.annotations.SerializedName

/** 分类（来自 ac=list 的 class 数组） */
data class VodClass(
    @SerializedName("type_id") val typeId: String? = null,
    @SerializedName("type_name") val typeName: String? = null
)

/** 分页信息 */
data class PageInfo(
    @SerializedName("page") val page: Int = 0,
    @SerializedName("pagecount") val pagecount: Int = 0,
    @SerializedName("pagesize") val pagesize: Int = 0,
    @SerializedName("recordcount") val recordcount: Int = 0
)

/** 列表/详情接口统一返回结构 */
data class VodListResponse(
    @SerializedName("code") val code: Int = 0,
    @SerializedName("msg") val msg: String? = null,
    @SerializedName("list") val list: List<Vod> = emptyList(),
    @SerializedName("class") val classes: List<VodClass> = emptyList(),
    @SerializedName("page") val page: PageInfo? = null
)

/** 影视条目（列表与详情共用，详情时带 play 字段） */
data class Vod(
    @SerializedName("vod_id") val vodId: String? = null,
    @SerializedName("vod_name") val vodName: String? = null,
    @SerializedName("vod_pic") val vodPic: String? = null,
    @SerializedName("vod_remarks") val vodRemarks: String? = null,
    @SerializedName("type_name") val typeName: String? = null,
    @SerializedName("vod_year") val vodYear: String? = null,
    @SerializedName("vod_area") val vodArea: String? = null,
    @SerializedName("vod_actor") val vodActor: String? = null,
    @SerializedName("vod_director") val vodDirector: String? = null,
    @SerializedName("vod_content") val vodContent: String? = null,
    @SerializedName("vod_play_from") val vodPlayFrom: String? = null,
    @SerializedName("vod_play_url") val vodPlayUrl: String? = null
)
