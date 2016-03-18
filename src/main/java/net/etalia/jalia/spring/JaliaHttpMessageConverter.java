package net.etalia.jalia.spring;

import java.io.IOException;
import java.nio.charset.Charset;

import net.etalia.jalia.ObjectMapper;
import net.etalia.jalia.TypeUtil;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

public class JaliaHttpMessageConverter extends AbstractHttpMessageConverter<Object> {
	
		public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
		
		private ObjectMapper mapper = new ObjectMapper();
		
		public JaliaHttpMessageConverter() {
			super(new MediaType("application", "json", DEFAULT_CHARSET));		
		}
		
		public void setObjectMapper(ObjectMapper mapper) {
			this.mapper = mapper;
		}
		
		@Override
		protected boolean supports(Class<?> clazz) {
			//return Persistent.class.isAssignableFrom(clazz);
			return true;
		}

		@Override
		protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
			return mapper.readValue(inputMessage.getBody(), TypeUtil.get(clazz));
		}

		@Override
		protected void writeInternal(Object t, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
			mapper.writeValue(outputMessage.getBody(), JaliaParametersFilter.getFields(), t);
		}

}
