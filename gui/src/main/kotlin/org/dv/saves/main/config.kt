package org.dv.saves.main

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.thomasnield.rxkotlinfx.additions
import com.github.thomasnield.rxkotlinfx.observeOnFx
import com.google.common.hash.HashFunction
import com.google.common.hash.Hashing
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import mu.KLogging
import org.dv.saves.extensions.isDirectory
import org.dv.saves.extensions.subscribe
import org.dv.saves.extensions.toObservableList
import org.springframework.stereotype.Component
import oshi.SystemInfo
import java.nio.file.Files
import java.nio.file.Paths

@Component
data class ConfigRepository(
        private val objectMapper: ObjectMapper,
        private val globalRepository: GlobalRepository
) {
    private val hasher: HashFunction = Hashing.sha256()

    val machine: Machine = readMachine()

    val machineStore = Store(readMachine())

    fun initData() {

        val configPath = globalRepository.globalConfig.backupLocation.let { Paths.get(it) }

        val config = baseConfig()
        saveConfig(config)

        val globalPath = configPath.resolve(System.getProperty("user.home"))
                .resolve(".saves.cfg")
                .toAbsolutePath()
        globalPath.toFile()
                .printWriter()
                .use { objectMapper.writeValue(it, GlobalConfig(configPath.toString())) }
    }

    private fun baseConfig(): Config {
        val id = machineId()
        return Config(
                setOf(Machine(
                        id,
                        mutableListOf(),
                        mutableSetOf()
                ))
        )
    }

    private fun readConfig(): Config {
        val backupLocation = globalRepository.globalConfig.backupLocation

        return Paths.get(backupLocation)
                .resolve("config.cfg")
                .toFile()
                .let { if (it.length() == 0L) null else it }
                ?.inputStream()
                ?.use { objectMapper.readValue<Config>(it) } ?: baseConfig()
    }

    fun saveMachine(machine: Machine) {

        val config = readConfig()
        config.copy(
                machines = config.machines.filter { it.machineId != machine.machineId }
                        .plus(machine)
                        .toSet()
        ).let { saveConfig(it) }
    }

    private fun saveConfig(config: Config) {

        val backupLocation = globalRepository.globalConfig.backupLocation
        Paths.get(backupLocation)
                .resolve("config.cfg")
                .toFile()
                .printWriter()
                .use { objectMapper.writeValue(it, config) }
    }

    private fun machineId(): String {
        return SystemInfo().hardware.processor.processorID
                .let { hasher.hashString(it, Charsets.UTF_8) }
                .toString()
    }

    private fun readMachine(): Machine {
        return readConfig()
                .machines
                .find { it.machineId == machineId() }!!
    }
}

@Component
data class ConfigViewModel(
        private val configRepository: ConfigRepository,
        private val globalViewModel: GlobalViewModel
) {
    companion object : KLogging()

    val addSourceDirectory: PublishSubject<Unit> = PublishSubject.create()

    val sourceDirectories: ObservableList<SourceDirectory>
    val sourceGames: ObservableList<SourceGame> = FXCollections.observableArrayList()

    init {
        configRepository.machineStore
                .observe()
                .doOnNext { logger.info { "Saving data.. " } }
                .subscribe { configRepository.saveMachine(it) }

        sourceDirectories = addSourceDirectory.map { "directory" }
                .subscribeOn(Schedulers.computation())
                .doOnNext { logger.info { "Adding new sourceDir '$it'" } }
                .startWith(configRepository.machineStore
                        .observe()
                        .take(1)
                        .flatMapIterable { it.sourceDirectories })
                .map { SourceDirectory(it) }
                .toObservableList()

        sourceDirectories.additions()
                .subscribeOn(Schedulers.computation())
                .doOnNext { logger.info { "sourceDir additions '$it'" } }
                .map { it.dir }
                .map { Paths.get(it) }
                .filter { it.isDirectory() }
                .map { configRepository.machine.withSourceDirectory(it) }
                .observeOnFx()
                .subscribe(configRepository.machineStore)

        configRepository.machineStore
                .observe()
                .subscribeOn(Schedulers.computation())
                .doOnNext { logger.info { "Updating sourceGames" } }
                .flatMap { Observable.fromIterable(it.sourceDirectories) }
                .toFlowable(BackpressureStrategy.BUFFER)
                .map { Paths.get(it) }
                .filter { it.isDirectory() }
                .switchMapSingle { src ->
                    Flowable.using(
                            { Files.newDirectoryStream(src) },
                            { reader ->
                                Flowable.fromIterable(reader)
                                        .filter { it.isDirectory() }
                                        .doOnNext { logger.info { "foo $it" } }
                                        .map {
                                            SourceGame(sourceDirectory = src.toString(), gameDirectory = it.toString())
                                        }
                            },
                            { reader -> reader.close() })
                            .withLatestFrom(configRepository.machineStore.observe().toFlowable(BackpressureStrategy.LATEST).map { it.sourceGames }) { disk, cfg ->
                                cfg.find { it.gameDirectory == disk.gameDirectory } ?: disk
                            }
                            .doOnNext { logger.info { "bar $it" } }
                            .sorted(compareBy { it.gameDirectory })
                            .toList()
                }
                .observeOnFx()
                .subscribe {
                    sourceGames.setAll(it)
                }
    }
}