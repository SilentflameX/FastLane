package com.example.multiplayertest.GameObjects

import KtorServer
import androidx.xr.runtime.math.Vector3

class NetworkedVar<T> (t : T){
    var value = t
    var dirty = false
    var type = ""
}
var totalNetworkedObjects = 0
class NetworkedObject : GameObject() {

    var networkedID = -1

    init {
        networkedID = totalNetworkedObjects++
    }

    val syncedData: MutableMap<String, Any> = mutableMapOf()

    fun <T>UpdateSyncedData(_name : String, _value : T) {
        val variable = syncedData[_name] as NetworkedVar<T>
        variable.value = _value
        variable.dirty = true
    }


    fun <T>UpdateFromServer(_name : String, _value : T) {
        val variable = syncedData[_name] as NetworkedVar<T>
        variable.value = _value
    }

    //Loops through all variables that need to be updated and send them to server
    fun UpdateVariables() {
        syncedData.forEach { (key, value) ->
            if ((value as NetworkedVar<*>).dirty) {
                SendUpdateToServer(value,key)
                value.dirty = false
            }
        }
    }

    //Creates the message and sends to the server
    private fun SendUpdateToServer(_variable :NetworkedVar<*>, _varName : String) {
        var packetString = CreatePacket(_variable.type, _variable.value, _varName)
        KtorClient.SendPacketToServer(packetString)
    }

    private fun CreatePacket(_type : String, _value : Any?, _name : String) : String{
        var retString = "$networkedID|"
        when(_type){
            "Vector3" ->  retString += "V3|$_name|" + (_value as Vector3).toString()
            "Int" ->  retString += "I|$_name|" +  (_value as Int).toString()
            "Float" ->  retString += "F|$_name|" +  (_value as Float).toString()
        }
        retString += "\n"
        return retString
    }


    inline fun <reified T>AddNetworkedVariable(_name: String, _value : T) {
        var newVar = NetworkedVar(_value)
        newVar.type = T::class.simpleName.toString()
        syncedData[_name] = newVar
    }

    fun GetNetworkedValue(_name : String) : Any?{
        return (syncedData[_name] as NetworkedVar<*>).value
    }


}