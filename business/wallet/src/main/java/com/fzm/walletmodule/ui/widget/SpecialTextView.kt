package com.fzm.walletmodule.ui.widget

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class SpecialTextView : AppCompatTextView {

    constructor(context: Context): super(context) {
        init()
    }
    constructor(context: Context,attrs:AttributeSet): super(context,attrs) {
        init()
    }
    constructor(context: Context,attrs:AttributeSet,defStyleAttr:Int): super(context,attrs,defStyleAttr) {
        init()
    }

    private fun init() {

        //设置字体图标
        this.typeface = Typeface.createFromAsset(context.assets, "specialtext.ttf")
    }
}