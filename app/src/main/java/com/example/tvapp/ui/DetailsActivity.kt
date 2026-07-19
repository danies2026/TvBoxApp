package com.example.tvapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.tvapp.data.ConfigManager
import com.example.tvapp.data.model.Episode
import com.example.tvapp.data.model.PlaySource
import com.example.tvapp.data.model.VideoDetail
import com.example.tvapp.databinding.ActivityDetailsBinding
import com.example.tvapp.ui.adapter.EpisodeAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailsBinding
    private var sources: List<PlaySource> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val vodId = intent.getStringExtra("vod_id")
        if (vodId.isNullOrEmpty()) {
            finish()
            return
        }
        loadDetail(vodId)
    }

    private fun loadDetail(vodId: String) {
        lifecycleScope.launch {
            try {
                val client = ConfigManager.spider()
                    ?: run {
                        toast("当前源暂不支持（仅支持 CMS / JS 类型）")
                        finish()
                        return@launch
                    }
                val d = withContext(Dispatchers.IO) { client.detail(vodId) }
                withContext(Dispatchers.Main) { render(d) }
            } catch (e: Exception) {
                toast("加载失败：${e.message}")
            }
        }
    }

    private fun render(d: VideoDetail) {
        binding.tvTitle.text = d.name
        val meta = listOfNotNull(d.year, d.area, d.actor, d.director)
            .filter { it.isNotBlank() }
            .joinToString("  ·  ")
        binding.tvMeta.text = meta
        binding.tvContent.text = d.content ?: ""
        Glide.with(binding.ivPoster).load(d.pic).placeholder(android.R.color.darker_gray)
            .into(binding.ivPoster)

        sources = d.sources
        if (d.sources.isEmpty()) {
            binding.tvEpisodeHint.text = getString(com.example.tvapp.R.string.no_episode)
            binding.spSources.visibility = View.GONE
            binding.rvEpisodes.visibility = View.GONE
            return
        }

        val names = d.sources.map { it.name }
        binding.spSources.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, names
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        binding.spSources.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>, v: View?, pos: Int, id: Long) {
                renderEpisodes(pos)
            }
            override fun onNothingSelected(p: AdapterView<*>) {}
        }
        renderEpisodes(0)
    }

    private fun renderEpisodes(pos: Int) {
        val eps: List<Episode> = sources.getOrNull(pos)?.episodes ?: return
        binding.rvEpisodes.layoutManager = GridLayoutManager(this, 3)
        binding.rvEpisodes.adapter = EpisodeAdapter(eps) { ep ->
            startActivity(
                Intent(this, PlayerActivity::class.java)
                    .putExtra("url", ep.url)
                    .putExtra("title", "${binding.tvTitle.text} - ${ep.name}")
            )
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}
