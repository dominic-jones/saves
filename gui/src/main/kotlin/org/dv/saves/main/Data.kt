package org.dv.saves.main

data class Data(
        val machines: Set<Machine>
)

data class Machine(
        val system: String
)