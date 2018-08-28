package solutions.cris.exceptions;

/**
 * Created by Chris Tyler on 02/06/2017.
 */

public class CRISParseDateException extends RuntimeException {

    public CRISParseDateException() {
        super("Invalid Date format, use dd/mm/yyyy");
    }
}