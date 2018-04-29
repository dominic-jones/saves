package org.dv.saves.extensions

import java.io.File

fun File.ifFile(): File? {
    return if (this.isFile) this else null
}