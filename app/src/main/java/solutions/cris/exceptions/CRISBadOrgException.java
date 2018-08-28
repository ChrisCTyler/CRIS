package solutions.cris.exceptions;

/**
 * Copyright CRIS.Solutions 10/01/2017.
 */

public class CRISBadOrgException extends RuntimeException {

    public CRISBadOrgException(String org1, String org2) {
        super(String.format("Incompatible Orgs: %s - %s", org1, org2));
    }
}
