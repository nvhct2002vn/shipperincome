package com.example.shipperincome.data.repository

import android.content.Context
import android.util.Log
import com.example.shipperincome.data.model.RankEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Luu cac lan ghi tong diem rank, moi ngay toi da mot dong. */
class RankRepository(private val dataDir: File) {

    constructor(context: Context) : this(context.filesDir)

    private val file = File(dataDir, FILE_NAME)
    private val lock = Mutex()
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    private val serializer = ListSerializer(RankEntry.serializer())

    suspend fun load(): List<RankEntry> = withContext(Dispatchers.IO) {
        lock.withLock {
            if (!file.exists() || file.length() == 0L) return@withLock emptyList()
            try {
                json.decodeFromString(serializer, file.readText())
            } catch (e: Exception) {
                // Giu lai file hong de con cuu duoc, khong de lan luu sau ghi de.
                Log.e(TAG, "Cannot decode $FILE_NAME", e)
                runCatching {
                    Files.move(
                        file.toPath(),
                        File(dataDir, "rank_data.corrupt-${System.currentTimeMillis()}.json").toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                    )
                }
                emptyList()
            }
        }
    }

    suspend fun save(entries: List<RankEntry>) = withContext(Dispatchers.IO) {
        lock.withLock {
            writeTextAtomically(dataDir, FILE_NAME, json.encodeToString(serializer, entries.sortedBy { it.date }))
        }
    }

    companion object {
        private const val TAG = "RankRepository"
        private const val FILE_NAME = "rank_data.json"
    }
}
