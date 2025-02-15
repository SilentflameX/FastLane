//import io.ktor.application.*
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.InetAddress
import java.net.NetworkInterface
import kotlin.concurrent.thread

object KtorServer {
    var isServer = false

    class ClientInfo(_socket : Socket,  _rChannel : ByteReadChannel, _sChannel : ByteWriteChannel, _cID : Int){
        var socket = _socket
        var receiveChannel = _rChannel
        var sendChannel = _sChannel
        var clientID = _cID
    }

    var clientsList = mutableListOf<ClientInfo>()

    fun StartServer(_hostIP: String, _hostPort: Int) : Boolean {
        if (isServer)
            return false
        thread {
            runBlocking {
                ServerLoop(_hostIP, _hostPort)
            }
        }
        //Since we also playing we need connect to ourselves
        KtorClient.ConnectToServer("localhost", _hostPort)
        return true
    }

    fun Update(deltaTime: Float){

    }

    private suspend fun ServerLoop(_hostIP: String, _hostPort: Int) {
        val selectorManager = SelectorManager(Dispatchers.IO)
        val serverSocket = aSocket(selectorManager).tcp().bind(_hostIP, _hostPort)
        println("Server: Server Started")

        while (true) {
            println("Looking for clients")
            var socket = serverSocket.accept()
            var receiveChannel = socket.openReadChannel()
            var sendChannel = socket.openWriteChannel(autoFlush = true)
            var newClientID = clientsList.size
            var newClient = ClientInfo(socket, receiveChannel, sendChannel, newClientID)
            clientsList.add(newClient)
            println("Server: Client Connected :${newClient.clientID}")
            //Send confirmation Msg
            //Send its networkID
            var confirmationMsg = "$newClientID"
            //Send all networkedObjects
            confirmationMsg += KtorClient.NetworkedObjectsToPacket()
            newClient.sendChannel.writeStringUtf8(confirmationMsg + "\n")
            thread {
                runBlocking {
                    CheckMessages(newClient.clientID)
                }
            }
        }

    }

    private suspend fun CheckMessages(clientID: Int) = coroutineScope{
        while (true) {
            val data = clientsList[clientID].receiveChannel.readUTF8Line()
            if(data != null) {
                println("Server: Message recieved from client:$clientID")
                //Read and then send to all connected
                for (client in clientsList)
                    client.sendChannel.writeStringUtf8(data + "\n")
                println("Server: Send to total clients:${clientsList.size}")
            }
        }
    }



    fun SendPacketToClient(clientID: Int,packet: String) {
        if (!isServer) return

        //Maybe can create some buffer here or add all into 1 packet and send at once
        GlobalScope.async {
            clientsList[clientID].sendChannel.writeStringUtf8(packet)
        }
    }

    fun StopServer() {
        //socket.close()
    }

    fun GetServerIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (networkInterface in interfaces) {
                if (networkInterface.isLoopback || !networkInterface.isUp) continue
                for (inetAddress in networkInterface.inetAddresses) {
                    if (!inetAddress.isLoopbackAddress && inetAddress is InetAddress) {
                        val hostAddress = inetAddress.hostAddress
                        //Skip IPv6 addresses if you only want IPv4
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