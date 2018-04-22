package org.dv.saves.main

import org.springframework.stereotype.Component
import tornadofx.*

@Component
class MainView : View() {
    override val root = stackpane {
        label("Test")
    }

}