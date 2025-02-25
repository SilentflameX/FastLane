package com.example.multiplayertest

import android.widget.ImageView
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.math.clamp
import com.example.multiplayertest.gameobjects.GameObject
import com.example.multiplayertest.gameobjects.NetworkedObject
import com.example.multiplayertest.gameobjects.NetworkedVar
import java.lang.Thread.sleep
import kotlin.concurrent.thread
import kotlin.random.Random


object GameScene {
    var goList = mutableListOf<GameObject>() //Used to store dynamic objects ie.cars
    var worldObjectList = mutableListOf<GameObject>() //Used to store static world objects ie. floor/walls
    var myPlayer : NetworkedObject? = null
    private var floor : GameObject? = null
    var healthIconList = mutableListOf<ImageView>()

    private var playerMoveX = 0f
    private var playerMoveY = 0f
    private var playerInvulnDuration = 0f
    private var playerBraking = false
    private var playerAccelerating = false
    var playerScore = 0

    private var carList = mutableListOf<GameObject>() //Used to track cars
    var randomGenerator = Random(1)
    private val carSpriteList = listOf(R.drawable.car_black_4, R.drawable.car_blue_4, R.drawable.car_green_4, R.drawable.car_red_4, R.drawable.car_yellow_4)


    //Player stats
    private const val INVULNERABLE_DURATION = 3.0f
    private const val PLAYER_ACCELERATION = 0.04f
    private const val PLAYER_BRAKE = 0.3f
    private const val PLAYER_DECELERATION = 0.01f
    private var playerLifes = 3

    private const val PLAYER_MAX_TURN_SPEED = 0.3f
    private const val PLAYER_TURN_SENSITIVITY = 0.2f

    var gamePaused = false

    fun reset(){
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

        gamePaused = false
    }

    fun start() {
        while (myPlayer == null) { sleep(100)}//wait

        loadLevel1()
        //Load all car sprites
        var carID = 0
        for(car in KtorClient.networkedObjectList){
            val spriteID = car.value.syncedVariables["Sprite"]
            if(spriteID == null) {
                car.value.sprite.loadSprite(R.drawable.car_black_1)
            }
            else{
                car.value.sprite.loadSprite((spriteID as NetworkedVar<*>).value as Int)
            }
            car.value.sprite.scale = Vector3(1.2f, 2.4f, 1f)
            car.value.sprite.position = Vector3(-2.85f + (2.85f * carID++), 0f, 0f)
        }
    }

    fun update(deltaTime: Float) {
        if(myPlayer == null || gamePaused)
            return

        //Update pedals
        if(playerAccelerating){
            playerMoveY += PLAYER_ACCELERATION * deltaTime
        }
        else
        {
            if (playerMoveY > 0f) {
                playerMoveY -= if(playerBraking) {
                    PLAYER_BRAKE * deltaTime
                } else{
                    PLAYER_DECELERATION * deltaTime
                }
            }
            else {
                playerMoveY = 0f
            }
        }

        //Player driving
        movePlayer(Vector3(playerMoveX, playerMoveY,0f))

        //Update floor
        val floorDist = myPlayer!!.sprite.position.y + 5 - floor!!.sprite.position.y
        if(floorDist >= 10f)
            floor!!.sprite.position += Vector3(0f,floorDist,0f)

        //Update cars
        for(car in carList){
            if( myPlayer!!.sprite.position.y - car.sprite.position.y >= 15f){
                car.sprite.position = randomCarPosition()
                car.sprite.loadSprite(carSpriteList.random(randomGenerator))
            }
        }

        //Update Camera to follow
        MyGLRenderer.glRenderer().updateCamera(myPlayer!!.sprite.position + Vector3(0f,5f,0f))
        //Update Score
        playerScore = myPlayer!!.sprite.position.y.toInt()
        MainActivity.getInstance().updateScoreText()
    }

    fun playerInput(xDelta : Float){
        //Deadzone check
        if(xDelta < 0.01f && xDelta > -0.01f)
            return

        //Sensitivity
        playerMoveX = xDelta  * playerMoveY * PLAYER_TURN_SENSITIVITY

        //Max turn speed
        playerMoveX = clamp(playerMoveX,-PLAYER_MAX_TURN_SPEED,PLAYER_MAX_TURN_SPEED)
    }

    private fun movePlayer(dir : Vector3) {
        //Moves player in direction
        if (myPlayer == null || playerLifes == 0)
            return


        var playerNewPos = myPlayer!!.sprite.position + dir

        //Check for collision first
        //We skip if player is invuln
        if(playerInvulnDuration <= 0f) {
            for (go in goList) {
                if (go == myPlayer)
                    continue
                //If there is collision
                if (aabbCollision(myPlayer!!,go)) {
                    //We move player back
                    //playerNewPos += Vector3(result.second.x, result.second.y, playerNewPos.z)
                    //We make player blinking and invulnerable
                    playerInvulnDuration = INVULNERABLE_DURATION
                    playerLifes -= 1
                    if(playerLifes == 0){
                        MainActivity.getInstance().gameOver()
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
        myPlayer!!.updateSyncedData("Position", playerNewPos)
        return
    }

    fun playerBrake(pressed : Boolean){
        playerBraking = pressed
    }

    fun playerAccelerate(pressed : Boolean){
        playerAccelerating = pressed
    }

    private fun aabbCollision(go1: GameObject, go2: GameObject): Boolean
    {
        val w1 = go1.sprite.scale.x / 2
        val w2 = go2.sprite.scale.x / 2
        val h1 = go1.sprite.scale.y / 2
        val h2 = go2.sprite.scale.y / 2

        return aabbCollision(go1.sprite.position.x, go2.sprite.position.x, go1.sprite.position.y, go2.sprite.position.y, w1, w2, h1, h2)
    }

    private fun aabbCollision(x1 : Float, x2 : Float, y1 : Float, y2 : Float, w1 : Float, w2 : Float, h1 : Float, h2 : Float) : Boolean {
        return  (x1 - w1 < x2 + w2) && (x1 + w1 > x2 - w2) &&
                (y1 - h1 < y2 + h2) && (y1 + h1 > y2 - h2)
    }

    //Lane positions = -8.55, -5.78, -2.85, 0, 2.85, 5.78, 8.55
    private fun loadLevel1(){
        floor = GameObject()
        floor!!.sprite.position = Vector3(0f,0f,0f)
        floor!!.sprite.scale = Vector3(40f,40f,1f)
        floor!!.sprite.loadSprite(R.drawable.road)
        worldObjectList.add(floor!!)

        createCar(randomCarPosition(),Vector3(1.2f,2.5f,1f))
        createCar(randomCarPosition(),Vector3(1.2f,2.5f,1f))
        createCar(randomCarPosition(),Vector3(1.2f,2.5f,1f))
        createCar(randomCarPosition(),Vector3(1.2f,2.5f,1f))
        createCar(randomCarPosition(),Vector3(1.2f,2.5f,1f))
        createCar(randomCarPosition(),Vector3(1.2f,2.5f,1f))
        createCar(randomCarPosition(),Vector3(1.2f,2.5f,1f))
        createCar(randomCarPosition(),Vector3(1.2f,2.5f,1f))
        createCar(randomCarPosition(),Vector3(1.2f,2.5f,1f))
        createCar(randomCarPosition(),Vector3(1.2f,2.5f,1f))
    }

    private fun createCar(position :Vector3, scale :Vector3){
        val car = GameObject()
        car.sprite.position = position
        car.sprite.scale = scale
        car.sprite.loadSprite(carSpriteList.random(randomGenerator))
        goList.add(car)
        carList.add(car)
    }

    private fun randomCarPosition() : Vector3{
        //Get which lane first
        var x = 0f
        val lane = randomGenerator.nextInt(0,7)
        when(lane){
            0 -> x = -8.55f
            1 -> x = -5.7f
            2 -> x = -2.85f
            3 -> x = 0f
            4 -> x = 2.85f
            5 -> x = 5.7f
            6 -> x = 8.55f
        }

        //we generate from 15 to 45 above player
        var y = randomGenerator.nextFloat() * (30f) + 15f + myPlayer!!.sprite.position.y
        //we also check and make sure there are no cars that we will collide with, if not we move up)
        var collision = true
        while(collision){
            collision = false
            for (go in goList) {
                if(aabbCollision(go.sprite.position.x,x,go.sprite.position.y,y,go.sprite.scale.x/2,go.sprite.scale.x/2,go.sprite.scale.y/2,go.sprite.scale.y/2)) {
                    collision = true
                    y += 5
                    break
                }
            }
        }
        return Vector3(x,y,0f)
    }
}