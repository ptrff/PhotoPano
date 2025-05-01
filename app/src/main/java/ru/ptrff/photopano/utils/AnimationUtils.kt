package ru.ptrff.photopano.utils

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import io.reactivex.rxjava3.core.Completable
import ru.ptrff.photopano.R
import ru.ptrff.photopano.models.Camera
import ru.ptrff.photopano.MainActivity
import java.io.File

class AnimationUtils(private val context: Context, private val duration: Float) {
    lateinit var changeAnimationDescription: (resId: Int) -> Unit
    private var cameras: List<Camera> = mutableListOf()
    private var bitmapFrames: List<Bitmap> = mutableListOf()
    private var framesConverted: List<IntArray> = mutableListOf()
    private var fps = 0f
    private val interpolateFactor = 3
    private val searchParam = 4
    private val scdThreshold = 100
    private val temp = File(context.filesDir, "temp")
    private val output = File(context.filesDir, "/")

    private var interpolate = false

    fun setCameras(cameras: List<Camera>) {
        this.cameras = cameras
        bitmapFrames = ArrayList()
        framesConverted = ArrayList()
        fps = cameras.size / duration
    }

    fun emptyTemp(): Completable {
        temp.mkdir()
        return Completable.fromAction {
            if (temp.exists()) {
                Log.d(MainActivity.TAG, "temp folder recycling:")
                for (file in temp.listFiles()!!) {
                    Log.d(MainActivity.TAG, "file deleted: ${file.name}")
                    file.delete()
                }
            }
        }
    }

    fun prepareEnvironment(): Completable {
        changeAnimationDescription(R.string.preparing)

        Log.d(MainActivity.TAG, "Preparing environment")

        return Completable.fromAction {
            File(context.filesDir, "output.gif").apply {
                if (exists()) {
                    Log.d(MainActivity.TAG, "existing output.gif deleted")
                    delete()
                }
            }
            File(context.filesDir, "output_i.gif").apply {
                if (exists()) {
                    Log.d(MainActivity.TAG, "existing output_i.gif deleted")
                    delete()
                }
            }
            File(context.filesDir, "palette.png").apply {
                if (exists()) {
                    Log.d(MainActivity.TAG, "existing palette.png deleted")
                    delete()
                }
            }
        }
    }

    fun combineImages(): Completable {
        changeAnimationDescription(R.string.combining_images)

        Log.d(MainActivity.TAG, "Combining images")

        if (!temp.exists()) {
            return Completable.error(Exception("temp folder not found"))
        }

        return Completable.fromAction {
            FFmpegKit.execute(
                "-framerate " + fps + " -i " + temp.absolutePath + "/%d.jpg " +
                    "-c:v h264 " +
                    "-b:v 2M " +
                    "-preset slow " +
                    temp.absolutePath + "/temp.mp4"
            )
        }
    }

    fun createPalette(): Completable {
        changeAnimationDescription(R.string.creating_palette)

        Log.d(MainActivity.TAG, "Creating palette")

        return Completable.fromAction {
            FFmpegKit.execute(
                "-i " + temp.absolutePath + "/temp.mp4 -vf \"palettegen\" -y " +
                    output.absolutePath + "/palette.png"
            )
        }
    }

    fun interpolation(): Completable {
        changeAnimationDescription(R.string.interpolation)

        Log.d(MainActivity.TAG, "Interpolation")

        interpolate = true

        return Completable.fromAction {
            FFmpegKit.execute(
                "-i " + temp.absolutePath + "/temp.mp4 " +
                    "-i " + output.absolutePath + "/palette.png " +
                    "-filter_complex \"minterpolate=fps=" + fps * interpolateFactor +
                    ":search_param=" + searchParam +
                    ":scd_threshold=" + scdThreshold +
                    " [vid]; [vid][1:v] paletteuse=dither=bayer:bayer_scale=5" +
                    "\" " + output.absolutePath + "/output_i.gif"
            )
        }
    }

    fun reverseAnimation(): Completable {
        changeAnimationDescription(R.string.reverse_animation)

        Log.d(MainActivity.TAG, "Reverse animation")

        return Completable.fromAction {
            if (interpolate) {
                FFmpegKit.execute(
                    "-i " + output.absolutePath + "/output_i.gif " +
                        "-filter_complex \"[0]reverse[r];[0][r]concat=n=2:v=1:a=0 [out];" +
                        "[out] setpts=PTS/" + interpolateFactor + "\" " +
                        output.absolutePath + "/output.gif"
                )
            } else {
                FFmpegKit.execute(
                    "-i " + temp.absolutePath + "/temp.mp4 " +
                        "-i " + output.absolutePath + "/palette.png " +
                        "-filter_complex \"[0:v]reverse[r];[0:v][r]concat=n=2:v=1:a=0 [vid];" +
                        "[vid][1:v] paletteuse=dither=bayer:bayer_scale=5\" " +
                        output.absolutePath + "/output.gif"
                )
            }
        }
    }

    fun convertToGif(): Completable {
        changeAnimationDescription(R.string.reverse_animation)

        Log.d(MainActivity.TAG, "Converting to gif")

        return Completable.fromAction {
            if (interpolate) {
                FFmpegKit.execute(
                    "-i " + output.absolutePath + "/output_i.gif " +
                        "-filter_complex \" setpts=PTS/" + interpolateFactor * 4 + "\" " +
                        output.absolutePath + "/output.gif"
                )
            } else {
                FFmpegKit.execute(
                    "-i " + temp.absolutePath + "/temp.mp4 " +
                        "-i " + output.absolutePath + "/palette.png " +
                        "-filter_complex \"paletteuse=dither=bayer:bayer_scale=5\" " +
                        output.absolutePath + "/output.gif"
                )
            }
        }
    }
}
