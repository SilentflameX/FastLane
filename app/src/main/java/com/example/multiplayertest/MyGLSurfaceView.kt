import android.content.Context
import android.opengl.GLSurfaceView

class MyGLSurfaceView(context: Context) : GLSurfaceView(context) {

   // private val renderer: MyGLRenderer

    init {
        // Specify OpenGL ES 2.0
        setEGLContextClientVersion(2)

        // Set the Renderer
        setRenderer(MyGLRenderer())

        // Optional: Set render mode to control how frames are drawn
        renderMode = RENDERMODE_CONTINUOUSLY
    }
}
