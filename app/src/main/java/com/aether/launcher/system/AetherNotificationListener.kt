package com.aether.launcher.system

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.aether.launcher.AetherRuntime
import com.aether.launcher.engine.island.ActivityType
import com.aether.launcher.engine.island.IslandActivity
import com.aether.launcher.engine.notification.AetherNotification

class AetherNotificationListener : NotificationListenerService() {
 override fun onListenerConnected(){AetherRuntime.initialize(applicationContext);activeNotifications?.forEach(::ingest)}
 override fun onNotificationPosted(sbn:StatusBarNotification)=ingest(sbn)
 override fun onNotificationRemoved(sbn:StatusBarNotification){if(!AetherRuntime.isInitialized())return;NotificationReplyBridge.forget(sbn);AetherRuntime.registry.notifications.remove(sbn.id,sbn.packageName);val cur=AetherRuntime.registry.island.activity;if(cur.key==keyFor(sbn)){AetherRuntime.registry.notifications.latest()?.let{n->AetherRuntime.registry.island.setActivity(IslandActivity(ActivityType.NOTIFICATION,n.title.ifBlank{"Activity"},n.text,0,keyFor(n.id,n.packageName)))}?:AetherRuntime.registry.island.clearActivity()};AetherOverlayService.refreshFromSystem()}
 private fun ingest(sbn:StatusBarNotification){if(!AetherRuntime.isInitialized())AetherRuntime.initialize(applicationContext);val n=sbn.notification;val title=n.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty();val text=n.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty();val type=when{n.category==Notification.CATEGORY_CALL->ActivityType.CALL;n.category==Notification.CATEGORY_ALARM||n.category==Notification.CATEGORY_EVENT->ActivityType.TIMER;n.category==Notification.CATEGORY_TRANSPORT||n.category==Notification.CATEGORY_PROGRESS->ActivityType.MEDIA;n.category==Notification.CATEGORY_SERVICE->ActivityType.RECORDING;else->ActivityType.NOTIFICATION};val key=keyFor(sbn);AetherRuntime.registry.notifications.add(AetherNotification(sbn.id,sbn.packageName,title,text,sbn.postTime));NotificationReplyBridge.remember(sbn);AetherRuntime.registry.island.setActivity(IslandActivity(type,title.ifBlank{"Activity"},text,if(type==ActivityType.CALL)20 else 0,key));AetherOverlayService.refreshFromSystem()}
 private fun keyFor(sbn:StatusBarNotification)="${sbn.packageName}:${sbn.id}"
 private fun keyFor(id:Int,packageName:String)="$packageName:$id"
}
