package io.changyeoga.app

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

internal fun TextView.cText(size:Float=16f,secondary:Boolean=false) {
    typeface=resources.getFont(R.font.pretendard_regular)
    textSize=size
    setTextColor(if(secondary) 0xFF697278.toInt() else 0xFF30383E.toInt())
    includeFontPadding=false
}
internal fun Button.cAction(end:Boolean=false) {
    cText(15f)
    isAllCaps=false
    backgroundTintList=null
    background=RippleDrawable(ColorStateList.valueOf(0x11426A92),null,null)
    setTextColor(ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_enabled),intArrayOf()),intArrayOf(0xFF8B9296.toInt(),0xFF426A92.toInt())))
    val d=resources.displayMetrics.density
    minHeight=(48*d).toInt();minimumHeight=minHeight
    setPadding((8*d).toInt(),(12*d).toInt(),(8*d).toInt(),(12*d).toInt())
    layoutParams=LinearLayout.LayoutParams(if(end) -2 else -1,-2).apply { gravity=if(end) Gravity.END else Gravity.START;topMargin=(8*d).toInt() }
    gravity=if(end) Gravity.CENTER else Gravity.START or Gravity.CENTER_VERTICAL
}
internal fun EditText.cField() {
    cText(16f)
    setHintTextColor(0xFF697278.toInt())
    backgroundTintList=ColorStateList.valueOf(0xFFADB9C2.toInt())
    minHeight=(52*resources.displayMetrics.density).toInt()
}
