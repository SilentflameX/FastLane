package com.example.multiplayertest
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.ServerSocket
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.IOException
import java.net.InetAddress
import java.net.NetworkInterface
import kotlin.concurrent.thread

object KtorServer {
    var isServer = false
    private var serverSocket : ServerSocket? = null
    private var selectorManager : SelectorManager? = null
    private var serverJob: Job? = null
    private val writeMutex = Mutex()

    class ClientInfo(socket : Socket,  rChannel : ByteReadChannel, sChannel : ByteWriteChannel, cID : Int){
        var socket = socket
        var receiveChannel = rChannel
        var sendChannel = sChannel
        var clientID = cID
    }

    private var clientsList = mutableListOf<ClientInfo>()

    fun startServer(hostIP: String, hostPort: Int){
        if (isServer)
            return

        isServer = true
        serverJob = CoroutineScope(Dispatchers.IO).launch {
            serverLoop(hostIP, hostPort)
        }
        //Since we also playing we need connect to ourselves
        KtorClient.connectToServer("localhost", hostPort)
    }

    fun update(deltaTime: Float){
        //Update whatever server side values
    }

    private suspend fun serverLoop(hostIP: String, hostPort: Int) {
        selectorManager = SelectorManager(Dispatchers.IO)
        serverSocket = aSocket(selectorManager!!).tcp().bind(hostIP, hostPort)

        while (isServer) {
            val socket = serverSocket!!.accept()
            val receiveChannel = socket.openReadChannel()
            val sendChannel = socket.openWriteChannel(autoFlush = true)
            val newClientID = clientsList.size
            val newClient = ClientInfo(socket, receiveChannel, sendChannel, newClientID)
            clientsList.add(newClient)
            //Send confirmation Msg
            //Send its networkID
            var confirmationMsg = "$newClientID"
            //Send all networkedObjects
            confirmationMsg += KtorClient.networkedObjectsToPacket()
            writeMutex.withLock {
                newClient.sendChannel.writeStringUtf8(confirmationMsg + "\n")
            }
            thread {
                runBlocking {
                    checkMessages(newClient.clientID)
                }
            }
            //Update all Clients that someone joined
            for (client in clientsList) {
                writeMutex.withLock {
                    client.sendChannel.writeStringUtf8("J" + clientsList.count() + "\n")
                }
            }
        }
    }

    private suspend fun checkMessages(clientID: Int) = coroutineScope {
        try {
            while (isServer) {
                val data = clientsList[clientID].receiveChannel.readUTF8Line() ?: break
                //Read and then send to all connected
                for (client in clientsList) {
                    writeMutex.withLock {
                        client.sendChannel.writeStringUtf8(data + "\n")
                    }
                }
            }

        } catch (e: IOException) {
            println("Client disconnected unexpectedly: ${e.message}")
            clientsList[clientID].socket.close()  // Ensure client socket is closed
            clientsList.removeAt(clientID)
        }
    }

    fun sendStartGameMessage(seed: Int) {
        println("Seed: $seed")
        GlobalScope.async {
            for (client in clientsList) {
                writeMutex.withLock {
                    client.sendChannel.writeStringUtf8("G$seed\n")
                }
            }
        }
    }

    fun shutdownServer() {
        isServer = false
        serverJob?.cancel()

        for(client in clientsList) {
            client.socket.close()
        }
        clientsList.clear()

        serverSocket?.close()
        selectorManager?.close()

    }

    fun getServerIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (networkInterface in interfaces) {
                if (networkInterface.isLoopback || !networkInterface.isUp) continue
                for (inetAddress in networkInterface.inetAddresses) {
                    if (!inetAddress.isLoopbackAddress && inetAddress is InetAddress) {
                        val hostAddress = inetAddress.hostAddress
                        //Skip IPv6 addresses if you only want IPv4
                        if (hostAddress != null) {
                            if (hostAddress.contains(":")) continue
                        }
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