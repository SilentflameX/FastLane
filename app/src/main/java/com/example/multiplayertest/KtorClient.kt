import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json

object KtorClient {
    private lateinit var client : HttpClient

    fun connectToServer(hostIp: String, port: Int) {
        client = HttpClient(CIO)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response: String = client.get("http://$hostIp:$port/").toString()
                println("Server Response: $response")

                val jsonResponse: String = client.get("http://$hostIp:$port/json").toString()
                println("JSON Response: $jsonResponse")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun closeServer(){
        client.close()
    }
}