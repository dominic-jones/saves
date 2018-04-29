package org.dv.saves.main

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import org.dv.saves.extensions.exists
import org.dv.saves.extensions.ifFile
import org.dv.saves.extensions.isDirectory
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

@Repository
class GlobalRepository(private val objectMapper: ObjectMapper) {
    val globalConfig: GlobalConfig

    init {
        globalConfig = readGlobal()
    }

    private fun readGlobal(): GlobalConfig {
        val globalPath = Paths.get(System.getProperty("user.home"))
                .resolve(".saves.cfg")
                .toFile()

        return globalPath
                .ifFile()
                ?.inputStream()
                ?.use {
                    objectMapper.readValue<GlobalConfig>(it)
                } ?: GlobalConfig("")
    }
}

enum class PathError {
    DIR_NOT_FOUND,
    IS_NOT_DIR,
    NONE
}

@Component
data class GlobalViewModel(private val globalRepository: GlobalRepository) {

    val backupLocationChanged: Subject<String> = BehaviorSubject.create()

    private val globalConfigState: Subject<GlobalConfig> = BehaviorSubject.create()

    val backupLocation: Observable<String> = globalConfigState
            .observeOn(Schedulers.io())
            .startWith(globalRepository.globalConfig)
            .doOnNext { MainController.logger.info { "Loading global config '$it'" } }
            .map { it.backupLocation }
            .cache()

    val pathErrors: Observable<PathError> = backupLocation
            .map { Paths.get(it) }
            .map {
                if (!it.exists()) PathError.DIR_NOT_FOUND
                else if (!it.isDirectory()) PathError.IS_NOT_DIR
                PathError.NONE
            }

    init {
        backupLocationChanged
                .distinctUntilChanged()
                .debounce(500, TimeUnit.MILLISECONDS)
                .doOnNext { MainController.logger.info { "Backup path changing to '$it'" } }
                .map { globalRepository.globalConfig.copy(backupLocation = it) }
                .subscribe(globalConfigState)
    }
}