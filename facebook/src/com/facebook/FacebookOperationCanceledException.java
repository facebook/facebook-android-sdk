package com.facebook;

/**
 * An Exception indicating that an operation was canceled before it completed.
 */
public class FacebookOperationCanceledException extends FacebookException {
    static final long serialVersionUID = 1;

    /**
     * Constructs a FacebookOperationCanceledException with no additional information.
     */
    public FacebookOperationCanceledException() {
        super();
    }

    /**
     * Constructs a FacebookOperationCanceledException with a message.
     *
     * @param message A String to be returned from getMessage.
     */
    public FacebookOperationCanceledException(String message) {
        super(message);
    }

    /**
     * Constructs a FacebookOperationCanceledException with a message and inner error.
     *
     * @param message   A String to be returned from getMessage.
     * @param throwable A Throwable to be returned from getCause.
     */
    public FacebookOperationCanceledException(String message, Throwable throwable) {
        super(message, throwable);
    }

    /**
     * Constructs a FacebookOperationCanceledException with an inner error.
     *
     * @param throwable A Throwable to be returned from getCause.
     */
    public FacebookOperationCanceledException(Throwable throwable) {
        super(throwable);
    }
}
