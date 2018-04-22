package org.dv.saves.main

import com.github.thomasnield.rxkotlinfx.actionEvents
import com.github.thomasnield.rxkotlinfx.events
import com.github.thomasnield.rxkotlinfx.toBinding
import io.reactivex.rxkotlin.zipWith
import javafx.scene.input.KeyEvent
import javafx.scene.paint.Color
import org.springframework.stereotype.Component
import tornadofx.*

@Component
class MainView : View() {

    private val controller: MainController by inject()

    override val root = vbox {
        hbox {
            label("Backup dir")
            textfield {
                events(KeyEvent.KEY_RELEASED)
                        .map { text }
                        .distinctUntilChanged()
                        .subscribe(controller.backupBath)

                controller.validPath
                        .subscribe {
                            when (it) {
                                true -> style = null
                                false -> style {
                                    textBoxBorder = Color.RED
                                    focusColor = Color.RED
                                }
                            }
                        }
            }
            text {
                controller.pathErrors
                        .subscribe { text = it }
            }
            button("Init") {
                enableWhen(
                        controller.validPath
                                .zipWith(controller.validConfig) { path, config -> path && !config }
                                .toBinding()
                )
                actionEvents()
                        .map { Unit }
                        .subscribe(controller.initConfig)
            }
        }
    }

}