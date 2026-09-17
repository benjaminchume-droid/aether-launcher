package com.aether.launcher.system

import android.content.Context
import android.content.Intent

/**
 * Bridges accessibility app transitions to the LockActivity surface.
 * Only launches when the package is protected and not session-unlocked.
 */
object SecurityOverlayController {

    @Synchronized
    fun show(context: Context, packageName: String) {
        if (packageName.isBlank()) return
        if (packageName == context.packageName) return

        val intent = Intent(context, LockActivity::class.java).apply {
            putExtra(LockActivity.EXTRA_PACKAGE, packageName)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION
            )
        }
        runCatching { context.startActivity(intent) }
    }
}
