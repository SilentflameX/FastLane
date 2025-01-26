package com.example.multiplayertest

import KtorClient
import KtorServer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import MyGLRenderer
import android.opengl.GLSurfaceView
import android.widget.Button
import android.widget.Toast

private val serverScope = CoroutineScope(Dispatchers.IO)
private val clientScope = CoroutineScope(Dispatchers.IO)


lateinit var glSurfaceView: GLSurfaceView
lateinit var glRenderer: MyGLRenderer
var serverConnected = false
var clientConnected = false


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Initialize the GLSurfaceView
        //glSurfaceView = findViewById(R.id.glSurfaceView)
        //glRenderer = MyGLRenderer(this)
        //glSurfaceView = MyGLSurfaceView(this,glRenderer)
        // Set the GLSurfaceView as the content view
        //setContentView(glSurfaceView)

        glSurfaceView = findViewById(R.id.glSurfaceView)
        glSurfaceView.setEGLContextClientVersion(2) // Use OpenGL ES 2.0
        glRenderer = MyGLRenderer(this)
        glSurfaceView.setRenderer(glRenderer)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

        // Set up button interactions
        val button1: Button = findViewById(R.id.button1)
        button1.setOnClickListener {
            if (serverConnected) {
                serverScope.launch {
                    //KtorServer.SendData()
                }
            } else {
                serverConnected = true
                serverScope.launch {
                    KtorServer.StartServer("0.0.0.0",9080)
                }
            }
        }

        val button2: Button = findViewById(R.id.button2)
        button2.setOnClickListener {
            if(clientConnected) {
                clientScope.launch {
                    //KtorClient.GetData()
                    GameScene.TestUpdateCar()
                }
                //Toast.makeText(this, KtorServer.DataToString(data.value), Toast.LENGTH_SHORT).show()
            }
            else {
                clientConnected = true
                clientScope.launch {
                    //KtorClient.ConnectToServer("localhost", 9080)
                    KtorClient.ConnectToServer("localhost", 8080)

                    //KtorClient.ConnectToServer("10.0.2.2", 9090)

                }
            }
        }
    }
}
/*
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    var connected = false

    Column {
        Text(
            text = "Hello $name!",
            modifier = modifier
        )
        Button(
            onClick = {
                connected = true
                serverScope.launch {
                    KtorServer.startServer("10.0.2.15",8090)
                }
            }) {
            Text("Server")
        }
        Button(
            onClick = {
                connected = true
                clientScope.launch {
                    KtorClient.connectToServer("192.168.1.198", 8000)
                }
            }) {
            Text("Client")
        }

        Button(
            onClick = {
                data.value.y += 1
            }) {
            Text("Up")
        }

        Button(
            onClick = {
                data.value.y -= 1
            }) {
            Text("Down")
        }

        Text(KtorServer.DataToString(data.value))

        Button(
            onClick = {
                KtorClient.getData()
            }) {
            Text("Refresh")
        }
    }


}
*/

/*
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MultiplayerTestTheme {
        Greeting("Android")
    }
}*/
