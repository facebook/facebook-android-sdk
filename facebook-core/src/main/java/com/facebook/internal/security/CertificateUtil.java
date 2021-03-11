package com.facebook.internal.security;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Base64;
import androidx.annotation.VisibleForTesting;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/*
   This would need to be changed if we change the docs for how to submit the certificate hashes
   https://developers.facebook.com/docs/android/getting-started/
*/
public class CertificateUtil {

  private CertificateUtil() {}

  @VisibleForTesting static final String DELIMITER = ":"; // not part of valid characters for base64

  /** @return String of concatenated signatures, since there can be more than one */
  public static String getCertificateHash(Context ctx) {
    try {
      Signature[] signatures =
          ctx.getPackageManager()
              .getPackageInfo(ctx.getPackageName(), PackageManager.GET_SIGNATURES)
              .signatures;
      StringBuilder sb = new StringBuilder();
      MessageDigest md = MessageDigest.getInstance("SHA1");
      for (Signature signature : signatures) {
        md.update(signature.toByteArray());
        sb.append(Base64.encodeToString(md.digest(), Base64.DEFAULT));
        sb.append(DELIMITER);
      }

      if (sb.length() > 0) {
        sb.setLength(sb.length() - 1); // remove last delimiter
      }

      return sb.toString();
    } catch (PackageManager.NameNotFoundException e) {
      // do nothing
    } catch (NoSuchAlgorithmException e) {
      // do nothing
    }
    return "";
  }
}
