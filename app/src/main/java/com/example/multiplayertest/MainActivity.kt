package com.example.multiplayertest

import KtorClient
import KtorServer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.multiplayertest.ui.theme.MultiplayerTestTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import MyGLRenderer
import android.opengl.GLSurfaceView
import android.widget.Button
import android.widget.Toast

private val serverScope = CoroutineScope(Dispatchers.IO)
private val clientScope = CoroutineScope(Dispatchers.IO)

class DataToSync(x : Int,y: Int, z:Int)
{
    var x by mutableStateOf(0)
    var y by mutableStateOf(0)
    var z by mutableStateOf(0)

}
lateinit var glSurfaceView: GLSurfaceView
lateinit var glRenderer: MyGLRenderer
var connected = false
var data = mutableStateOf(DataToSync(0,0,0))

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
            if (connected) {
                data.value.y += 1
            } else {
                connected = true
                serverScope.launch {
                    KtorServer.startServer(8080)
                }
            }
        }

        val button2: Button = findViewById(R.id.button2)
        button2.setOnClickListener {
            if(connected) {
                KtorClient.getData()
                Toast.makeText(this, KtorServer.DataToString(data.value), Toast.LENGTH_SHORT).show()
            }
            else {
                connected = true
                clientScope.launch {
                    KtorClient.connectToServer("192.168.1.198", 8080)
                }
            }
        }
    }
}
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
                    KtorServer.startServer(8080)
                }
            }) {
            Text("Server")
        }
        Button(
            onClick = {
                connected = true
                clientScope.launch {
                    KtorClient.connectToServer("192.168.1.198", 8080)
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

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MultiplayerTestTheme {
        Greeting("Android")
    }
}