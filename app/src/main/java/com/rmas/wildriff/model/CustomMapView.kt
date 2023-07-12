package com.rmas.wildriff.model

import android.content.Context
import android.util.AttributeSet
import org.osmdroid.views.MapView

class CustomMapView(context: Context, attrs: AttributeSet) : MapView(context, attrs) {
    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}