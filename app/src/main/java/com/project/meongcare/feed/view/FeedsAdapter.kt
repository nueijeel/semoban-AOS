package com.project.meongcare.feed.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.project.meongcare.databinding.ItemSearchFeedBinding
import com.project.meongcare.feed.model.entities.Feed

class FeedsAdapter : ListAdapter<Feed, FeedsAdapter.FeedsViewHolder>(diffUtil) {
    inner class FeedsViewHolder(private val binding: ItemSearchFeedBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Feed) {
            binding.run {
                // image
                textviewSearchfeedBrand.text = item.brandName
                textviewSearchfeedName.text = item.feedName
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): FeedsViewHolder {
        val itemSearchFeedBinding =
            ItemSearchFeedBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )

        return FeedsViewHolder(itemSearchFeedBinding)
    }

    override fun onBindViewHolder(
        holder: FeedsViewHolder,
        position: Int,
    ) {
        holder.bind(currentList[position])
    }

    companion object {
        val diffUtil =
            object : DiffUtil.ItemCallback<Feed>() {
                override fun areItemsTheSame(
                    oldItem: Feed,
                    newItem: Feed,
                ): Boolean {
                    return oldItem == newItem
                }

                override fun areContentsTheSame(
                    oldItem: Feed,
                    newItem: Feed,
                ): Boolean {
                    return oldItem == newItem
                }
            }
    }
}
