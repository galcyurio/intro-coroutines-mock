package com.example

import io.ktor.application.*
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.*
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.utils.io.bits.useMemory
import kotlinx.coroutines.time.delay
import java.io.File
import java.io.InputStream
import java.time.Duration

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    fun getMockResponseInputStream(fileName: String): InputStream? {
        return javaClass.classLoader.getResourceAsStream("mock-response/$fileName")
    }

    intercept(ApplicationCallPipeline.Features) {
        delay(Duration.ofMillis(100L))
    }
    routing {
        get("/") {
            call.respondText { "Hello World!" }
        }
        get("orgs/kotlin/repos") {
            val inputStream = getMockResponseInputStream("orgs^kotlin^repos.json")
            if (inputStream == null) {
                call.respond(HttpStatusCode.InternalServerError, "failed get kotlin repositories")
                return@get
            }

            call.respondOutputStream(contentType = ContentType.Application.Json) {
                inputStream.use { inputStream.copyTo(this) }
            }
        }
        get("repos/kotlin/{repo}/contributors") {
            val repo = call.parameters["repo"]
            if (repo.isNullOrEmpty()) {
                call.respond(HttpStatusCode.BadRequest, "need repository name")
                return@get
            }

            val inputStream = getMockResponseInputStream("repos^kotlin^$repo^contributors.json")
            if (inputStream == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            call.respondOutputStream(contentType = ContentType.Application.Json) {
                inputStream.use { inputStream.copyTo(this) }
            }
        }
    }
}

