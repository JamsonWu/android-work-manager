/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.bluromatic.data
import android.content.Context
import android.net.Uri
import androidx.lifecycle.asFlow
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.bluromatic.IMAGE_MANIPULATION_WORK_NAME
import com.example.bluromatic.KEY_BLUR_LEVEL
import com.example.bluromatic.KEY_IMAGE_URI
import com.example.bluromatic.TAG_OUTPUT
import com.example.bluromatic.getImageUri
import com.example.bluromatic.workers.BlurWorker
import com.example.bluromatic.workers.CleanupWorker
import com.example.bluromatic.workers.SaveImageToFileWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
// 任务管理仓库
class WorkManagerBluromaticRepository(context: Context) : BluromaticRepository {

    private var imageUri: Uri = context.getImageUri()
    // 任务管理器：持久性工作库，重启后可继续运行，可按计划执行
    private val workManager = WorkManager.getInstance(context)
    // 如何获取WorkInfo？
    // override val outputWorkInfo: Flow<WorkInfo?> = MutableStateFlow(null)
    // 仓库中返回输出的实时任务信息，便于ViewModel中调用
    override val outputWorkInfo: Flow<WorkInfo> =
        // 读取指定Tag的任务执行实时状态数据
        // 如果Tag不存在，程序直接崩溃，看不出问题在哪
        // 报错指向：LiveData 与 List 为空
        // 通过Tag从所有任务中读取一个WorkInfo
        workManager.getWorkInfosByTagLiveData(TAG_OUTPUT)
            // 将LiveData转为Flow
            .asFlow()
            // 确保流中有数据
            .mapNotNull {
                // 集合非空，则取第一条，注意 it是一个列表List
                // 判断非空需要使用 isNotEmpty()
                if (it.isNotEmpty()) it.first() else null
        }
    /**
     * Create the WorkRequests to apply the blur and save the resulting image
     * 创建一个工作任务
     * @param blurLevel The amount to blur the image
     */
    override fun applyBlur(blurLevel: Int) {
        // 添加结束，即任务执行前提条件
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        // Add WorkRequest to Cleanup temporary images
        // 1.WorkContinuation 支持链式调用，从清理工作任务开始
        // var continuation = workManager.beginWith(OneTimeWorkRequest.from(CleanupWorker::class.java))
        var continuation = workManager
            // 在同一时间只有一个链式任务在运行
            .beginUniqueWork(
                // 链式任务的唯一名字
                IMAGE_MANIPULATION_WORK_NAME,
                // 发现任务已存在时处理策略
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequest.from(CleanupWorker::class.java)
            )
        // 2.创建缩放任务
        val blurBuilder = OneTimeWorkRequestBuilder<BlurWorker>()
        // PeriodicWorkRequestBuilder
        // 添加附加数据到工作任务中
        blurBuilder.setInputData(createInputDataForWorkRequest(blurLevel, imageUri))
        // 给任务添加约束，当电量不低的情况下才会执行这个任务
        // 直到前置条件满足时才会执行这个任务
        blurBuilder.setConstraints(constraints)
        // 一次性的任务请求入队，加入后台处理
        // workManager.enqueue(blurBuilder.build())
        // 工作任务执行结果在哪里处理？

        // Add the blur work request to the chain
        // 3.将缩放任务加入调用链中
        continuation = continuation.then(blurBuilder.build())

        // Add WorkRequest to save the image to the filesystem
        // 4.创建保存图片工作任务
        val save = OneTimeWorkRequestBuilder<SaveImageToFileWorker>()
            // 给任务添加TAG,可以通过这个TAG来查询与取消任务等
            .addTag(TAG_OUTPUT)
            .build()
        // 5.将保存图片任务加入调用链中
        continuation = continuation.then(save)
        // 6.链式调用入队
        continuation.enqueue()
    }

    /**
     * Cancel any ongoing WorkRequests
     * 取消正在运行的工作任务
     * */
    override fun cancelWork() {
        // 任务正在进行中如何取消
        workManager.cancelUniqueWork(IMAGE_MANIPULATION_WORK_NAME)
    }

    /**
     * Creates the input data bundle which includes the blur level to
     * update the amount of blur to be applied and the Uri to operate on
     * @return Data which contains the Image Uri as a String and blur level as an Integer
     * Data是轻量级键值对存储容器
     * 创建一个存储键值对的对象
     */
    private fun createInputDataForWorkRequest(blurLevel: Int, imageUri: Uri): Data {
        val builder = Data.Builder()
        builder.putString(KEY_IMAGE_URI, imageUri.toString()).putInt(KEY_BLUR_LEVEL, blurLevel)
        return builder.build()
    }
}
