package com.aether.launcher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

/** Legacy compatibility view retained for older entry points. The real launcher surface is AetherHomeView. */
class AetherView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.color = Color.BLACK
        canvas.drawColor(Color.BLACK)
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 18f
        canvas.drawText("Aether is starting…", width / 2f, height / 2f, paint)
    }
}
