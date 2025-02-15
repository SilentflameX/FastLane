
import androidx.xr.runtime.math.Vector3
import com.example.multiplayertest.GameObjects.NetworkedObject
import com.example.multiplayertest.GameObjects.NetworkedVar
import com.example.multiplayertest.GameScene.goList
import com.example.multiplayertest.GameScene.myPlayer
import io.ktor.client.*
import io.ktor.client.engine.cio.*
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
import kotlin.concurrent.thread
import kotlin.time.TimeSource

object KtorClient {
    private var hostIP = ""
    private var port = 0

    lateinit var socket: Socket
    lateinit var receiveChannel : ByteReadChannel
    lateinit var sendChannel : ByteWriteChannel

    var networkID = -1

    val networkedObjectList: MutableMap<Int, NetworkedObject> = mutableMapOf()

    var delayBuffer = 500//in milliseconds
    var prevMsgTime : TimeSource.Monotonic.ValueTimeMark = TimeSource.Monotonic.markNow()


    fun IsConnected() : Boolean{
        return networkID != -1
    }

    fun ConnectToServer(_hostIp: String, _port: Int): Boolean {
        if(IsConnected())
            return true
        hostIP = _hostIp
        port = _port
        thread {
            runBlocking {
                ClientLoop(_hostIp, _port)
            }
        }
        return true
    }

    suspend fun ClientLoop(_hostIp: String, _port: Int){
        val selectorManager = SelectorManager(Dispatchers.IO)
        socket = aSocket(selectorManager).tcp().connect(_hostIp, _port)
        receiveChannel = socket.openReadChannel()
        sendChannel = socket.openWriteChannel(autoFlush = true)
        //first init msg
        var initData = receiveChannel.readUTF8Line()
        if(initData == null)//if init msg is null means server got issue
            return;
        else
        {
            //get our networkID
            networkID = initData.first().toInt()
            initData = initData.drop(2)

            //If got networkedObjects to sync
            if(initData.length != 0) {
                println("InitMsg: " + initData)
                //get all alr existing networkedObjects
                for (obj in initData.split("}")) {
                    //For each networkedObject
                    val objNetworkID = obj.first().digitToInt()
                    val newNetworkedObj = NetworkedObject()
                    newNetworkedObj.objNetworkID = objNetworkID

                    //Get all syncedVariables
                    val v = obj.drop(2)
                    for (value in v.split("{")) {
                        val split = value.split("|")
                        newNetworkedObj.syncedVariables.put(split[0], NetworkedVar(StringToVar(split[1])))
                    }
                    goList.add(newNetworkedObj)
                    networkedObjectList.put(objNetworkID, newNetworkedObj)
                }
            }
        }

        println("Client Connected")
        //Spawn our player
        myPlayer = SpawnNetworkedObject()

        //Client receive loop
        while (true) {
            var stream = receiveChannel.readUTF8Line()
            if(stream != null)
            {
                //split msg up if got multiple msgs
                var messages = stream.split("\n")
                for(msg in messages) {
                    println("Client: Message recieved")
                    //parse message type
                    val type = msg.first()
                    var data = msg.drop(1)
                    when (type) {
                        'U' -> UpdateNetworkedValue(data) //Value update
                        'S' -> ServerSpawnNetworkedObject(data)
                    }
                }
            }
        }
    }

    fun CloseServer(){
        socket.close()
    }

    fun Update(deltaTime: Float){
        for (obj in networkedObjectList){
            obj.value.UpdateVariables()
            //quite dirty but do here temp
        }
    }

    fun UpdateNetworkedValue(data : String) {
        val split = data.split("|")
        //first would be networkedID
        val objNetworkedID: Int = split[0].toInt()
        //Update
        networkedObjectList[objNetworkedID]?.UpdateFromServer(split[1], StringToVar(split[2]))
    }

    fun StringToVar(data : String) : Any {
        val type = data.first()
        val vari = data.drop(1)

        when (type) {
            'V' -> return StringToVector3(vari)
            'I' -> return vari.toInt()
            'F' -> return vari.toFloat()
        }
        return Any()
    }

    fun NetworkedObjectsToPacket() : String{
        var packet = ""
        for(obj in networkedObjectList){
            packet += "}"
            packet += obj.value.ObjectToPacket()
        }
        return packet
    }

    fun SpawnNetworkedObject() : NetworkedObject{
        val newNetworkedObj = NetworkedObject()
        //tell all to spawn
        //Create packetMsg
        val packet = "S" + newNetworkedObj.ObjectToPacket()

        SendPacketToServer(packet)
        goList.add(newNetworkedObj)
        networkedObjectList.put(newNetworkedObj.objNetworkID,newNetworkedObj)
        return newNetworkedObj
    }

    fun ServerSpawnNetworkedObject(data: String) {
        if (networkedObjectList.get(data.toInt()) == null) {
            var newNetworkedObj = NetworkedObject()
            goList.add(newNetworkedObj)
            networkedObjectList.put(data.toInt(), newNetworkedObj)
        }
    }


    fun StringToVector3(vString: String) : Vector3 {
        val xyz = (vString.substring(1, vString.length - 1)).split(", ")
        return Vector3(xyz[0].drop(2).toFloat(), xyz[1].drop(2).toFloat(), xyz[2].drop(2).toFloat())
    }

    private suspend fun SendData(packet: String) = coroutineScope{
        sendChannel.writeStringUtf8(packet  + "\n")
        sendChannel.flush()
    }

    fun SendPacketToServer(packet: String) {
        if (!IsConnected()) return

        //Maybe can create some buffer here or add all into 1 packet and send at once
        if(prevMsgTime.elapsedNow().inWholeMilliseconds < delayBuffer)
        {
            return
        }
        prevMsgTime = TimeSource.Monotonic.markNow()
        GlobalScope.async {
            SendData(packet)
        }
    }

    fun RegisterNetworkedObject(newObject : NetworkedObject) {
        networkedObjectList.put(newObject.objNetworkID, newObject)
    }

}