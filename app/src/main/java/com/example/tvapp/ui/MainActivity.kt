package com.example.tvapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tvapp.data.Spider
import com.example.tvapp.data.ConfigManager
import com.example.tvapp.databinding.ActivityMainBinding
import com.example.tvapp.ui.adapter.RowData
import com.example.tvapp.ui.adapter.RowsAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val rows = mutableListOf<RowData>()
    private var spider: Spider? = null
    private var rowsAdapter: RowsAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSearch.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        if (ConfigManager.hasConfig()) load()
    }

    private fun load() {
        binding.tvEmpty.visibility = View.GONE
        binding.rvRows.visibility = View.VISIBLE
        binding.tvSite.text = ConfigManager.activeSite()?.name ?: ""

        lifecycleScope.launch {
            try {
                val sp = ConfigManager.spider() ?: run {
                    toast("当前源暂不支持（非 CMS / 非 JS 类型）")
                    return@launch
                }
                spider = sp
                rows.clear()

                val (latest, latestMore) = withContext(Dispatchers.IO) { sp.latest(1) }
                rows.add(
                    RowData(
                        title = getString(com.example.tvapp.R.string.row_latest),
                        typeId = null,
                        items = latest.toMutableList(),
                        page = 1,
                        hasMore = latestMore
                    )
                )

                val cats = withContext(Dispatchers.IO) { sp.categories() }
                cats.take(12).forEach { c ->
                    val (items, hasMore) = withContext(Dispatchers.IO) {
                        sp.listByCategory(c.typeId ?: "", 1)
                    }
                    if (items.isNotEmpty()) {
                        rows.add(
                            RowData(
                                title = c.typeName ?: "分类",
                                typeId = c.typeId,
                                items = items.toMutableList(),
                                page = 1,
                                hasMore = hasMore
                            )
                        )
                    }
                }

                withContext(Dispatchers.Main) { renderRows() }
            } catch (e: Exception) {
                toast("加载失败：${e.message}")
            }
        }
    }

    private fun renderRows() {
        rowsAdapter = RowsAdapter(rows, { item ->
            startActivity(
                Intent(this, DetailsActivity::class.java)
                    .putExtra("vod_id", item.id)
            )
        }, ::loadMore)
        binding.rvRows.layoutManager = LinearLayoutManager(this)
        binding.rvRows.adapter = rowsAdapter
    }

    private fun loadMore(position: Int) {
        val row = rows.getOrNull(position) ?: return
        if (row.loading || !row.hasMore) return
        row.loading = true
        lifecycleScope.launch {
            try {
                val sp = spider ?: return@launch
                val (items, hasMore) = if (row.typeId == null) {
                    withContext(Dispatchers.IO) { sp.latest(row.page + 1) }
                } else {
                    withContext(Dispatchers.IO) { sp.listByCategory(row.typeId, row.page + 1) }
                }
                val start = row.items.size
                row.items.addAll(items)
                row.page += 1
                row.hasMore = hasMore
                row.loading = false
                rowsAdapter?.notifyAppended(position, start, items.size)
            } catch (e: Exception) {
                row.loading = false
                toast("加载更多失败：${e.message}")
            }
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}
