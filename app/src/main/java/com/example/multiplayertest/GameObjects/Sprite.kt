package com.example.multiplayertest.GameObjects
import MyGLRenderer
import android.opengl.Matrix
import androidx.xr.runtime.math.Vector3
import com.example.multiplayertest.MainActivity
import com.example.multiplayertest.glRenderer

class Sprite {
    var position = Vector3(0.0f,0.0f,0.0f)
    var scale = Vector3(1f,1f,1f)
    var rotation = 0.0f

    var textureId = -1
    var modelMatrix  = FloatArray(16)

    fun UpdateMatrix()
    {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, position.x, position.y, position.z) // Translate
        Matrix.rotateM(modelMatrix, 0, rotation, 0f, 0f, 1f)   // Rotate
        Matrix.scaleM(modelMatrix, 0, scale.x, scale.y, scale.z)     // Scale
    }

    fun LoadSprite(resourceId: Int)
    {
        textureId = glRenderer.loadTexture(resourceId)
    }


}