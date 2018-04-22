package org.dv.saves.main

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT
import io.reactivex.Observable
import io.reactivex.rxkotlin.zipWith
import io.reactivex.subjects.PublishSubject
import mu.KLogging
import org.springframework.stereotype.Service
import tornadofx.*
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

@Service
class MainController : Controller() {

    companion object : KLogging()

    val backupPath: PublishSubject<String> = PublishSubject.create()
    val configFile: Observable<Path>

    val initConfig: PublishSubject<Unit> = PublishSubject.create()

    val pathErrors: Observable<String>
    val validPath: Observable<Boolean>
    val validConfig: Observable<Boolean>

    val objectMapper = ObjectMapper()
            .enable(INDENT_OUTPUT)

    init {
        val configPath = backupPath.debounce(500, TimeUnit.MILLISECONDS)
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


        configFile = configPath.map { it.resolve("config.json") }
        validConfig = configFile
                .map {
                    it.toFile().exists()
                }.startWith(false)

        initConfig
                .doOnNext { logger.info { "Initialising config.. " } }
                .map { Data(setOf(Machine("test"))) }
                .zipWith(configFile) { data, path -> path.toFile().printWriter().use { objectMapper.writeValue(it, data) } }
                .subscribe()
    }
}
