package com.example.multiplayertest
import androidx.xr.runtime.math.Vector3
import com.example.multiplayertest.GameScene.goList
import com.example.multiplayertest.GameScene.myPlayer
import com.example.multiplayertest.GameScene.randomGenerator
import com.example.multiplayertest.gameobjects.NetworkedObject
import com.example.multiplayertest.gameobjects.NetworkedVar
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
import kotlin.random.Random
import kotlin.time.TimeSource

object KtorClient {
    var hostIP = ""
    private var port = 0

    private lateinit var socket: Socket
    private lateinit var receiveChannel : ByteReadChannel
    private lateinit var sendChannel : ByteWriteChannel

    var networkID = -1

    val networkedObjectList: MutableMap<Int, NetworkedObject> = mutableMapOf()

    private var delayBuffer = 100//in milliseconds
    private var prevMsgTime : TimeSource.Monotonic.ValueTimeMark = TimeSource.Monotonic.markNow()

    var totalClientCount = -1

    val profileSpriteList = listOf(R.drawable.car_black_1, R.drawable.car_blue_1, R.drawable.car_green_1, R.drawable.car_red_1, R.drawable.car_yellow_1)


    private fun isConnected() : Boolean{
        return networkID != -1
    }

    fun connectToServer(hostIp: String, hostPort: Int): Boolean {
        if(isConnected())
            return true
        hostIP = hostIp
        port = hostPort
        thread {
            runBlocking {
                try {
                    clientLoop(hostIp, port)
                }
                catch (_: ConnectException){
                    //MainMenu.GetInstance().LoadMainMenu()
                }
            }
        }
        return false
    }

    private suspend fun clientLoop(hostIp: String, port: Int){
        val selectorManager = SelectorManager(Dispatchers.IO)
        socket = aSocket(selectorManager).tcp().connect(hostIp, port)
        receiveChannel = socket.openReadChannel()
        sendChannel = socket.openWriteChannel(autoFlush = true)
        //first init msg
        var initData = receiveChannel.readUTF8Line()
        if(initData == null)//if init msg is null means server got issue
            return
        else
        {
            //get our networkID
            networkID = initData.first().digitToInt()
            initData = initData.drop(2)

            //If got networkedObjects to sync
            if(initData.isNotEmpty()) {
                //get all alr existing networkedObjects
                for (obj in initData.split("}")) {
                    //For each networkedObject
                    val objNetworkID = obj.first().digitToInt()
                    val newNetworkedObj = NetworkedObject()
                    newNetworkedObj.objNetworkID = objNetworkID

                    //Get all syncedVariables
                    val v = obj.drop(2)
                    if(v.isNotEmpty()) {
                        for (value in v.split("{")) {
                            val split = value.split("|")
                            newNetworkedObj.syncedVariables[split[0]] = NetworkedVar(stringToVar(
                                split[1]
                            ))
                        }
                    }
                    goList.add(newNetworkedObj)
                    networkedObjectList[objNetworkID] = newNetworkedObj
                }
            }
        }
        //Spawn our player
        myPlayer = spawnNetworkedObject()
        //Lane positions = -8.55, -5.78, -2.85, 0, 2.85, 5.78, 8.55
        myPlayer!!.sprite.position = Vector3(-2.85f + (2.85f * networkID), 0f, 0f)
        myPlayer!!.updateSyncedData("Position", myPlayer!!.sprite.position)
        update(0f)

        //Client receive loop
        while (networkID != -1) {
            val stream = receiveChannel.readUTF8Line()
            if(!stream.isNullOrEmpty())
            {
                //split msg up if got multiple msgs
                val messages = stream.split("\n")
                for(msg in messages) {
                    //parse message type
                    val type = msg.first()
                    val data = msg.drop(1)
                    when (type) {
                        'U' -> updateNetworkedValue(data) //Value update
                        'S' -> serverSpawnNetworkedObject(data)
                        'J' -> newClientJoined(data) //New client joined
                        'G' -> startGame(data) //Start game
                    }
                }
            }
        }
    }

    private fun startGame(data: String) {
        randomGenerator = Random(data.toInt())
        MainMenu.getInstance().startGame()
    }

    fun disconnect(){
        networkID = -1
        socket.close()
        //Reset everything
        GameScene.reset()

        networkedObjectList.clear()
        totalClientCount = -1
    }

    fun update(deltaTime: Float){
        for (obj in networkedObjectList){
            obj.value.updateVariables()
        }
    }

    private fun updateNetworkedValue(data : String) {
        val split = data.split("|")
        //first would be networkedID
        val objNetworkedID: Int = split[0].toInt()
        //Update
        networkedObjectList[objNetworkedID]?.updateFromServer(split[1], stringToVar(split[2]))
    }

    private fun stringToVar(data : String) : Any {
        val type = data.first()
        val vari = data.drop(1)

        when (type) {
            'V' -> return stringToVector3(vari)
            'I' -> return vari.toInt()
            'F' -> return vari.toFloat()
        }
        return Any()
    }

    fun networkedObjectsToPacket() : String{
        var packet = ""
        for(obj in networkedObjectList){
            packet += "}"
            packet += obj.value.objectToPacket()
        }
        return packet
    }

    private fun spawnNetworkedObject() : NetworkedObject {
        val newNetworkedObj = NetworkedObject()
        //Create packetMsg
        val packet = "S" + newNetworkedObj.objectToPacket()

        //Send to server to tell all clients to spawn
        sendPacketToServer(packet)
        goList.add(newNetworkedObj)
        networkedObjectList[newNetworkedObj.objNetworkID] = newNetworkedObj
        return newNetworkedObj
    }

    private fun serverSpawnNetworkedObject(data: String) {
        val netObjID = data.toInt()
        //We only spawn if its not already spawned
        if (networkedObjectList[netObjID] == null) {
            val newNetworkedObj = NetworkedObject()
            goList.add(newNetworkedObj)
            networkedObjectList[netObjID] = newNetworkedObj
        }
    }


    private fun stringToVector3(vString: String) : Vector3 {
        val xyz = (vString.substring(1, vString.length - 1)).split(", ")
        return Vector3(xyz[0].drop(2).toFloat(), xyz[1].drop(2).toFloat(), xyz[2].drop(2).toFloat())
    }

    private suspend fun sendData(packet: String) = coroutineScope{
        sendChannel.writeStringUtf8(packet  + "\n")
        sendChannel.flush()
    }

    fun sendPacketToServer(packet: String) {
        if (!isConnected()) return

        //Maybe can create some buffer here or add all into 1 packet and send at once
        if(prevMsgTime.elapsedNow().inWholeMilliseconds < delayBuffer)
        {
            return
        }
        prevMsgTime = TimeSource.Monotonic.markNow()
        GlobalScope.async {
            sendData(packet)
        }
    }

    private fun newClientJoined(packet: String){
        totalClientCount = packet.toInt()
        MainMenu.getInstance().updateLobby()
    }

}