package org.dv.saves.main

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
