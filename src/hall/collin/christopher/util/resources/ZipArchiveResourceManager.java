/*
 * The MIT License
 *
 * Copyright 2015 Christopher Collin Hall.
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

import hall.collin.christopher.util.ZipUtils;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class ZipArchiveResourceManager extends ArchiveResourceManager{
	
	private static final Charset UTF8 = Charset.forName("UTF-8");
	private final Path dir;
	private final AtomicBoolean closed;
	private final Lock iolock = new ReentrantLock();
	
	private ZipArchiveResourceManager(Path unpackDir){
		this.dir = unpackDir;
		this.closed = new AtomicBoolean(false);
	}
	/**
	 * Opens a zip archive as a managed resource archive
	 * @param zipFilePath Path to a zip file (file extension does not need to be 
	 * .zip)
	 * @return An instance of this class
	 * @throws IOException Thrown if the zip archive could not be opened or the 
	 * necessary temp files could not be created.
	 */
	public static ZipArchiveResourceManager openZipResourceArchive(Path zipFilePath) throws IOException{
		Path unpackDir = Files.createTempDirectory(zipFilePath.getFileName().toString());
		Files.createDirectories(unpackDir);
		ZipUtils.extractZipArchive(zipFilePath, unpackDir);
		ZipArchiveResourceManager m = new ZipArchiveResourceManager(unpackDir);
		return m;
	}
	
	/**
	 * Creates a new managed resource archive
	 * @return An instance of this class
	 * @throws IOException Thrown if the necessary temp files could not be 
	 * created.
	 */
	public static ZipArchiveResourceManager newZipResourceArchive() throws IOException{
		Path unpackDir = Files.createTempDirectory(ZipArchiveResourceManager.class.getSimpleName());
		Files.createDirectories(unpackDir);
		ZipArchiveResourceManager m = new ZipArchiveResourceManager(unpackDir);
		return m;
	}
	
	private Path toFilePath(Path locator){
		return Paths.get(dir.toString(), locator.toString());
	}
	private Path fileToLocator(Path filePath){
		return dir.relativize(filePath);
	}
	/**
	 * Checks whether a given resource exists in the archive
	 * @param locator
	 * @return 
	 */
	@Override
	public boolean exists(Path locator){
		checkValid();
		iolock.lock();
		try{
			return Files.exists(toFilePath(locator));
		}finally{
			iolock.unlock();
		}
	}
	
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
	 */
	@Override
	public java.util.List<Path> listSubResources(Path locatorPrefix, boolean includeDirectories, boolean recursive) throws IOException{
		final java.util.List<Path> list = new java.util.LinkedList<>();
		iolock.lock();
		try{
			Iterator<Path> i = Files.list(toFilePath(locatorPrefix)).iterator();
			while(i.hasNext()){
				Path p = i.next();
				if(Files.isDirectory(p)){
					if(includeDirectories){
						list.add(fileToLocator(p));
					}
					if(recursive){
						list.addAll(listSubResources(fileToLocator(p),includeDirectories,recursive));
					}
				} else {
					list.add(fileToLocator(p));
				}
			}
			return list;
		}finally{
			iolock.unlock();
		}
	}
	/**
	 * Removes a specific resource from the archive.
	 * @param locator The identifier path to the image. It should have an image 
	 * format suffix.
	 * @return Returns true if the resource being deleted existed, false if 
	 * the resource did not already exist (and therefore was not deleted).
	 * @throws IOException Thrown if there was an error while deleting the 
	 * resource file.
	 */
	@Override
	public boolean delete(Path locator) throws IOException{
		checkValid();
		iolock.lock();
		try{
			if(Files.exists(toFilePath(locator))){
				Files.deleteIfExists(toFilePath(locator));
				return true;
			} else {
				return false;
			}
		}finally{
			iolock.unlock();
		}
	}

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
	@Override
	public BufferedImage getImage(Path locator, Supplier<BufferedImage> doIfNotExists) throws IOException{
		checkValid();
		iolock.lock();
		try{
			if(exists(locator)){
				// it already exists
				return ImageIO.read(toFilePath(locator).toFile());
			} else if(doIfNotExists != null){
				// it doesn't exist, make it
				String format = "png";
				String filename = locator.getFileName().toString();
				if(filename.contains(".")){
					format = filename.substring(filename.lastIndexOf(".")+1).toLowerCase(Locale.US);
				}
				BufferedImage bimg = doIfNotExists.get();
				if (bimg != null) {
					// callback failed to make image
					Files.createDirectories(toFilePath(locator).getParent());
					OutputStream fout = Files.newOutputStream(toFilePath(locator));
					ImageIO.write(bimg, format, fout);
					fout.close();
					return bimg;
				} else {
					// user doesn't want to make an image if it doesn't already exist
					return null;
				}
			}else{
				return null;
			}
		}finally{
			iolock.unlock();
		}
	}
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
	@Override
	public Map<String,String> getProperties(Path locator, Map<String,String> defaultValues) throws IOException{
		checkValid();
		iolock.lock();
		try{
			if(exists(locator)){
				// it already exists
				Properties p = new Properties();
				Map<String,String> m = new LinkedHashMap<>();
				Reader r = Files.newBufferedReader(toFilePath(locator), UTF8);
				p.load(r);
				r.close();
				for(Map.Entry e : p.entrySet()){
					m.put(e.getKey().toString(), e.getValue().toString());
				}
				// add previously unset defaults
				if(defaultValues != null) {
					boolean rewrite = false;
					for (Map.Entry<String, String> e : defaultValues.entrySet()) {
						if (m.containsKey(e.getKey()) == false) {
							// default value not present in stored properties
							m.put(e.getKey(), e.getValue());
							rewrite = true;
						}
					}
					if (rewrite) {
						Files.createDirectories(toFilePath(locator).getParent());
						Writer w = Files.newBufferedWriter(toFilePath(locator), UTF8);
						p.store(w, "");
						w.close();
					}
				}
				return m;
			} else if(defaultValues != null){
				// it doesn't exist, make it
				Properties p = new Properties();
				for(Map.Entry<String,String> e : defaultValues.entrySet()){
					p.setProperty(e.getKey(), e.getValue());
				}
				Files.createDirectories(toFilePath(locator).getParent());
				Writer w = Files.newBufferedWriter(toFilePath(locator), UTF8);
				p.store(w, "Created on "+java.time.ZonedDateTime.now().format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
				w.close();
				return new LinkedHashMap<>(defaultValues);
			}else{
				return null;
			}
		}finally{
			iolock.unlock();
		}
	}
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
	@Override
	public Document getXML(Path locator, Supplier<org.w3c.dom.Document> doIfNotExists) throws IOException {
		checkValid();
		iolock.lock();
		try{
			if(exists(locator)){
				// it already exists
				DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				return builder.parse(toFilePath(locator).toFile());
			} else if(doIfNotExists != null){
				Document doc = doIfNotExists.get();
				if(doc != null){
					Files.createDirectories(toFilePath(locator).getParent());
					Transformer t = TransformerFactory.newInstance().newTransformer();
					t.transform(new DOMSource(doc), new StreamResult(toFilePath(locator).toFile()));
					return doc;
				} else {
					return null;
				}
			}else{
				return null;
			}
		}catch(SAXException | ParserConfigurationException | TransformerException ex){
			throw new IOException("XML error",ex);
		}finally{
			iolock.unlock();
		}
	}
	/**
	 * Throws IllegalStateException if called after close().
	 */
	private void checkValid(){
		if(closed.get()){
			throw new IllegalStateException(this.getClass().getSimpleName()+" is cloReadWriteLock Cannot get or set resourceReentrantReadWriteLocks on closed instance");
		}
	}
	/**
	 * Closes the archive and deletes all temporary files.
	 * @throws IOException Thrown if there was an error while cleaning-up temp 
	 * files.
	 */
	@Override public void close() throws IOException{
		iolock.lock();
		try{
			closed.set(true);
			deleteAll(dir.toFile());
		}finally{
			iolock.unlock();
		}
	}
	/**
	 * Packages all of the data stored in this object and saves it in a zip 
	 * archive (not necessarily .zip file extension)
	 * @param destinationFile The file to save the data
	 * @throws IOException Thrown if there was a problem writing to the file.
	 */
	@Override
	public void save(Path destinationFile) throws IOException {
		iolock.lock();
		try{
			ZipUtils.createZipArchive(dir,destinationFile);
		}finally{
			iolock.unlock();
		}
	}

	private void deleteAll(File f) throws IOException{
		if(f.isDirectory()){
			for(File f2 : f.listFiles()){
				deleteAll(f2);
			}
		}
		f.delete();
	}
}
