package me.soknight.minigram.chats

import io.ktor.server.application.*
import io.ktor.server.cio.*
import me.soknight.minigram.chats.di.AppModule
import me.soknight.minigram.chats.plugin.*
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.ktor.ext.get
import org.koin.ktor.plugin.Koin

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module(vararg extraModules: Module = emptyArray()) {
    val app = this
    install(Koin) {
        modules(
            module { single { AppModule(app) } },
            *extraModules
        )
    }

    configureDatabase(get())
    configureSerialization()
    configureSecurity(get())
    configureStatusPages()
    configureRouting(get(), get(), get(), get())
    configureWebSockets(get(), get())
}
