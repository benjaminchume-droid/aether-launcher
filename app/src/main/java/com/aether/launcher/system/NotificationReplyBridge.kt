package com.aether.launcher.system

import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.service.notification.StatusBarNotification

/** Keeps only the transient reply action needed by the live capsule. */
object NotificationReplyBridge {
    private data class ReplyTarget(val pendingIntent: PendingIntent, val input: RemoteInput)
    private val targets = mutableMapOf<String, ReplyTarget>()

    @Synchronized
    fun remember(sbn: StatusBarNotification) {
        val key = keyFor(sbn)
        val action = sbn.notification.actions.orEmpty()
            .firstOrNull { it.remoteInputs?.any { input -> input.allowFreeFormInput } == true }
            ?: run { targets.remove(key); return }
        val input = action.remoteInputs.first { it.allowFreeFormInput }
        targets[key] = ReplyTarget(action.actionIntent, input)
        if (targets.size > 64) targets.remove(targets.keys.first())
    }

    @Synchronized fun forget(sbn: StatusBarNotification) { targets.remove(keyFor(sbn)) }

    @Synchronized
    fun canReply(key: String): Boolean = targets.containsKey(key)

    fun reply(key: String, text: String): Boolean {
        if (text.isBlank()) return false
        val target = synchronized(this) { targets[key] } ?: return false
        return runCatching {
            val fillIn = Intent()
            val results = Bundle()
            results.putCharSequence(target.input.resultKey, text)
            RemoteInput.addResultsToIntent(arrayOf(target.input), fillIn, results)
            target.pendingIntent.send(null, 0, fillIn)
            true
        }.getOrDefault(false)
    }

    private fun keyFor(sbn: StatusBarNotification) = "${sbn.packageName}:${sbn.id}"
}
