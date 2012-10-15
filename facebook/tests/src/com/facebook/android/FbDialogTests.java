package com.facebook.android;

import android.os.Bundle;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

public class FbDialogTests extends AndroidTestCase {

    @SmallTest
    @MediumTest
    @LargeTest
    public void testDialogListener() {
        Facebook.DialogListener testListener = new Facebook.DialogListener() {
            @Override
            public void onComplete(Bundle values) {
                assertNotNull("Got a bundle", values);
            }

            @Override
            public void onFacebookError(FacebookError e) {
                fail("Should not reach this method");
            }

            @Override
            public void onError(DialogError e) {
                fail("Should not reach this method");
            }

            @Override
            public void onCancel() {
                fail("Should not reach this method");
            }
        };

        Facebook.DialogListener singleDispatchListener =
                new FbDialog.SingleDispatchDialogListener(testListener);

        singleDispatchListener.onComplete(new Bundle());
        singleDispatchListener.onFacebookError(null);
        singleDispatchListener.onError(null);
        singleDispatchListener.onCancel();
    }
}
