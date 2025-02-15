package com.example.multiplayertest

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.multiplayertest.GameScene.myPlayer
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class MainMenu : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_menu)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val hostButton: Button = findViewById(R.id.hostButton)
        hostButton.setOnClickListener {
            val ipText: TextInputEditText = findViewById(R.id.ipAddress)
            val portText: TextInputEditText = findViewById(R.id.port)

            var serverStarted = KtorServer.StartServer(ipText.text.toString(), portText.text.toString().toInt())

            if(serverStarted) {
                val intent = Intent(this@MainMenu, MainActivity::class.java)
                startActivity(intent)
            }
        }

        val joinButton: Button = findViewById(R.id.joinButton)
        joinButton.setOnClickListener {
            val ipText: TextInputEditText = findViewById(R.id.ipAddress)
            val portText: TextInputEditText = findViewById(R.id.port)
            var clientConnected = KtorClient.ConnectToServer(ipText.text.toString(), portText.text.toString().toInt())

            if(clientConnected) {
                val intent = Intent(this@MainMenu, MainActivity::class.java)
                startActivity(intent)
            }
        }
    }
}