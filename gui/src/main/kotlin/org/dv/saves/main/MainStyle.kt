package org.dv.saves.main

import javafx.scene.paint.Color
import tornadofx.*

class MainStyle : Stylesheet() {
    companion object {
        val invalid by cssclass()
    }

    init {
        invalid { textFill = Color.RED }
    }
}