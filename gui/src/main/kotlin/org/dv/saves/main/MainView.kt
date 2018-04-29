package org.dv.saves.main

import com.github.thomasnield.rxkotlinfx.actionEvents
import com.github.thomasnield.rxkotlinfx.events
import com.github.thomasnield.rxkotlinfx.toBinding
import com.github.thomasnield.rxkotlinfx.toObservable
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
    private val globalViewModel: GlobalViewModel by di()
    private val configViewModel: ConfigViewModel by di()

    override val root = vbox {
        vbox {
            hbox {
                label("Backup dir")
                textfield {
                    prefWidth = 400.0
                    globalViewModel.backupLocation.subscribe { text = it }

                    events(ActionEvent.ACTION)
                            .map { text }
                            .subscribe(globalViewModel.backupLocationChanged)

                    globalViewModel.pathErrors
                            .subscribe {
                                when (it) {
                                    PathError.NONE -> style = null
                                    else -> style {
                                        textBoxBorder = Color.RED
                                        focusColor = Color.RED
                                    }
                                }
                            }
                }
            }
            button("Init") {
                enableWhen(
                        globalViewModel.pathErrors.map { it != PathError.NONE }.toBinding()
                )
                actionEvents()
                        .map { Unit }
                        .subscribe(controller.initConfig)
            }
            text {
                bind(globalViewModel.pathErrors
                        .filter { it != PathError.NONE }
                        .map { it.name }
                        .toBinding())
            }
        }
        vbox {
            listview(configViewModel.sourceDirectories) {
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
                        .subscribe(configViewModel.addSourceDirectory)
            }
        }
        tableview(configViewModel.sourceGames) {
            prefHeight = 140.0
            selectionModel.selectedItemProperty()
                    .toObservable()
                    .subscribe(controller.selectedGame)
            readonlyColumn("SourceDir", SourceGame::sourceDirectory)
            readonlyColumn("GameDir", SourceGame::gameDirectory)
            column("Glob", SourceGame::glob).makeEditable().setOnEditCommit {
                controller.onCellEdit.onNext(it.rowValue.copy(glob = it.newValue))
            }
        }
        tableview(controller.gameFiles) {
            prefHeight = 140.0
            readonlyColumn("Save File", GameFile::file)
        }
    }

}