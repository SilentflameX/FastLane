package com.example.multiplayertest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import MyGLRenderer
import android.opengl.GLSurfaceView
import android.widget.Button

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
        val upButton: Button = findViewById(R.id.upButton)
        upButton.setOnClickListener {
            GameScene.TempMoveCar(0)
        }

        val downButton: Button = findViewById(R.id.downButton)
        downButton.setOnClickListener {
            GameScene.TempMoveCar(1)
        }

        val leftButton: Button = findViewById(R.id.leftButton)
        leftButton.setOnClickListener {
            GameScene.TempMoveCar(2)
        }

        val rightButton: Button = findViewById(R.id.rightButton)
        rightButton.setOnClickListener {
            GameScene.TempMoveCar(3)
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
