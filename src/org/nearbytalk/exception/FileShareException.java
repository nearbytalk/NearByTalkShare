package org.nearbytalk.exception;

import org.nearbytalk.http.ErrorResponse;

public class FileShareException extends NearByTalkException {

	private static final long serialVersionUID = 1L;

	private final ErrorResponse error;

	public FileShareException(ErrorResponse error) {
		super();
		this.error = error;
	}

	public ErrorResponse getError() {
		return error;
	}

}
