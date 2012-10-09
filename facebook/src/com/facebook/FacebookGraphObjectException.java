package com.facebook;

/**
 * Represents an error specific to the {@link GraphObject GraphObject} class.
 */
public class FacebookGraphObjectException extends FacebookException {
    static final long serialVersionUID = 1;

    /**
     * Constructs a new FacebookGraphObjectException.
     */
    public FacebookGraphObjectException() {
        super();
    }

    /**
     * Constructs a new FacebookGraphObjectException.
     * 
     * @param message
     *            the detail message of this exception
     */
    public FacebookGraphObjectException(String message) {
        super(message);
    }

    /**
     * Constructs a new FacebookGraphObjectException.
     * 
     * @param message
     *            the detail message of this exception
     * @param throwable
     *            the cause of this exception
     */
    public FacebookGraphObjectException(String message, Throwable throwable) {
        super(message, throwable);
    }

    /**
     * Constructs a new FacebookGraphObjectException.
     * 
     * @param throwable
     *            the cause of this exception
     */
    public FacebookGraphObjectException(Throwable throwable) {
        super(throwable);
    }
}
