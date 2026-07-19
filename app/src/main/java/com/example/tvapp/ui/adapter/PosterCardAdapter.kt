package com.example.tvapp.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tvapp.data.model.VideoItem
import com.example.tvapp.databinding.ItemPosterBinding

class PosterCardAdapter(
    private val items: List<VideoItem>,
    private val onClick: (VideoItem) -> Unit
) : RecyclerView.Adapter<PosterCardAdapter.VH>() {

    class VH(val binding: ItemPosterBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemPosterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.tvTitle.text = item.name
        holder.binding.tvRemarks.text = item.remarks ?: ""
        Glide.with(holder.binding.ivPoster)
            .load(item.pic)
            .placeholder(android.R.color.darker_gray)
            .into(holder.binding.ivPoster)
        holder.binding.root.setOnClickListener { onClick(item) }
    }
}
