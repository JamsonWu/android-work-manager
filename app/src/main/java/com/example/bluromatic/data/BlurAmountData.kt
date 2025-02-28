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

import com.example.bluromatic.R

object BlurAmountData {
    // 触发数量三种选择
    val blurAmount = listOf(
        BlurAmount(
            blurAmountRes = R.string.blur_lv_1,
            blurAmount = 1
        ),
        BlurAmount(
            blurAmountRes = R.string.blur_lv_2,
            blurAmount = 2
        ),
        BlurAmount(
            blurAmountRes = R.string.blur_lv_3,
            blurAmount = 3
        )
    )
}
