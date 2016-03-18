package net.etalia.jalia.spring;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import net.etalia.jalia.OutField;

public class JaliaParametersFilter implements Filter {
	
	private static ThreadLocal<OutField> fields = new ThreadLocal<>();
	
	private String parameterName;

	public static OutField getFields() {
		return fields.get();
	}
	
	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		String properties = req.getParameter(parameterName);
		if (properties != null) {
			OutField root = new OutField(null);
			for (String prop : properties.split(",")) {
				root.getCreateSub(prop);
			}
			fields.set(root);
		}
		try {
			chain.doFilter(req, res);
		} finally {
			fields.remove();
		}
	}

	@Override
	public void init(FilterConfig config) throws ServletException {
		parameterName = config.getInitParameter("parameterName");
	}

	public static void clean() {
		fields.remove();
	}
	
	public static void set(OutField root) {
		fields.set(root);
	}

}
