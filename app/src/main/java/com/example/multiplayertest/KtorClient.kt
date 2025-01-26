
import androidx.xr.runtime.math.Vector3
import com.example.multiplayertest.GameObjects.NetworkedObject
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.request
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlin.concurrent.thread

object KtorClient {
    private lateinit var client: HttpClient
    private var hostIP = ""
    private var port = 0

    lateinit var socket: Socket
    lateinit var receiveChannel : ByteReadChannel
    lateinit var sendChannel : ByteWriteChannel

    var connected = false

    val networkedObjectList: MutableMap<Int, NetworkedObject> = mutableMapOf()


    fun ConnectToServer(_hostIp: String, _port: Int): Boolean {
        if(connected)
            return true
        hostIP = _hostIp
        port = _port
        client = HttpClient(CIO)
        thread {
            runBlocking {
                ClientLoop(_hostIp, _port)
            }
        }
        return connected
    }

    suspend fun ClientLoop(_hostIp: String, _port: Int){
        val selectorManager = SelectorManager(Dispatchers.IO)
        socket = aSocket(selectorManager).tcp().connect(_hostIp, _port)
        receiveChannel = socket.openReadChannel()
        sendChannel = socket.openWriteChannel(autoFlush = true)
        println("Client Connected")
        connected = true
        while (true) {
            val data = receiveChannel.readUTF8Line()
            if(data != null)
            {
                println("Client: Message recieved")
                ParseData(data)
            }
        }
    }

    fun CloseServer(){
        client.close()
    }

    fun Update(deltaTime: Float){
        for (obj in networkedObjectList){
            obj.value.UpdateVariables()
        }
    }

    fun ParseData(data : String)
    {
        val split = data.split("|")
        //first would be networkedID
        val networkedID : Int = split[0].toInt()
        //type
        val type : String = split[1]
        //name
        val name : String = split[2]
        //value
        var value = Any()
        when(type){
            "V3" -> value = StringToVector3(split[3])
            "I" -> value = split[3].toInt()
            "F" -> value = split[3].toFloat()
        }

        networkedObjectList[networkedID]?.UpdateFromServer(name, value)
    }

    fun StringToVector3(vString: String) : Vector3 {
        val xyz = (vString.substring(1, vString.length - 1)).split(", ")
        return Vector3(xyz[0].drop(2).toFloat(), xyz[1].drop(2).toFloat(), xyz[2].drop(2).toFloat())
    }

    private suspend fun SendData(packet: String) = coroutineScope{
        sendChannel.writeStringUtf8(packet)
        sendChannel.flush()
    }

    fun SendPacketToServer(packet: String) {
        if (!connected) return

        //Maybe can create some buffer here or add all into 1 packet and send at once
        GlobalScope.async {
            SendData(packet)
        }
    }

    fun RegisterNetworkedObject(newObject : NetworkedObject) {
        networkedObjectList.put(newObject.networkedID, newObject)
    }

}