package org.nearbytalk.runtime;

import java.text.SimpleDateFormat;


public class DateFormaterThreadInstance {

	private static ThreadLocal<SimpleDateFormat> dateFormat = new ThreadLocal<SimpleDateFormat>() {

		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat(Global.DATE_FORMAT);
		}
	};

	public static SimpleDateFormat get() {
		return dateFormat.get();
	}
}
