
import android.view.View.VISIBLE
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.xr.runtime.math.Vector3
import com.example.multiplayertest.GameObjects.NetworkedObject
import com.example.multiplayertest.GameObjects.NetworkedVar
import com.example.multiplayertest.GameScene
import com.example.multiplayertest.GameScene.goList
import com.example.multiplayertest.GameScene.myPlayer
import com.example.multiplayertest.MainMenu
import com.example.multiplayertest.R
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
import java.net.ConnectException
import kotlin.concurrent.thread
import kotlin.time.TimeSource

object KtorClient {
    var hostIP = ""
    private var port = 0

    lateinit var socket: Socket
    lateinit var receiveChannel : ByteReadChannel
    lateinit var sendChannel : ByteWriteChannel

    var networkID = -1

    val networkedObjectList: MutableMap<Int, NetworkedObject> = mutableMapOf()

    var delayBuffer = 100//in milliseconds
    var prevMsgTime : TimeSource.Monotonic.ValueTimeMark = TimeSource.Monotonic.markNow()

    var totalClientCount = -1

    val profileSpriteList = listOf(R.drawable.car_black_1, R.drawable.car_blue_1, R.drawable.car_green_1, R.drawable.car_red_1, R.drawable.car_yellow_1)


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
                try {
                    ClientLoop(_hostIp, _port)
                }
                catch (_: ConnectException){
                }
            }
        }
        return false
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
            networkID = initData.first().digitToInt()
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
                    if(v.length != 0) {
                        for (value in v.split("{")) {
                            val split = value.split("|")
                            newNetworkedObj.syncedVariables.put(split[0],NetworkedVar(StringToVar(split[1])))
                        }
                    }
                    goList.add(newNetworkedObj)
                    networkedObjectList.put(objNetworkID, newNetworkedObj)
                }
            }
        }
        //Spawn our player
        myPlayer = SpawnNetworkedObject()
        //Lane positions = -8.55, -5.78, -2.85, 0, 2.85, 5.78, 8.55
        myPlayer!!.sprite.position = Vector3(-2.85f + (2.85f * networkID), 0f, 0f)
        myPlayer!!.UpdateSyncedData("Position", myPlayer!!.sprite.position)
        Update(0f)

        //Client receive loop
        while (networkID != -1) {
            var stream = receiveChannel.readUTF8Line()
            if(!stream.isNullOrEmpty())
            {
                //split msg up if got multiple msgs
                var messages = stream.split("\n")
                for(msg in messages) {
                    //parse message type
                    val type = msg.first()
                    var data = msg.drop(1)
                    when (type) {
                        'U' -> UpdateNetworkedValue(data) //Value update
                        'S' -> ServerSpawnNetworkedObject(data)
                        'J' -> NewClientJoined(data) //New client joined
                        'G' -> StartGame(data) //Start game
                    }
                }
            }
        }
    }

    private fun StartGame(data: String) {
        MainMenu.GetInstance().StartGame()
    }

    fun Disconnect(){
        networkID = -1
        socket.close()
        //Reset everything
        GameScene.Reset()

        networkedObjectList.clear()
        totalClientCount = -1
    }

    fun Update(deltaTime: Float){
        for (obj in networkedObjectList){
            obj.value.UpdateVariables()
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
        var NetObjID = data.toInt()
        //We only spawn if its not already spawned
        if (networkedObjectList.get(NetObjID) == null) {
            var newNetworkedObj = NetworkedObject()
            goList.add(newNetworkedObj)
            networkedObjectList.put(NetObjID, newNetworkedObj)
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

    fun NewClientJoined(packet: String){
        totalClientCount = packet.toInt()
        MainMenu.GetInstance().UpdateLobby()
    }

}