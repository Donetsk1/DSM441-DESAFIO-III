package com.example.desafio_iii.util

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter

class NoFilterArrayAdapter(context: Context, items: List<String>) :
    ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, items) {

    private val items = items

    private val noFilter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            return FilterResults().apply {
                values = items
                count = items.size
            }
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            notifyDataSetChanged()
        }
    }

    override fun getFilter(): Filter = noFilter
}
