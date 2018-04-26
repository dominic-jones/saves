package org.dv.saves.main

import java.nio.file.Paths

data class GlobalConfig(
        val backupLocation: String = ""
) {
    fun isValid() = backupLocation != ""
}

data class Config(
        val machines: Set<Machine>
)

data class Machine(
        val system: String
)

data class SourceDirectory(
        val dir: String
) {
    fun isValid() = Paths.get(dir).toFile().isDirectory
}
