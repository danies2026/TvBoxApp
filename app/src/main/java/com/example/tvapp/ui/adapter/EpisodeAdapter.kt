package com.example.tvapp.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.tvapp.data.model.Episode
import com.example.tvapp.databinding.ItemEpisodeBinding

class EpisodeAdapter(
    private val episodes: List<Episode>,
    private val onClick: (Episode) -> Unit
) : RecyclerView.Adapter<EpisodeAdapter.VH>() {

    class VH(val binding: ItemEpisodeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemEpisodeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun getItemCount(): Int = episodes.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val ep = episodes[position]
        holder.binding.tvEp.text = ep.name
        holder.binding.root.setOnClickListener { onClick(ep) }
    }
}
