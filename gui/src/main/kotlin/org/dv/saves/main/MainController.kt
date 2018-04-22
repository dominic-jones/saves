package org.dv.saves.main

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import mu.KLogging
import org.springframework.stereotype.Service
import tornadofx.*
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

@Service
class MainController : Controller() {

    companion object : KLogging()

    val backupBath: PublishSubject<String> = PublishSubject.create()

    val initConfig: PublishSubject<Unit> = PublishSubject.create()

    val pathErrors: Observable<String>
    val validPath: Observable<Boolean>
    val validConfig: Observable<Boolean>

    init {
        val configPath = backupBath.debounce(500, TimeUnit.MILLISECONDS)
                .doOnNext { logger.info { "Backup path changed to '$it'" } }
                .map { Paths.get(it) }

        pathErrors = configPath
                .map { it.toFile() }
                .map {
                    if (!it.exists()) "Path does not exist"
                    else if (!it.isDirectory) "Path is not a directory"
                    else ""
                }

        validPath = pathErrors.map {
            when (it) {
                "" -> true
                else -> false
            }
        }.doOnNext { logger.info { "Path is valid: '$it'" } }
                .startWith(false)

        validConfig = configPath.map { it.resolve("config.json") }
                .map {
                    it.toFile().exists()
                }.startWith(false)

        initConfig
                .doOnNext { logger.info { "Initialising config.. " } }
                .subscribe()
    }
}
