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
 * Helpers for doing basic file IO (gotta love Java).
 * 
 * @author yariv
 *
 */
public class FileIO {

	public static void write(Activity activity, String data, String fileName) throws IOException {
		FileOutputStream fo = activity.openFileOutput(fileName, 0);
		BufferedWriter bf = new BufferedWriter(new FileWriter(fo.getFD()));
		bf.write(data);
		bf.flush();
		bf.close();
	}

	public static String read(Activity activity, String fileName) throws IOException {
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
