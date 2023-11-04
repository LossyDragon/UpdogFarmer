package com.steevsapps.idledaddy.fragments

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.GridLayoutManager

class GridLayoutManagerWrapper : GridLayoutManager {

    constructor(context: Context?, spanCount: Int) : super(context, spanCount)

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    constructor(
        context: Context?,
        spanCount: Int,
        orientation: Int,
        reverseLayout: Boolean
    ) : super(context, spanCount, orientation, reverseLayout)

    // Prevents a crash for some reason
    override fun supportsPredictiveItemAnimations(): Boolean = false
}
