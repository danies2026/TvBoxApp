package com.example.tvapp.data

import com.example.tvapp.data.model.VideoDetail
import com.example.tvapp.data.model.VideoItem
import com.example.tvapp.data.model.VodClass

/**
 * 影视源统一接口。不同来源（CMS / drpy JS / 其他）各自实现。
 * 列表类方法返回 (本页数据, 是否还有下一页)，供 UI 做翻页加载。
 */
interface Spider {
    suspend fun categories(): List<VodClass>
    suspend fun latest(page: Int = 1): Pair<List<VideoItem>, Boolean>
    suspend fun listByCategory(typeId: String, page: Int = 1): Pair<List<VideoItem>, Boolean>
    suspend fun search(keyword: String, page: Int = 1): Pair<List<VideoItem>, Boolean>
    suspend fun detail(vodId: String): VideoDetail
}
