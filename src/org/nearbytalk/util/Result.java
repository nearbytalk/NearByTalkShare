package org.nearbytalk.util;

public class Result {

	private boolean success = true;

	private Object detail = null;

	public Result(boolean success, Object detail) {
		super();
		this.success = success;
		this.detail = detail;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public Object getDetail() {
		return detail;
	}

	public void setDetail(Object detail) {
		this.detail = detail;
	}

}
