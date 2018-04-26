package org.dv.saves.main

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.hash.HashFunction
import com.google.common.hash.Hashing
import mu.KLogging
import org.springframework.stereotype.Service
import oshi.SystemInfo
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

@Service
class ConfigService(
        private val hasher: HashFunction = Hashing.sha256()
) {

    companion object : KLogging()

    private val objectMapper: ObjectMapper = ObjectMapper()
            .registerModule(KotlinModule())
            .enable(SerializationFeature.INDENT_OUTPUT)
            .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)

    private fun File.ifFile(): File? {
        return if (this.isFile) this else null
    }

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
                        mutableSetOf()
                ))
        )
        saveConfig(configPath, config)

        val globalPath = configPath.resolve(System.getProperty("user.home"))
                .resolve(".saves.cfg")
                .toAbsolutePath()
        globalPath.toFile()
                .printWriter()
                .use { objectMapper.writeValue(it, GlobalConfig(configPath.toString())) }
    }

    private fun machineId(): String {
        return SystemInfo().hardware.processor.processorID
                .also { it -> logger.info { "Id '$it'" } }
                .let { hasher.hashString(it, Charsets.UTF_8) }
                .toString()
    }

    private fun readConfig(backupLocation: String): Config {

        return Paths.get(backupLocation)
                .resolve("config.cfg")
                .toFile()
                .inputStream()
                .use { objectMapper.readValue(it) }
    }

    private fun saveConfig(configPath: Path, config: Config) {
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
        saveConfig(Paths.get(backupLocation), config)
    }

    fun removeSourceDirectory(dir: String) {
        val backupLocation = readGlobal().backupLocation
        val config = readConfig(backupLocation)
        config.machines
                .find { it.machineId == machineId() }
                ?.sourceDirectories
                ?.remove(dir)
        saveConfig(Paths.get(backupLocation), config)
    }

}
