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

import hall.collin.christopher.util.JSONConverter;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonWriter;

/**
 * This interface provides the API for storing and retrieving data using a 
 * folder structure that is automatically zipped/unzipped for convenience.
 * @author CCHall <a href="mailto:hallch20@msu.edu">hallch20@msu.edu</a>
 */
public abstract class ArchiveResourceManager implements Closeable{
	
	
	/** lock provided so that all subclasses can coordinate I/O in a 
	 * multi-threaded environment */
	protected final ReadWriteLock ioLock = new ReentrantReadWriteLock();
	/**
	 * Packages all of the data stored in this object and saves it in a zip 
	 * archive (not necessarily .zip file extension)
	 * @param archiveFile The file to save the data
	 * @throws IOException Thrown if there was a problem writing to the file.
	 */
	public abstract void save(File archiveFile) throws IOException;
	
	/**
	 * IMPLEMENTATIONS ARE NOT REQUIRED TO SAVE CHANGES ON CLOSE! This method 
	 * closes and cleans-up the archive.
	 * @throws IOException Thrown if there was a problem closing this object
	 */
	@Override
	public abstract void close() throws IOException;
	/**
	 * Gets a stream to read data from a file (or other data container) in the 
	 * data store archive.
	 * @param locatorPath The locator (path within the zip archive) of the 
	 * desired resource.
	 * @return An input stream, or null if the resource in question does not 
	 * exist (see {@link #exists(java.nio.file.Path) exists(...)});
	 * @throws IOException Thrown if there was a problem opening the source of 
	 * the input stream
	 */
	protected abstract InputStream getInputStream(Path locatorPath) throws IOException;
	/**
	 * Gets a stream to write data to a file (or other data container) in the 
	 * data store archive.
	 * @param locatorPath The locator (path within the zip archive) of the 
	 * desired resource.
	 * @return An output stream to the given resource path
	 * @throws IOException Thrown if there was a problem opening the source of 
	 * the input stream
	 */
	protected abstract OutputStream getOutputStream(Path locatorPath) throws IOException;
	/**
	 * Checks whether a resource exists.
	 * @param locatorPath The locator (path within the zip archive) of the 
	 * desired resource.
	 * @return <code>true</code> if the resource exists (can be read via input 
	 * stream or other method), <code>false</code> otherwise.
	 * @throws IOException Thrown if there was a problem opening the location path
	 */
	public abstract boolean exists(Path locatorPath) throws IOException;
	/**
	 * Gets a value from a properties file stored in the archive.
	 * @param locatorPath The locator (path within the zip archive) of the 
	 * desired properties file.
	 * @param propertyName The property to get/set
	 * @param defaultValue The value to set if the given property is absent
	 * @return The stored property, or the default value if it is absent.
	 * @throws IOException Thrown if there was a problem opening the location path
	 */
	public abstract String getProperty(Path locatorPath, String propertyName, String defaultValue) throws IOException;
	/**
	 * Gets a value from a properties file stored in the archive.
	 * @param locatorPath The locator (path within the zip archive) of the 
	 * desired properties file.
	 * @param propertyName The property to get/set
	 * @param newValue The value to set
	 * @throws IOException Thrown if there was a problem opening the location path
	 */
	public abstract void setProperty(Path locatorPath, String propertyName, String newValue) throws IOException;
	/**
	 * Checks whether a property exists (see {@link #getProperty(java.nio.file.Path, java.lang.String, java.lang.String) getProperty(...)}
	 * and {@link #setProperty(java.nio.file.Path, java.lang.String, java.lang.String) setProperty(...)}
	 * @param locatorPath The locator (path within the zip archive) of the 
	 * desired properties file.
	 * @param propertyName The property to get/set
	 * @return <code>true</code> if the indicated property has been set to some 
	 * value, <code>false</code> otherwise.
	 * @throws IOException Thrown if there was a problem opening the location path
	 */
	public abstract boolean hasProperty(Path locatorPath, String propertyName) throws IOException;
	/**
	 * Like {@link #getProperty(java.nio.file.Path, java.lang.String, java.lang.String) getProperty(...)},
	 * but with automatic number parsing.
	 * @param locatorPath The locator (path within the zip archive) of the 
	 * desired properties file.
	 * @param propertyName The property to get/set
	 * @param defaultValue The value to set if the given property is absent
	 * @return The stored property, or the default value if it is absent.
	 * @throws IOException Thrown if there was a problem opening the location path
	 * @throws NumberFormatException Thrown if the property stored is not a valid number.
	 */
	public Number getNumber(Path locatorPath, String propertyName, Number defaultValue) throws IOException, NumberFormatException{
		ioLock.readLock().lock();
		try{
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
		}finally{
			ioLock.readLock().unlock();
		}
	}
	/**
	 * Like {@link #setProperty(java.nio.file.Path, java.lang.String, java.lang.String) setProperty(...)},
	 * but with automatic number parsing.
	 * @param locatorPath The locator (path within the zip archive) of the 
	 * desired properties file.
	 * @param propertyName The property to get/set
	 * @param newValue The value to set
	 * @throws IOException Thrown if there was a problem opening the location path
	 */
	public void setNumber(Path locatorPath, String propertyName, Number newValue) throws IOException{
		ioLock.writeLock().lock();
		try{
			setProperty(locatorPath,propertyName,newValue.toString());
		}finally{
			ioLock.writeLock().unlock();
		}
	}
	/**
	 * Like {@link #hasProperty(java.nio.file.Path, java.lang.String) hasProperty(...)},
	 * but with automatic number parsing.
	 * @param locatorPath The locator (path within the zip archive) of the 
	 * desired properties file.
	 * @param propertyName The property to get/set
	 * @return <code>true</code> if the indicated property has been set to some 
	 * value, <code>false</code> otherwise.
	 * @throws IOException Thrown if there was a problem opening the location path
	 */
	public boolean hasNumber(Path locatorPath, String propertyName) throws IOException{
		ioLock.readLock().lock();
		try{
			return hasProperty(locatorPath,propertyName);
		}finally{
			ioLock.readLock().unlock();
		}
	}
	/**
	 * Gets a stored image.
	 * @param locatorPath The locator (path within the zip archive) of the 
	 * desired properties file.
	 * @param format The image format to use for storage, e.g. "png" or "jpg" or "gif"
	 * @param defaultValue A function to determine what to store and return if 
	 * the image doe not already exist. Note that implementations must not store 
	 * anything if this function is null ore returns a null.
	 * @return The image stored at the given location.
	 * @throws IOException Thrown if there was a problem opening the location path
	 */
	public abstract BufferedImage getImage(Path locatorPath, String format, java.util.function.Supplier<BufferedImage> defaultValue) throws IOException;
	/**
	 * Gets a stored image.
	 * @param locatorPath The locator (path within the zip archive) of the 
	 * desired properties file.
	 * @param format The image format to use for storage, e.g. "png" or "jpg" or "gif"
	 * @return The image stored at the given location, or null if no such image exists.
	 * @throws IOException Thrown if there was a problem opening the location path
	 */
	public BufferedImage getImage(Path locatorPath, String format) throws IOException{
		return getImage(locatorPath,format,()->{return null;});
	}
	/**
	 * Stores an image
	 * @param locatorPath The locator (path within the zip archive) of the 
	 * desired properties file.
	 * @param format The image format to use for storage, e.g. "png" or "jpg" or "gif"
	 * @param newValue The image to store
	 * @throws IOException Thrown if there was a problem accessing the location path
	 */
	public abstract void setImage(Path locatorPath, String format, BufferedImage newValue) throws IOException;
	/**
	 * Loads data from a map stored as a JSON object in the provided location.
	 * @param locatorPath The locator (path within the zip archive) of the 
	 * desired properties file.
	 * @param map A map (usually a HashMap instance) that uses strings as keys. 
	 * The values be also be maps with string keys, forming a nested map data tree.
	 * @throws IOException Thrown if there was a problem accessing the location path
	 */
	public void readDataMap(Path locatorPath, Map<String,Object> map) throws IOException{
		JsonReader jsonReader = Json.createReader(getInputStream(locatorPath));
		Map<String, Object> constructedMap = JSONConverter.constructMap(jsonReader.readObject());
		jsonReader.close();
		map.putAll(constructedMap);
	}
	/**
	 * Stores a nested map as a JSON data structure in the provided location.
	 * @param locatorPath The locator (path within the zip archive) of the 
	 * desired properties file.
	 * @param map A map (usually a HashMap instance) that uses strings as keys. 
	 * The values be also be maps with string keys, forming a nested map data tree.
	 * @throws IOException Thrown if there was a problem accessing the location path
	 * @throws IllegalArgumentException Thrown if the map contains classes that 
	 * are not supported in JSON storage.
	 */
	public void writeDataMap(Path locatorPath, Map<String,Object> map) throws IOException, IllegalArgumentException{
		JsonWriter jsonWriter = Json.createWriter(getOutputStream(locatorPath));
		jsonWriter.writeObject(JSONConverter.constructJSONObject(map));
		jsonWriter.close();
	}
	
}
