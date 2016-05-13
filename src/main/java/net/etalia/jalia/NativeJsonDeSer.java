package net.etalia.jalia;

import java.io.IOException;
import java.io.Writer;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.etalia.jalia.stream.JsonReader;
import net.etalia.jalia.stream.JsonToken;
import net.etalia.jalia.stream.JsonWriter;

public class NativeJsonDeSer implements JsonDeSer {
	
	private final static Logger LOG = Logger.getLogger(NativeJsonDeSer.class.getName());

	@Override
	public int handlesSerialization(JsonContext context, Class<?> clazz) {
		if (clazz == null) return 10;
		if (clazz.isPrimitive()) return 10;
		if (Date.class.isAssignableFrom(clazz)) return 10;
		if (Number.class.isAssignableFrom(clazz)) return 10;
		if (Boolean.class.isAssignableFrom(clazz)) return 10;
		if (CharSequence.class.isAssignableFrom(clazz)) return 10;
		if (Enum.class.isAssignableFrom(clazz)) return 10;
		if (Class.class.isAssignableFrom(clazz)) return 10;
		return -1;
	}
	
	@Override
	public int handlesDeserialization(JsonContext context, TypeUtil hint) {
		if (hint != null && hint.hasConcrete()) {
			if (hint.isNumber()) return 10;
			if (hint.isCharSequence()) return 10;
			if (hint.isEnum()) return 10;
			if (Class.class.isAssignableFrom(hint.getConcrete())) return 10;
		}
		JsonToken peek = null;
		try {
			peek = context.getInput().peek();
		} catch (Exception e) {}
		if (peek == JsonToken.NULL) return 5;
		
		if (hint != null && hint.hasConcrete()) {
			return handlesSerialization(context, hint.getConcrete());
		}
		if (peek == JsonToken.NUMBER) return 10;
		if (peek == JsonToken.BOOLEAN) return 10;
		if (peek == JsonToken.STRING) return 10;
		return -1;
	}

	@Override
	public void serialize(Object obj, JsonContext context) throws IOException {
		JsonWriter output = context.getOutput();
		if (obj == null) {
			output.nullValue();
		} else if (obj instanceof Date) {
			output.value(((Date)obj).getTime());
		} else if (obj instanceof Number) {
			output.value((Number)obj);
		} else if (obj instanceof Boolean) {
			output.value(((Boolean)obj).booleanValue());
		} else if (obj instanceof CharSequence) {
			output.value(((CharSequence)obj).toString());
		} else if (obj instanceof Enum) {
			output.value(((Enum)obj).name());
		} else if (obj instanceof Class) {
			output.value(((Class)obj).getName());
		} else {
			throw new IllegalStateException("Cannot serialize " + obj + " at " + context.getStateLog());
		}
	}
	
	public void serializeRaw(Object obj, Writer output) throws IOException {
		if (obj == null) {
			output.write("null");
		} else if (obj instanceof Date) {
			output.write(Long.toString(((Date)obj).getTime()));
		} else if (obj instanceof Number) {
			output.write(((Number)obj).toString());
		} else if (obj instanceof Boolean) {
			output.write(((Boolean)obj).toString());
		} else if (obj instanceof CharSequence) {
			output.write(((CharSequence)obj).toString());
		} else if (obj instanceof Enum) {
			output.write(((Enum)obj).name());
		} else if (obj instanceof Class) {
			output.write(((Class)obj).getName());
		} else {
			throw new IllegalStateException("Cannot raw serialize " + obj);
		}
	}

	@Override
	public Object deserialize(JsonContext context, Object pre, TypeUtil hint) throws IOException {
		JsonReader input = context.getInput();
		JsonToken peek = input.peek();
		Object ret = null;
		if (peek == JsonToken.NULL) {
			input.nextNull();
			if (hint != null && !hint.isNullable()) {
				throw new IllegalArgumentException("Was expecting a primitive " + hint + " but got null, can't set null on a primitive  at " + context.getStateLog());
			}
			return null;
		}
		if (peek == JsonToken.STRING) {
			ret = input.nextString();
			// A string could be an enum
			if (hint != null) {
				if (hint.isEnum()) {
					ret = hint.getEnumValue((String)ret);
				} else if (hint.hasConcrete() && Class.class.isAssignableFrom(hint.getConcrete())) {
					try {
						ret = Class.forName((String)ret);
					} catch (ClassNotFoundException e) {
						throw new IllegalStateException("Cannot deserialize a class " + ret  + " at " + context.getStateLog(), e);
					}
				} else if (!hint.isCharSequence()) {
					// Check if it's possible to convert it
					if (hint.isLong()) {
						ret = Long.parseLong((String)ret);
					} else if (hint.isInteger()) {
						ret = Integer.parseInt((String)ret);
					} else if (hint.isShort()) {
						ret = Short.parseShort((String)ret);
					} else if (hint.isDouble()) {
						ret = Double.parseDouble((String)ret);
					} else if (hint.isFloat()) {
						ret = Float.parseFloat((String)ret);
					} else if (hint.isBoolean()) {
						ret = Boolean.parseBoolean((String)ret);
					} else if (Date.class.isAssignableFrom(hint.getConcrete())) {
						String dateStr = (String)ret;
						Date d = null;
						if (dateStr.indexOf('-') != -1) {
							try {
								d = javax.xml.bind.DatatypeConverter.parseDateTime(dateStr).getTime();
							} catch (Exception e) {
							}
						}
						if (d == null) {
							d = new Date(Long.parseLong((String)ret));
						}
						ret = d;
					} else {
						throw new IllegalStateException("Found a string, but was expecting " + hint + " at " + context.getStateLog());
					}
					LOG.log(Level.WARNING, "Had to convert String to {0} {1}", new Object[] { hint.getType(), context.getStateLog() });
				}
			}
		} else if (peek == JsonToken.BOOLEAN) {
			ret = input.nextBoolean();
		} else if (peek == JsonToken.NUMBER) {
			if (hint == null) {
				// If we don't have a hint, use double or long
				String valstr = input.nextString();
				if (valstr.indexOf('.') == -1) {
					ret = Long.parseLong(valstr);
				} else {
					ret = Double.parseDouble(valstr);
				}
			} else if (hint.isDouble()) {
				ret = input.nextDouble();
			} else if (hint.isFloat()) {
				ret = (float)input.nextDouble();
			} else if (hint.isInteger()) {
				ret = input.nextInt();
			} else if (hint.isShort()) {
				ret = (short)input.nextInt();
			} else if (hint.isLong()) {
				ret = input.nextLong();
			} else if (Date.class.isAssignableFrom(hint.getConcrete())) {
				long ms = input.nextLong();
				ret = new Date(ms);
			} else {
				throw new IllegalStateException("Found a number, but was expecting " + hint + " at " + context.getStateLog());
			}
		} else {
			throw new IllegalStateException("Was expecting a string, boolean or number, but got " + peek + " at " + context.getStateLog());
		}
		return ret;
	}

}
