package com.facebook;

public class FacebookGraphObjectException extends FacebookException {
	static final long serialVersionUID = 1;

    public FacebookGraphObjectException() {
        super();
    }

    public FacebookGraphObjectException(String message) {
        super(message);
    }

    public FacebookGraphObjectException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public FacebookGraphObjectException(Throwable throwable) {
        super(throwable);
    }
}
