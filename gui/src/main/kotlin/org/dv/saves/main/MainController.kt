package org.dv.saves.main

import io.reactivex.Observable
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import mu.KLogging
import tornadofx.Controller
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

class MainController : Controller() {

    companion object : KLogging()

    private val configService: ConfigService by di()

    val backupPath: Subject<String> = BehaviorSubject.create()

    val initConfig: PublishSubject<Unit> = PublishSubject.create()

    val pathErrors: Observable<String>
    val validPath: Observable<Boolean>

    val global: Observable<GlobalConfig> = Observable.fromCallable { configService.readGlobal() }
            .doOnNext { logger.info { "Found global '$it'" } }
            .filter { it.isValid() }
            .doOnNext { logger.info { "global isValid '$it'" } }
            .cache()

    init {
        val configPath = backupPath.debounce(500, TimeUnit.MILLISECONDS)
                .map { Paths.get(it) }
                .doOnNext { logger.info { "Backup path changed to '$it'" } }
                .cache()

        pathErrors = configPath
                .map { it.toFile() }
                .map {
                    if (!it.exists()) "Path does not exist"
                    else if (!it.isDirectory) "Path is not a directory"
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
    }
}
