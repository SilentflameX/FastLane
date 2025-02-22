package com.example.multiplayertest

import MyGLRenderer
import android.app.Activity
import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import androidx.xr.runtime.math.Vector2
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.math.clamp
import com.example.multiplayertest.GameObjects.GameObject
import com.example.multiplayertest.GameObjects.NetworkedObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Float.max
import java.lang.Float.min
import java.lang.Thread.sleep
import kotlin.concurrent.thread
import kotlin.math.floor
import kotlin.random.Random


object GameScene {
    var goList = mutableListOf<GameObject>() //Used to store dynamic objects
    var worldObjectList = mutableListOf<GameObject>() //Used to store static world objects ie. walls
    var myPlayer : NetworkedObject? = null
    var floor : GameObject? = null
    var healthIconList = mutableListOf<ImageView>()

    var playerMoveX = 0f
    var playerMoveY = 0f
    var playerInvulnDuration = 0f
    var playerBraking = false
    var playerAccelerating = false
    var playerScore = 0

    var carList = mutableListOf<GameObject>() //Used to track cars
    val randomGenerator = Random(1)


    //Player stats
    val InvulnerableDuration = 3.0f
    val PlayerAcceleration = 0.05f
    val PlayerBrake = 0.3f
    val PlayerDeclaration = 0.01f
    var playerLifes = 3


    fun Reset(){
        goList.clear()
        worldObjectList.clear()
        myPlayer = null
        floor = null
        healthIconList.clear()
        carList.clear()

        playerMoveX = 0f
        playerMoveY = 0f
        playerInvulnDuration = 0f
        playerBraking = false
        playerAccelerating = false
        playerScore = 0
        playerLifes = 3
    }

    fun Start() {
        while (myPlayer == null) {}//wait

        LoadLevel1()
        //blueCar2.UpdateVariable()
        myPlayer!!.sprite.scale = Vector3(1.2f, 2.4f, 1f)
        myPlayer!!.sprite.position = Vector3(0f, 0f, 0f)
        myPlayer!!.UpdateSyncedData("Position", myPlayer!!.sprite.position)
        myPlayer!!.UpdateSyncedData("Sprite", R.drawable.car_blue_1)
        myPlayer!!.UpdateSyncedData("Scale", myPlayer!!.sprite.scale)
    }

    fun Update(deltaTime: Float) {
        if(myPlayer == null)
            return
        //Update pedals
        if(playerAccelerating){
            playerMoveY += PlayerAcceleration * deltaTime
        }
        else
        {
            if (playerMoveY > 0f) {
                if(playerBraking) {
                    playerMoveY -= PlayerBrake * deltaTime
                }
                else{
                    playerMoveY -= PlayerDeclaration * deltaTime
                }
            }
            else {
                playerMoveY = 0f
            }
        }

        //Player driving
        MovePlayer(Vector3(playerMoveX, playerMoveY,0f))

        //Update floor
        var floorDist = myPlayer!!.sprite.position.y + 5 - floor!!.sprite.position.y
        if(floorDist >= 10f)
            floor!!.sprite.position += Vector3(0f,floorDist,0f)

        //Update cars
        for(car in carList){
            if( myPlayer!!.sprite.position.y - car.sprite.position.y >= 15f){
                car.sprite.position = RandomCarPosition()
            }
        }

        //Update Camera to follow
        MyGLRenderer.glRenderer().UpdateCamera(myPlayer!!.sprite.position + Vector3(0f,5f,0f))
        //Update Score
        playerScore = floor(myPlayer!!.sprite.position.y * 1).toInt()
        MainActivity.GetInstance().UpdateScoreText()
    }

    fun PLayerInput(xDelta : Float){
        //Deadzone check
        if(xDelta < 0.01f && xDelta > -0.01f)
            return

        //Sensitivity
        playerMoveX = xDelta  * playerMoveY * 0.2f

        //Max turn speed
        playerMoveX = clamp(playerMoveX,-0.3f,0.3f)
    }

    fun MovePlayer(dir : Vector3) {
        //Moves player in direction
        if (myPlayer == null || playerLifes == 0)
            return


        var playerNewPos = myPlayer!!.sprite.position + dir

        //Check for collision first
        //We skip if player is invuln
        if(playerInvulnDuration > 0f) {

        }
        else{
            for (go in goList) {
                if (go == myPlayer)
                    continue
//If there is collision
                if (AABBCollision(myPlayer!!,go)) {
                    //We move player back
                    //playerNewPos += Vector3(result.second.x, result.second.y, playerNewPos.z)
                    //We make player blinking and invulnerable
                    playerInvulnDuration = InvulnerableDuration
                    playerLifes -= 1
                    if(playerLifes == 0){
                        MainActivity.GetInstance().GameOver()
                    }
                    //Reduce lifeIcon
                    healthIconList[playerLifes].alpha = 0f
                    thread {
                        while (myPlayer != null && playerInvulnDuration > 0f) {
                            if (myPlayer!!.sprite.alpha == 0.2f)
                                myPlayer!!.sprite.alpha = 1f
                            else
                                myPlayer!!.sprite.alpha = 0.2f
                            sleep(200)
                            playerInvulnDuration -= 0.2f
                        }
                        if (myPlayer != null) {
                            myPlayer!!.sprite.alpha = 1f
                            playerInvulnDuration = 0f
                        }
                    }
                    break
                }

            }
        }

        //Clamp player x pos
        playerNewPos = Vector3(clamp(playerNewPos.x,-8.75f,8.75f),playerNewPos.y,0f)

        //Update player position
        myPlayer!!.UpdateSyncedData("Position", playerNewPos)
        return
    }

    fun PlayerBrake(pressed : Boolean){
        playerBraking = pressed
    }

    fun PlayerAccelerate(pressed : Boolean){
        playerAccelerating = pressed
    }

    private fun AABBCollision(go1:GameObject, go2:GameObject): Boolean
    {
        val w1 = go1.sprite.scale.x / 2
        val w2 = go2.sprite.scale.x / 2
        val h1 = go1.sprite.scale.y / 2
        val h2 = go2.sprite.scale.y / 2

        return  go1.sprite.position.x - w1 < go2.sprite.position.x + w2 &&  // Right edge of A doesn't pass left edge of B
                go1.sprite.position.x + w1 > go2.sprite.position.x - w2 &&  // Left edge of A doesn't pass right edge of B
                go1.sprite.position.y - h1 < go2.sprite.position.y + h2 &&  // Bottom edge of A doesn't pass top edge of B
                go1.sprite.position.y + h1 > go2.sprite.position.y - h2    // Top edge of A doesn't pass bottom edge of B
    }

    private fun AABBCollision(x1 : Float,x2 : Float,y1 : Float,y2 : Float,w1 : Float,w2 : Float,h1 : Float,h2 : Float) : Boolean {
        return  (x1 - w1 < x2 + w2) && (x1 + w1 > x2 - w2) &&
                (y1 - h1 < y2 + h2) && (y1 + h1 > y2 - h2)
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
        floor!!.sprite.position = Vector3(0f,0f,0f)
        floor!!.sprite.scale = Vector3(40f,40f,1f)
        floor!!.sprite.LoadSprite(R.drawable.road)
        worldObjectList.add(floor!!)

        CreateCar(RandomCarPosition() + Vector3(0f,0f,0f),Vector3(1.2f,2.5f,1f))
        CreateCar(RandomCarPosition() + Vector3(0f,0f,0f),Vector3(1.2f,2.5f,1f))
        CreateCar(RandomCarPosition() + Vector3(0f,5f,0f),Vector3(1.2f,2.5f,1f))
        CreateCar(RandomCarPosition() + Vector3(0f,5f,0f),Vector3(1.2f,2.5f,1f))
        CreateCar(RandomCarPosition() + Vector3(0f,10f,0f),Vector3(1.2f,2.5f,1f))
        CreateCar(RandomCarPosition() + Vector3(0f,10f,0f),Vector3(1.2f,2.5f,1f))
        CreateCar(RandomCarPosition() + Vector3(0f,15f,0f),Vector3(1.2f,2.5f,1f))
        CreateCar(RandomCarPosition() + Vector3(0f,15f,0f),Vector3(1.2f,2.5f,1f))
        CreateCar(RandomCarPosition() + Vector3(0f,20f,0f),Vector3(1.2f,2.5f,1f))
        CreateCar(RandomCarPosition() + Vector3(0f,20f,0f),Vector3(1.2f,2.5f,1f))
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
        var lane = randomGenerator.nextInt(0,7)
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
        var y = randomGenerator.nextFloat() * (15f) + 15f + myPlayer!!.sprite.position.y
        //we also check and make sure there are no cars that we will collide with, if not we move up)
        var collision = true
        while(collision){
            collision = false
            for (go in goList) {
                if(AABBCollision(go.sprite.position.x,x,go.sprite.position.y,y,go.sprite.scale.x/2,go.sprite.scale.x/2,go.sprite.scale.y/2,go.sprite.scale.y/2)){
                    collision = true
                    y += 5
                    break
                }
            }
        }
        return Vector3(x,y,0f)
    }
}