/*
 *
 *  *    Copyright (C) 2019 Amit Shekhar
 *  *    Copyright (C) 2011 Android Open Source Project
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 */

package com.amitshekhar.utils;

import java.io.UnsupportedEncodingException;

/**
 * Created by amitshekhar on 06/02/17.
 */

public class ConverterUtils {

    private static final int MAX_BLOB_LENGTH = 512;

    private static final String UNKNOWN_BLOB_LABEL = "{blob}";

    private ConverterUtils() {
        // This class in not publicly instantiable
    }

    public static String blobToString(byte[] blob) {
        if (blob.length <= MAX_BLOB_LENGTH) {
            if (fastIsAscii(blob)) {
                try {
                    return new String(blob, "US-ASCII");
                } catch (UnsupportedEncodingException ignored) {

                }
            }
        }
        return UNKNOWN_BLOB_LABEL;
    }

    public static boolean fastIsAscii(byte[] blob) {
        for (byte b : blob) {
            if ((b & ~0x7f) != 0) {
                return false;
            }
        }
        return true;
    }

}
