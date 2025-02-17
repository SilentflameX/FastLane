package com.example.multiplayertest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import MyGLRenderer
import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.GLSurfaceView
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast


lateinit var glSurfaceView: GLSurfaceView
lateinit var glRenderer: MyGLRenderer


class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var gyroscope: Sensor? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        //Initialize the GLSurfaceView
        glSurfaceView = findViewById(R.id.glSurfaceView)
        glSurfaceView.setEGLContextClientVersion(2) //Use OpenGL ES 2.0
        glRenderer = MyGLRenderer(this)
        glSurfaceView.setRenderer(glRenderer)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

        //Set up Gyroscope
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (gyroscope == null) {
            Toast.makeText(this, "Gyroscope not available!", Toast.LENGTH_SHORT).show()
        }

        //Set up button interactions
        val leftButton: ImageButton = findViewById(R.id.brake)
        leftButton.setOnTouchListener { v, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {GameScene.PlayerBrake(true)}
                MotionEvent.ACTION_UP -> {GameScene.PlayerBrake(false)}
            }
            v?.onTouchEvent(event) ?: true
        }

        val rightButton: ImageButton = findViewById(R.id.accelerate)
        rightButton.setOnTouchListener { v, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {GameScene.PlayerAccelerate(true)}
                MotionEvent.ACTION_UP -> {GameScene.PlayerAccelerate(false)}
            }
            v?.onTouchEvent(event) ?: true
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_A -> {
                GameScene.PLayerInput(-2f)
                true
            }
            KeyEvent.KEYCODE_D -> {
                GameScene.PLayerInput(2f)
                true
            }
            KeyEvent.KEYCODE_S -> {
                GameScene.PLayerInput(0f)
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onResume() {
        super.onResume()
        gyroscope?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        return
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            var deltaX = event.values[0] //Rotation around X-axis (pitch)
            var deltaY = event.values[1] //Rotation around Y-axis (roll)
            var deltaZ = event.values[2] //Rotation around Y-axis (roll)

            GameScene.PLayerInput(deltaY)
            //GameScene.MovePlayer(Vector3(-deltaX, 0f, 0f) * moveSpeed)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        //Not needed for basic movement
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
