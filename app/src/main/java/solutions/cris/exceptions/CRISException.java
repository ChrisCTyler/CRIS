package solutions.cris.exceptions;

/**
 * Copyright CRIS.Solutions 12/10/2016.
 */

public class CRISException extends RuntimeException {

    public CRISException(String message) {
        super(message);
    }
    public CRISException(Exception cause) {
        super(cause);
    }
}
