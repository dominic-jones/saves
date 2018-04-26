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
        val machineId: String,
        val sourceDirectories: MutableSet<String>
)

data class SourceDirectory(
        val dir: String
) {
    fun isValid() = Paths.get(dir).toFile().isDirectory
}

data class SourceGame(
        val sourceDirectory: String,
        val gameDirectory: String
)
