package org.dv.saves.main

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.rxkotlin.zipWith
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import mu.KLogging
import org.dv.saves.extensions.isDirectory
import tornadofx.*
import java.nio.file.Files.exists
import java.nio.file.Files.isDirectory
import java.nio.file.Files.newDirectoryStream
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit.MILLISECONDS

class MainController : Controller() {

    companion object : KLogging()

    private val configService: ConfigService by di()

    private val localConfig: Subject<Machine> = BehaviorSubject.create()

    val initConfig: PublishSubject<Unit> = PublishSubject.create()

    val gameFiles: ObservableList<GameFile> = FXCollections.observableArrayList()

    val selectedGame: PublishSubject<SourceGame> = PublishSubject.create()

    val onCellEdit: PublishSubject<SourceGame> = PublishSubject.create()

    init {
        Observable.just(Unit)
                .doOnNext { logger.info { "${this.javaClass.name} started" } }
                .subscribe()

        localConfig.distinct()
                .doOnNext { configService.update(it) }
                .doOnNext { logger.info { "Saving config '$it'.. " } }
                .subscribe()

        onCellEdit
                .doOnNext { logger.info { "Updating sourceGame '$it'" } }
                .zipWith(localConfig) { sourceGame, machine ->
                    machine.sourceGames.removeIf { sourceGame.gameDirectory == it.gameDirectory }
                    machine.sourceGames.add(sourceGame)
                    machine
                }.subscribe(localConfig)

        selectedGame.debounce(500, MILLISECONDS)
                .doOnNext { logger.info { "Game selected '$it'" } }
                .doOnNext { gameFiles.clear(); }
                .toFlowable(BackpressureStrategy.BUFFER)
                .flatMap { src ->
                    Flowable.using(
                            { newDirectoryStream(Paths.get(src.gameDirectory), src.glob) },
                            { reader -> Flowable.fromIterable(reader).filter { !it.isDirectory() } },
                            { reader -> reader.close() }
                    )
                }
                .map { GameFile(file = it.toString()) }
                .subscribe { gameFiles.add(it) }
    }
}
