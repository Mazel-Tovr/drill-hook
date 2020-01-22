package com.epam.drill.hook.http

actual fun configureHttpHooks() = configureHttpHooksBuild {
    println("Configuration for macos")
}