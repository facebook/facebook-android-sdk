package com.facebook.samples.kotlinsampleapp.sharing

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.BitmapFactory
import android.net.Uri
import com.facebook.FacebookSdk.getApplicationContext
import com.facebook.share.model.ShareContent
import com.facebook.share.model.ShareHashtag
import com.facebook.share.model.ShareLinkContent
import com.facebook.share.model.SharePhoto
import com.facebook.share.model.SharePhotoContent
import com.facebook.share.model.ShareVideo
import com.facebook.share.model.ShareVideoContent
import com.facebook.share.widget.ShareDialog
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

fun shareLink(
    context: Context,
    link: String,
    hashtag: String? = null,
    quote: String? = null
): Boolean {
  val content =
      ShareLinkContent.Builder()
          .setContentUrl(Uri.parse(link))
          .setShareHashtag(ShareHashtag.Builder().setHashtag(hashtag).build())
          .setQuote(quote)
          .build()
  return share(context, content)
}

fun sharePhotos(
    context: Context,
    drawableIds: List<Int>,
    hashtag: String? = null,
): Boolean {
  val content =
      SharePhotoContent.Builder()
          .setPhotos(
              drawableIds.map { image ->
                SharePhoto.Builder()
                    .setBitmap(
                        BitmapFactory.decodeResource(getApplicationContext().resources, image))
                    .build()
              })
          .setShareHashtag(ShareHashtag.Builder().setHashtag(hashtag).build())
          .build()
  return share(context, content)
}

fun shareVideo(
    context: Context,
    videoResId: Int,
    hashtag: String? = null,
): Boolean {
  val content =
      ShareVideoContent.Builder()
          .setVideo(
              ShareVideo.Builder().setLocalUrl(getResourceUri(resourceId = videoResId)).build())
          .setShareHashtag(ShareHashtag.Builder().setHashtag(hashtag).build())
          .build()
  return share(context, content)
}

private fun share(
    context: Context,
    content: ShareContent<*, *>,
    mode: ShareDialog.Mode = ShareDialog.Mode.AUTOMATIC
): Boolean {
  val activity = getActivity(context) ?: return false
  ShareDialog(activity).show(content, mode)
  return true
}

private fun getResourceUri(resourceId: Int): Uri {
  val dst = File.createTempFile("temp", null, getApplicationContext().cacheDir)
  val input = getApplicationContext().resources.openRawResource(resourceId)
  val out: OutputStream = FileOutputStream(dst)

  // Transfer bytes from input to out
  val buf = ByteArray(1024)
  var len: Int
  while (input.read(buf).also { len = it } > 0) {
    out.write(buf, 0, len)
  }
  input.close()
  out.close()
  return Uri.fromFile(dst)
}

private fun getActivity(context: Context): Activity? {
  var ctx = context
  while (ctx is ContextWrapper) {
    if (ctx is Activity) {
      return ctx
    }
    ctx = ctx.baseContext
  }
  return null
}
