package com.example.multiplayertest

import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.multiplayertest.GameScene.myPlayer
import com.example.multiplayertest.gameobjects.NetworkedVar
import com.google.android.material.textfield.TextInputEditText
import kotlin.random.Random


class MainMenu : ComponentActivity() {
    init {
        instance = this
    }

    companion object {
        private var instance: MainMenu? = null

        fun getInstance() : MainMenu{
            return instance!!
        }

    }

    private var selectedProfileID = 0
    private lateinit var backgroundAnimation: AnimationDrawable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fullscreen mode
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
        enableEdgeToEdge()
        loadMainButtons()
    }

    override fun onStart() {
        super.onStart()
        backgroundAnimation.start()
    }

    private fun loadMainButtons(){

        setContentView(R.layout.activity_main_menu)
/*        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }*/


        findViewById<ImageView>(R.id.backgroundAnim).apply {
            setBackgroundResource(R.drawable.background_animation)
            backgroundAnimation = background as AnimationDrawable
        }
        backgroundAnimation.start()


        //Host server and connect ourselves
        val singlePlayer: Button = findViewById(R.id.SingleplayerButton)
        singlePlayer.setOnClickListener {
            KtorServer.startServer("0.0.0.0", getString(R.string.defaultPort).toInt())
            startGame()
        }

        //Show MultiplayerLayout and hide MainLayout
        val multiplayer: Button = findViewById(R.id.MultiplayerButton)
        multiplayer.setOnClickListener {
            (findViewById<ConstraintLayout>(R.id.MainLayout)!!).visibility = INVISIBLE
            (findViewById<ConstraintLayout>(R.id.MultiplayerLayout)!!).visibility = VISIBLE
        }

        //Show ConnectLayout and hide MultiplayerLayout
        val join: Button = findViewById(R.id.JoinButton)
        join.setOnClickListener {
            (findViewById<ConstraintLayout>(R.id.MultiplayerLayout)!!).visibility = INVISIBLE
            (findViewById<ConstraintLayout>(R.id.ConnectLayout)!!).visibility = VISIBLE
        }


        val hostButton: Button = findViewById(R.id.HostButton)
        hostButton.setOnClickListener {
            //We start server
            KtorServer.startServer("0.0.0.0", getString(R.string.defaultPort).toInt())
            loadLobby()
        }

        val backMultiplayer: Button = findViewById(R.id.backButton_Multiplayer)
        backMultiplayer.setOnClickListener {
            (findViewById<ConstraintLayout>(R.id.MainLayout)!!).visibility = VISIBLE
            (findViewById<ConstraintLayout>(R.id.MultiplayerLayout)!!).visibility = INVISIBLE
        }


        val connectButton: Button = findViewById(R.id.ConnectButton)
        connectButton.setOnClickListener {
            val ipText: TextInputEditText = findViewById(R.id.ipAddress)
            KtorClient.connectToServer(ipText.text.toString(), getString(R.string.defaultPort).toInt())
            loadLobby()
        }

        val backConnect: Button = findViewById(R.id.backButton_Connect)
        backConnect.setOnClickListener {
            (findViewById<ConstraintLayout>(R.id.MainLayout)!!).visibility = VISIBLE
            (findViewById<ConstraintLayout>(R.id.ConnectLayout)!!).visibility = INVISIBLE
        }
    }
    fun startGame(){
        val intent = Intent(this@MainMenu, MainActivity::class.java)
        startActivity(intent)
    }

    private fun loadMainMenu(){
        runOnUiThread {
            setContentView(R.layout.activity_main_menu)
            loadMainButtons()
        }
    }

    private fun loadLobby() {
        runOnUiThread {
            //Set new view
            setContentView(R.layout.activity_lobby)
            //Set host ip
            if (KtorServer.isServer)
                findViewById<TextView>(R.id.HostIP).text = KtorServer.getServerIpAddress()
            else
                findViewById<TextView>(R.id.HostIP).text = KtorClient.hostIP

            val backLobby: Button = findViewById(R.id.backButton_Lobby)
            backLobby.setOnClickListener {
                //Close client
                if(KtorClient.networkID != -1)
                    KtorClient.disconnect()
                //Shut down server if we hosted
                if (KtorServer.isServer)
                    KtorServer.shutdownServer()


                loadMainMenu()
            }

            val startButton: Button = findViewById(R.id.StartButton)
            startButton.setOnClickListener {
                //Opens Waiting for players screen
                //Message all to start
                KtorServer.sendStartGameMessage(Random.nextInt())
            }
            //If not server we disable the start button
            if (!KtorServer.isServer)
                startButton.visibility = INVISIBLE

            updateLobby()
        }
    }

    fun updateLobby(){
        runOnUiThread {
            if(findViewById<ConstraintLayout>(R.id.Player1) == null)
                return@runOnUiThread
            if (KtorClient.totalClientCount > 0) {
                (findViewById<ConstraintLayout>(R.id.Player1)!!).visibility = VISIBLE

                val button = findViewById<ImageButton>(R.id.Player1Profile)
                val nValue = KtorClient.networkedObjectList[0]?.syncedVariables?.get("Sprite")
                if (nValue != null)
                    button.setImageResource((nValue as NetworkedVar<*>).value as Int)

                if (KtorClient.networkID == 0)
                    button.isEnabled = true
                else
                    button.isEnabled = false

                button.setOnClickListener {
                    if (++selectedProfileID >= KtorClient.profileSpriteList.count())
                        selectedProfileID = 0
                    myPlayer!!.updateSyncedData("Sprite", KtorClient.profileSpriteList[selectedProfileID])
                    KtorClient.update(0f)
                }
            }
            if (KtorClient.totalClientCount > 1) {
                (findViewById<ConstraintLayout>(R.id.Player2)!!).visibility = VISIBLE

                val button = findViewById<ImageButton>(R.id.Player2Profile)
                val nValue = KtorClient.networkedObjectList[1]?.syncedVariables?.get("Sprite")
                if(nValue != null)
                    button.setImageResource((nValue as NetworkedVar<*>).value as Int)

                if(KtorClient.networkID == 1)
                    button.isEnabled = true
                else
                    button.isEnabled = false

                button.setOnClickListener {
                    if(++selectedProfileID >= KtorClient.profileSpriteList.count())
                        selectedProfileID = 0
                    myPlayer!!.updateSyncedData("Sprite", KtorClient.profileSpriteList[selectedProfileID])
                    KtorClient.update(0f)
                }
            }
            if (KtorClient.totalClientCount > 2) {
                (findViewById<ConstraintLayout>(R.id.Player3)!!).visibility = VISIBLE

                val button = findViewById<ImageButton>(R.id.Player3Profile)
                val nValue = KtorClient.networkedObjectList[2]?.syncedVariables?.get("Sprite")
                if(nValue != null)
                    button.setImageResource((nValue as NetworkedVar<*>).value as Int)

                if(KtorClient.networkID == 2)
                    button.isEnabled = true
                else
                    button.isEnabled = false

                button.setOnClickListener {
                    if(++selectedProfileID >= KtorClient.profileSpriteList.count())
                        selectedProfileID = 0
                    myPlayer!!.updateSyncedData("Sprite", KtorClient.profileSpriteList[selectedProfileID])
                    KtorClient.update(0f)
                }
            }
            if (KtorClient.totalClientCount > 3) {
                (findViewById<ConstraintLayout>(R.id.Player4)!!).visibility = VISIBLE

                val button = findViewById<ImageButton>(R.id.Player4Profile)
                val nValue = KtorClient.networkedObjectList[3]?.syncedVariables?.get("Sprite")
                if(nValue != null)
                    button.setImageResource((nValue as NetworkedVar<*>).value as Int)

                if(KtorClient.networkID == 3)
                    button.isEnabled = true
                else
                    button.isEnabled = false

                button.setOnClickListener {
                    if(++selectedProfileID >= KtorClient.profileSpriteList.count())
                        selectedProfileID = 0
                    myPlayer!!.updateSyncedData("Sprite", KtorClient.profileSpriteList[selectedProfileID])
                    KtorClient.update(0f)
                }
            }
        }
    }
}