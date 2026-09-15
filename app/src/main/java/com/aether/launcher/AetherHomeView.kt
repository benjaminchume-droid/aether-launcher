package com.aether.launcher

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import com.aether.launcher.engine.island.ActivityType
import com.aether.launcher.settings.AetherSettingsStore
import com.aether.launcher.settings.HomeMode
import com.aether.launcher.settings.QuickEdge
import com.aether.launcher.ui.AetherSearchActivity
import com.aether.launcher.ui.AetherSurface
import com.aether.launcher.ui.AetherSurfaceActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** Aether's polished, wallpaper-first home surface. */
class AetherHomeView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val settings = AetherSettingsStore(context)
    private val layout = AetherHomeLayoutStore(context)
    private val folders = AetherFolderStore(context)
    private val dock = AetherDockStore(context)
    private val history = AetherHistoryStore(context)
    private val handler = Handler(Looper.getMainLooper())
    private val d get() = resources.displayMetrics.density
    private var cfg = settings.load()
    private var order = mutableListOf<String>()
    private var page = 0
    private var pageVisual = 0f
    private var downX = 0f
    private var downY = 0f
    private var dragX = 0f
    private var dragY = 0f
    private var dragToken: String? = null
    private var dragOrigin = -1
    private var dragTarget = -1
    private var pressedToken: String? = null
    private var pressScale = 1f
    private var searchOpening = false
    private var lastWidth = 0

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        order = layout.order(AetherRuntime.registry.launcher.apps().map { it.packageName })
        isClickable = true
    }

    override fun onDraw(c: Canvas) {
        cfg = settings.load()
        syncApps()
        if (lastWidth != width) { lastWidth = width; pageVisual = page.toFloat() }
        drawAtmosphere(c); drawHeader(c); drawIsland(c); drawGrid(c); drawDock(c); drawDots(c); drawEdge(c); drawDragGhost(c)
    }

    private fun syncApps() {
        order = layout.order(AetherRuntime.registry.launcher.apps().map { it.packageName })
        page = page.coerceIn(0, pageCount() - 1)
        pageVisual = pageVisual.coerceIn(0f, pageCount() - 1f)
    }
    private fun pageSize() = cfg.grid.columns.coerceIn(4, 6) * cfg.grid.rows.coerceIn(4, 7)
    private fun pageCount() = max(1, (order.size + pageSize() - 1) / pageSize())

    private fun drawAtmosphere(c: Canvas) {
        val w=width.toFloat(); val h=height.toFloat(); c.drawColor(Color.TRANSPARENT)
        paint.shader=LinearGradient(0f,0f,0f,h,0x14060A10,0x3302070C,Shader.TileMode.CLAMP); c.drawRect(0f,0f,w,h,paint)
        paint.shader=RadialGradient(w*.15f,h*.22f,w*.70f,0x303C6EA0,0x00000000,Shader.TileMode.CLAMP); c.drawRect(0f,0f,w,h,paint)
        paint.shader=RadialGradient(w*.88f,h*.72f,w*.70f,0x241F6A5B,0x00000000,Shader.TileMode.CLAMP); c.drawRect(0f,0f,w,h,paint); paint.shader=null
    }

    private fun drawHeader(c: Canvas) {
        val w=width.toFloat()
        if(cfg.appearance.showTime){paint.textAlign=Paint.Align.LEFT;paint.typeface=Typeface.create("sans",Typeface.NORMAL);paint.textSize=11f*d;paint.color=0xB8FFFFFF.toInt();c.drawText(SimpleDateFormat("EEE  •  d MMM",Locale.getDefault()).format(Date()).uppercase(Locale.getDefault()),24f*d,44f*d,paint);paint.typeface=Typeface.DEFAULT_BOLD;paint.textSize=28f*d;paint.color=Color.WHITE;c.drawText(SimpleDateFormat("HH:mm",Locale.getDefault()).format(Date()),24f*d,75f*d,paint)}
        paint.textAlign=Paint.Align.RIGHT;paint.typeface=Typeface.DEFAULT;paint.textSize=8f*d;paint.letterSpacing=.25f;paint.color=0x80FFFFFF.toInt();if(cfg.appearance.showAetherLabel)c.drawText("A E T H E R",w-24f*d,48f*d,paint);paint.letterSpacing=0f;paint.textAlign=Paint.Align.CENTER
    }

    private fun drawIsland(c: Canvas) {
        if(!cfg.island.enabled)return
        val a=AetherRuntime.registry.island.activity;val active=a.type!=ActivityType.NONE||a.title.isNotBlank();val iw=if(active)min(width*.72f,230f*d)else min(width*.34f,132f*d);val ih=(if(active)42f else 34f)*d;val top=max(5f*d,cfg.island.topOffset*d);val r=RectF(width/2f-iw/2f,top,width/2f+iw/2f,top+ih)
        glass(c,r,ih/2f,if(active)0xCA171F29.toInt()else 0xAA111720.toInt(),true)
        if(cfg.island.cutoutAware){val cut=min(iw*.30f,42f*d);paint.color=0xD9000000.toInt();c.drawRoundRect(RectF(width/2f-cut/2f,r.top-1f,width/2f+cut/2f,r.bottom+1f),ih/2f,ih/2f,paint)}
        paint.color=when(a.type){ActivityType.RECORDING->0xFFFF5F6D.toInt();ActivityType.CALL->0xFF79E59A.toInt();ActivityType.NAVIGATION->0xFF80C9FF.toInt();ActivityType.CHARGING->0xFF8FE98B.toInt();else->0x70FFFFFF};c.drawCircle(r.left+18f*d,r.centerY(),3f*d,paint)
        paint.textAlign=Paint.Align.LEFT;paint.typeface=Typeface.DEFAULT_BOLD;paint.textSize=(if(active)11f else 10f)*d;paint.color=Color.WHITE;c.drawText(if(active)a.title.ifBlank{"Aether Activity"}.take(24)else"AETHER",r.left+28f*d,r.centerY()+4f*d,paint)
        if(active&&a.detail.isNotBlank()){paint.textAlign=Paint.Align.RIGHT;paint.typeface=Typeface.DEFAULT;paint.textSize=8f*d;paint.color=0xBFFFFFFF.toInt();c.drawText(a.detail.take(14),r.right-12f*d,r.centerY()+3f*d,paint)};paint.textAlign=Paint.Align.CENTER
    }

    private fun drawGrid(c: Canvas) {
        val cols=cfg.grid.columns.coerceIn(4,6);val left=cfg.grid.horizontalPadding*d;val gap=cfg.grid.horizontalSpacing*d;val cell=(width-left*2f-gap*(cols-1))/cols;val top=118f*d;val reserve=if(cfg.dock.enabled)cfg.dock.height*d+cfg.dock.bottomPadding*d+38f*d else 18f*d;val rows=cfg.grid.rows.coerceIn(4,7);val available=max(240f*d,height-top-reserve);val rowGap=cfg.grid.verticalSpacing*d;val rowH=max(70f*d,(available-rowGap*(rows-1))/rows);val iconSize=cfg.grid.iconSize.coerceIn(44,70)*d;val base=floor(pageVisual).toInt().coerceIn(0,pageCount()-1);val frac=pageVisual-base
        drawGridPage(c,base,0f,cols,left,gap,cell,top,rowH,iconSize);if(frac>.001f&&base+1<pageCount())drawGridPage(c,base+1,width*(1f-frac),cols,left,gap,cell,top,rowH,iconSize)
    }

    private fun drawGridPage(c:Canvas,pageIndex:Int,offset:Float,cols:Int,left:Float,gap:Float,cell:Float,top:Float,rowH:Float,iconSize:Float){val start=pageIndex*pageSize();val end=min(order.size,start+pageSize());for(i in start until end){val token=order[i];if(token==dragToken)continue;val local=i-start;val col=local%cols;val row=local/cols;val cx=left+col*(cell+gap)+cell/2f+offset;val cy=top+row*(rowH+cfg.grid.verticalSpacing*d)+iconSize*.45f;val scale=if(pressedToken==token)pressScale else 1f;val size=iconSize*scale;val r=RectF(cx-size/2f,cy-size/2f,cx+size/2f,cy+size/2f);if(token.startsWith("folder:"))drawFolder(c,r,token.removePrefix("folder:"))else drawIcon(c,AetherRuntime.registry.launcher.icon(token),r);if(cfg.grid.showLabels){paint.textAlign=Paint.Align.CENTER;paint.typeface=Typeface.DEFAULT;paint.textSize=cfg.grid.labelSize.coerceIn(9,13)*d;paint.color=0xF2FFFFFF.toInt();c.drawText(label(token).take(14),cx,r.bottom+15f*d,paint)};if(i==dragTarget&&dragToken!=null&&offset==0f){paint.style=Paint.Style.STROKE;paint.strokeWidth=2f*d;paint.color=0xBFFFFFFF.toInt();c.drawRoundRect(RectF(cx-iconSize*.58f,cy-iconSize*.66f,cx+iconSize*.58f,cy+iconSize*.66f),24f*d,24f*d,paint);paint.style=Paint.Style.FILL}}}

    private fun drawDock(c:Canvas){if(!cfg.dock.enabled)return;val bottom=height-cfg.dock.bottomPadding*d;val top=bottom-cfg.dock.height*d;val r=RectF(12f*d,top,width-12f*d,bottom);glass(c,r,cfg.dock.radius*d,0xA0101722.toInt(),true);val apps=(dock.favorites()+order.filterNot{it.startsWith("folder:")}).distinct().take(cfg.dock.appCount.coerceIn(4,6));if(apps.isEmpty())return;val slot=r.width()/apps.size;val size=min(56f*d,r.height()*.70f);apps.forEachIndexed{i,pkg->val x=r.left+slot*(i+.5f);val s=if(pressedToken==pkg)pressScale else 1f;val sz=size*s;drawIcon(c,AetherRuntime.registry.launcher.icon(pkg),RectF(x-sz/2f,r.centerY()-sz/2f,x+sz/2f,r.centerY()+sz/2f))}}
    private fun drawDots(c:Canvas){if(pageCount()<=1)return;val y=height-(if(cfg.dock.enabled)cfg.dock.height*d+cfg.dock.bottomPadding*d+7f*d else 12f*d);val total=(pageCount()-1)*8f*d;for(i in 0 until pageCount()){paint.color=if(i==page)0xF2FFFFFF.toInt()else 0x55FFFFFF;c.drawCircle(width/2f+i*8f*d-total/2f,y,if(i==page)3f*d else 2f*d,paint)}}
    private fun drawEdge(c:Canvas){if(!cfg.quickSpace.enabled)return;val x=if(cfg.quickSpace.edge==QuickEdge.RIGHT)width-5f*d else 5f*d;val y=height*.46f;paint.setShadowLayer(12f*d,0f,0f,0x50000000);paint.color=0xD8FFFFFF.toInt();c.drawRoundRect(RectF(x-2f*d,y,x+2f*d,y+cfg.quickSpace.handleLength*d),2f*d,2f*d,paint);paint.clearShadowLayer()}
    private fun glass(c:Canvas,r:RectF,radius:Float,base:Int,sheen:Boolean){paint.style=Paint.Style.FILL;paint.color=base;paint.setShadowLayer(cfg.glass.depth.coerceAtLeast(4)*d,0f,7f*d,0x70000000);c.drawRoundRect(r,radius,radius,paint);paint.clearShadowLayer();paint.shader=LinearGradient(0f,r.top,0f,r.bottom,0x50FFFFFF,0x0AFFFFFF,Shader.TileMode.CLAMP);c.drawRoundRect(RectF(r.left+1f,r.top+1f,r.right-1f,r.bottom-1f),radius,radius,paint);paint.shader=null;if(sheen){paint.shader=LinearGradient(r.left,r.top,r.right,r.bottom,0x2EFFFFFF,0x00FFFFFF,Shader.TileMode.CLAMP);c.drawRoundRect(RectF(r.left+1f,r.top+1f,r.right-1f,r.bottom-1f),radius,radius,paint);paint.shader=null};paint.style=Paint.Style.STROKE;paint.strokeWidth=max(1f,d*.8f);paint.color=0x66FFFFFF;c.drawRoundRect(RectF(r.left+.5f,r.top+.5f,r.right-.5f,r.bottom-.5f),radius,radius,paint);paint.style=Paint.Style.FILL}
    private fun drawIcon(c:Canvas,icon:Drawable?,r:RectF){icon?:return;icon.setBounds(r.left.toInt(),r.top.toInt(),r.right.toInt(),r.bottom.toInt());icon.draw(c)}
    private fun drawFolder(c:Canvas,r:RectF,id:String){glass(c,r,min(r.width(),r.height())*.27f,0xB61B2530.toInt(),true);val apps=folders.load().firstOrNull{it.id==id}?.packages.orEmpty().take(4);val s=r.width()*.29f;apps.forEachIndexed{i,pkg->val x=r.left+r.width()*.17f+(i%2)*(s+r.width()*.12f);val y=r.top+r.height()*.17f+(i/2)*(s+r.height()*.12f);drawIcon(c,AetherRuntime.registry.launcher.icon(pkg),RectF(x,y,x+s,y+s))}}
    private fun label(token:String):String=if(token.startsWith("folder:"))folders.load().firstOrNull{it.id==token.removePrefix("folder:")}?.name?:"Folder"else AetherRuntime.registry.launcher.apps().firstOrNull{it.packageName==token}?.label?:"App"

    override fun onTouchEvent(e:MotionEvent):Boolean{
        when(e.actionMasked){
            MotionEvent.ACTION_DOWN->{downX=e.x;downY=e.y;dragX=e.x;dragY=e.y;pressedToken=hitToken(e.x,e.y);dragOrigin=order.indexOf(pressedToken);if(pressedToken!=null&&dragOrigin>=0){handler.postDelayed({val token=pressedToken?:return@postDelayed;if(dragToken==null&&abs(dragX-downX)<18f*d&&abs(dragY-downY)<18f*d)beginDrag(token)},360)};invalidate()}
            MotionEvent.ACTION_MOVE->{dragX=e.x;dragY=e.y;if(dragToken!=null){dragTarget=hitIndex(e.x,e.y);invalidate()}else if(abs(e.x-downX)>12f*d||abs(e.y-downY)>12f*d){pressedToken=null;handler.removeCallbacksAndMessages(null)}}
            MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->{handler.removeCallbacksAndMessages(null);val dx=e.x-downX;val dy=e.y-downY;if(dragToken!=null){finishDrag();return true};pressedToken=null;if(dy>90f*d&&abs(dy)>abs(dx)*1.15f){if(cfg.homeMode==HomeMode.ALL_APPS&&!searchOpening){searchOpening=true;context.startActivity(Intent(context,AetherSearchActivity::class.java));postDelayed({searchOpening=false},500)}else if(page>0)animatePage(page-1);return true};if(dy< -90f*d&&abs(dy)>abs(dx)*1.15f){if(page<pageCount()-1)animatePage(page+1)else if(cfg.homeMode!=HomeMode.ALL_APPS)open(AetherSurface.DRAWER);return true};val edge=if(cfg.quickSpace.edge==QuickEdge.RIGHT)downX>width-100f*d else downX<100f*d;if(edge&&abs(dx)>cfg.quickSpace.triggerDistance*d&&abs(dx)>abs(dy)*1.1f){open(AetherSurface.QUICK);return true};if(abs(dx)>100f*d&&abs(dx)>abs(dy)*1.2f){animatePage((page+if(dx<0f)1 else -1).coerceIn(0,pageCount()-1));return true};if(abs(dx)<24f*d&&abs(dy)<24f*d){if(islandHit(downX,downY)){open(AetherSurface.NOTIFICATIONS);return true};hitToken(e.x,e.y)?.let(::launch)}}
        return true
    }

    private fun islandHit(x:Float,y:Float):Boolean{val iw=min(width*.72f,230f*d);val ih=42f*d;val top=max(5f*d,cfg.island.topOffset*d);return cfg.island.enabled&&x in(width/2f-iw/2f)..(width/2f+iw/2f)&&y in top..(top+ih)}
    private fun beginDrag(token:String){dragToken=token;dragX=downX;dragY=downY;dragTarget=dragOrigin;animatePress(1.08f);performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);invalidate()}
    private fun finishDrag(){val token=dragToken?:return;val target=dragTarget;if(target in order.indices&&target!=dragOrigin){val other=order[target];when{!token.startsWith("folder:")&&other.startsWith("folder:")->addToFolder(token,other.removePrefix("folder:"));!token.startsWith("folder:")&&!other.startsWith("folder:")&&token!=other->mergeApps(token,other);else->{order.remove(token);order.add(target.coerceIn(0,order.size),token);layout.save(order)}}};dragToken=null;dragOrigin=-1;dragTarget=-1;animatePress(1f);invalidate()}
    private fun addToFolder(pkg:String,id:String){val folder=folders.load().firstOrNull{it.id==id}?:return;if(pkg !in folder.packages){folders.save(folder.copy(packages=folder.packages+pkg));order.remove(pkg);layout.save(order)}}
    private fun mergeApps(a:String,b:String){val existing=folders.load().firstOrNull{a in it.packages||b in it.packages};val id=existing?.id?:UUID.randomUUID().toString();val packages=(existing?.packages.orEmpty()+a+b).distinct();val name=existing?.name?:"${label(a)} & ${label(b)}";folders.save(AetherFolder(id,name,packages));val pos=minOf(order.indexOf(a),order.indexOf(b)).coerceAtLeast(0);order.removeAll{it==a||it==b};order.add(pos.coerceAtMost(order.size),"folder:$id");layout.save(order);page=(pos/pageSize()).coerceIn(0,pageCount()-1)}
    private fun hitToken(x:Float,y:Float):String?{val index=hitIndex(x,y);return if(index in order.indices)order[index]else hitDock(x,y)}
    private fun hitIndex(x:Float,y:Float):Int{val cols=cfg.grid.columns.coerceIn(4,6);val left=cfg.grid.horizontalPadding*d;val gap=cfg.grid.horizontalSpacing*d;val cell=(width-left*2f-gap*(cols-1))/cols;val top=118f*d;val reserve=if(cfg.dock.enabled)cfg.dock.height*d+cfg.dock.bottomPadding*d+38f*d else 18f*d;val available=max(240f*d,height-top-reserve);val rows=cfg.grid.rows.coerceIn(4,7);val rowGap=cfg.grid.verticalSpacing*d;val rowH=max(70f*d,(available-rowGap*(rows-1))/rows);val col=((x-left)/(cell+gap)).toInt();val row=((y-top)/(rowH+rowGap)).toInt();if(col !in 0 until cols||row !in 0 until rows)return -1;val index=page*pageSize()+row*cols+col;return if(index in order.indices)index else -1}
    private fun hitDock(x:Float,y:Float):String?{if(!cfg.dock.enabled)return null;val bottom=height-cfg.dock.bottomPadding*d;val top=bottom-cfg.dock.height*d;if(y !in top..bottom)return null;val r=RectF(12f*d,top,width-12f*d,bottom);val apps=(dock.favorites()+order.filterNot{it.startsWith("folder:")}).distinct().take(cfg.dock.appCount.coerceIn(4,6));if(apps.isEmpty())return null;val slot=r.width()/apps.size;return apps.getOrNull(((x-r.left)/slot).toInt())}
    private fun animatePage(target:Int){val t=target.coerceIn(0,pageCount()-1);if(t==page)return;val from=pageVisual;ValueAnimator.ofFloat(from,t.toFloat()).apply{duration=300L;interpolator=OvershootInterpolator(.72f);addUpdateListener{pageVisual=it.animatedValue as Float;invalidate()};start()};page=t}
    private fun animatePress(target:Float){ValueAnimator.ofFloat(pressScale,target).apply{duration=170L;interpolator=OvershootInterpolator(1.2f);addUpdateListener{pressScale=it.animatedValue as Float;invalidate()};start()}}
    private fun drawDragGhost(c:Canvas){val token=dragToken?:return;val size=cfg.grid.iconSize.coerceIn(48,72)*d;val r=RectF(dragX-size/2f,dragY-size/2f,dragX+size/2f,dragY+size/2f);glass(c,r,24f*d,0xD61A2430.toInt(),true);if(token.startsWith("folder:"))drawFolder(c,RectF(r.left+7f,r.top+7f,r.right-7f,r.bottom-7f),token.removePrefix("folder:"))else drawIcon(c,AetherRuntime.registry.launcher.icon(token),RectF(r.left+7f,r.top+7f,r.right-7f,r.bottom-7f))}
    private fun launch(token:String){if(token.startsWith("folder:")){open(AetherSurface.FOLDER,token.removePrefix("folder:"));return};history.record(token);AetherRuntime.registry.launcher.launchIntent(token)?.let{context.startActivity(it)}}
    private fun open(surface:String,folderId:String?=null){context.startActivity(Intent(context,AetherSurfaceActivity::class.java).apply{putExtra("surface",surface);if(folderId!=null)putExtra("folder_id",folderId)})}
}
