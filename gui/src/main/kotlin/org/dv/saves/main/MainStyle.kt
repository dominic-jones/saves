package org.dv.saves.main

import javafx.scene.paint.Color
import tornadofx.Stylesheet
import tornadofx.cssclass

class MainStyle : Stylesheet() {
    companion object {
        val invalid by cssclass()
    }

    init {
        invalid { textFill = Color.RED }
    }
}