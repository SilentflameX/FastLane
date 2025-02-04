package com.example.multiplayertest

import KtorClient
import androidx.xr.runtime.math.Vector3
import com.example.multiplayertest.GameObjects.GameObject
import com.example.multiplayertest.GameObjects.NetworkedObject
import com.example.multiplayertest.GameObjects.NetworkedVar

object GameScene {
    var goList = mutableListOf<GameObject>()
    var blueCar2 = NetworkedObject()
    fun Start()
    {
        var blueCar = GameObject()
        blueCar.sprite.LoadSprite(R.drawable.car_blue_1)
        blueCar.sprite.scale = Vector3(0.2f,0.4f,1.0f)
        blueCar.sprite.UpdateMatrix()
        goList.add(blueCar)


        blueCar2.sprite.LoadSprite(R.drawable.car_blue_1)
        blueCar2.sprite.scale = Vector3(0.2f,0.4f,1.0f)
        blueCar2.sprite.position = Vector3(0.4f,0.4f,0.0f)
        blueCar2.sprite.rotation = 45.0f
        blueCar2.sprite.UpdateMatrix()
        goList.add(blueCar2)

        blueCar2.AddNetworkedVariable("Position",Vector3(0.0f,0.0f,0.0f))
        KtorClient.RegisterNetworkedObject(blueCar2)
        //blueCar2.UpdateVariable()
    }

    fun Update(deltaTime: Float) {
        blueCar2.sprite.position = blueCar2.GetNetworkedValue("Position") as Vector3
    }

    fun TestUpdateCar(){
        blueCar2.UpdateSyncedData("Position", blueCar2.sprite.position + Vector3(0.1f,0.1f,0.1f))
    }

    fun TempMoveCar(dir : Int) : Boolean{
        //Temp movement TODO: Move using gyroscope
        when(dir) {
            0 -> blueCar2.UpdateSyncedData("Position",blueCar2.sprite.position + Vector3(0f, 0.1f, 0f));
            1 -> blueCar2.UpdateSyncedData("Position",blueCar2.sprite.position + Vector3(0f, -0.1f, 0f));
            2 -> blueCar2.UpdateSyncedData("Position",blueCar2.sprite.position + Vector3(-0.1f, 0f, 0f));
            3 -> blueCar2.UpdateSyncedData("Position",blueCar2.sprite.position + Vector3(0.1f, 0f, 0f));
        }

        return false
    }
}