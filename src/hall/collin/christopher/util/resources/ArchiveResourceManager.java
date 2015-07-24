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
package hall.collin.christopher.util.resources;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonWriter;

/**
 * This interface provides the API for storing and retrieving data using a 
 * folder structure that is automatically zipped/unzipped for convenience.
 * @author CCHall <a href="mailto:hallch20@msu.edu">hallch20@msu.edu</a>
 */
public abstract class ArchiveResourceManager {
	
	public abstract void open(File zipFile) throws IOException;
	
	public abstract void save(File zipFile) throws IOException;
	
	public abstract InputStream getInputStream(Path locatorPath) throws IOException;
	
	public abstract OutputStream getOutputStream(Path locatorPath) throws IOException;
	
	public abstract boolean exists(Path locatorPath) throws IOException;
	
	public abstract String getProperty(Path locatorPath, String propertyName, String defaultValue) throws IOException;
	
	public abstract void setProperty(Path locatorPath, String propertyName, String newValue) throws IOException;
	
	public abstract boolean hasProperty(Path locatorPath, String propertyName, String newValue) throws IOException;
	
	public Number getNumber(Path locatorPath, String propertyName, Number defaultValue) throws IOException, NumberFormatException{
		String text = getProperty(locatorPath,propertyName,defaultValue.toString());
		if(text.equals("inf")){
			return Double.POSITIVE_INFINITY;
		} else if(text.equals("-inf")){
			return Double.NEGATIVE_INFINITY;
		} else if(text.equals("nan") || text.equals("NaN")){
			return Double.NaN;
		} else if(text.contains(".")){
			return Double.parseDouble(text);
		} else {
			return Long.parseLong(text);
		}
	}
	
	public void setNumber(Path locatorPath, String propertyName, Number newValue) throws IOException{
		setProperty(locatorPath,propertyName,newValue.toString());
	}
	
	public boolean hasNumber(Path locatorPath, String propertyName, String newValue) throws IOException{
		return hasProperty(locatorPath,propertyName,newValue);
	}
	
	public abstract BufferedImage getImage(Path locatorPath, String format, java.util.function.Supplier<BufferedImage> defaultValue) throws IOException;
	
	public BufferedImage getImage(Path locatorPath, String format) throws IOException{
		return getImage(locatorPath,format,()->{return null;});
	}
	
	public abstract void setImage(Path locatorPath, String propertyName, String newValue) throws IOException;
	
	public void readDataMap(Path locatorPath, Map<String,Object> map) throws IOException, IllegalArgumentException{
		JsonReader jsonReader = Json.createReader(getInputStream(locatorPath));
		Map<String, Object> constructedMap = JSONConverter.constructMap(jsonReader.readObject());
		jsonReader.close();
		map.putAll(constructedMap);
	}
	
	public void writeDataMap(Path locatorPath, Map<String,Object> map) throws IOException, IllegalArgumentException{
		JsonWriter jsonWriter = Json.createWriter(getOutputStream(locatorPath));
		jsonWriter.writeObject(JSONConverter.constructJSONObject(map));
		jsonWriter.close();
	}
	
}
