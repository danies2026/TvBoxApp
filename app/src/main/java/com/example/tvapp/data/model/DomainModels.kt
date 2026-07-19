package com.example.tvapp.data.model

/** UI 层使用的领域模型 */

data class VideoItem(
    val id: String,
    val name: String,
    val pic: String?,
    val remarks: String?
)

data class Episode(
    val name: String,
    val url: String
)

data class PlaySource(
    val name: String,
    val episodes: List<Episode>
)

data class VideoDetail(
    val id: String,
    val name: String,
    val pic: String?,
    val remarks: String?,
    val year: String?,
    val area: String?,
    val actor: String?,
    val director: String?,
    val content: String?,
    val sources: List<PlaySource>
)
