//import io.ktor.application.*
import io.ktor.http.*
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.*
import kotlin.system.*

object KtorServer {
    private var server: NettyApplicationEngine? = null

    fun startServer(port: Int = 8080) {
        embeddedServer(Netty, port = 8080,host = "0.0.0.0") {
            routing {
                get("/") {
                    call.respondText("Hello, world!")
                }
                get("/json") {
                    call.respond(mapOf("message" to "This is JSON response"))
                }
            }
        }.start(wait = true)
    }

    fun stopServer() {
        server?.stop(1000, 2000)
        server = null
    }
}