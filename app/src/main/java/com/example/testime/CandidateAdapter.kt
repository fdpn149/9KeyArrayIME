package com.example.testime

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CandidateAdapter(
    private val context: Context,
    private val keyboard: BaseKeyboard
) : RecyclerView.Adapter<CandidateAdapter.ViewHolder>() {

    private val candidates = mutableListOf<String>()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val candidateTextView: TextView = itemView.findViewById(R.id.candidateTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_candidate, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position < candidates.size) {
            val candidate = candidates[position]
            holder.candidateTextView.text = candidate
            holder.itemView.setOnClickListener { handleCandidateClick(candidate) }
        } else {
            holder.candidateTextView.text = ""
            holder.itemView.setOnClickListener(null)
        }
    }

    override fun getItemCount(): Int {
        return if (candidates.isEmpty()) 1 else candidates.size
    }

    private fun handleCandidateClick(candidate: String) {
        keyboard.handleCandidateClick(candidate)
        clearCandidates()
    }

    fun updateCandidates(newCandidates: List<String>) {
        candidates.clear()
        candidates.addAll(newCandidates)
        notifyDataSetChanged()
    }

    fun clearCandidates() {
        candidates.clear()
        notifyDataSetChanged()
    }
}