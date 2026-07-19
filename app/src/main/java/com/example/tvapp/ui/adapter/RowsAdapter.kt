package com.example.tvapp.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tvapp.data.model.VideoItem
import com.example.tvapp.databinding.RowItemBinding

/** 一行（一个分类）的展示与翻页状态 */
class RowData(
    val title: String,
    val typeId: String?,          // null 表示「最近更新」
    val items: MutableList<VideoItem> = mutableListOf(),
    var page: Int = 1,
    var hasMore: Boolean = true,
    var loading: Boolean = false,
    var adapter: PosterCardAdapter? = null
)

class RowsAdapter(
    private val rows: List<RowData>,
    private val onItemClick: (VideoItem) -> Unit,
    private val onLoadMore: (Int) -> Unit
) : RecyclerView.Adapter<RowsAdapter.VH>() {

    class VH(val binding: RowItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = RowItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun getItemCount(): Int = rows.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val row = rows[position]
        holder.binding.tvRowTitle.text = row.title

        val adapter = PosterCardAdapter(row.items, onItemClick)
        row.adapter = adapter
        holder.binding.rvCards.layoutManager =
            LinearLayoutManager(holder.binding.root.context, LinearLayoutManager.HORIZONTAL, false)
        holder.binding.rvCards.isNestedScrollingEnabled = false
        holder.binding.rvCards.adapter = adapter
        holder.binding.rvCards.clearOnScrollListeners()
        holder.binding.rvCards.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                val lm = rv.layoutManager as? LinearLayoutManager ?: return
                val total = lm.itemCount
                val last = lm.findLastVisibleItemPosition()
                if (!row.loading && row.hasMore && last >= total - 4) {
                    onLoadMore(position)
                }
            }
        })
    }

    /** 翻页追加后通知内层 adapter 刷新 */
    fun notifyAppended(position: Int, start: Int, count: Int) {
        rows.getOrNull(position)?.adapter?.notifyItemRangeInserted(start, count)
    }
}
