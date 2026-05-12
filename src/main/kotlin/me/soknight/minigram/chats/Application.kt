package me.soknight.minigram.chats

import io.ktor.server.application.*
import io.ktor.server.cio.*
import me.soknight.minigram.chats.di.AppModule
import me.soknight.minigram.chats.di.buildKoinModule
import me.soknight.minigram.chats.plugin.*
import org.koin.core.module.Module
import org.koin.ktor.ext.get
import org.koin.ktor.plugin.Koin

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() = setup()

fun Application.setup(vararg extraModules: Module = emptyArray()) {
    install(Koin) {
        if (extraModules.isNotEmpty()) allowOverride(true)
        modules(AppModule(this@setup).buildKoinModule(), *extraModules)
    }

    configureDatabase(get())
    configureSerialization()
    configureSecurity(get())
    configureStatusPages()
    configureRouting(get(), get(), get(), get(), get())
    configureWebSockets(get(), get(), get())
    configureSwagger()
}
