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
import java.net.InetAddress
import java.net.NetworkInterface
import kotlin.system.*

object KtorServer {
    private var server: NettyApplicationEngine? = null
    var isServer = false

    fun startServer(port: Int = 8080) {
        if(isServer)
        {
            val ipAddr = getServerIpAddress()
            println(ipAddr)
            return
        }
        isServer = true
        embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
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

    fun DataToString(data: DataToSync): String {
        var retStr = ""
        retStr = data.x.toString() + "," + data.y.toString() + "," + data.z.toString()
        return retStr
    }


    fun stopServer() {
        server?.stop(1000, 2000)
        server = null
    }

    fun getServerIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (networkInterface in interfaces) {
                if (networkInterface.isLoopback || !networkInterface.isUp) continue
                for (inetAddress in networkInterface.inetAddresses) {
                    if (!inetAddress.isLoopbackAddress && inetAddress is InetAddress) {
                        val hostAddress = inetAddress.hostAddress
                        // Skip IPv6 addresses if you only want IPv4
                        if (hostAddress.contains(":")) continue
                        return hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}