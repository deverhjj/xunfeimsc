/*
 * Copyright (C) $year Huajian Jiang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huajianjiang.flutter.plugins.xunfeimsc;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * <p>Author: Huajian Jiang
 * <br>Date: 2020/9/2
 * <br>Email: developer.huajianjiang@gmail.com
 */
public class Util {
    /**
     * 读取 asset 目录下文本文件。
     * @return 文件内容
     */
    @SuppressWarnings({"CharsetObjectCanBeUsed", "ResultOfMethodCallIgnored"})
    public static String readFile(Context context, String file)
    {
        String result = "";
        InputStream is = null;
        try {
            is = context.getAssets().open(file);
            byte[] bytes = new byte[is.available()];
            is.read(bytes);
            result = new String(bytes, Charset.forName("utf-8"));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }
}
