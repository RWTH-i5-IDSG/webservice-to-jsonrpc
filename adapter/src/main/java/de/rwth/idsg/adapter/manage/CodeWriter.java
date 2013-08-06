package de.rwth.idsg.adapter.manage;

import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.file.*;

class CodeWriter {
	
	/**
	 * Returns a zip file system
	 * @param zipFilename to construct the file system from
	 * @param create true if the zip file should be created
	 * @return a zip file system
	 * @throws IOException
	 */	
	static FileSystem createZipFileSystem(String zipFilename,
	                                              boolean create)
	                                              throws IOException {
	  // convert the filename to a URI
	  final Path path = Paths.get(zipFilename);
	  final URI uri = URI.create("jar:file:" + path.toUri().getPath());
	 
	  final Map<String, String> env = new HashMap<String, String>();
	  if (create) {
	    env.put("create", "true");
	  }
	  return FileSystems.newFileSystem(uri, env);
	}
	

	/*
	void toZip(String fileToEmbed, String zipFile){
	    // locate file system by using the syntax defined in java.net.JarURLConnection
	    URI uri = URI.create("jar:file:/codeSamples/zipfs/zipfstest.zip");
	    
	    try (FileSystem zipfs = FileSystems.newFileSystem(uri, null)) {
	        Path externalTxtFile = Paths.get("/codeSamples/zipfs/SomeTextFile.txt");
	        Path pathInZipfile = zipfs.getPath("/SomeTextFile.txt");          
	        // copy a file into the zip file
	        //externalTxtFile.copyTo(pathInZipfile); 
		} catch (IOException e) {
			e.printStackTrace();
		}
    } */
	
	static void toFile(Object dirPath, String content){
		try{
			BufferedWriter out;
			
			if (dirPath instanceof Path) {
				out = Files.newBufferedWriter((Path) dirPath, 
											Charset.forName("UTF-8"), 
											new OpenOption[]{StandardOpenOption.WRITE, 
															StandardOpenOption.TRUNCATE_EXISTING, 
															StandardOpenOption.CREATE});
			} else {
				out = new BufferedWriter(new FileWriter((String) dirPath));
			}
			out.write(content);
			out.close();
			
		} catch (Exception e){e.printStackTrace();}
	}
	
	protected void addFilesToZip(File source, File[] files, String path){
	    try{
	        File tmpZip = File.createTempFile(source.getName(), null);
	        tmpZip.delete();
	        if(!source.renameTo(tmpZip)){
	            throw new Exception("Could not make temp file (" + source.getName() + ")");
	        }
	        byte[] buffer = new byte[4096];
	        ZipInputStream zin = new ZipInputStream(new FileInputStream(tmpZip));
	        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(source));
	        for(int i = 0; i < files.length; i++){
	            InputStream in = new FileInputStream(files[i]);
	            out.putNextEntry(new ZipEntry(path + files[i].getName()));
	            for(int read = in.read(buffer); read > -1; read = in.read(buffer)){
	                out.write(buffer, 0, read);
	            }
	            out.closeEntry();
	            in.close();
	        }
	        for(ZipEntry ze = zin.getNextEntry(); ze != null; ze = zin.getNextEntry()){
	            if(!zipEntryMatch(ze.getName(), files, path)){
	                out.putNextEntry(ze);
	                for(int read = zin.read(buffer); read > -1; read = zin.read(buffer)){
	                    out.write(buffer, 0, read);
	                }
	                out.closeEntry();
	            }
	        }
	        out.close();
	        zin.close();
	        tmpZip.delete();
	    }catch(Exception e){
	        e.printStackTrace();
	    }
	}

	private boolean zipEntryMatch(String zeName, File[] files, String path){
	    for(int i = 0; i < files.length; i++){
	        if((path + files[i].getName()).equals(zeName)){
	            return true;
	        }
	    }
	    return false;
	}
	
	static String getAdapterAddress(){
		// Trying to at least half way determine the host address
		
		try {
			return "http://" + InetAddress.getLocalHost().getHostAddress() + ":8080/adapter-test/request/";
		} catch (UnknownHostException e) {
			return "http://localhost:8080/adapter-test/request/";
		}
	}
}
