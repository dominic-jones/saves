package org.dv.saves.main

import com.github.thomasnield.rxkotlinfx.additions
import com.github.thomasnield.rxkotlinfx.removals
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import mu.KLogging
import tornadofx.*
import java.nio.file.Files.exists
import java.nio.file.Files.isDirectory
import java.nio.file.Files.newDirectoryStream
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

class MainController : Controller() {

    companion object : KLogging()

    private val configService: ConfigService by di()

    val backupPath: Subject<String> = BehaviorSubject.create()
    val initConfig: PublishSubject<Unit> = PublishSubject.create()

    val addSourceDirectory: PublishSubject<Unit> = PublishSubject.create()

    val pathErrors: Observable<String>
    val validPath: Observable<Boolean>

    val sourceDirectories: ObservableList<SourceDirectory> = FXCollections.observableArrayList()
    val sourceGames: ObservableList<SourceGame> = FXCollections.observableArrayList()

    val global: Observable<GlobalConfig> = Observable.fromCallable { configService.readGlobal() }
            .doOnNext { logger.info { "Found global '$it'" } }
            .filter { it.isValid() }
            .doOnNext { logger.info { "global isValid '$it'" } }
            .cache()

    init {
        Observable.just(Unit)
                .doOnNext { logger.info { "${this.javaClass.name} started" } }
                .subscribe()

        val configPath = backupPath.debounce(500, TimeUnit.MILLISECONDS)
                .map { Paths.get(it) }
                .doOnNext { logger.info { "Backup path changed to '$it'" } }
                .cache()

        configPath.map { configService.readThisMachine(it.toString()) }
                .doOnNext { logger.info { "This happened '$it'" } }
                .map { it.sourceDirectories.map { SourceDirectory(it) } }
                .subscribe { sourceDirectories.addAll(it) }

        pathErrors = configPath
                .map {
                    if (!it.exists()) "Path does not exist"
                    else if (!it.isDirectory()) "Path is not a directory"
                    else ""
                }

        validPath = pathErrors
                .doOnNext { logger.info { "pathErrors '$it'" } }
                .map {
                    when (it) {
                        "" -> true
                        else -> false
                    }
                }
                .doOnNext { logger.info { "Path is valid: '$it'" } }
                .startWith(false)
                .cache()

        initConfig
                .doOnNext { logger.info { "Initialising config.. " } }
                .withLatestFrom(configPath) { _, path -> configService.initData(path) }
                .subscribe()

        addSourceDirectory.map { "directory" }
                .map { SourceDirectory(it) }
                .subscribe { sourceDirectories += it }

        sourceDirectories.removals()
                .doOnNext { logger.info { "sourceDir removals '$it'" } }
                .doOnNext { configService.removeSourceDirectory(it.dir) }
                .subscribe { src -> sourceGames.removeIf { it.sourceDirectory == src.dir } }

        sourceDirectories.additions()
                .doOnNext { logger.info { "sourceDir additions '$it'" } }
                .map { it.dir }
                .map { Paths.get(it) }
                .filter { it.isDirectory() }
                .doOnNext { configService.addSourceDirectory(it.toString()) }
                .toFlowable(BackpressureStrategy.BUFFER)
                .flatMap { src ->
                    Flowable.using(
                            { newDirectoryStream(src) },
                            { reader -> Flowable.fromIterable<Path>(reader).filter { it.isDirectory() }.map { SourceGame(sourceDirectory = src.toString(), gameDirectory = it.toString()) } },
                            { reader -> reader.close() }
                    )
                }
                .subscribe { sourceGames.add(it) }
    }

    private fun Path.exists(): Boolean = exists(this)
    private fun Path.isDirectory(): Boolean = isDirectory(this)
}
