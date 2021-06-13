package com.facebook.login;

import android.content.Context;
import android.content.Intent;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.facebook.FacebookException;

/**
 * {@link ActivityResultContract} for login operations.
 */
public class FacebookLoginContract extends ActivityResultContract<FacebookLoginContract.Request, FacebookLoginContract.Result> {

  @NonNull
  @Override
  public Intent createIntent(@NonNull Context context, Request input) {
    return ((Request.Login) input).createIntent();
  }

  @Nullable
  @Override
  public SynchronousResult<Result> getSynchronousResult(@NonNull Context context, Request input) {
    if (input instanceof Request.ActivityLaunchingError) {
      return new SynchronousResult<Result>(new Result.Error(((Request.ActivityLaunchingError) input).getRequest(),
              LoginManager.ACTIVITY_RESOLVE_ERROR));
    } else if (input instanceof Request.Login) {
      Request.Login loginInput = (Request.Login) input;
      LoginManager loginManager = loginInput.getLoginManager();
      // Make sure the static handler for login is registered if there isn't an explicit callback
      loginManager.registerDefaultCallback();
      loginManager.logStartLogin(context, loginInput.getRequest());
      // Sanity check to avoid launching if the activity isn't present
      if (loginManager.resolveIntent(loginInput.createIntent())) {
        return null;
      } else {
        return new SynchronousResult<Result>(new Result.Error(loginInput.getRequest(),
                LoginManager.ACTIVITY_RESOLVE_ERROR));
      }
    }
    return new SynchronousResult<Result>(new Result.Error(null, "Invalid request"));
  }

  @Override
  public Result parseResult(int resultCode, @Nullable Intent intent) {
    return new Result.Success(resultCode, intent);
  }

  public abstract static class Request {

    static class Login extends Request {
      private final LoginManager loginManager;
      private final LoginClient.Request request;

      public Login(@NonNull LoginManager loginManager, @NonNull LoginClient.Request request) {
        this.loginManager = loginManager;
        this.request = request;
      }

      @NonNull
      public LoginManager getLoginManager() {
        return loginManager;
      }

      @NonNull
      public LoginClient.Request getRequest() {
        return request;
      }

      public Intent createIntent() {
        return loginManager.getFacebookActivityIntent(request);
      }
    }

    static class ActivityLaunchingError extends Request {
      private final LoginClient.Request request;

      public ActivityLaunchingError(@Nullable LoginClient.Request request) {
        this.request = request;
      }

      @Nullable
      public LoginClient.Request getRequest() {
        return request;
      }
    }
  }

  public abstract static class Result {

    static class Error extends Result {
      private final LoginClient.Request request;
      private final FacebookException exception;

      public Error(@Nullable LoginClient.Request request, @NonNull FacebookException exception) {
        this.request = request;
        this.exception = exception;
      }

      public Error(@Nullable LoginClient.Request request, @NonNull String message) {
        this(request, new FacebookException(message));
      }

      @Nullable
      public LoginClient.Request getRequest() {
        return request;
      }

      @NonNull
      public FacebookException getException() {
        return exception;
      }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    static class Success extends Result {
      private final int resultCode;
      private final Intent intent;

      public Success(int resultCode, @Nullable Intent intent) {
        this.resultCode = resultCode;
        this.intent = intent;
      }

      public int getResultCode() {
        return resultCode;
      }

      @Nullable
      public Intent getIntent() {
        return intent;
      }
    }
  }
}
