package com.aether.launcher

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import com.aether.launcher.settings.AetherSettingsActivity
import com.aether.launcher.settings.AetherSettingsStore
import com.aether.launcher.settings.HomeMode
import com.aether.launcher.settings.QuickEdge
import kotlin.math.abs
import kotlin.math.min

class AetherHomeView(context: Context) : View(context) {
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    private val store=AetherSettingsStore(context)
    private var cfg=store.load()
    private val apps=AetherRuntime.registry.launcher.apps()
    private var quick=false
    private var downX=0f;private var downY=0f
    init{setLayerType(View.LAYER_TYPE_SOFTWARE,null);p.typeface=Typeface.DEFAULT}
    override fun onDraw(c:Canvas){super.onDraw(c);cfg=store.load();val w=width.toFloat();val h=height.toFloat();val d=resources.displayMetrics.density
        p.color=0xFF080A0E.toInt();c.drawRect(0f,0f,w,h,p);p.color=0x162A3440;c.drawCircle(w*.18f,h*.23f,w*.42f,p);p.color=0x121D3B32;c.drawCircle(w*.84f,h*.68f,w*.45f,p)
        if(cfg.appearance.showTime){p.color=Color.WHITE;p.textAlign=Paint.Align.CENTER;p.textSize=16f;p.typeface=Typeface.DEFAULT_BOLD;c.drawText(timeText(),w/2,h*.11f,p)}
        if(cfg.appearance.showAetherLabel){p.textSize=11f;p.typeface=Typeface.DEFAULT;c.drawText("AETHER",w/2,h*.11f+20f,p)}
        if(cfg.island.enabled){val iw=min(w*.46f,cfg.island.width*d);val ih=cfg.island.height*d;val y=cfg.island.topOffset*d;p.color=0xD916181D.toInt();c.drawRoundRect(RectF(w/2-iw/2,y,w/2+iw/2,y+ih),cfg.island.radius*d,cfg.island.radius*d,p);p.color=0x45FFFFFF;c.drawRoundRect(RectF(w/2-iw/2+2,y+2,w/2+iw/2-2,y+ih-2),cfg.island.radius*d,cfg.island.radius*d,p)}
        val cols=cfg.grid.columns.coerceIn(3,7);val gap=cfg.grid.horizontalSpacing*d;val left=cfg.grid.horizontalPadding*d;val cell=(w-left*2-gap*(cols-1))/cols;val icon=cfg.grid.iconSize*d;val top=h*.20f+cfg.grid.verticalPadding*d;val rowH=icon+cfg.grid.verticalSpacing*d+22
        val shown=when(cfg.homeMode){HomeMode.ALL_APPS->apps.take(cols*cfg.grid.rows);HomeMode.HOME_AND_DRAWER->apps.take(cols*min(cfg.grid.rows,3));HomeMode.DRAWER_ONLY->apps.take(cols*min(cfg.grid.rows,2))}
        shown.forEachIndexed{i,a->val col=i%cols;val row=i/cols;val x=left+col*(cell+gap);val y=top+row*rowH;p.color=0x451C222A;p.setShadowLayer(cfg.glass.depth*d,0f,2f,0x50000000);c.drawRoundRect(RectF(x,y,x+cell,y+icon),cfg.glass.cornerRadius*d,cfg.glass.cornerRadius*d,p);p.clearShadowLayer();if(cfg.grid.showLabels){p.color=0xCFFFFFFF.toInt();p.textAlign=Paint.Align.CENTER;p.textSize=cfg.grid.labelSize*d;c.drawText(a.label.take(12),x+cell/2,y+icon+17*d,p)}}
        if(cfg.dock.enabled){val dockH=cfg.dock.height*d;val bottom=h-cfg.dock.bottomPadding*d-dockH;p.color=0xBF161B21.toInt();p.setShadowLayer(cfg.dock.radius*d,0f,8f,0x70000000);c.drawRoundRect(RectF(w*.06f,bottom,w*.94f,bottom+dockH),cfg.dock.radius*d,cfg.dock.radius*d,p);p.clearShadowLayer()}
        if(cfg.quickSpace.enabled){val right=cfg.quickSpace.edge==QuickEdge.RIGHT;val th=cfg.quickSpace.handleThickness*d;val len=cfg.quickSpace.handleLength*d;val x=if(right)w-8*d else 0f;p.color=0xDDFFFFFF.toInt();c.drawRoundRect(if(right)RectF(x-th,h*.5f-len/2,x,h*.5f+len/2)else RectF(x,h*.5f-len/2,x+th,h*.5f+len/2),th,th,p)}
        if(quick)drawQuick(c,w,h)
    }
    private fun drawQuick(c:Canvas,w:Float,h:Float){p.color=0xEA171A20.toInt();c.drawRoundRect(RectF(w*.06f,h*.50f,w*.94f,h*.90f),30f,30f,p);p.color=Color.WHITE;p.textAlign=Paint.Align.CENTER;p.textSize=18f;c.drawText("Quick Space",w/2,h*.57f,p);p.textSize=12f;c.drawText("Notes   •   Voice   •   Calculator   •   Screenshot",w/2,h*.69f,p);c.drawText("Clipboard   •   Recent   •   Shortcuts",w/2,h*.77f,p)}
    private fun timeText()=java.text.SimpleDateFormat("HH:mm",java.util.Locale.getDefault()).format(java.util.Date())
    override fun onTouchEvent(e:MotionEvent):Boolean{when(e.actionMasked){MotionEvent.ACTION_DOWN->{downX=e.x;downY=e.y;return true};MotionEvent.ACTION_UP->{val dx=e.x-downX;val dy=e.y-downY;if(downY<90&&downX>width-110&&abs(dx)<24&&abs(dy)<24){context.startActivity(Intent(context,AetherSettingsActivity::class.java));return true};val edgeHit=(cfg.quickSpace.edge==QuickEdge.RIGHT&&downX>width-100)||(cfg.quickSpace.edge==QuickEdge.LEFT&&downX<100);if(cfg.quickSpace.enabled&&edgeHit&&abs(dx)>cfg.quickSpace.triggerDistance){quick=if(cfg.quickSpace.edge==QuickEdge.RIGHT)dx<0 else dx>0;invalidate();return true};if(quick&&abs(dx)>50){quick=false;invalidate();return true};if(!quick&&dy==0f&&downY>height*.18f){val d=resources.displayMetrics.density;val cols=cfg.grid.columns.coerceIn(3,7);val gap=cfg.grid.horizontalSpacing*d;val left=cfg.grid.horizontalPadding*d;val cell=(width-left*2-gap*(cols-1))/cols;val icon=cfg.grid.iconSize*d;val rowH=icon+cfg.grid.verticalSpacing*d+22;val top=height*.20f+cfg.grid.verticalPadding*d;val col=((e.x-left)/(cell+gap)).toInt();val row=((e.y-top)/rowH).toInt();val i=row*cols+col;if(col in 0 until cols&&row>=0&&i<apps.size){context.packageManager.getLaunchIntentForPackage(apps[i].packageName)?.let{context.startActivity(it)}}};return true}};return true}
}
