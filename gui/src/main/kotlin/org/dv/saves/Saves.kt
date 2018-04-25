package org.dv.saves

import javafx.application.Application
import javafx.stage.Stage
import org.dv.saves.config.Config
import org.dv.saves.main.MainView
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import tornadofx.App
import tornadofx.DIContainer
import tornadofx.FX
import kotlin.reflect.KClass

fun main(args: Array<String>) {
    Application.launch(Saves::class.java, *args)
}

class Saves : App(MainView::class) {
    init {
        val context = AnnotationConfigApplicationContext(Config::class.java)
        FX.dicontainer = object : DIContainer {
            override fun <T : Any> getInstance(type: KClass<T>): T = context.getBean(type.java)
        }
    }

    override fun start(stage: Stage) {
        super.start(stage)
        stage.width = 1024.0
        stage.height = 768.0
    }
}
