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
package hall.collin.christopher.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 *
 * @author Christopher Collin Hall
 */
public abstract class ZipUtils {

	
	/**
	 * Extracts a zip file into a given folder location
	 * @param zipFile A zip archive
	 * @param folder A folder to store the contents of the zip archive
	 */
	public static void extractZipArchive(Path zipFile, Path folder) {

		byte[] buffer = new byte[4096]; // 4K is natural block size on most systems

		try {

			
			try ( ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile.toFile()))) {
				//get the zipped file list entry
				ZipEntry ze = zis.getNextEntry();

				while (ze != null) {
					Path newFile = Paths.get(folder.toString(), ze.getName());

					Logger.getLogger(ZipUtils.class.getName()).log(Level.INFO, String.format("%s.extractZipArchive(%s,%s): %s -> %s",
							ZipUtils.class.getName(), zipFile.toString(), folder.toString(), ze.getName(), newFile.toString()));

					// create all non exists folders
					Files.createDirectories(newFile.getParent());

					try (FileOutputStream fos = new FileOutputStream(newFile.toFile())) {
						int len;
						while ((len = zis.read(buffer)) > 0) {
							fos.write(buffer, 0, len);
						}
					}
					ze = zis.getNextEntry();
				}

				zis.closeEntry();
			}

		} catch (IOException ex) {
			Logger.getLogger(ZipUtils.class.getName()).log(Level.SEVERE, "Error while extracting zip file " + zipFile, ex);
		}
	}

	public static void createZipArchive(final Path sourceDir, final Path destFile) throws IOException{
		final byte[] buffer = new byte[4096]; // 4K is natural block size on most systems
		
		final ZipOutputStream zout = new ZipOutputStream(Files.newOutputStream(destFile));
		
		Files.walkFileTree(sourceDir, new FileVisitor<Path>(){

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				String zipName = sourceDir.relativize(file).toString();
				if(File.separator.equals("/") == false){
					zipName = zipName.replace(File.separator, "/");
				}
				ZipEntry e = new ZipEntry(zipName);
				FileInputStream in = new FileInputStream(file.toFile());
				zout.putNextEntry(e);
				int len;
				while ((len = in.read(buffer)) > 0) {
					zout.write(buffer, 0, len);
				}
				zout.closeEntry();
				in.close();
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				return FileVisitResult.CONTINUE;
			}
		});
		
		zout.close();
	}
}
