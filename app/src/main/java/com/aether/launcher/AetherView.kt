package com.aether.launcher

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.Toast
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class AetherView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val apps = AetherRuntime.registry.launcher.apps().take(24)
    private var quickOpen = false
    private var downX = 0f
    private var downY = 0f
    private var island = 0f
    private var quick = 0f

    init { setLayerType(View.LAYER_TYPE_SOFTWARE, null); paint.typeface = Typeface.create("sans", Typeface.NORMAL) }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat(); val h = height.toFloat()
        paint.style = Paint.Style.FILL
        paint.color = 0xFF0A0B0F.toInt(); c.drawRect(0f, 0f, w, h, paint)
        paint.color = 0x182A3440.toInt(); c.drawCircle(w*.18f, h*.25f, w*.42f, paint)
        paint.color = 0x141D3B32.toInt(); c.drawCircle(w*.84f, h*.68f, w*.45f, paint)

        val active = AetherRuntime.registry.island.activity
        val iw = min(w*.42f, 190f) * max(.55f, island)
        val ih = 34f + 30f*island
        val islandRect = RectF(w/2-iw/2, 14f, w/2+iw/2, 14f+ih)
        paint.color = 0xE915171B.toInt(); c.drawRoundRect(islandRect, ih/2, ih/2, paint)
        paint.color = 0x55FFFFFF; c.drawRoundRect(RectF(islandRect.left+2,islandRect.top+2,islandRect.right-2,islandRect.bottom-2),ih/2,ih/2,paint)
        if (active.type != com.aether.launcher.engine.island.ActivityType.NONE) {
            paint.color = 0xFFFFFFFF.toInt(); paint.textAlign = Paint.Align.CENTER; paint.textSize = 11f
            c.drawText(active.title.take(26), w/2, islandRect.centerY()+4f, paint)
        }

        paint.color = 0xFFFFFFFF.toInt(); paint.textAlign = Paint.Align.CENTER; paint.textSize = 16f; paint.typeface = Typeface.DEFAULT_BOLD
        c.drawText(timeText(), w/2, h*.12f, paint)
        paint.textSize = 12f; paint.typeface = Typeface.DEFAULT
        c.drawText("AETHER", w/2, h*.12f+22f, paint)

        val cols = 4; val gap = 14f; val left = 22f; val top = h*.27f; val cellW=(w-left*2-gap*(cols-1))/cols
        apps.forEachIndexed { i, app ->
            val col=i%cols; val row=i/cols; val x=left+col*(cellW+gap); val y=top+row*92f
            paint.color=0x661B1E24; c.drawRoundRect(RectF(x,y,x+cellW,y+64),20f,20f,paint)
            paint.color=0xAAFFFFFF; paint.textSize=11f; paint.textAlign=Paint.Align.CENTER
            c.drawText(app.label.take(12),x+cellW/2,y+84,paint)
        }

        val handleW=5f+quick*42f; val handleH=42f+quick*110f
        paint.color=0xCCFFFFFF.toInt(); c.drawRoundRect(RectF(w-7f-handleW,w/2-handleH/2,w-4f,w/2+handleH/2),handleW,handleW,paint)
        if(quickOpen) drawQuickSpace(c, w, h)
    }

    private fun drawQuickSpace(c: Canvas, w: Float, h: Float) {
        paint.color=0xDD17191E.toInt(); c.drawRoundRect(RectF(w*.08f,h*.53f,w*.92f,h*.91f),30f,30f,paint)
        paint.color=0xEEFFFFFF.toInt(); paint.textAlign=Paint.Align.CENTER; paint.textSize=18f
        c.drawText("Quick Space",w/2,h*.59f,paint)
        paint.textSize=12f
        c.drawText("Notes        Voice        Calculator        Screenshot",w/2,h*.70f,paint)
        paint.textSize=10f
        c.drawText("Tap an action • Swipe right to close",w/2,h*.82f,paint)
    }

    private fun timeText(): String = java.text.SimpleDateFormat("HH:mm",java.util.Locale.getDefault()).format(java.util.Date())

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when(e.actionMasked){
            MotionEvent.ACTION_DOWN -> {downX=e.x;downY=e.y;return true}
            MotionEvent.ACTION_UP -> {
                val dx=e.x-downX; val dy=e.y-downY
                if(downX>width-90 && abs(dx)>40){ quickOpen=dx<0; quick=if(quickOpen)1f else 0f; invalidate(); return true }
                if(quickOpen && downY>height*.62f && downY<height*.76f) {
                    when((e.x/(width/4f)).toInt()) {
                        0 -> showNote()
                        1 -> Toast.makeText(context,"Voice capture is next: microphone permission will be requested.",Toast.LENGTH_SHORT).show()
                        2 -> showCalculator()
                        3 -> context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                    return true
                }
                if(abs(dy)>45 && downY<120){ island=if(dy>0)1f else 0f; invalidate(); return true }
                if(!quickOpen && downY>height*.25f){
                    val cols=4; val gap=14f; val left=22f; val cellW=(width-left*2-gap*(cols-1))/cols
                    val col=((e.x-left)/(cellW+gap)).toInt(); val row=((e.y-height*.27f)/92f).toInt()
                    if(col in 0..3 && row>=0){val i=row*cols+col;if(i in apps.indices){context.packageManager.getLaunchIntentForPackage(apps[i].packageName)?.let{context.startActivity(it)}}}
                }
                return true
            }
        }
        return true
    }

    private fun showNote() {
        val input = EditText(context); input.hint = "Write a quick note…"
        android.app.AlertDialog.Builder(context).setTitle("Aether Note").setView(input)
            .setNegativeButton("Cancel", null).setPositiveButton("Save") { _, _ ->
                val text=input.text.toString().trim(); if(text.isNotEmpty()) {
                    AetherRuntime.registry.notes.capture(text)
                    Toast.makeText(context,"Note saved",Toast.LENGTH_SHORT).show()
                }
            }.show()
    }

    private fun showCalculator() {
        val input = EditText(context); input.hint = "e.g. 12 * 8 + 4"; input.inputType=2 or 8192
        android.app.AlertDialog.Builder(context).setTitle("Aether Calculator").setView(input)
            .setNegativeButton("Close", null).setPositiveButton("Calculate") { _, _ ->
                Toast.makeText(context, evaluate(input.text.toString()) ?: "Invalid expression", Toast.LENGTH_SHORT).show()
            }.show()
    }

    private fun evaluate(s: String): String? = try {
        val cleaned=s.replace(" ",""); if(!cleaned.matches(Regex("[-+*/.0-9]+"))) return null
        val values=cleaned.split(Regex("(?=[-+*/])|(?<=[-+*/])")).filter{it.isNotEmpty()}
        if(values.isEmpty()) return null
        var total=values[0].toDouble(); var i=1
        while(i+1<values.size){ val op=values[i]; val n=values[i+1].toDouble(); total=when(op){"+"->total+n;"-"->total-n;"*"->total*n;"/"->if(n==0.0)return null else total/n;else->return null}; i+=2 }
        if(total%1.0==0.0) total.toLong().toString() else "%.4f".format(total)
    } catch(_: Exception){ null }
}
