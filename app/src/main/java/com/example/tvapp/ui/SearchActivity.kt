package com.example.tvapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tvapp.data.Spider
import com.example.tvapp.data.ConfigManager
import com.example.tvapp.databinding.ActivitySearchBinding
import com.example.tvapp.ui.adapter.PosterCardAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private var spider: Spider? = null
    private var adapter: PosterCardAdapter? = null
    private val items = mutableListOf<com.example.tvapp.data.model.VideoItem>()
    private var keyword = ""
    private var page = 1
    private var hasMore = true
    private var loading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSearch.setOnClickListener { doSearch() }
        binding.etQuery.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                doSearch()
                true
            } else false
        }
        binding.etQuery.requestFocus()

        binding.rvResults.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                val lm = rv.layoutManager as? GridLayoutManager ?: return
                val total = lm.itemCount
                val last = lm.findLastVisibleItemPosition()
                if (!loading && hasMore && last >= total - 8) loadMore()
            }
        })
    }

    private fun doSearch() {
        keyword = binding.etQuery.text.toString().trim()
        if (keyword.isEmpty()) return
        page = 1
        hasMore = true
        loading = false
        items.clear()

        lifecycleScope.launch {
            try {
                val sp = ConfigManager.spider() ?: run {
                    toast("当前源暂不支持（非 CMS / 非 JS 类型）")
                    return@launch
                }
                spider = sp
                val (result, more) = withContext(Dispatchers.IO) { sp.search(keyword, 1) }
                withContext(Dispatchers.Main) {
                    items.addAll(result)
                    hasMore = more
                    if (items.isEmpty()) toast("未找到结果")
                    adapter = PosterCardAdapter(items) { item ->
                        startActivity(
                            Intent(this@SearchActivity, DetailsActivity::class.java)
                                .putExtra("vod_id", item.id)
                        )
                    }
                    binding.rvResults.layoutManager = GridLayoutManager(this@SearchActivity, 6)
                    binding.rvResults.adapter = adapter
                }
            } catch (e: Exception) {
                toast("搜索失败：${e.message}")
            }
        }
    }

    private fun loadMore() {
        if (loading || !hasMore) return
        loading = true
        lifecycleScope.launch {
            try {
                val sp = spider ?: return@launch
                val (result, more) = withContext(Dispatchers.IO) { sp.search(keyword, page + 1) }
                val start = items.size
                items.addAll(result)
                page += 1
                hasMore = more
                loading = false
                withContext(Dispatchers.Main) { adapter?.notifyItemRangeInserted(start, result.size) }
            } catch (e: Exception) {
                loading = false
                toast("加载更多失败：${e.message}")
            }
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}
