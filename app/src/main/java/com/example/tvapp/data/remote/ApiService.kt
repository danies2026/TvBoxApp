package com.example.tvapp.data.remote

import com.example.tvapp.data.model.VodListResponse
import retrofit2.http.GET
import retrofit2.http.Url

interface ApiService {
    /** 拉取原始文本（用于 config.json） */
    @GET
    suspend fun getString(@Url url: String): String

    /** 拉取影视列表 / 详情（CMS 标准返回） */
    @GET
    suspend fun getVod(@Url url: String): VodListResponse
}
