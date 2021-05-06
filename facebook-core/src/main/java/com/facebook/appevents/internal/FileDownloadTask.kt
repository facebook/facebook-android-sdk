/*
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.facebook.appevents.internal

import android.os.AsyncTask
import androidx.annotation.VisibleForTesting
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class FileDownloadTask(
    private val uriStr: String,
    private val destFile: File,
    private val onSuccess: Callback
) : AsyncTask<String?, Void?, Boolean>() {

  @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
  public override fun doInBackground(vararg args: String?): Boolean {
    try {
      val url = URL(uriStr)
      val conn = url.openConnection()
      val contentLength = conn.contentLength
      val stream = DataInputStream(url.openStream())
      val buffer = ByteArray(contentLength)
      stream.readFully(buffer)
      stream.close()
      val fos = DataOutputStream(FileOutputStream(destFile))
      fos.write(buffer)
      fos.flush()
      fos.close()
      return true
    } catch (e: Exception) {
      /* no op */
    }
    return false
  }

  override fun onPostExecute(isSuccess: Boolean) {
    if (isSuccess) {
      onSuccess.onComplete(destFile)
    }
  }

  fun interface Callback {
    fun onComplete(file: File)
  }
}
