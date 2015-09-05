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
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Supplier;
import org.w3c.dom.Document;

/**
 * This interface provides the API for storing and retrieving data using a 
 * folder structure that is automatically zipped/unzipped for convenience.
 * @author CCHall <a href="mailto:hallch20@msu.edu">hallch20@msu.edu</a>
 */
public abstract class ArchiveResourceManager implements Closeable {
	
	
	/**
	 * Packages all of the data stored in this object and saves it in a zip 
	 * archive (not necessarily .zip file extension)
	 * @param archiveFile The file to save the data
	 * @throws IOException Thrown if there was a problem writing to the file.
	 */
	public abstract void save(Path archiveFile) throws IOException;
	
	/**
	 * IMPLEMENTATIONS ARE NOT REQUIRED TO SAVE CHANGES ON CLOSE! This method 
	 * closes and cleans-up the archive.
	 * @throws IOException Thrown if there was a problem closing this object
	 */
	@Override
	public abstract void close() throws IOException;
	
	/**
	 * Checks whether a given resource exists in the archive
	 * @param locator
	 * @return 
	 */
	public abstract boolean exists(Path locator);
	/**
	 * Removes a specific resource from the archive.
	 * @param locator The identifier path to the image. It should have an image 
	 * format suffix.
	 * @return Returns true if the resource being deleted existed, false if 
	 * the resource did not already exist (and therefore was not deleted).
	 * @throws IOException Thrown if there was an error while deleting the 
	 * resource file.
	 */
	public abstract boolean delete(Path locator) throws IOException;

	/**
	 * Gets an image from the archive, if it exists. If it doesn't exist, and 
	 * the Supplier function <code>doIfNotExists</code> is not null, then the
	 * Supplier function will be invoked to create a new image, which will be 
	 * added to the archive and then returned (unless the Supplier returns 
	 * null).
	 * @param locator The identifier path to the image. It should have an image 
	 * format suffix.
	 * @param doIfNotExists A Supplier function to generate an image if the 
	 * requested image does not already exist (can be null).
	 * @return Returns the existing or created image, or null if the image does 
	 * not exist and the Supplier function <code>doIfNotExists</code> is null or 
	 * returns null.
	 * @throws IllegalStateException Thrown if the archive is closed.
	 * @throws IOException Thrown if there was an error reading or writing the 
	 * archive file.
	 */
	public abstract BufferedImage getImage(Path locator, Supplier<BufferedImage> doIfNotExists) throws IOException;
	/**
	 * Gets a set of properties from the archive, if it exists. If it doesn't 
	 * exist, and the map <code>defaultValues</code> is not null, then the
	 * default values will be saved and then returned
	 * @param locator The identifier path to the properties. 
	 * @param defaultValues The default values to use/store if there are no values 
	 * @return 
	 * @throws IllegalStateException Thrown if the archive is closed.
	 * @throws IOException Thrown if there was an error reading or writing the 
	 * archive file.
	 */
	public abstract Map<String,String> getProperties(Path locator, Map<String,String> defaultValues) throws IOException;
	/**
	 * Gets an XML document from the archive, if it exists. If it doesn't exist, 
	 * and the Supplier function <code>doIfNotExists</code> is not null, then 
	 * the Supplier function will be invoked to create a new document, which 
	 * will be added to the archive and then returned (unless the Supplier 
	 * returns null).
	 * @param locator The identifier path to the XML document
	 * @param doIfNotExists A Supplier function to generate a document if the 
	 * requested document does not already exist (can be null).
	 * @return Returns the existing or created XML DOM, or null if the document 
	 * does not exist and the Supplier function <code>doIfNotExists</code> is 
	 * null or returns null.
	 * @throws IllegalStateException Thrown if the archive is closed.
	 * @throws IOException Thrown if there was an error reading or writing the 
	 * archive file.
	 */
	public abstract Document getXML(Path locator, Supplier<org.w3c.dom.Document> doIfNotExists) throws IOException ;
	/**
	 * Lists all of the resources that start with the given locator prefix.
	 * @param locatorPrefix parent tree path (i.e. directory path) to scan for 
	 * resources
	 * @param includeDirectories If true, include directory names in the output. 
	 * If false, include only resources (files).
	 * @param recursive If true, then follow sub-directories and add their 
	 * contents to the list as well.
	 * @return A collection of resources that begin with the specified locator 
	 * prefix.
	 * @throws java.io.IOException Thrown if there was an error while reading 
	 * the archive
	 */
	public abstract java.util.List<Path> listSubResources(Path locatorPrefix, boolean includeDirectories, boolean recursive) throws IOException;
}
