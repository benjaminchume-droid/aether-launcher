package com.aether.launcher.system

import android.content.Context
import android.content.Intent

object SecurityOverlayController {
    fun show(context: Context, packageName: String) {
        val intent = Intent(context, LockActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            .putExtra(LockActivity.EXTRA_PACKAGE, packageName)
        context.startActivity(intent)
    }
}
