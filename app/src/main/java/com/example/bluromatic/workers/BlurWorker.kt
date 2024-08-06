package com.example.bluromatic.workers

import android.content.ContentValues.TAG
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.bluromatic.DELAY_TIME_MILLIS
import com.example.bluromatic.KEY_BLUR_LEVEL
import com.example.bluromatic.KEY_IMAGE_URI
import com.example.bluromatic.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

// 定义工作任务内容，继承协程工作任务
class BlurWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    // 重写 doWork 方法
    override suspend fun doWork(): Result {
        // 读取任务发起方传参数据
        val resourceUri = inputData.getString(KEY_IMAGE_URI)
        val blurLevel = inputData.getInt(KEY_BLUR_LEVEL, 1)
        // 在协程上下文中执行异步方法直到完成才返回结果
        return withContext(Dispatchers.IO) {
            // This is an utility function added to emulate slower work.
            // 以下是定义任务具体内容
            // 延迟执行
            delay(DELAY_TIME_MILLIS)

            // 为啥用@withContext? todo...
            return@withContext try {

                require(!resourceUri.isNullOrBlank()) {
                    val errorMessage =
                        applicationContext.resources.getString(R.string.invalid_input_uri)
                    Log.e(TAG, errorMessage)
                    errorMessage
                }
                val resolver = applicationContext.contentResolver
                // 读取图片位图
                val picture = BitmapFactory.decodeResource(
                    applicationContext.resources,
                    R.drawable.android_cupcake
                )
                // 位图缩放
                val output = blurBitmap(picture, blurLevel)

                // 将位图写到文件中
                val outputUri = writeBitmapToFile(applicationContext, output)
                // 发起通知
                makeStatusNotification(
                    "Output is $outputUri",
                    applicationContext
                )
                // 转换键值对列表数据转为 Work.Data数据格式
                // KEY_IMAGE_URI to outputUri.toString() to前 是键 to后 是值
                val outputData = workDataOf(KEY_IMAGE_URI to outputUri.toString())
                // Result是任务执行结果，如何执行成功，则传入结果值
                Result.success(outputData)
            } catch (throwable: Throwable) {
                Log.e(
                    TAG,
                    applicationContext.resources.getString(R.string.error_applying_blur),
                    throwable
                )
                Result.failure()
            }
        }
    }
}