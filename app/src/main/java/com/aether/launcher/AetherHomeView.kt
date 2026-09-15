package com.aether.launcher

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import com.aether.launcher.settings.AetherSettingsStore
import com.aether.launcher.settings.HomeMode
import com.aether.launcher.settings.QuickEdge
import com.aether.launcher.ui.AetherSurface
import com.aether.launcher.ui.AetherSurfaceActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Primary Aether surface. No fake search field, no package-name UI, and the home order is user-owned. */
class AetherHomeView(context: Context) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val store = AetherSettingsStore(context)
    private val layout = AetherHomeLayoutStore(context)
    private val folders = AetherFolderStore(context)
    private val dock = AetherDockStore(context)
    private val history = AetherHistoryStore(context)
    private val handler = Handler(Looper.getMainLooper())
    private val d get() = resources.displayMetrics.density
    private var cfg = store.load()
    private var order = mutableListOf<String>()
    private var downX = 0f
    private var downY = 0f
    private var dragX = 0f
    private var dragY = 0f
    private var dragToken: String? = null
    private var dragOrigin = -1
    private var dragTarget = -1
    private var searchLock = false

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        order = layout.order(AetherRuntime.registry.launcher.apps().map { it.packageName })
        isClickable = true
    }

    override fun onDraw(c: Canvas) {
        cfg = store.load()
        syncOrder()
        val w = width.toFloat(); val h = height.toFloat()
        p.shader = LinearGradient(0f, 0f, 0f, h, 0x12030810, 0x08030810, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p); p.shader = null
        drawHeader(c, w)
        drawIsland(c, w)
        drawGrid(c, w, h)
        drawDock(c, w, h)
        drawEdge(c, w, h)
        drawDragged(c)
    }

    private fun syncOrder() {
        val installed = AetherRuntime.registry.launcher.apps().map { it.packageName }
        if (installed.any { it !in order } || order.none { it in installed }) order = layout.order(installed)
    }

    private fun drawHeader(c: Canvas, w: Float) {
        p.textAlign = Paint.Align.LEFT; p.typeface = Typeface.DEFAULT
        p.color = 0xAFFFFFFF.toInt(); p.textSize = 11f*d
        c.drawText(SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date()).uppercase(Locale.getDefault()), 24f*d, 40f*d, p)
        p.color = Color.WHITE; p.typeface = Typeface.DEFAULT_BOLD; p.textSize = 27f*d
        c.drawText(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()), 24f*d, 70f*d, p)
        p.textAlign = Paint.Align.RIGHT; p.typeface = Typeface.DEFAULT; p.textSize = 9f*d; p.color = 0x9AFFFFFF.toInt()
        c.drawText("AETHER", w-24f*d, 43f*d, p); p.textSize = 7f*d; p.color = 0x62FFFFFF
        c.drawText("LIQUID / FLUID / PRIVATE", w-24f*d, 57f*d, p); p.textAlign = Paint.Align.CENTER
    }

    private fun drawIsland(c: Canvas, w: Float) {
        if (!cfg.island.enabled) return
        val a = AetherRuntime.registry.island.activity
        val width = min(w*.56f, cfg.island.width*d * if (a.title.isBlank()) 1f else 1.5f)
        val height = max(30f*d, cfg.island.height*d); val y = max(7f*d, cfg.island.topOffset*d)
        val r = RectF(w/2-width/2, y, w/2+width/2, y+height)
        glass(c, r, height/2f, 0xB914171D.toInt())
        if (cfg.island.cutoutAware) {
            val cut = min(34f*d, width*.3f); p.color = 0xEE000000.toInt()
            c.drawRoundRect(RectF(w/2-cut/2, r.top-1f, w/2+cut/2, r.bottom+1f), height/2, height/2, p)
        }
        if (a.title.isNotBlank()) { p.color=Color.WHITE; p.typeface=Typeface.DEFAULT_BOLD; p.textSize=10f*d; c.drawText(a.title.take(25),w/2,r.centerY()+4f*d,p); p.typeface=Typeface.DEFAULT }
    }

    private fun drawGrid(c: Canvas, w: Float, h: Float) {
        if (cfg.homeMode == HomeMode.DRAWER_ONLY) return
        val cols=cfg.grid.columns.coerceIn(4,8); val left=cfg.grid.horizontalPadding*d; val gap=cfg.grid.horizontalSpacing*d
        val cell=(w-left*2-gap*(cols-1))/cols; val top=112f*d
        val dockReserve=if(cfg.dock.enabled) cfg.dock.height*d+cfg.dock.bottomPadding*d+18f*d else 8f*d
        val available=max(200f*d,h-top-dockReserve); val rowGap=cfg.grid.verticalSpacing*d
        val rows=cfg.grid.rows.coerceIn(4,10); val rowH=max(72f*d,(available-rowGap*(rows-1))/rows)
        val size=min(cfg.grid.iconSize*d,cell*.66f).coerceIn(34f*d,70f*d)
        order.take(cols*rows).forEachIndexed { i,token ->
            if(token==dragToken) return@forEachIndexed
            val col=i%cols; val row=i/cols; val cx=left+col*(cell+gap)+cell/2f; val y=top+row*(rowH+rowGap)
            val r=RectF(cx-size/2,y,cx+size/2,y+size)
            if(token.startsWith("folder:")) folderIcon(c,r,token.removePrefix("folder:")) else icon(c,AetherRuntime.registry.launcher.icon(token),r)
            if(cfg.grid.showLabels){p.textAlign=Paint.Align.CENTER;p.typeface=Typeface.DEFAULT;p.textSize=10.5f*d;p.color=0xE8FFFFFF.toInt();c.drawText(label(token),cx,r.bottom+17f*d,p)}
        }
    }

    private fun dockPackages(): List<String> = (dock.favorites()+order.filterNot { it.startsWith("folder:") }).distinct().take(cfg.dock.appCount.coerceIn(3,8))

    private fun drawDock(c: Canvas,w:Float,h:Float){
        if(!cfg.dock.enabled)return
        val bottom=h-cfg.dock.bottomPadding*d; val top=bottom-cfg.dock.height*d; val r=RectF(14f*d,top,w-14f*d,bottom)
        glass(c,r,cfg.dock.radius*d,0xA91A1E26.toInt()); val list=dockPackages(); if(list.isEmpty())return
        val slot=r.width()/list.size; val size=min(56f*d,r.height()*.68f)
        list.forEachIndexed{ i,pkg -> val x=r.left+slot*(i+.5f); icon(c,AetherRuntime.registry.launcher.icon(pkg),RectF(x-size/2,r.centerY()-size/2,x+size/2,r.centerY()+size/2)) }
    }

    private fun drawEdge(c:Canvas,w:Float,h:Float){if(!cfg.quickSpace.enabled)return;val x=if(cfg.quickSpace.edge==QuickEdge.RIGHT)w-5f*d else 5f*d;p.color=0xDFFFFFFF.toInt();c.drawRoundRect(RectF(x-2f*d,h*.46f,x+2f*d,h*.55f),3f*d,3f*d,p)}

    private fun glass(c:Canvas,r:RectF,rad:Float,base:Int){
        p.color=base;p.setShadowLayer(22f*d,0f,9f*d,0x70000000);c.drawRoundRect(r,rad,rad,p);p.clearShadowLayer()
        p.shader=LinearGradient(0f,r.top,0f,r.bottom,0x4AFFFFFF,0x0CFFFFFF,Shader.TileMode.CLAMP);c.drawRoundRect(RectF(r.left+1,r.top+1,r.right-1,r.bottom-1),rad,rad,p);p.shader=null
        p.style=Paint.Style.STROKE;p.strokeWidth=max(1f,d);p.color=0x5CFFFFFF.toInt();c.drawRoundRect(RectF(r.left+1,r.top+1,r.right-1,r.bottom-1),rad,rad,p);p.style=Paint.Style.FILL
        p.shader=LinearGradient(0f,r.top,0f,r.top+r.height()*.25f,0x35FFFFFF,0x00FFFFFF,Shader.TileMode.CLAMP);c.drawRoundRect(r,rad,rad,p);p.shader=null
    }

    private fun icon(c:Canvas,dr:Drawable?,r:RectF){dr?:return;dr.setBounds(r.left.toInt(),r.top.toInt(),r.right.toInt(),r.bottom.toInt());dr.draw(c)}
    private fun folderIcon(c:Canvas,r:RectF,id:String){glass(c,r,r.width()*.25f,0xB91D222A.toInt());val pkgs=folders.load().firstOrNull{it.id==id}?.packages.orEmpty().take(4);val s=r.width()*.30f;pkgs.forEachIndexed{i,pkg->{val x=r.left+r.width()*.18f+(i%2)*(s+r.width()*.12f);val y=r.top+r.height()*.18f+(i/2)*(s+r.height()*.12f);icon(c,AetherRuntime.registry.launcher.icon(pkg),RectF(x,y,x+s,y+s))}}
    private fun label(token:String)=if(token.startsWith("folder:"))folders.load().firstOrNull{it.id==token.removePrefix("folder:")}?.name?:"Folder" else AetherRuntime.registry.launcher.apps().firstOrNull{it.packageName==token}?.label?:"App"

    override fun onTouchEvent(e:MotionEvent):Boolean{
        when(e.actionMasked){
            MotionEvent.ACTION_DOWN->{downX=e.x;downY=e.y;dragX=e.x;dragY=e.y;dragOrigin=hit(e.x,e.y);dragTarget=dragOrigin;if(dragOrigin>=0)handler.postDelayed({if(abs(dragX-downX)<16f*d&&abs(dragY-downY)<16f*d){dragToken=order.getOrNull(dragOrigin);invalidate()}},430)}
            MotionEvent.ACTION_MOVE->{dragX=e.x;dragY=e.y;if(dragToken!=null){dragTarget=hit(e.x,e.y);invalidate()}}
            MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->{
                handler.removeCallbacksAndMessages(null);val dx=e.x-downX;val dy=e.y-downY
                if(dragToken!=null){finishDrag();return true}
                if(cfg.quickSpace.enabled&&abs(dx)>cfg.quickSpace.triggerDistance*d&&((cfg.quickSpace.edge==QuickEdge.RIGHT&&downX>width-110f*d)||(cfg.quickSpace.edge==QuickEdge.LEFT&&downX<110f*d))){open(AetherSurface.QUICK);return true}
                if(dy>95f*d&&downY<height*.55f&&!searchLock){searchLock=true;context.startActivity(Intent(context,AetherSearchActivity::class.java));postDelayed({searchLock=false},450);return true}
                if(dy< -95f*d&&cfg.homeMode!=HomeMode.ALL_APPS){open(AetherSurface.DRAWER);return true}
                if(abs(dx)<22f*d&&abs(dy)<22f*d&&dragOrigin>=0){launch(order.getOrNull(dragOrigin));return true}
            }
        };return true
    }

    private fun finishDrag(){
        val token=dragToken?:return;val target=dragTarget
        if(target>=0&&target<order.size&&target!=dragOrigin){
            val other=order[target]
            if(!token.startsWith("folder:")&&!other.startsWith("folder:")&&other!=token){merge(token,other)}
            else {order.remove(token);order.add(target.coerceIn(0,order.size),token);layout.save(order)}
        }
        dragToken=null;dragOrigin=-1;dragTarget=-1;invalidate()
    }

    private fun merge(a:String,b:String){
        val existing=folders.load().firstOrNull{a in it.packages||b in it.packages};val id=existing?.id?:UUID.randomUUID().toString()
        val pkgs=(existing?.packages.orEmpty()+a+b).distinct();val name=existing?.name?:"${label(a)} & ${label(b)}";folders.save(AetherFolder(id,name,pkgs))
        val pos=minOf(order.indexOf(a),order.indexOf(b)).coerceAtLeast(0);order.removeAll{it==a||it==b};order.add(pos.coerceAtMost(order.size),"folder:$id");layout.save(order)
    }

    private fun hit(x:Float,y:Float):Int{
        if(cfg.homeMode==HomeMode.DRAWER_ONLY||y<104f*d)return -1
        val cols=cfg.grid.columns.coerceIn(4,8);val left=cfg.grid.horizontalPadding*d;val gap=cfg.grid.horizontalSpacing*d;val cell=(width-left*2-gap*(cols-1))/cols;val top=112f*d
        val dockReserve=if(cfg.dock.enabled)cfg.dock.height*d+cfg.dock.bottomPadding*d+18f*d else 8f*d;val available=max(200f*d,height-top-dockReserve);val rows=cfg.grid.rows.coerceIn(4,10);val rowGap=cfg.grid.verticalSpacing*d;val rowH=max(72f*d,(available-rowGap*(rows-1))/rows)
        val col=((x-left)/(cell+gap)).toInt();val row=((y-top)/(rowH+rowGap)).toInt();if(col !in 0 until cols||row !in 0 until rows)return -1;val i=row*cols+col;return if(i in order.indices)i else -1
    }

    private fun launch(token:String?){if(token.isNullOrBlank())return;if(token.startsWith("folder:")){open(AetherSurface.FOLDER,token.removePrefix("folder:"));return};history.record(token);AetherRuntime.registry.launcher.launchIntent(token)?.let{context.startActivity(it)}}
    private fun open(surface:String,folderId:String?=null){context.startActivity(Intent(context,AetherSurfaceActivity::class.java).apply{putExtra("surface",surface);folderId?.let{putExtra("folder_id",it)}})}

    private fun drawDragged(c:Canvas){val token=dragToken?:return;val size=70f*d;val r=RectF(dragX-size/2,dragY-size/2,dragX+size/2,dragY+size/2);glass(c,r,23f*d,0xD1222730.toInt());if(token.startsWith("folder:"))folderIcon(c,RectF(r.left+6,r.top+6,r.right-6,r.bottom-6),token.removePrefix("folder:"))else icon(c,AetherRuntime.registry.launcher.icon(token),RectF(r.left+7,r.top+7,r.right-7,r.bottom-7))}
}
