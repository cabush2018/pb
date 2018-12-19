package pb.limiter.locklimiter.service;

public class LockLimiterException extends RuntimeException {

	private static final long serialVersionUID = -1640502075288205400L;

	public LockLimiterException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public LockLimiterException(String arg0) {
		super(arg0);
	}

	public LockLimiterException(Throwable arg0) {
		super(arg0);
	}

}
