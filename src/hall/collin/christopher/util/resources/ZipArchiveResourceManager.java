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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 *
 * @author Christopher Collin Hall
 */
public class ZipArchiveResourceManager extends ArchiveResourceManager{
	
	private Path dir;
	private final AtomicBoolean closed;
	
	private ZipArchiveResourceManager(Path unpackDir){
		this.dir = unpackDir;
		closed = new AtomicBoolean(false);
	}
	
	public static ZipArchiveResourceManager openFile(File archive) throws IOException{
		// init
		Path tempDirectory = Files.createTempDirectory(archive.getName());
		tempDirectory.toFile().deleteOnExit();
		ZipArchiveResourceManager zarm = new ZipArchiveResourceManager(tempDirectory);
		zarm.open(archive);
		return zarm;
	}

	
	private void checkValid() throws FileNotFoundException{
		if(closed.get() || Files.isDirectory(dir) == false){
			throw new FileNotFoundException("Resource manager is already closed. Cannot perform any more operations on this resource manager");
		}
	}
	/**
	 * Synchronizes cached data to ensure that all changes will be correctly 
	 * written to file on save and then clears the cached data.
	 */
	public void flush(){
		// ensure all cached data is written to file and then clear all caches
	}
	
	
	@Override
	protected final void open(File zipFile) throws IOException {
		checkValid();
		ZipUtils.extractZipArchive(zipFile.toPath(), dir);
		closed.set(false);
	}

	@Override
	public void save(File zipFile) throws IOException {
		checkValid();
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public InputStream getInputStream(Path locatorPath) throws IOException {
		checkValid();
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public OutputStream getOutputStream(Path locatorPath) throws IOException {
		checkValid();
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public boolean exists(Path locatorPath) throws IOException {
		checkValid();
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public String getProperty(Path locatorPath, String propertyName, String defaultValue) throws IOException {
		checkValid();
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void setProperty(Path locatorPath, String propertyName, String newValue) throws IOException {
		checkValid();
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public boolean hasProperty(Path locatorPath, String propertyName) throws IOException {
		checkValid();
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public BufferedImage getImage(Path locatorPath, String format, Supplier<BufferedImage> defaultValue) throws IOException {
		checkValid();
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void setImage(Path locatorPath, String format, BufferedImage newValue) throws IOException {
		checkValid();
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void close() throws IOException {
		checkValid();
		closed.set(true);
		// delete files
		Files.walkFileTree(dir, new FileVisitor<Path>(){

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				// do nothing
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.deleteIfExists(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				// do nothing
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.deleteIfExists(dir);
				return FileVisitResult.CONTINUE;
			}
		});
		// then delete folders
		Files.deleteIfExists(dir);
	}
	
}
