package org.dv.saves.main

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KLogging
import org.springframework.stereotype.Service
import oshi.SystemInfo
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest

@Service
class ConfigService(
        private val messageDigest: MessageDigest = MessageDigest.getInstance("SHA-256")
) {

    companion object : KLogging()

    private val objectMapper: ObjectMapper = ObjectMapper()
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

        val id = SystemInfo().hardware.processor.processorID
                .toByteArray()
                .let { messageDigest.digest(it) }
                .toString()

        val config = Config(setOf(Machine(id)))
        configPath.resolve("config.cfg")
                .toFile()
                .printWriter()
                .use { objectMapper.writeValue(it, config) }

        val globalPath = configPath.resolve(System.getProperty("user.home"))
                .resolve(".saves.cfg")
                .toAbsolutePath()
        globalPath.toFile()
                .printWriter()
                .use { objectMapper.writeValue(it, GlobalConfig(configPath.toString())) }
    }

}
