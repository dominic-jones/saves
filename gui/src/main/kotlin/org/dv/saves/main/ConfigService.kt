package org.dv.saves.main

import mu.KLogging
import org.springframework.stereotype.Service
import oshi.SystemInfo
import java.security.MessageDigest

@Service
class ConfigService(
        private val messageDigest: MessageDigest = MessageDigest.getInstance("SHA-256")
) {

    companion object : KLogging()

    fun initData(): Config {

        val id = SystemInfo().hardware.processor.processorID
                .toByteArray()
                .let { messageDigest.digest(it) }
                .toString()

        return Config(setOf(Machine(id)))
    }

}
