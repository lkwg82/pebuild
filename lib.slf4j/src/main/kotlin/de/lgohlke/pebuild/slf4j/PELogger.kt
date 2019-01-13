package de.lgohlke.pebuild.slf4j

import org.slf4j.Logger
import org.slf4j.Marker
import org.slf4j.event.Level
import org.slf4j.event.Level.*
import org.slf4j.helpers.MessageFormatter
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class PELogger(private val clazz: String,
               private val outStream: OutputStream = System.err,
               private val properties: Properties = System.getProperties()) : Logger {

    val logPrefix = "org.slf4j.simpleLogger"
    private val level = lazyInitLogLevel()
    private val dateFormat = SimpleDateFormat("HH:mm:ss:SSS")

    private val formattedDate: String
        get() {
            val now = Date()
            synchronized(dateFormat) {
                return dateFormat.format(now)
            }
        }

    private fun log(level: Level,
                    message: String,
                    t: Throwable?) {
        if (!isLevelEnabled(level)) {
            return
        }

        val buf = StringBuilder(32)
                .append(formattedDate)
                .append(" [")
                .append(Thread.currentThread().name)
                .append("] ")
                .append(level)
                .append(' ')
                .append(clazz)
                .append(" - ")
                .append(message)

        printOut(buf.toString())
    }

    private fun printOut(line: String) {
        outStream.write((line + "\n").toByteArray())
    }

    private fun isLevelEnabled(level: Level): Boolean {
        val selected = this.level.toInt()
        return selected <= level.toInt()
    }

    private fun lazyInitLogLevel(): Level {

        val (_, levelGlobal) = detectLogLevel("$logPrefix.defaultLogLevel")
        val (foundClazz, levelClazz) = detectLogLevel("$logPrefix.log.$clazz")

        var computelLevel = levelGlobal
        if (foundClazz) {
            computelLevel = levelClazz
        }
//        printOut("LOGGGING  ${this.clazz} level: $computelLevel")

        return computelLevel
    }

    private fun detectLogLevel(key: String): Pair<Boolean, Level> {
        val property = properties.getProperty(key)
        return if (null == property) {
            Pair(false, WARN)
        } else {
            try {
                Pair(true, Level.valueOf(property))
            } catch (e: Exception) {
                printOut("illegal log level: $property set to WARN")
                Pair(false, WARN)
            }
        }
    }

    override fun getName(): String {
        throw TODO()
    }

    internal inner class TODO : RuntimeException()

    override fun isTraceEnabled(): Boolean {
        throw TODO()
    }

    override fun trace(msg: String) {
        throw TODO()
    }

    override fun trace(format: String,
                       arg: Any) {
        throw TODO()
    }

    override fun trace(format: String,
                       arg1: Any,
                       arg2: Any) {
        throw TODO()
    }

    override fun trace(format: String,
                       vararg arguments: Any) {
        throw TODO()
    }

    override fun trace(msg: String,
                       t: Throwable) {
        throw TODO()
    }

    override fun isTraceEnabled(marker: Marker): Boolean {
        throw TODO()
    }

    override fun trace(marker: Marker,
                       msg: String) {
        throw TODO()
    }

    override fun trace(marker: Marker,
                       format: String,
                       arg: Any) {
        throw TODO()
    }

    override fun trace(marker: Marker,
                       format: String,
                       arg1: Any,
                       arg2: Any) {
        throw TODO()
    }

    override fun trace(marker: Marker,
                       format: String,
                       vararg argArray: Any) {
        throw TODO()
    }

    override fun trace(marker: Marker,
                       msg: String,
                       t: Throwable) {
        throw TODO()
    }

    override fun isDebugEnabled(): Boolean {
        return isLevelEnabled(DEBUG)
    }

    override fun debug(msg: String) {
        log(DEBUG, msg, null)
    }

    override fun debug(format: String,
                       arg: Any) {
        logAndFormat(DEBUG, format, arg, null)
    }

    override fun debug(format: String,
                       arg1: Any,
                       arg2: Any) {
        logAndFormat(DEBUG, format, arg1, arg2)
    }

    override fun debug(format: String,
                       vararg arguments: Any) {
        logAndFormat(DEBUG, format, *arguments)
    }

    override fun debug(msg: String,
                       t: Throwable) {
        throw TODO()
    }

    override fun isDebugEnabled(marker: Marker): Boolean {
        throw TODO()
    }

    override fun debug(marker: Marker,
                       msg: String) {
        throw TODO()
    }

    override fun debug(marker: Marker,
                       format: String,
                       arg: Any) {
        throw TODO()
    }

    override fun debug(marker: Marker,
                       format: String,
                       arg1: Any,
                       arg2: Any) {
        throw TODO()
    }

    override fun debug(marker: Marker,
                       format: String,
                       vararg arguments: Any) {
        throw TODO()
    }

    override fun debug(marker: Marker,
                       msg: String,
                       t: Throwable) {

    }

    override fun isInfoEnabled(): Boolean {
        return isLevelEnabled(INFO)
    }

    override fun info(msg: String) {
        log(INFO, msg, null)
    }

    override fun info(format: String,
                      arg: Any) {
        logAndFormat(INFO, format, arg, null)
    }

    private fun logAndFormat(level: Level,
                             format: String,
                             vararg args: Any) {
        if (!isLevelEnabled(level)) {
            return
        }
        val tp = MessageFormatter.arrayFormat(format, args)
        log(level, tp.message, tp.throwable)
    }

    private fun logAndFormat(level: Level,
                             format: String,
                             arg1: Any,
                             arg2: Any?) {
        if (!isLevelEnabled(level)) {
            return
        }
        val tp = MessageFormatter.format(format, arg1, arg2)
        log(level, tp.message, tp.throwable)
    }

    override fun info(format: String,
                      arg1: Any,
                      arg2: Any) {
        throw TODO()
    }

    override fun info(format: String,
                      vararg arguments: Any) {
        throw TODO()
    }

    override fun info(msg: String,
                      t: Throwable) {
        throw TODO()
    }

    override fun isInfoEnabled(marker: Marker): Boolean {
        throw TODO()
    }

    override fun info(marker: Marker,
                      msg: String) {
        throw TODO()
    }

    override fun info(marker: Marker,
                      format: String,
                      arg: Any) {
        throw TODO()
    }

    override fun info(marker: Marker,
                      format: String,
                      arg1: Any,
                      arg2: Any) {
        throw TODO()
    }

    override fun info(marker: Marker,
                      format: String,
                      vararg arguments: Any) {
        throw TODO()
    }

    override fun info(marker: Marker,
                      msg: String,
                      t: Throwable) {
        throw TODO()
    }

    override fun isWarnEnabled(): Boolean {
        return isLevelEnabled(WARN)
    }

    override fun warn(msg: String) {
        log(WARN, msg, null)
    }

    override fun warn(format: String,
                      arg: Any) {
        logAndFormat(WARN, format, arg)
    }

    override fun warn(format: String,
                      vararg arguments: Any) {
        logAndFormat(WARN, format, arguments)
    }

    override fun warn(format: String,
                      arg1: Any,
                      arg2: Any) {
        logAndFormat(WARN, format, arg1, arg2)
    }

    override fun warn(msg: String,
                      t: Throwable) {
        logAndFormat(WARN, msg, t)
    }

    override fun isWarnEnabled(marker: Marker): Boolean {
        throw TODO()
    }

    override fun warn(marker: Marker,
                      msg: String) {
        throw TODO()
    }

    override fun warn(marker: Marker,
                      format: String,
                      arg: Any) {
        throw TODO()
    }

    override fun warn(marker: Marker,
                      format: String,
                      arg1: Any,
                      arg2: Any) {
        throw TODO()
    }

    override fun warn(marker: Marker,
                      format: String,
                      vararg arguments: Any) {
        throw TODO()
    }

    override fun warn(marker: Marker,
                      msg: String,
                      t: Throwable) {
        throw TODO()
    }

    override fun isErrorEnabled(): Boolean {
        return isLevelEnabled(ERROR)
    }

    override fun error(msg: String) {
        log(ERROR, msg, null)
    }

    override fun error(format: String,
                       arg: Any) {
        logAndFormat(ERROR, format, arg)
    }

    override fun error(format: String,
                       arg1: Any,
                       arg2: Any) {
        throw TODO()
    }

    override fun error(format: String,
                       vararg arguments: Any) {
        throw TODO()
    }

    override fun error(msg: String,
                       t: Throwable) {
        throw TODO()
    }

    override fun isErrorEnabled(marker: Marker): Boolean {
        throw TODO()
    }

    override fun error(marker: Marker,
                       msg: String) {
        throw TODO()
    }

    override fun error(marker: Marker,
                       format: String,
                       arg: Any) {
        throw TODO()
    }

    override fun error(marker: Marker,
                       format: String,
                       arg1: Any,
                       arg2: Any) {
        throw TODO()
    }

    override fun error(marker: Marker,
                       format: String,
                       vararg arguments: Any) {
        throw TODO()
    }

    override fun error(marker: Marker,
                       msg: String,
                       t: Throwable) {
        throw TODO()
    }
}
