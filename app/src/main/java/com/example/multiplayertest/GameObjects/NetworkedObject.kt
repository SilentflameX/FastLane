package com.example.multiplayertest.GameObjects

import KtorClient.networkedObjectList
import androidx.xr.runtime.math.Vector3
import com.example.multiplayertest.GameScene.myPlayer

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

    inline fun <reified T>UpdateSyncedData(_name : String, _value : T) {
        if (!syncedVariables.containsKey(_name)){
            AddNetworkedVariable(_name, _value)
            (syncedVariables[_name] as NetworkedVar<T>).dirty = true
            return
        }

        val variable = syncedVariables[_name] as NetworkedVar<T>
        variable.value = _value
        variable.dirty = true
    }


    inline fun <reified T>UpdateFromServer(_name : String, _value : T) {
        if (!syncedVariables.containsKey(_name)){
            AddNetworkedVariable(_name, _value)
            return
        }

        val variable = syncedVariables[_name] as NetworkedVar<T>
        variable.value = _value
    }

    //Loops through all variables that need to be updated and send them to server
    fun UpdateVariables() {
        syncedVariables.forEach { (key, value) ->
            if ((value as NetworkedVar<*>).dirty) {
                SendUpdateToServer(value,key)
                value.dirty = false
            }
            if(key == "Position")
                sprite.position = value.value as Vector3
            else if(key == "Sprite" && sprite.textureId != value.value as Int)
                sprite.LoadSprite(value.value as Int)
        }
    }

    //Creates the message and sends to the server
    private fun SendUpdateToServer(_variable :NetworkedVar<*>, _varName : String) {
        var packetString = "U$objNetworkID|" + CreatePacket(_variable, _varName)
        KtorClient.SendPacketToServer(packetString)
    }

    private fun CreatePacket(_variable :NetworkedVar<*>, _name : String) : String {
        when (_variable.type) {
            "Vector3" -> return "$_name|V" + ( _variable.value as Vector3).toString()
            "Int" -> return "$_name|I" + ( _variable.value as Int).toString()
            "Float" -> return "$_name|F" + ( _variable.value as Float).toString()
        }
        return ""
    }

    fun ObjectToPacket() : String{
        var packet = "$objNetworkID"
        for(sV in syncedVariables){
            packet += "{" + CreatePacket((sV.value as NetworkedVar<*>),sV.key)
        }
        return packet
    }


    inline fun <reified T>AddNetworkedVariable(_name: String, _value : T) {
        var newVar = NetworkedVar(_value)
        newVar.type = T::class.simpleName.toString()
        syncedVariables[_name] = newVar
    }

    fun GetNetworkedValue(_name : String) : Any?{
        return (syncedVariables[_name] as NetworkedVar<*>).value
    }


}