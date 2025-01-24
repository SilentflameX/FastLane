package com.example.multiplayertest

import androidx.xr.runtime.math.Vector3
import com.example.multiplayertest.GameObjects.GameObject

object GameScene {
    var goList = mutableListOf<GameObject>()

    fun Start()
    {
        var blueCar = GameObject()
        blueCar.sprite.LoadSprite(R.drawable.car_blue_1)
        blueCar.sprite.scale = Vector3(0.2f,0.4f,1.0f)
        blueCar.sprite.UpdateMatrix()
        goList.add(blueCar)

        var blueCar2 = GameObject()
        blueCar2.sprite.LoadSprite(R.drawable.car_blue_1)
        blueCar2.sprite.scale = Vector3(0.2f,0.4f,1.0f)
        blueCar2.sprite.position = Vector3(0.4f,0.4f,0.0f)
        blueCar2.sprite.rotation = 45.0f
        blueCar2.sprite.UpdateMatrix()
        goList.add(blueCar2)
    }

    fun Update(deltaTime: Float) {

    }
}