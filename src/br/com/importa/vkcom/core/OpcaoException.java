package br.com.importa.vkcom.core;

public class OpcaoException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public OpcaoException() {
		super();
	}

	public OpcaoException(String message) {
		super(message);
	}

	public OpcaoException(String message, Throwable cause) {
		super(message, cause);
	}

	public OpcaoException(Throwable cause) {
		super(cause);
	}

	protected OpcaoException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
