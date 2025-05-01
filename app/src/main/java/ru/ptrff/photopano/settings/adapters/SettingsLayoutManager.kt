package ru.ptrff.photopano.settings.adapters

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager

class SettingsLayoutManager(context: Context?, spanCount: Int, preferencesCount: Int) :
    GridLayoutManager(context, spanCount) {

    init {
        spanSizeLookup = object : SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int = if (
                position >= getItemCount() - preferencesCount || position == 0
            ) getSpanCount() else 1
        }
    }
}
