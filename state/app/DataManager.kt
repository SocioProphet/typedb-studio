/*
 * Copyright (C) 2021 Vaticle
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.vaticle.typedb.studio.state.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy
import ch.qos.logback.core.util.FileSize
import com.vaticle.typedb.studio.state.common.util.Property
import com.vaticle.typedb.studio.state.common.util.Property.OS.LINUX
import com.vaticle.typedb.studio.state.common.util.Property.OS.MACOS
import com.vaticle.typedb.studio.state.common.util.Property.OS.WINDOWS
import java.lang.System.getProperty
import java.lang.System.getenv
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import kotlin.io.path.exists
import kotlin.io.path.notExists
import mu.KotlinLogging
import org.slf4j.LoggerFactory

class DataManager {

    companion object {
        private val DATA_DIR: Path = when (Property.OS.Current) {
            WINDOWS -> Path.of(getenv("AppData")).resolve("TypeDB Studio")
            MACOS -> Path.of(getProperty("user.home"), "Library", "Application Support").resolve("TypeDB Studio")
            LINUX -> Path.of("var", "lib").resolve("typedb-studio")
        }
        private var LOG_DIR = DATA_DIR.resolve("log")
        private var LOG_FILE = LOG_DIR.resolve("typedb-studio.log").toFile()
        private var PROPERTIES_DIR = DATA_DIR.resolve("properties")
        private var PROPERTIES_FILE = PROPERTIES_DIR.resolve("typedb-studio.properties").toFile()
        private val LOGGER = KotlinLogging.logger {}
    }

    inner class Project {

        private val PROJECT_PATH = "project.path"

        var path: Path?
            get() = properties?.getProperty(PROJECT_PATH)?.let { Path.of(it) }
            set(value) = value?.let { setProperty(PROJECT_PATH, it.toString()) } ?: Unit
    }

    inner class Connection {

        private val CONNECTION_SERVER = "connection.server"
        private val CONNECTION_ADDRESS = "connection.address"
        private val CONNECTION_USERNAME = "connection.username"
        private val CONNECTION_TLS_ENABLED = "connection.tls_enabled"
        private val CONNECTION_CA_CERTIFICATE = "connection.ca_certificate"

        var server: Property.Server?
            get() = properties?.getProperty(CONNECTION_SERVER)?.let { Property.Server.of(it) }
            set(value) = value?.let { setProperty(CONNECTION_SERVER, it.displayName) } ?: Unit
        var address: String?
            get() = properties?.getProperty(CONNECTION_ADDRESS)
            set(value) = value?.let { setProperty(CONNECTION_ADDRESS, it) } ?: Unit
        var username: String?
            get() = properties?.getProperty(CONNECTION_USERNAME)
            set(value) = value?.let { setProperty(CONNECTION_USERNAME, it) } ?: Unit
        var tlsEnabled: Boolean?
            get() = properties?.getProperty(CONNECTION_TLS_ENABLED)?.toBooleanStrictOrNull()
            set(value) = value?.let { setProperty(CONNECTION_TLS_ENABLED, it.toString()) } ?: Unit
        var caCertificate: String?
            get() = properties?.getProperty(CONNECTION_CA_CERTIFICATE)
            set(value) = value?.let { setProperty(CONNECTION_CA_CERTIFICATE, it) } ?: Unit
    }

    var properties: Properties? by mutableStateOf(null)
    var project = Project()
    var connection = Connection()

    private fun setProperty(key: String, value: String) {
        properties?.setProperty(key, value)
        properties?.store(PROPERTIES_FILE.outputStream(), null)
    }

    fun initialise() {
        var isEnabled = false
        try {
            if (DATA_DIR.notExists()) Files.createDirectory(DATA_DIR)
            if (Files.isWritable(DATA_DIR)) {
                initPropertiesFile()
                initLogFile()
                isEnabled = true
            } else {
                LOGGER.error { "Does not have write permission to Application Data Directory: $DATA_DIR" }
            }
        } catch (e: Exception) {
            LOGGER.error { "An exception occurred while setting up Application Data Directory" }
            LOGGER.error { e }
            isEnabled = false
        } finally {
            if (!isEnabled) LOGGER.error { "Application properties and logging may be disabled." }
        }
    }

    private fun initPropertiesFile() {
        if (!PROPERTIES_DIR.exists()) Files.createDirectories(PROPERTIES_DIR)
        if (!PROPERTIES_FILE.exists()) Files.createFile(PROPERTIES_FILE.toPath())
        properties = Properties().also { it.load(PROPERTIES_FILE.inputStream()) }
    }

    private fun initLogFile() {
        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
        val fileAppender = RollingFileAppender<ILoggingEvent>().apply {
            context = loggerContext
            name = "LOGFILE"
            file = LOG_FILE.path
        }
        val encoder = PatternLayoutEncoder().apply {
            context = loggerContext
            pattern = "%date{ISO8601} [%thread] [%-5level] %logger{36} - %msg%n"
        }
        encoder.start()
        fileAppender.encoder = encoder
        val rollingPolicy = SizeAndTimeBasedRollingPolicy<ILoggingEvent>().apply {
            context = loggerContext
            setParent(fileAppender)
            fileNamePattern = "${LOG_FILE.path}-%d{yyyy-MM}.%i.log.gz"
            setMaxFileSize(FileSize.valueOf("50MB"))
            maxHistory = 60
            setTotalSizeCap(FileSize.valueOf("1GB"))
        }
        rollingPolicy.start()
        fileAppender.rollingPolicy = rollingPolicy
        fileAppender.start()
        val rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME) as Logger
        rootLogger.addAppender(fileAppender)
    }
}
