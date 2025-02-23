package com.example.multiplayertest.GameObjects
import MyGLRenderer
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import androidx.xr.runtime.math.Vector3
import com.example.multiplayertest.MainActivity


class Sprite {
    var position = Vector3(0.0f,0.0f,0.0f)
    var scale = Vector3(1f,1f,1f)
    var rotation = 0.0f

    var textureId = -1
    var modelMatrix  = FloatArray(16)
    var alpha = 1f

    fun UpdateMatrix()
    {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, position.x, position.y, position.z) // Translate
        Matrix.rotateM(modelMatrix, 0, rotation, 0f, 0f, 1f)   // Rotate
        Matrix.scaleM(modelMatrix, 0, scale.x, scale.y, scale.z)     // Scale
    }

    fun LoadSprite(resourceId: Int)
    {
        if(MyGLRenderer.glRenderer().loadedSpriteMap.get(resourceId) == null){
            textureId = loadTexture(resourceId)
            MyGLRenderer.glRenderer().loadedSpriteMap[resourceId] = textureId
        }
        else {
            textureId = MyGLRenderer.glRenderer().loadedSpriteMap.get(resourceId)!!
        }
    }

    private fun loadTexture(resourceId: Int): Int {
        val textureHandle = IntArray(1)

        //Generate a texture object
        GLES20.glGenTextures(1, textureHandle, 0)
        if (textureHandle[0] == 0) {
            throw RuntimeException("Error generating texture handle")
        }

        //Load the bitmap from resources
        val bitmap: Bitmap =
            BitmapFactory.decodeResource(MainActivity.applicationContext().resources, resourceId)
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