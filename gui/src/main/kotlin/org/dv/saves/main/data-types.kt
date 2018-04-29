package org.dv.saves.main

import java.nio.file.Path
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
        val sourceDirectories: List<String>,
        val sourceGames: MutableSet<SourceGame>
) {
    fun withSourceDirectory(src: Path): Machine {

        return copy(sourceDirectories = sourceDirectories.plus(src.toString()))
    }

    fun withoutSourceDirectory(src: String): Machine {
        return copy(sourceDirectories = sourceDirectories.minus(src))
    }
}

data class SourceDirectory(
        val dir: String
) {
    fun isValid() = Paths.get(dir).toFile().isDirectory
}

data class SourceGame(
        val sourceDirectory: String,
        val gameDirectory: String,
        var glob: String = "*"
)

data class GameFile(
        val file: String
)
