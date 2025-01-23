import com.example.multiplayertest.DataToSync
import com.example.multiplayertest.MainActivity
import com.example.multiplayertest.data
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.request
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json

object KtorClient {
    private lateinit var client: HttpClient
    private var hostIP = ""
    private var port = 0

    fun connectToServer(_hostIp: String, _port: Int) {
        hostIP = _hostIp
        port = _port
        client = HttpClient(CIO)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response: String = client.get("http://$hostIP:$port/").toString()
                println("Server Response: $response")

                val posResponse: String = client.get("http://$hostIP:$port/position").body()
                println("Position Response: $posResponse")
                parseString(posResponse)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val posResponse: String = client.get("http://$hostIP:$port/position").body()
                println("Position Response: $posResponse")
                parseString(posResponse)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }



    fun closeServer(){
        client.close()
    }

    fun parseString(str : String)
    {
        val split = str.split(",")
        var d = DataToSync(0,0,0)
        d.x = split[0].toInt()
        d.y = split[1].toInt()
        d.z = split[2].toInt()
        data.value = d
    }

}