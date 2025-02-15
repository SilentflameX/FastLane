import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLUtils
import java.nio.ShortBuffer
import android.opengl.Matrix
import androidx.xr.runtime.math.Vector3
import com.example.multiplayertest.GameScene
import com.example.multiplayertest.GameScene.myPlayer

class MyGLRenderer(_context: Context) : GLSurfaceView.Renderer {

    private var context = _context

    private val vertexCoords = floatArrayOf(
        -0.5f, 0.5f, 0.0f,  //Top-left
        -0.5f, -0.5f, 0.0f,  //Bottom-left
        0.5f, -0.5f, 0.0f,  //Bottom-right
        0.5f, 0.5f, 0.0f   //Top-right
    )

    private val textureCoords = floatArrayOf(
        0.0f, 0.0f, //Top-left
        0.0f, 1.0f, //Bottom-left
        1.0f, 1.0f, //Bottom-right
        1.0f, 0.0f  //Top-right
    )

    private val drawOrder = shortArrayOf(0, 1, 2, 0, 2, 3) //Order to draw vertices


    private val vertexBuffer: FloatBuffer =
        ByteBuffer.allocateDirect(vertexCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(vertexCoords)
                position(0)
            }

    private val textureBuffer: FloatBuffer =
        ByteBuffer.allocateDirect(textureCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(textureCoords)
                position(0)
            }

    private val drawListBuffer: ShortBuffer =
        ByteBuffer.allocateDirect(drawOrder.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer().apply {
                put(drawOrder)
                position(0)
            }

    private val vertexShaderCode = """
    attribute vec4 vPosition;  //Vertex position (x, y, z, w)
    attribute vec2 vTexCoord; //Texture coordinates (u, v)
    
    uniform mat4 uMVPMatrix;  //Model-View-Projection matrix
    
    varying vec2 outTexCoord; //Pass texture coordinates to fragment shader
    
    void main() {
        gl_Position = uMVPMatrix * vPosition; //Transform vertex position
        outTexCoord = vTexCoord;             //Pass texture coordinates
    }
""".trimIndent()

    private val fragmentShaderCode = """
    precision mediump float;   //Precision for fragment shader calculations

    uniform sampler2D uTexture; //Texture sampler
    varying vec2 outTexCoord;   //Interpolated texture coordinates

    void main() {
        gl_FragColor = texture2D(uTexture, outTexCoord); //Sample texture and set pixel color
    }
""".trimIndent()

    private var shaderProgram: Int = -1

    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.153f, 0.682f, 0.376f, 1.0f)

        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        //Compile shaders and link program
        shaderProgram = createProgram(vertexShaderCode, fragmentShaderCode)
        GameScene.Start()
    }


    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        //Adjust the viewport based on geometry changes
        GLES20.glViewport(0, 0, width, height)

        val ratio = width.toFloat() / height
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 1f, 30f)

        //Set the camera position
        Matrix.setLookAtM(
            viewMatrix, 0,
            0f, 0f, 10f,  //Camera position
            0f, 0f, 0f,  //Look-at point
            0f, 1f, 0f   //Up vector
        )
    }

    fun UpdateCamera(position: Vector3) {
        Matrix.setLookAtM(
            viewMatrix, 0,
            position.x, position.y, 10f,  //Camera position
            position.x,  position.y, 0f,  //Look-at point
            0f, 1f, 0f   //Up vector
        )
    }

    private fun drawFrame()
    {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        GLES20.glUseProgram(shaderProgram)

        //Get handles
        val positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition")
        val texCoordHandle = GLES20.glGetAttribLocation(shaderProgram, "vTexCoord")
        val mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix")
        val textureHandle = GLES20.glGetUniformLocation(shaderProgram, "uTexture")

        //Enable vertex arrays
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer)

        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 8, textureBuffer)

        //Draw static world objects first
        for (go in GameScene.worldObjectList) {
            //Bind the texture
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, go.sprite.textureId)
            GLES20.glUniform1i(textureHandle, 0)

            //Apply transformation matrix
            go.sprite.UpdateMatrix()
            val mvpMatrix = FloatArray(16)
            Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, go.sprite.modelMatrix, 0)
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

            //Draw the object
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.size, GLES20.GL_UNSIGNED_SHORT, drawListBuffer)
        }

        //Draw dynamic objects
        //Maybe have Z-layer sorting?
        for (go in GameScene.goList) {
            //Bind the texture
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, go.sprite.textureId)
            GLES20.glUniform1i(textureHandle, 0)

            //Apply transformation matrix
            go.sprite.UpdateMatrix()
            val mvpMatrix = FloatArray(16)
            Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, go.sprite.modelMatrix, 0)
             Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

            //Draw the object
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.size, GLES20.GL_UNSIGNED_SHORT, drawListBuffer)
        }

        //Disable vertex arrays
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }

    var lastTime : Long = 0

    override fun onDrawFrame(gl: GL10?) {
        //Calculate delta time
        val currentTime = System.nanoTime()
        val deltaTime = (currentTime - lastTime) / 1_000_000_000.0f //Convert to seconds
        lastTime = currentTime

        //Update game state
        GameScene.Update(deltaTime)
        KtorClient.Update(deltaTime)
        KtorServer.Update(deltaTime)

        drawFrame()
    }

    fun loadShader(type: Int, shaderCode: String): Int {
        //Create a shader object
        val shader = GLES20.glCreateShader(type)

        //Load and compile the shader code
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)

        //Check for compilation errors
        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val error = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw RuntimeException("Shader compilation failed: $error")
        }

        return shader
    }

    fun createProgram(vertexShaderCode: String, fragmentShaderCode: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        //Create and link the program
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        //Check for linking errors
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val error = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program)
            throw RuntimeException("Program linking failed: $error")
        }
        return program
    }


    fun loadTexture(resourceId: Int): Int {
        val textureHandle = IntArray(1)

        //Generate a texture object
        GLES20.glGenTextures(1, textureHandle, 0)
        if (textureHandle[0] == 0) {
            throw RuntimeException("Error generating texture handle")
        }

        //Load the bitmap from resources
        val bitmap: Bitmap =
            BitmapFactory.decodeResource(context.resources, resourceId)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0])

        //Set texture parameters
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)

        //Load the bitmap into the bound texture
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

        //Recycle the bitmap, since its data is now on the GPU
        bitmap.recycle()

        return textureHandle[0]
    }

}
