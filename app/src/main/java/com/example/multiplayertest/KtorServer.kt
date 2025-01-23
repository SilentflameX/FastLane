//import io.ktor.application.*
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.multiplayertest.DataToSync
import com.example.multiplayertest.MainActivity
import com.example.multiplayertest.data
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
    var isServer = false

    fun startServer(port: Int = 8080) {
        isServer = true
        embeddedServer(Netty, port = 8080,host = "10.0.2.16") {
            routing {
                get("/") {
                    call.respondText("Hello, world!")
                }
                get("/position") {
                    call.respond(DataToString(data.value))
                }
            }
        }.start(wait = true)
    }

    fun DataToString(data : DataToSync): String {
        var retStr = ""
        retStr = data.x.toString() + "," + data.y.toString() + "," +data.z.toString()
        return retStr
    }


    fun stopServer() {
        server?.stop(1000, 2000)
        server = null
    }
}