package cf.maybelambda.httpvalidator.springboot.persistence;

public class XMLParseException extends Exception {
    public XMLParseException(Exception e, String s) {
        super(s, e);
    }
}
