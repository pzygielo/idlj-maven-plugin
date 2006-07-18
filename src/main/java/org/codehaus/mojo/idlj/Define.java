package org.codehaus.mojo.idlj;

public class Define {
    /**
     * The symbol to define
     *
     * @parameter symbol
     */
    private String symbol;

    /**
     * The value of the symbol. This is optional.
     *
     * @parameter value
     */
    private String value;


    public String getSymbol() {
        return symbol;
    }

    public String getValue() {
        return value;
    }
}
