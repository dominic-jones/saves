package org.dv.saves.main

import com.github.thomasnield.rxkotlinfx.actionEvents
import com.github.thomasnield.rxkotlinfx.events
import com.github.thomasnield.rxkotlinfx.subscribeOnFx
import com.github.thomasnield.rxkotlinfx.toBinding
import javafx.event.ActionEvent
import javafx.scene.paint.Color
import mu.KLogging
import tornadofx.*

class MainView : View() {

    companion object : KLogging()

    private val controller: MainController by inject()

    override val root = vbox {
        hbox {
            label("Backup dir")
            textfield {
                useMaxWidth = true
                controller.global.subscribe { text = it.backupLocation }

                events(ActionEvent.ACTION)
                        .map { Unit }
                        .startWith(Unit)
                        .map { text }
                        .distinctUntilChanged()
                        .subscribe(controller.backupPath)

                controller.validPath
                        .subscribeOnFx()
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
        }
        button("Init") {
            enableWhen(
                    controller.validPath
                            .subscribeOnFx()
                            .toBinding()
            )
            actionEvents()
                    .map { Unit }
                    .subscribe(controller.initConfig)
        }
        text {
            controller.pathErrors
                    .subscribeOnFx()
                    .subscribe { text = it }
        }
    }

}