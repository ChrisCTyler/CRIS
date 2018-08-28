package solutions.cris.exceptions;

/**
 * Created by Chris Tyler on 02/06/2017.
 */

public class CRISParseTimeException extends RuntimeException {

    public CRISParseTimeException() {
        super("Invalid Time format, use hh:mm");
    }
}
