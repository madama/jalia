package net.etalia.jalia.spring;

import net.etalia.jalia.ObjectMapper;
import net.etalia.jalia.OutField;
import org.springframework.web.servlet.view.AbstractView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class JaliaJsonView extends AbstractView {
	/**
	 * Default content type: "application/json".
	 * Overridable through {@link #setContentType}.
	 */
	public static final String DEFAULT_CONTENT_TYPE = "application/json";


	private ObjectMapper objectMapper = new ObjectMapper();

	private OutField outfields;
	
	private String jsonPrefix;

	private boolean disableCaching = true;

	private boolean updateContentLength = false;


	public JaliaJsonView() {
		setContentType(DEFAULT_CONTENT_TYPE);
		setExposePathVariables(false);
	}


	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public final ObjectMapper getObjectMapper() {
		return this.objectMapper;
	}

	public void setJsonPrefix(String jsonPrefix) {
		this.jsonPrefix = jsonPrefix;
	}

	public void setPrefixJson(boolean prefixJson) {
		this.jsonPrefix = (prefixJson ? "{} && " : null);
	}

	public void setOutfields(OutField outfields) {
		this.outfields = outfields;
	}
	
	public OutField getOutfields() {
		return outfields;
	}

	public void setDisableCaching(boolean disableCaching) {
		this.disableCaching = disableCaching;
	}

	public void setUpdateContentLength(boolean updateContentLength) {
		this.updateContentLength = updateContentLength;
	}


	@Override
	protected void prepareResponse(HttpServletRequest request, HttpServletResponse response) {
		setResponseContentType(request, response);
		response.setCharacterEncoding("UTF-8");
		if (this.disableCaching) {
			response.addHeader("Pragma", "no-cache");
			response.addHeader("Cache-Control", "no-cache, no-store, max-age=0");
			response.addDateHeader("Expires", 1L);
		}
	}

	@Override
	protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		OutputStream stream = (this.updateContentLength ? createTemporaryOutputStream() : response.getOutputStream());
		writeContent(stream, model, this.jsonPrefix);
		if (this.updateContentLength) {
			// Will not give class cast exception cause the same condition is checked above and
			// createTemporaryOutputStream is used to obtain a ByteArrayOutputStream
			//noinspection ConstantConditions
			writeToResponse(response, (ByteArrayOutputStream) stream);
		}
	}

	protected void writeContent(OutputStream stream, Object value, String jsonPrefix) throws IOException {
		if (jsonPrefix != null) {
			stream.write(jsonPrefix.getBytes(StandardCharsets.UTF_8));
		}
		this.objectMapper.writeValue(stream, this.outfields, value);
	}


}
