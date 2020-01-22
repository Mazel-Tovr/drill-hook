package com.epam.drill.hook

import com.epam.drill.hook.gen.*
import kotlinx.cinterop.*

@Suppress("UNUSED_PARAMETER", "UNUSED")
@CName("Agent_OnLoad")
fun agentOnLoad(vmPointer: Long, options: String, reservedPtr: Long): Int {
    try {
        main()
    } catch (ex: Throwable) {
        println("Can't load the agent. Ex: ${ex.message}")
    }
    return 0
}


fun main() {
    create_funchook()
    read_hook(staticCFunction(::drill_read).reinterpret())
    write_hook(staticCFunction(::drill_write).reinterpret())
    install_funchooks()
}