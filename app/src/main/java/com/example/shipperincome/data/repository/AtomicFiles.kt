package com.example.shipperincome.data.repository

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Ghi ra file tam roi doi ten de file that khong bao gio bi ghi do dang (vd. app bi tat giua chung). */
internal fun writeTextAtomically(dir: File, fileName: String, content: String) {
    val target = File(dir, fileName)
    val tmp = File(dir, "$fileName.tmp")
    tmp.outputStream().use { out ->
        out.write(content.toByteArray())
        out.fd.sync()
    }
    Files.move(
        tmp.toPath(),
        target.toPath(),
        StandardCopyOption.REPLACE_EXISTING,
        StandardCopyOption.ATOMIC_MOVE
    )
}
