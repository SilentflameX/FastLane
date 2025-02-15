package com.example.multiplayertest

import androidx.xr.runtime.math.Vector2
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.math.clamp
import com.example.multiplayertest.GameObjects.GameObject
import com.example.multiplayertest.GameObjects.NetworkedObject
import java.lang.Float.min
import java.lang.Float.max
import kotlin.random.Random


object GameScene {
    var goList = mutableListOf<GameObject>() //Used to store dynamic objects
    var worldObjectList = mutableListOf<GameObject>() //Used to store static world objects ie. walls
    lateinit var myPlayer : NetworkedObject
    lateinit var floor : GameObject

    private val playerMoveSpeed = 0.005f
    var playerDirection = Vector3(0f,0f,0f)
    var playerMoveX = 0f

    var carList = mutableListOf<GameObject>() //Used to track cars
    val randomGenerator = Random(1)

    fun Start()
    {
        LoadLevel1()
        while(!this::myPlayer.isInitialized){}//wait
        //blueCar2.UpdateVariable()
        myPlayer.sprite.scale = Vector3(1.2f,2.4f,1f)
        myPlayer.sprite.position = Vector3(0f, 0f, 0f)
        myPlayer.UpdateSyncedData("Sprite",R.drawable.car_blue_1);

    }

    fun Update(deltaTime: Float) {
        //Player driving
        MovePlayer(Vector3(playerMoveX,0.1f,0f))

        //Update floor
        var floorDist = myPlayer.sprite.position.y - floor.sprite.position.y
        if(floorDist >= 10f)
            floor.sprite.position += Vector3(0f,floorDist,0f)

        //Update cars
        for(car in carList){
            if( myPlayer.sprite.position.y - car.sprite.position.y >= 15f){
                car.sprite.position = RandomCarPosition()
            }
        }

        glRenderer.UpdateCamera(myPlayer.sprite.position)
    }

    fun PLayerInput(xDelta : Float){
        //Sensitivity
        playerMoveX = xDelta * 0.1f

        //Deadzone check
        if(playerMoveX < 0.05f && playerMoveX > -0.05f)
            playerMoveX = 0.0f

        //Max turn speed
        playerMoveX = clamp(playerMoveX,-0.3f,0.3f)
    }

    fun MovePlayer(dir : Vector3) {
        //Moves player in direction
        if (!this::myPlayer.isInitialized)
            return


        var playerNewPos = myPlayer.sprite.position + dir

        //Check for collision first
        for (go in goList) {
            if(go == myPlayer)
                continue

            val x1 = go.sprite.position.x + go.sprite.scale.x / 2
            val x2 = go.sprite.position.x - go.sprite.scale.x / 2
            val y1 = go.sprite.position.y + go.sprite.scale.y / 2
            val y2 = go.sprite.position.y - go.sprite.scale.y / 2
            val min = Vector2(min(x1, x2), min(y1, y2))
            val max = Vector2(max(x1, x2), max(y1, y2))

            val result = CircleAABB(
                Vector2(myPlayer.sprite.position.x, myPlayer.sprite.position.y),
                Vector2(playerNewPos.x, playerNewPos.y),
                myPlayer.sprite.scale.x / 2,
                min,
                max
            )
            if (result.first) {
                playerNewPos += Vector3(result.second.x, result.second.y, playerNewPos.z)
                break
            }
        }

        //Clamp player x pos
        playerNewPos = Vector3(clamp(playerNewPos.x,-8.75f,8.75f),playerNewPos.y,0f)

        //Update player position
        myPlayer.UpdateSyncedData("Position", playerNewPos)
        return
    }



    private fun CircleAABB(circleOriPos : Vector2, circlePos : Vector2, circleRadius : Float, min : Vector2, max : Vector2) : Pair<Boolean , Vector2> {
        var closestPointToCircleX = circlePos.x
        var closestPointToCircleY = circlePos.y

        if (closestPointToCircleX < min.x)
            closestPointToCircleX = min.x
        else if (closestPointToCircleX > max.x)
            closestPointToCircleX = max.x

        if (closestPointToCircleY < min.y)
            closestPointToCircleY = min.y
        else if (closestPointToCircleY > max.y)
            closestPointToCircleY = max.y

        var circleToBox = circlePos - Vector2(closestPointToCircleX, closestPointToCircleY)
        var collided = circleToBox.lengthSquared <= circleRadius * circleRadius;
        var pushback = Vector2()
        if (collided) {
            //Find edge and find normal
            if (circleOriPos.y > max.y)//top
                pushback = Vector2(0f, circleRadius - (circlePos.y - max.y))
            else if (circleOriPos.y < min.y)//bottom
                pushback = Vector2(0f, - (circleRadius - (min.y - circlePos.y)))
            else if (circleOriPos.x < min.x)//left
                pushback = Vector2(-(circleRadius - (min.x - circlePos.x)),0f)
            else//right
                pushback = Vector2(circleRadius - (circlePos.x - max.x),0f)
        }
        return Pair(collided, pushback)
    }


    //Lane positions = -8.55, -5.78, -2.85, 0, 2.85, 5.78, 8.55
    private fun LoadLevel1(){
        floor = GameObject()
        floor.sprite.position = Vector3(0f,0f,0f)
        floor.sprite.scale = Vector3(40f,40f,1f)
        floor.sprite.LoadSprite(R.drawable.road)
        worldObjectList.add(floor)

        CreateCar(RandomCarPosition() + Vector3(0f,0f,0f),Vector3(1.2f,0f,1f))
        CreateCar(RandomCarPosition() + Vector3(0f,0f,0f),Vector3(1.2f,2.5f,1f))
        CreateCar(RandomCarPosition() + Vector3(0f,5f,0f),Vector3(1.2f,2.5f,1f))
        CreateCar(RandomCarPosition() + Vector3(0f,5f,0f),Vector3(1.2f,2.5f,1f))
        CreateCar(RandomCarPosition() + Vector3(0f,10f,0f),Vector3(1.2f,2.5f,1f))
        CreateCar(RandomCarPosition() + Vector3(0f,10f,0f),Vector3(1.2f,2.5f,1f))
        CreateCar(RandomCarPosition() + Vector3(0f,15f,0f),Vector3(1.2f,2.5f,1f))
        CreateCar(RandomCarPosition() + Vector3(0f,15f,0f),Vector3(1.2f,2.5f,1f))
        CreateCar(RandomCarPosition() + Vector3(0f,20f,0f),Vector3(1.2f,2.5f,1f))
        CreateCar(RandomCarPosition() + Vector3(0f,20f,0f),Vector3(1.2f,2.5f,1f))

        //CreateCar(Vector3(2.85f,22.5f,0f),Vector3(1.2f,2.5f,1f))
        //CreateCar(Vector3(5.7f,22.5f,0f),Vector3(1.2f,2.5f,1f))
        //CreateCar(Vector3(8.55f,10.5f,0f),Vector3(1.2f,2.5f,1f))
        //CreateCar(Vector3(-2.85f,25.5f,0f),Vector3(1.2f,2.5f,1f))
        //CreateCar(Vector3(-5.7f,25.5f,0f),Vector3(1.2f,2.5f,1f))
        //CreateCar(Vector3(-8.55f,15.5f,0f),Vector3(1.2f,2.5f,1f))
    }

    private fun CreateCar(position :Vector3, scale :Vector3){
        var car = GameObject()
        car.sprite.position = position
        car.sprite.scale = scale
        car.sprite.LoadSprite(R.drawable.car_black_1)
        goList.add(car)
        carList.add(car)
    }

    private fun RandomCarPosition() : Vector3{
        //Get which lane first
        var x = 0f
        var lane = randomGenerator.nextInt(0,6)
        when(lane){
            0 -> x = -8.55f
            1 -> x = -5.7f
            2 -> x = -2.85f
            3 -> x = 0f
            4 -> x = 2.85f
            5 -> x = -5.7f
            6 -> x = 8.55f
        }

        //we generate from 15 to 30 above player
        var y = randomGenerator.nextFloat() * (15f) + 15f + myPlayer.sprite.position.y
        return Vector3(x,y,0f)
    }


}