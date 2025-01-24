package com.example.multiplayertest

import KtorClient
import KtorServer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.multiplayertest.ui.theme.MultiplayerTestTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import MyGLRenderer
import MyGLSurfaceView

private val serverScope = CoroutineScope(Dispatchers.IO)
private val clientScope = CoroutineScope(Dispatchers.IO)

class DataToSync(x : Int,y: Int, z:Int)
{
    var x by mutableStateOf(0)
    var y by mutableStateOf(0)
    var z by mutableStateOf(0)

}

class MainActivity : ComponentActivity() {
    private lateinit var glSurfaceView: MyGLSurfaceView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize the GLSurfaceView
        glSurfaceView = MyGLSurfaceView(this)

        // Set the GLSurfaceView as the content view
        setContentView(glSurfaceView)

        /*setContent {
            MultiplayerTestTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }*/

        // Start the server in a coroutine

    }
}

var data = mutableStateOf(DataToSync(0,0,0))

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