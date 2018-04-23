package org.dv.saves.main

data class Config(
        val machines: Set<Machine>
)

data class Machine(
        val system: String
)