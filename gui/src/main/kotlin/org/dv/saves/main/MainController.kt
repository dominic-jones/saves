package org.dv.saves.main

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import mu.KLogging
import org.springframework.stereotype.Service
import tornadofx.*
import java.io.File
import java.util.concurrent.TimeUnit

@Service
class MainController : Controller() {

    companion object : KLogging()

    val backupBath: PublishSubject<String> = PublishSubject.create()

    val pathErrors: Observable<String>
    val validPath: Observable<Boolean>

    init {
        pathErrors = backupBath.debounce(500, TimeUnit.MILLISECONDS)
                .doOnNext { logger.info { "Backup path changed to '$it'" } }
                .map { File(it) }
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

    }
}
