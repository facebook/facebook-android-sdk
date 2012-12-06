package com.facebook.widget;

import android.os.Bundle;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.MediumTest;
import com.facebook.SessionDefaultAudience;
import com.facebook.SessionLoginBehavior;
import com.facebook.sdk.tests.R;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class LoginFragmentTests extends FragmentTestCase<LoginFragmentTests.TestActivity> {

    public LoginFragmentTests() {
        super(TestActivity.class);
    }

    @MediumTest
    @LargeTest
    public void testCanSetParametersViaLayout() throws Throwable {
        TestActivity activity = getActivity();
        assertNotNull(activity);

        final LoginFragment fragment = activity.getFragment();
        assertNotNull(fragment);

        assertEquals(SessionLoginBehavior.SUPPRESS_SSO, fragment.getLoginBehavior());
        assertEquals(SessionDefaultAudience.EVERYONE, fragment.getDefaultAudience());
        List<String> permissions = fragment.getPermissions();
        assertEquals(2, permissions.size());
        assertEquals("read_1", permissions.get(0));
    }

    public static class TestActivity extends FragmentTestCase.TestFragmentActivity<LoginFragment> {
        public TestActivity() {
            super(LoginFragment.class);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getSupportFragmentManager().executePendingTransactions();
            LoginFragment fragment = getFragment();
            fragment.setLoginBehavior(SessionLoginBehavior.SUPPRESS_SSO);
            fragment.setReadPermissions(Arrays.asList("read_1", "read_2"));
            fragment.setDefaultAudience(SessionDefaultAudience.EVERYONE);
        }
    }
}
