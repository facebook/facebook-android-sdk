package com.facebook;

public class FacebookOperationCanceledException extends FacebookException {
	static final long serialVersionUID = 1;

    public FacebookOperationCanceledException() {
        super();
    }

    public FacebookOperationCanceledException(String message) {
        super(message);
    }

    public FacebookOperationCanceledException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public FacebookOperationCanceledException(Throwable throwable) {
        super(throwable);
    }
}
