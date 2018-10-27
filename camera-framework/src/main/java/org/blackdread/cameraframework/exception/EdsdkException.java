package org.blackdread.cameraframework.exception;

/**
 * Based exception for the framework
 *
 * <p>Created on 2018/10/04.<p>
 *
 * @author Yoann CAPLAIN
 */
public abstract class EdsdkException extends RuntimeException {

    protected EdsdkException(final String message) {
        super(message);
    }
}
