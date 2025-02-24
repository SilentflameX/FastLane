package com.example.multiplayertest.gameobjects

import com.example.multiplayertest.KtorClient.networkedObjectList
import androidx.xr.runtime.math.Vector3
import com.example.multiplayertest.KtorClient
import com.example.multiplayertest.MainMenu

class NetworkedVar<T> (t : T){
    var value = t
    var dirty = false
    var type = ""
}

class NetworkedObject : GameObject() {

    var objNetworkID = -1

    init {
        objNetworkID = networkedObjectList.size
    }

    val syncedVariables: MutableMap<String, Any> = mutableMapOf()

    inline fun <reified T>updateSyncedData(name : String, value : T) {
        if (!syncedVariables.containsKey(name)){
            addNetworkedVariable(name, value)
            (syncedVariables[name] as NetworkedVar<*>).dirty = true
            return
        }

        val variable = syncedVariables[name] as NetworkedVar<T>
        variable.value = value
        variable.dirty = true
    }


    inline fun <reified T>updateFromServer(name : String, value : T) {
        if (!syncedVariables.containsKey(name)){
            addNetworkedVariable(name, value)
            if(name == "Sprite")//We update the screen with new selected sprite
                MainMenu.getInstance().updateLobby()
            return
        }

        val variable = syncedVariables[name] as NetworkedVar<T>
        variable.value = value

        if(name == "Sprite")//We update the screen with new selected sprite
            MainMenu.getInstance().updateLobby()
    }

    //Loops through all variables that need to be updated and send them to server
    fun updateVariables() {
        syncedVariables.forEach { (key, value) ->
            if ((value as NetworkedVar<*>).dirty) {
                sendUpdateToServer(value,key)
                value.dirty = false
            }
            if(key == "Position")
                sprite.position = value.value as Vector3
            else if(key == "Scale")
                sprite.scale = value.value as Vector3
        }
    }

    //Adds more info to the packet and send to server
    private fun sendUpdateToServer(variable : NetworkedVar<*>, varName : String) {
        val packetString = "U$objNetworkID|" + createPacket(variable, varName)
        KtorClient.sendPacketToServer(packetString)
    }

    //Converts the variable to a formated packet
    private fun createPacket(variable : NetworkedVar<*>, name : String) : String {
        when (variable.type) {
            "Vector3" -> return "$name|V" + ( variable.value as Vector3).toString()
            "Int" -> return "$name|I" + ( variable.value as Int).toString()
            "Float" -> return "$name|F" + ( variable.value as Float).toString()
        }
        return ""
    }

    //Converts a NetworkedObject into a string to send through network
    fun objectToPacket() : String{
        var packet = "$objNetworkID"
        for(sV in syncedVariables){
            packet += "{" + createPacket((sV.value as NetworkedVar<*>),sV.key)
        }
        return packet
    }

    //Adds a new variable to map of synced variables
    inline fun <reified T>addNetworkedVariable(name: String, value : T) {
        val newVar = NetworkedVar(value)
        newVar.type = value!!::class.simpleName.toString()
        syncedVariables[name] = newVar
    }
}