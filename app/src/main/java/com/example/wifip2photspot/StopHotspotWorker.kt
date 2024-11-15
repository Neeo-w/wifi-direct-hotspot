package com.example.wifip2photspot

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class StopHotspotWorker(
    appContext: Context,
    workerParams: WorkerParameters
): CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        // Stop the hotspot
        return Result.success()
    }
}
