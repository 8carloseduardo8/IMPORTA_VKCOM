package br.com.importa.vkcom.core;

public class FlaviosDateException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public FlaviosDateException() {
		super();
	}

	public FlaviosDateException(String message) {
		super(message);
	}

	public FlaviosDateException(String message, Throwable cause) {
		super(message, cause);
	}

	public FlaviosDateException(Throwable cause) {
		super(cause);
	}

	protected FlaviosDateException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
