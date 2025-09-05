package com.example.commentanalyze

import android.content.Intent

object ScreenshotPermissionManager {
    private var resultCode: Int? = null
    private var data: Intent? = null
    private var isPermissionUsed = false

    fun setPermission(resultCode: Int, data: Intent) {
        this.resultCode = resultCode
        this.data = data
        this.isPermissionUsed = false  // Fresh permission
        println("ScreenshotPermissionManager: Permission set, not used yet")
    }

    fun hasPermission(): Boolean {
        return resultCode != null && data != null && !isPermissionUsed
    }

    fun markPermissionUsed() {
        isPermissionUsed = true
        println("ScreenshotPermissionManager: Permission marked as used")
    }

    fun clearPermission() {
        resultCode = null
        data = null
        isPermissionUsed = false
        println("ScreenshotPermissionManager: Permission cleared")
    }

    fun getResultCode(): Int? = resultCode
    fun getData(): Intent? = data
}