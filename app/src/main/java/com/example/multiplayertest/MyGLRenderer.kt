import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer



class MyGLRenderer : GLSurfaceView.Renderer {

    private lateinit var vertexBuffer: FloatBuffer

    private val triangleCoords = floatArrayOf(
        0.0f,  0.5f, 0.0f,  // Top
        -0.5f, -0.5f, 0.0f,  // Bottom left
        0.5f, -0.5f, 0.0f   // Bottom right
    )

    // Color (RGBA)
    private val color = floatArrayOf(1.0f, 0.0f, 0.0f, 1.0f)

    init {
        // Allocate a ByteBuffer for the vertices
        val bb = ByteBuffer.allocateDirect(triangleCoords.size * 4)
        bb.order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer()
        vertexBuffer.put(triangleCoords)
        vertexBuffer.position(0)
    }

    private val vertexShaderCode = """
    attribute vec4 vPosition;
    void main() {
        gl_Position = vPosition;
    }
""".trimIndent()

    private val fragmentShaderCode = """
    precision mediump float;
    void main() {
        gl_FragColor = vec4(0.0, 1.0, 0.0, 1.0); // Green color
    }
""".trimIndent()

    private var shaderProgram : Int = 0


    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {

        shaderProgram = createProgram(vertexShaderCode, fragmentShaderCode)
        // Set the background color (RGBA)
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        // Adjust the viewport based on geometry changes
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        // Clear the screen
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // Use the program
        GLES20.glUseProgram(shaderProgram)

        // Get attribute and uniform locations
        val positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition")
        val colorHandle = GLES20.glGetUniformLocation(shaderProgram, "vColor")

        // Pass the vertex data
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(
            positionHandle,
            3, GLES20.GL_FLOAT, false,
            3 * 4, vertexBuffer
        )

        // Pass the color data
        GLES20.glUniform4fv(colorHandle, 1, color, 0)

        // Draw the triangle
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3)

        // Disable the vertex array
        GLES20.glDisableVertexAttribArray(positionHandle)
    }


    fun loadShader(type: Int, shaderCode: String): Int {
        // Create a shader object
        val shader = GLES20.glCreateShader(type)

        // Load and compile the shader code
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)

        // Check for compilation errors
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

        // Create and link the program
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        // Check for linking errors
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val error = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program)
            throw RuntimeException("Program linking failed: $error")
        }

        return program
    }

}
