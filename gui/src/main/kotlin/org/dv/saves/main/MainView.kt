package org.dv.saves.main

import com.github.thomasnield.rxkotlinfx.actionEvents
import com.github.thomasnield.rxkotlinfx.events
import com.github.thomasnield.rxkotlinfx.subscribeOnFx
import com.github.thomasnield.rxkotlinfx.toBinding
import javafx.event.ActionEvent
import javafx.scene.control.cell.TextFieldListCell
import javafx.scene.paint.Color
import javafx.util.Callback
import javafx.util.StringConverter
import mu.KLogging
import tornadofx.*

class MainView : View() {

    companion object : KLogging()

    private val controller: MainController by inject()

    override val root = vbox {
        vbox {
            hbox {
                label("Backup dir")
                textfield {
                    prefWidth = 400.0
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
        vbox {
            listview(controller.sourceDirectories) {
                isEditable = true
                prefHeight = 140.0
                cellFactory = Callback {
                    object : TextFieldListCell<SourceDirectory>(
                            object : StringConverter<SourceDirectory>() {
                                override fun toString(dir: SourceDirectory): String = dir.dir
                                override fun fromString(string: String): SourceDirectory = SourceDirectory(string)
                            }
                    ) {
                        override fun updateItem(item: SourceDirectory?, empty: Boolean) {
                            super.updateItem(item, empty)
                            item ?: return
                            toggleClass(MainStyle.invalid, !item.isValid())
                        }
                    }
                }
            }
            button("Add") {
                actionEvents()
                        .map { Unit }
                        .subscribe(controller.addSourceDirectory)
            }
        }
        tableview(controller.sourceGames) {
            readonlyColumn("SourceDir", SourceGame::sourceDirectory)
            readonlyColumn("GameDir", SourceGame::gameDirectory)
        }
    }

}