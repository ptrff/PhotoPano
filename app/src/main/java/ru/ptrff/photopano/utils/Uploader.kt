package ru.ptrff.photopano.utils

import android.content.Context
import android.util.Log
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest
import ru.ptrff.photopano.BuildConfig
import ru.ptrff.photopano.ui.MainActivity.Companion.TAG
import java.io.File
import java.net.URL
import java.util.Date
import java.util.UUID

class Uploader {
    private val cr: AWSCredentials = BasicAWSCredentials(
        BuildConfig.AWS_KEY_ID,
        BuildConfig.AWS_SECRET_KEY
    )

    private val s3client = AmazonS3Client(cr).apply {
        endpoint = "https://storage.yandexcloud.net"
    }

    private lateinit var key: String

    fun upload(context: Context, file: File, listener: TransferListener) {
        TransferNetworkLossHandler.getInstance(context)

        if (file.isFile) {
            key = "photopano/" + UUID.randomUUID() + ".gif"
            Log.d(TAG, "S3 Generated key: $key")

            TransferUtility.builder()
                .context(context)
                .defaultBucket(BuildConfig.BUCKET_NAME)
                .awsConfiguration(AWSMobileClient.getInstance().configuration)
                .s3Client(s3client)
                .build()
                .upload(key, file)
                .setTransferListener(listener)
        }
    }

    val fileUrl: URL
        get() {
            val request = GeneratePresignedUrlRequest(BuildConfig.BUCKET_NAME, key)
            request.expiration = Date(System.currentTimeMillis() + 3600 * 1000)
            return s3client.generatePresignedUrl(request)
        }
}
