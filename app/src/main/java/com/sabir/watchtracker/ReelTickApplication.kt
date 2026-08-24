package com.sabir.watchtracker

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.sabir.watchtracker.data.backup.ReelTickBackupManager
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReelTickApplication : Application(), DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val safetyBackupFile: File by lazy {
        File(filesDir, "reeltick-safety-backup.json")
    }

    private val backupManager: ReelTickBackupManager by lazy {
        ReelTickBackupManager(this)
    }

    override fun onCreate() {
        super<Application>.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        super<DefaultLifecycleObserver>.onStop(owner)
        createAutomaticSafetyBackup()
    }

    private fun createAutomaticSafetyBackup() {
        scope.launch {
            runCatching {
                val json = backupManager.createBackupJson()
                val preview = backupManager.inspectBackupJson(json)

                // Never replace a good safety backup with an empty database snapshot.
                if (
                    preview.titleCount == 0 &&
                    preview.episodeCount == 0 &&
                    preview.rewatchCount == 0 &&
                    preview.customListCount == 0
                ) {
                    return@runCatching
                }

                val tempFile = File(
                    filesDir,
                    "reeltick-safety-backup.json.tmp"
                )

                tempFile.writeText(json)

                if (!tempFile.renameTo(safetyBackupFile)) {
                    safetyBackupFile.delete()
                    if (!tempFile.renameTo(safetyBackupFile)) {
                        error("Unable to finalize automatic safety backup.")
                    }
                }
            }
        }
    }
}
