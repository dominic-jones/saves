package org.dv.saves

import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage
import org.dv.saves.config.Config
import org.dv.saves.main.MainView
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import tornadofx.*
import javax.inject.Inject
import kotlin.reflect.KClass

fun main(args: Array<String>) {
    Application.launch(Saves::class.java, *args)
}

class Saves : Application() {

    @Inject
    lateinit var mainView: MainView

    init {
        val context = AnnotationConfigApplicationContext(Config::class.java)
        FX.dicontainer = object : DIContainer {
            override fun <T : Any> getInstance(type: KClass<T>): T = context.getBean(type.java)
        }
        context.autowireCapableBeanFactory.autowireBean(this)
    }

    override fun start(primaryStage: Stage) {
        primaryStage.title = "org.dv.saves.Saves"
        primaryStage.scene = Scene(
                mainView.root,
                1024.0,
                768.0
        )
        primaryStage.show()
    }

}