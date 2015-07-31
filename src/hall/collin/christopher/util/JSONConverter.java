/*
 * The MIT License
 *
 * Copyright 2015 CCHall <a href="mailto:hallch20@msu.edu">hallch20@msu.edu</a>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hall.collin.christopher.util;

import java.io.StringReader;
import java.io.StringWriter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.json.*;
import javax.json.JsonValue.ValueType;

/**
 * This is a utility class (all static methods) for converting between JSON data 
 * and Map objects.
 * @author CCHall <a href="mailto:hallch20@msu.edu">hallch20@msu.edu</a>
 */
public abstract class JSONConverter {
	/** List of types that can be reliably written to and read back from JSON 
	 * via functions in this class.
	 */
	public static final Class[] SUPPORTED_BASIC_CLASSES = {Number.class, Boolean.class, String.class, java.time.Instant.class, byte[].class};
	/**
	 * Suffix appended to name of variables storing a time value
	 */
	public static final String TIME_SUFFIX = "@ISOtime";
	/** Suffix appended to names of variable storing binary data (in base64 encoding) */
	public static final String BINARY_SUFFIX = "@base64";
	
	/** 
	 * Converts String from JSON into a java.time instant. This method is not 
	 * tolerant of variations in time format.
	 * @param jsonEntry Text
	 * @return Text parsed as time
	 * @throws java.time.format.DateTimeParseException Thrown if the time could 
	 * not be read from the text
	 */
	private static java.time.Instant parseAsTime(String jsonEntry) throws java.time.format.DateTimeParseException{
		return java.time.Instant.from(java.time.format.DateTimeFormatter.ISO_INSTANT.parse(jsonEntry));
	}
	/**
	 * reverse of parseAsTime(String)
	 * @param time
	 * @return 
	 */
	private static String timeToJSONEntry(TemporalAccessor time){
		return java.time.format.DateTimeFormatter.ISO_INSTANT.format(time);
	}
	/**
	 * Decodes a base64 byte array from a string
	 * @param jsonEntry Text
	 * @return The binary data encoded in the text as base64 encoding
	 * @throws IllegalArgumentException Thrown if the text is not valid base64 
	 * encoded data
	 */
	private static byte[] parseAsByteArray(String jsonEntry) throws IllegalArgumentException{
		return Base64.getDecoder().decode(jsonEntry);
	}
	/**
	 * reverse of parseAsByteArray(String)
	 * @param binary
	 * @return 
	 */
	private static String byteArrayToJSONEntry(byte[] binary){
		return Base64.getEncoder().encodeToString(binary);
	}
	/**
	 * Makes a JSON object to represent given map. This is the reverse operation 
	 * of constructMap(JsonObject)
	 * @param data A map (typically  HashMap instance) with Strings as keys
	 * @return JSON object to store the map
	 * @throws IllegalArgumentException Thrown if there was a problem (usually 
	 * an unsupported class type) while generating the JSON object
	 */
	public static JsonObject constructJSONObject(Map<String,Object> data) throws IllegalArgumentException{
		JsonObjectBuilder json = Json.createObjectBuilder();
		for(Map.Entry<String,Object> e : data.entrySet()){
			try {
				String name = e.getKey();
				Object value = e.getValue();
				if (name == null) {
					continue;
				}
				if (value == null) {
					json.addNull(name);
				} else if (value instanceof TemporalAccessor) {
					name = name.concat(TIME_SUFFIX);
					json.add(name, timeToJSONEntry((TemporalAccessor) value));
				} else if (value instanceof byte[]) {
					name = name.concat(BINARY_SUFFIX);
					json.add(name, byteArrayToJSONEntry((byte[]) value));
				} else {
					if (value.getClass().isArray()) {
						value = Arrays.asList(value);
					}
					if (value instanceof List) {
						List list = (List) value;
						if (isTimeList(list)) {
							name = name.concat(TIME_SUFFIX);
						} else if (isBinaryList(list)) {
							name = name.concat(BINARY_SUFFIX);
						}
						json.add(name, constructJSONArray(list));
					} else if (value instanceof Map) {
						json.add(name, constructJSONObject((Map) value));
					} else if (value instanceof Long || value instanceof Integer
							|| value instanceof Short || value instanceof Byte) {
						json.add(name, ((Number) value).longValue());
					} else if (value instanceof Number) {
						json.add(name, ((Number) value).doubleValue());
					} else if (value instanceof Boolean) {
						json.add(name, ((Boolean) value));
					} else if (value instanceof CharSequence) {
						json.add(name, ((CharSequence) value).toString());
					} else {
						throw new IllegalArgumentException("Cannot store object of type " + value.getClass().getCanonicalName());
					}
				}
			}catch(Exception ex){
				throw new IllegalArgumentException("Failed to store entry "+e.getKey(),ex);
			}
		}
		return json.build();
	}
	/**
	 * Makes a JSON array to represent given list.
	 * @param data A list of objects
	 * @return JSON array to store the list
	 * @throws IllegalArgumentException Thrown if there was a problem (usually 
	 * an unsupported class type) while generating the JSON array
	 */
	public static JsonArray constructJSONArray(List data) throws IllegalArgumentException{
		JsonArrayBuilder array = Json.createArrayBuilder();
		if(isTimeList(data)){
			for(Object o : data){
				array.add(timeToJSONEntry((TemporalAccessor)o));
			}
		} else if(isBinaryList(data)){
			for(Object o : data){
				array.add(byteArrayToJSONEntry((byte[])o));
			}
		} else {
			for(Object o : data){
				if(o == null){
					array.addNull();
				} else if(o instanceof Long || o instanceof Integer || o instanceof Short || o instanceof Byte ) {
					array.add(((Number)o).longValue());
				} else if(o instanceof Number) {
					array.add(((Number)o).doubleValue());
				} else if(o instanceof Boolean) {
					array.add(((Boolean)o));
				} else if(o instanceof CharSequence) {
					array.add(((CharSequence)o).toString());
				} else if(o instanceof Map) {
					array.add(constructJSONObject((Map)o));
				} else if(o instanceof List) {
					array.add(constructJSONArray((List)o));
				} else {
					throw new IllegalArgumentException("Cannot store object of type " + o.getClass().getCanonicalName());
				}
			}
		}
		return array.build();
	}
	
	private static boolean isTimeList(List l){
		if(l.isEmpty()) return false;
		return l.stream().allMatch((Object o)->o instanceof TemporalAccessor);
	}
	
	private static boolean isBinaryList(List l){
		if(l.isEmpty()) return false;
		return l.stream().allMatch((Object o)->o instanceof byte[]);
	}
	/**
	 * Converts a JSON object into a has map for use in java.
	 * @param json JSON (parsed from file)
	 * @return A map that is equivalent to the JSON object.
	 */
	public static Map<String,Object> constructMap(JsonObject json){
		Map<String,Object> output = new HashMap<>();
		fillMap(output,json);
		return output;
	}
	private static void fillMap(Map<String,Object> map, JsonObject json) throws IllegalArgumentException{
		Set<Map.Entry<String, JsonValue>> entrySet = json.entrySet();
		for(Map.Entry<String, JsonValue> e : entrySet){
			try{
				String name = e.getKey();
				if(name.endsWith(TIME_SUFFIX) && e.getValue().getValueType() != ValueType.ARRAY){
					map.put(name.substring(0, name.lastIndexOf(TIME_SUFFIX)), parseAsTime(json.getString(name)));
				} else if(name.endsWith(BINARY_SUFFIX) && e.getValue().getValueType() != ValueType.ARRAY){
					map.put(name.substring(0, name.lastIndexOf(BINARY_SUFFIX)), parseAsByteArray(json.getString(name)));
				} else {
					switch(e.getValue().getValueType()){
						case NULL:
							map.put(name, null);
							break;
						case NUMBER:
							JsonNumber n = json.getJsonNumber(name);
							map.put(name, toNumber(n));
							break;
						case TRUE:
							map.put(name, new Boolean(true));
							break;
						case FALSE:
							map.put(name, new Boolean(false));
							break;
						case OBJECT:
							Map<String,Object> newMap = new HashMap<>();
							fillMap(newMap,json.getJsonObject(name));
							map.put(name,newMap);
							break;
						case ARRAY:
							List<Object> list = new ArrayList<>(json.getJsonArray(name).size());
							if(name.endsWith(TIME_SUFFIX)){
								fillListWithTimes(list,json.getJsonArray(name));
							} else if(name.endsWith(BINARY_SUFFIX)){
								fillListWithByteArrays(list,json.getJsonArray(name));
							} else {
								fillList(list,json.getJsonArray(name));
							}
							map.put(name, list);
							break;
						case STRING:
						default:
							map.put(name, json.getString(name));
							break;
					}
				}
			} catch(Exception ex){
				throw new IllegalArgumentException("Error while parsing entry \""+e.getKey()+"\": "+e.getValue().toString(),ex);
			}
		}
	}

	private static void fillListWithTimes(List<Object> list, JsonArray jsonArray) {
		for(int i = 0; i < jsonArray.size(); i++){
			list.add(parseAsTime(jsonArray.getString(i)));
		}
	}
	private static void fillListWithByteArrays(List<Object> list, JsonArray jsonArray) {
		for(int i = 0; i < jsonArray.size(); i++){
			list.add(parseAsByteArray(jsonArray.getString(i)));
		}
	}
	private static void fillList(List<Object> list, JsonArray jsonArray) {
		for(int i = 0; i < jsonArray.size(); i++){
			JsonValue v = jsonArray.get(i);
			switch(v.getValueType()){
				case NULL:
					list.add(null);
					break;
				case NUMBER:
					list.add(toNumber(jsonArray.getJsonNumber(i)));
					break;
				case TRUE:
					list.add(new Boolean(true));
					break;
				case FALSE:
					list.add(new Boolean(false));
					break;
				case ARRAY:
					List<Object> newList = new ArrayList<>(jsonArray.getJsonArray(i).size());
					fillList(newList,jsonArray.getJsonArray(i));
					list.add(newList);
					break;
				case OBJECT:
					Map<String,Object> map = new HashMap<>();
					fillMap(map,jsonArray.getJsonObject(i));
					list.add(map);
					break;
				case STRING:
				default:
					list.add(jsonArray.getString(i));
					break;
			}
		}
	}

	private static Number toNumber(JsonNumber n) {
		if(n.isIntegral()){
			return n.longValue();
		} else {
			return n.doubleValue();
		}
	}
	
	public static String doIndentation(CharSequence JSONstring,CharSequence indentString){
		StringBuilder out = new StringBuilder();
		int level = 0;
		for(int i = 0; i < JSONstring.length(); i++){
			char c = JSONstring.charAt(i);
			char nextC = '\0';
			if(i < JSONstring.length() - 1){
				nextC = JSONstring.charAt(i+1);
			}
			out.append(c);
			if(c == '{' || c == '['){
				level++;
			} else if(c == '}' || c == ']'){
				level--;
			}
			if(c == '{' || c == '[' || c == ',' || nextC == ']' || nextC == '}'){
				//if(nextC != ','){ // next character will cause newline, so skip this one
					out.append('\n');
					for (int n = 0; n < level; n++) {
						out.append(indentString);
					}
				//}
			}
		}
		return out.toString();
	}
	

	
	/**
	 * Testing use only
	 * @param z
	 * @deprecated
	 */
	@Deprecated public static void main(String[] z){
		String json0 = "{\n" +
"    \"glossary\": {\n" +
"        \"title\": \"example glossary\",\n" +
"		\"GlossDiv\": {\n" +
"            \"title\": \"S\",\n" +
"			\"GlossList\": {\n" +
"                \"GlossEntry\": {\n" +
"                    \"ID\": \"SGML\",\n" +
"                    \"IconData@base64\": \"R0lGODlhDwAPAPcAAAAAAEVFRRn/NP6dAP+0AP/JAP/OAP/lAP/qAP/9E//+k///x///6wAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACH5BAEAAAIALAAAAAAPAA8AAAh6AAUIFBigYMGBCAUUZLBAgQIDBxEGYJggAYKLBgYEGBigIYCLFwEAyLhRoYKPIFNCVLjAIgKULy8W0BhAgUuYKAsQKKggpU8EOgu6/ClzZwADREUemLnxaEqRMlcqNGDgAFQES0lypFqga1etEgMQGEuAZkKOBksODAgAOw==\",\n" +
"                    \"IconFormat\": \"gif\",\n" +
"					\"SortAs\": \"SGML\",\n" +
"					\"GlossTerm\": \"Standard Generalized Markup Language\",\n" +
"					\"LastAccessed@ISOtime\": \"2015-07-23T14:34:05.980Z\",\n" +
"					\"Acronym\": \"SGML\",\n" +
"					\"Abbrev\": \"ISO 8879:1986\",\n" +
"					\"GlossDef\": {\n" +
"                        \"para\": \"A meta-markup language, used to create markup languages such as DocBook.\",\n" +
"						\"GlossSeeAlso\": [\"GML\", \"XML\"]\n" +
"                    },\n" +
"					\"GlossSee\": \"markup\"\n" +
"                }\n" +
"            }\n" +
"        }\n" +
"    }\n" +
"}";
		
		System.out.println("Original:");
		System.out.println(json0);
		System.out.println();
		
		Map<String,Object> options  = new HashMap<>();
		JsonWriterFactory jsonWriterFactory = Json.createWriterFactory(options);
		
		
		Map m1 = JSONConverter.constructMap(Json.createReader(new StringReader(json0)).readObject());
		StringWriter sw1 = new StringWriter();
		JsonWriter jw1 = jsonWriterFactory.createWriter(sw1);
		jw1.writeObject(JSONConverter.constructJSONObject(m1));
		jw1.close();
		String json1 = sw1.toString();
		
		System.out.println("Parsed:");
		System.out.println(doIndentation(json1," "));
		System.out.println();
		
		Map m2 = JSONConverter.constructMap(Json.createReader(new StringReader(json1)).readObject());
		StringWriter sw2 = new StringWriter();
		JsonWriter jw2 = jsonWriterFactory.createWriter(sw2);
		jw2.writeObject(JSONConverter.constructJSONObject(m2));
		jw2.close();
		String json2 = sw2.toString();
		
		System.out.println("Saved and reparsed:");
		System.out.println(doIndentation(json2," "));
		System.out.println();
		
		System.out.println("Pass == "+json1.equals(json2));
		
	}
}
