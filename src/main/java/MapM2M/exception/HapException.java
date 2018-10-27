package MapM2M.exception;


public class HapException extends RuntimeException {

    private final transient Object[] parameters;

    public HapException(String message){
        this(message, null);
    }
    /**
     *
     * @param message 异常信息
     * @param parameters parameters
     */
    public HapException(String message, Object ... parameters){
        super(message);
        this.parameters = parameters;
    }

    public Object[] getParameters() {
        return parameters;
    }
}
