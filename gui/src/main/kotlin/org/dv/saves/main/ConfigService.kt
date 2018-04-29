package org.dv.saves.main

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.hash.HashFunction
import com.google.common.hash.Hashing
import mu.KLogging
import org.dv.saves.extensions.ifFile
import org.springframework.stereotype.Service
import oshi.SystemInfo
import java.nio.file.Path
import java.nio.file.Paths

@Service
class ConfigService(
        private val hasher: HashFunction = Hashing.sha256(),
        private val objectMapper: ObjectMapper
) {

    companion object : KLogging()

    fun readGlobal(): GlobalConfig {
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

    fun initData(configPath: Path) {

        val id = machineId()

        val config = Config(
                setOf(Machine(
                        id,
                        mutableSetOf(),
                        mutableSetOf()
                ))
        )
        saveConfig(config, configPath)

        val globalPath = configPath.resolve(System.getProperty("user.home"))
                .resolve(".saves.cfg")
                .toAbsolutePath()
        globalPath.toFile()
                .printWriter()
                .use { objectMapper.writeValue(it, GlobalConfig(configPath.toString())) }
    }

    private fun machineId(): String {
        return SystemInfo().hardware.processor.processorID
                .let { hasher.hashString(it, Charsets.UTF_8) }
                .toString()
    }

    fun readThisMachine(backupLocation: String = readGlobal().backupLocation): Machine {
        return readConfig(backupLocation)
                .machines
                .find { it.machineId == machineId() }!!
    }

    private fun readConfig(backupLocation: String = readGlobal().backupLocation): Config {

        return Paths.get(backupLocation)
                .resolve("config.cfg")
                .toFile()
                .inputStream()
                .use { objectMapper.readValue(it) }
    }

    private fun saveConfig(config: Config, configPath: Path = Paths.get(readGlobal().backupLocation)) {
        configPath.resolve("config.cfg")
                .toFile()
                .printWriter()
                .use { objectMapper.writeValue(it, config) }
    }

    fun addSourceDirectory(dir: String) {
        val backupLocation = readGlobal().backupLocation
        val config = readConfig(backupLocation)
        config.machines
                .find { it.machineId == machineId() }
                ?.sourceDirectories?.add(dir)
        saveConfig(config, Paths.get(backupLocation))
    }

    fun removeSourceDirectory(dir: String) {
        val backupLocation = readGlobal().backupLocation
        val config = readConfig(backupLocation)
        config.machines
                .find { it.machineId == machineId() }
                ?.sourceDirectories
                ?.remove(dir)
        saveConfig(config, Paths.get(backupLocation))
    }

    fun update(machine: Machine) {
        val config = readConfig()
        val newConfig = config.copy(
                machines = setOf(
                        *config.machines.filter { it.machineId != machineId() }.toTypedArray(),
                        machine
                )
        )
        saveConfig(newConfig)
    }

}
