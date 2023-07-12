package com.rmas.wildriff.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.rmas.wildriff.R
import com.rmas.wildriff.data.Riff
import org.w3c.dom.Text


interface RiffClickListener{
    fun onRiffClick(riff : Riff)
}

class RiffsAdapter(
    private var riffsList: List<Riff>,
    private val listener: RiffClickListener
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    companion object {
        private const val VIEW_TYPE_RIFF = 0
        private const val VIEW_TYPE_PLACEHOLDER = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (riffsList.isEmpty()) {
            VIEW_TYPE_PLACEHOLDER
        } else {
            VIEW_TYPE_RIFF
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder{
        return when (viewType) {
            VIEW_TYPE_RIFF -> {
                val itemView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_riff, parent, false)
                RiffViewHolder(itemView)
            }
            VIEW_TYPE_PLACEHOLDER -> {
                val itemView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_placeholder, parent, false)
                PlaceholderViewHolder(itemView)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    fun updateRiffsList(riffs:List<Riff>){
        val previousSize = this.itemCount
        riffsList = riffs

        if (previousSize < riffsList.size) {
            this.notifyItemRangeInserted(previousSize, riffsList.size - previousSize)
        } else if (previousSize > riffsList.size) {
            this.notifyItemRangeRemoved(riffsList.size, previousSize - riffsList.size)
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            VIEW_TYPE_RIFF -> {
                val riffHolder = holder as RiffViewHolder
                val riff = riffsList[position]
                riffHolder.bind(riff)
            }
            VIEW_TYPE_PLACEHOLDER -> {
                // Bind placeholder UI elements if needed
            }
        }
    }

    override fun getItemCount(): Int {
        return if (riffsList.isEmpty()) {
            1
        } else {
            riffsList.size
        }
    }

    inner class PlaceholderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Bind placeholder UI elements if needed
    }

    inner class RiffViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val riff = riffsList[position]
                    listener.onRiffClick(riff)
                }
            }
        }
        fun bind(riff: Riff) {
            itemView.findViewById<TextView>(R.id.textRiffName).text = riff.name
            itemView.findViewById<TextView>(R.id.textRiffPitch).text = riff.pitch
            itemView.findViewById<TextView>(R.id.textRiffTonality).text = riff.tonality
            itemView.findViewById<TextView>(R.id.textRiffKey).text = riff.key
            itemView.findViewById<TextView>(R.id.textViewGrade).text = riff.avgGrade.toFloat().toString()
        }
    }

}
