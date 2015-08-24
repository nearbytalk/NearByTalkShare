package org.nearbytalk.http;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.nearbytalk.runtime.UniqueObject;


public class DataStoreFilter implements Filter{

	@Override
	public void destroy() {
		
		//we can not put threadRecycle here ,since this callback
		//is called only once when shutdown
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		
		try {
			
			chain.doFilter(request, response);
			
		}finally{
			
			UniqueObject.getInstance().getDataStore().threadRecycle();
		}
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {

	}

}
