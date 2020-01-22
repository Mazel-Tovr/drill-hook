package com.epam.drill.hook.http

import com.epam.drill.hook.*
import com.epam.drill.hook.gen.*
import kotlinx.cinterop.*
import kotlin.native.SharedImmutable
import kotlin.native.ThreadLocal
import kotlin.native.concurrent.*


@SharedImmutable
private val storageThread = Worker.start(true)

@ThreadLocal
private val _httpReadCallbacks = mutableSetOf<(String) -> Unit>()

@ThreadLocal
private var _httpWriteCallback: (() -> Map<String, String>)? = null

internal val httpReadCallbacks
    get() = storageThread.execute(TransferMode.UNSAFE, {}) { _httpReadCallbacks }.result

internal val httpWriteCallback: (() -> Map<String, String>)?
    get() = storageThread.execute(TransferMode.UNSAFE, {}) { _httpWriteCallback }.result

fun addHttpReadCallbacks(httpRequest: (String) -> Unit) =
    storageThread.execute(TransferMode.UNSAFE, { httpRequest }) { _httpReadCallbacks += it }

fun addHttpWriteCallback(writeCallback: () -> Map<String, String>) =
    storageThread.execute(TransferMode.UNSAFE, { writeCallback }) { _httpWriteCallback = it }


fun configureHttpHooksBuild(block: () -> Unit) {
    create_funchook()
    funchook_prepare(funchook, read_func_point, staticCFunction(::drillRead))
    funchook_prepare(funchook, write_func_point, staticCFunction(::drillWrite))
    funchook_prepare(funchook, send_func_point, staticCFunction(::drillSend))
    block()
    install_funchooks()
}