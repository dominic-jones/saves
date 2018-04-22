package org.dv.saves

import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage
import tornadofx.*

fun main(args: Array<String>) {
    Application.launch(Saves::class.java, *args)
}

class Saves : Application() {
    override fun start(primaryStage: Stage) {
        primaryStage.title = "org.dv.saves.Saves"
        primaryStage.scene = Scene(
                find(MainView::class).root,
                1024.0,
                768.0
        )
        primaryStage.show()
    }

}