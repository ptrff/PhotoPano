package ru.ptrff.photopano.parameters.ui

import android.content.Context
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.withSave
import ru.ptrff.photopano.R

class RotatedGradientDrawable(
    context: Context,
    colors: IntArray,
    positions: FloatArray,
    width: Int,
    height: Int
) : Drawable() {
    private val paint = Paint()
    private val gradient = LinearGradient(
        0f,
        0f,
        width.toFloat(),
        height.toFloat(),
        colors,
        positions,
        Shader.TileMode.CLAMP
    )
    private val noiseDrawable = AppCompatResources.getDrawable(
        context,
        R.drawable.noise_bitmap
    ) as BitmapDrawable
    private val noiseShader = BitmapShader(
        noiseDrawable.bitmap,
        Shader.TileMode.REPEAT,
        Shader.TileMode.REPEAT
    )
    private var rotationDegrees = 0f
    private val centerX = width / 2
    private val centerY = height / 2

    override fun draw(canvas: Canvas) = canvas.withSave {
        val matrix = Matrix()
        matrix.postRotate(rotationDegrees, centerX.toFloat(), centerY.toFloat())

        gradient.setLocalMatrix(matrix)
        paint.setShader(gradient)
        drawRect(
            bounds.left.toFloat(),
            bounds.top.toFloat(),
            bounds.right.toFloat(),
            bounds.bottom.toFloat(),
            paint
        )

        noiseShader.setLocalMatrix(matrix)
        paint.setShader(noiseShader)
        drawRect(bounds, paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.setColorFilter(colorFilter)
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int {
        return paint.alpha
    }

    fun setRotationDegrees(degrees: Float) {
        rotationDegrees = degrees
        invalidateSelf()
    }
}
