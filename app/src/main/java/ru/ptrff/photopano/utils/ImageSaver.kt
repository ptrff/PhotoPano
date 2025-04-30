package ru.ptrff.photopano.utils

import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.util.Log
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.nio.ByteBuffer

class ImageSaver(
    private val image: Image,
    private val file: File,
    private val onSaved: () -> Unit
) : Runnable {

    override fun run() {
        // time start
        val start = System.currentTimeMillis()

        val ib = ByteBuffer.allocate(image.height * image.width * 2)

        val y = image.planes[0].buffer
        val cr = image.planes[1].buffer
        val cb = image.planes[2].buffer
        ib.put(y)
        ib.put(cb)
        ib.put(cr)

        val yuvImage = YuvImage(
            ib.array(),
            ImageFormat.NV21,
            image.width,
            image.height,
            null
        )

        try {
            yuvImage.compressToJpeg(
                Rect(
                    0, 0,
                    image.width, image.height
                ), 50, FileOutputStream(file)
            )

            image.close()
            ib.clear()
            y.clear()
            cr.clear()
            cb.clear()

            onSaved()

            // time end
            val end = System.currentTimeMillis()
            Log.d(TAG, "Save time: ${end - start}")
        } catch (e: FileNotFoundException) {
            Log.e(TAG, e.toString())
        }
    }

    companion object {
        private const val TAG = "ImageSaver"
    }
}
