package com.facebook.internal.security

import android.content.Context
import android.content.pm.PackageManager
import android.util.Base64
import androidx.annotation.VisibleForTesting
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/*
  This would need to be changed if we change the docs for how to submit the certificate hashes
  https://developers.facebook.com/docs/android/getting-started/
*/

object CertificateUtil {
  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  internal const val DELIMITER = ":" // not part of valid characters for base64

  /** @return String of concatenated signatures, since there can be more than one */
  @JvmStatic
  fun getCertificateHash(ctx: Context): String {
    try {
      val signatures =
          ctx.packageManager.getPackageInfo(ctx.packageName, PackageManager.GET_SIGNATURES)
              .signatures
      val sb = StringBuilder()
      val md = MessageDigest.getInstance("SHA1")
      for (signature in signatures) {
        md.update(signature.toByteArray())
        sb.append(Base64.encodeToString(md.digest(), Base64.DEFAULT))
        sb.append(DELIMITER)
      }
      if (sb.isNotEmpty()) {
        sb.setLength(sb.length - 1) // remove last delimiter
      }
      return sb.toString()
    } catch (e: PackageManager.NameNotFoundException) {
      // do nothing
    } catch (e: NoSuchAlgorithmException) {
      // do nothing
    }
    return ""
  }
}
