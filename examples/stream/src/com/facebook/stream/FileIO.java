/*
 * Copyright 2010 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.stream;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import android.app.Activity;

/**
 * Helpers for doing basic file IO.
 * 
 * @author yariv
 *
 */
public class FileIO {

    /**
     * Write the data to the file indicate by fileName. The file is created
     * if it doesn't exist.
     * 
     * @param activity
     * @param data
     * @param fileName
     * @throws IOException
     */
    public static void write(
            Activity activity, String data, String fileName)
            throws IOException {
        FileOutputStream fo = activity.openFileOutput(fileName, 0);
        BufferedWriter bf = new BufferedWriter(new FileWriter(fo.getFD()));
        bf.write(data);
        bf.flush();
        bf.close();
    }

    /**
     * Read the contents of the file indicated by fileName
     * 
     * @param activity
     * @param fileName
     * @return the contents
     * @throws IOException
     */
    public static String read(Activity activity, String fileName) 
            throws IOException {
        FileInputStream is = activity.openFileInput(fileName);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        while (br.ready()) {
            String line = br.readLine();
            sb.append(line);
        }
        String data = sb.toString();
        return data;
    }
}
