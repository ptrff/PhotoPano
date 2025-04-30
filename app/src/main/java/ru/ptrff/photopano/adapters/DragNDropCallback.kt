package ru.ptrff.photopano.adapters

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder

class DragNDropCallback(
    private val preferenceCount: Int,
    private val adapterCallback: (fromPosition: Int, toPosition: Int) -> Boolean
) : ItemTouchHelper.Callback() {

    override fun isItemViewSwipeEnabled(): Boolean = false
    override fun onSwiped(viewHolder: ViewHolder, direction: Int) = error("not implemented")

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: ViewHolder
    ): Int {
        if (viewHolder.layoutPosition == 0 ||
            viewHolder.layoutPosition >= recyclerView.adapter!!.itemCount - preferenceCount
        ) return 0

        return makeMovementFlags(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or
                    ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT, 0
        )
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: ViewHolder,
        target: ViewHolder
    ): Boolean {
        if (target.adapterPosition == 0 ||
            target.adapterPosition >= recyclerView.adapter!!.itemCount - preferenceCount
        ) return false

        adapterCallback(viewHolder.adapterPosition, target.adapterPosition)
        return true
    }
}