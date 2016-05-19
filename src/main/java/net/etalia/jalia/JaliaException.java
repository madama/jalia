package net.etalia.jalia;

public class JaliaException extends RuntimeException {

	public JaliaException() {
		super();
	}

	public JaliaException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public JaliaException(String message, Throwable cause) {
		super(message, cause);
	}

	public JaliaException(String message) {
		super(message);
	}

	public JaliaException(Throwable cause) {
		super(cause);
	}
	
}
