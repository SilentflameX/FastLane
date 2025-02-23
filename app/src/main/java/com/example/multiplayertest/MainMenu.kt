package com.example.multiplayertest

import KtorClient
import KtorServer
import android.content.Context
import android.content.Intent
import android.graphics.Paint.Join
import android.os.Bundle
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.multiplayertest.GameScene.myPlayer
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlin.concurrent.thread
import android.widget.Toast

class MainMenu : ComponentActivity() {
    init {
        instance = this
    }

    companion object {
        private var instance: MainMenu? = null

        fun applicationContext() : Context {
            return instance!!.applicationContext
        }

        fun GetInstance() : MainMenu{
            return instance!!
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fullscreenmode
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
        enableEdgeToEdge()

        setContentView(R.layout.activity_main_menu)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //Host server and connect ourselves
        val SinglePlayer: Button = findViewById(R.id.SingleplayerButton)
        SinglePlayer.setOnClickListener {
            var serverStarted = KtorServer.StartServer("0.0.0.0", 8080)
            if (serverStarted) {
                StartGame()
            }
        }

        //Show multiplayerlayout and hide mainlayout
        val Multiplayer: Button = findViewById(R.id.MultiplayerButton)
        Multiplayer.setOnClickListener {
            (findViewById<ConstraintLayout>(R.id.MainLayout)!!).visibility = INVISIBLE
            (findViewById<ConstraintLayout>(R.id.MultiplayerLayout)!!).visibility = VISIBLE
        }

        //Show connectlayout and hide multiplayerlayout
        val Join: Button = findViewById(R.id.JoinButton)
        Join.setOnClickListener {
            (findViewById<ConstraintLayout>(R.id.MultiplayerLayout)!!).visibility = INVISIBLE
            (findViewById<ConstraintLayout>(R.id.ConnectLayout)!!).visibility = VISIBLE
        }


        val hostButton: Button = findViewById(R.id.HostButton)
        hostButton.setOnClickListener {
            //We start server
            var serverStarted = KtorServer.StartServer("0.0.0.0", 8080)
            if (serverStarted) {
                //Opens Waiting for players screen
                LoadLobby()
            }
        }


        val connectButton: Button = findViewById(R.id.ConnectButton)
        connectButton.setOnClickListener {
            val ipText: TextInputEditText = findViewById(R.id.ipAddress)
            val ipAddress = ipText.text.toString().trim()

            if (!isValidIPAddress(ipAddress)) {
                runOnUiThread {
                    Toast.makeText(this, "Invalid IP Address! Please enter a valid IP.", Toast.LENGTH_LONG).show()
                }
                return@setOnClickListener  // Prevents connection attempt
            }

            val clientConnected = KtorClient.ConnectToServer(ipAddress, 8080)
            if (clientConnected) {
                LoadLobby()
            } else {
                runOnUiThread {
                    Toast.makeText(this, "Failed to connect. Check IP or server availability.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun StartGame(){
        val intent = Intent(this@MainMenu, MainActivity::class.java)
        startActivity(intent)
    }

    fun LoadLobby() {
        //Set new view
        setContentView(R.layout.activity_lobby)
        //Set host ip
        if (KtorServer.isServer)
            findViewById<TextView>(R.id.HostIP).text = KtorServer.GetServerIpAddress()
        else
            findViewById<TextView>(R.id.HostIP).text = KtorClient.hostIP

        val startButton: Button = findViewById(R.id.StartButton)
        startButton.setOnClickListener {
            //Opens Waiting for players screen
            //Message all to start
            KtorServer.SendStartGameMessage()
        }
        //If not server we disable the start button
        if (!KtorServer.isServer)
            startButton.visibility = INVISIBLE

        UpdateLobby()
    }

    fun UpdateLobby(){
        runOnUiThread {
            if(findViewById<ConstraintLayout>(R.id.Player1) == null)
                return@runOnUiThread
            if (KtorClient.totalClientCount >= 1) {
                (findViewById<ConstraintLayout>(R.id.Player1)!!).visibility = VISIBLE
            }
            if (KtorClient.totalClientCount >= 2) {
                (findViewById<ConstraintLayout>(R.id.Player2)!!).visibility = VISIBLE
            }
            if (KtorClient.totalClientCount >= 3) {
                (findViewById<ConstraintLayout>(R.id.Player3)!!).visibility = VISIBLE
            }
            if (KtorClient.totalClientCount >= 4) {
                (findViewById<ConstraintLayout>(R.id.Player4)!!).visibility = VISIBLE
            }
        }
    }

    fun isValidIPAddress(ip: String): Boolean {
        val ipPattern =
            Regex("^((25[0-5]|2[0-4][0-9]|[0-1]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[0-1]?[0-9][0-9]?)$")

        // Reject "0.0.0.0" explicitly
        return ipPattern.matches(ip) && ip != "0.0.0.0"
    }
}