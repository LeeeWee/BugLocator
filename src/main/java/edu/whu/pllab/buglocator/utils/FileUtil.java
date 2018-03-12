package edu.whu.pllab.buglocator.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;


public class FileUtil {

	/** 
     * copy single file 
     * @param srcFileName source file 
     * @param descFileName destination file 
     * @param overlay whether overlay when destination file exists
     */  
    public static void copyFile(String srcFilePath, String destFilePath, boolean overlay) {  
        // check if destination file exists
        File destFile = new File(destFilePath);  
        if (destFile.exists()) { 
        	// if overlay, delete original destination file
            if (overlay) {  
                new File(destFilePath).delete();  
            }  else {
            	System.err.println("Destination file exists!");
            }
        } else {  
            if (!destFile.getParentFile().exists()) {  
                destFile.getParentFile().mkdirs(); 
            }  
        }  
        // copying
        int byteread = 0;  // read source file by byte 
        InputStream in = null;  
        OutputStream out = null;  
        try {  
            in = new FileInputStream(new File(srcFilePath));  
            out = new FileOutputStream(destFile);  
            byte[] buffer = new byte[1024];  
  
            while ((byteread = in.read(buffer)) != -1) {  
                out.write(buffer, 0, byteread);  
            }  
            return;  
        } catch (Exception e) {  
        	e.printStackTrace();
        } finally {  
            try {  
                out.close();  
                in.close();  
            } catch (IOException e) {  
                e.printStackTrace();  
            }  
        }  
    } 
    
	/** 
    * copy all files in source directory to destination directory 
    * @param srcDirName source directory 
    * @param destDirName destination directory 
    * @param overlay whether overlay when destination directory exists 
    */  
    public static void copyDirectory(String srcDirName, String destDirName, boolean overlay) {  
        File srcDir = new File(srcDirName);  
        if (!srcDir.isDirectory()) {  
        	System.err.print("source file isn't a directory");
            return;  
        }  
        if (!destDirName.endsWith(File.separator)) {  
            destDirName = destDirName + File.separator;  
        }  
        File destDir = new File(destDirName);  
        if (destDir.exists()) {  
            // if overlay, delete destination file  
            if (overlay) {  
                new File(destDirName).delete();  
            } else {  
                System.err.println("Destination file exists!");  
            }  
        } else {  
            destDir.mkdirs(); 
        }  
        File[] files = srcDir.listFiles();  
        for (int i = 0; i < files.length; i++) {  
            if (files[i].isFile()) {  
                FileUtil.copyFile(files[i].getAbsolutePath(), destDirName + files[i].getName(), overlay);  
            } else if (files[i].isDirectory()) {  
                FileUtil.copyDirectory(files[i].getAbsolutePath(), destDirName + files[i].getName(), overlay);  
            }  
        }  
    }
    
	
    /**
     * get all files from given directory
     */
    public static List<String> getAllFiles(String dir) {
    	List<String> files = new ArrayList<String>();
    	if(files != null)
    		files.clear();
    	return getFile(dir, null, files);
    }
	
    /**
     * get all files with specific suffix from given directory
     */
    public static List<String> getAllFiles(String dir, String suffix) {
    	List<String> files = new ArrayList<String>();
    	if(files != null)
    		files.clear();
    	return getFile(dir, suffix, files);
    }
    
    /**
     * get all files with specific suffix from given directory
     * @param dir input directory path
     * @param suffix input suffix name
     * @param files data used in recursion
     * @return all files with specific suffix in directory
     */
    private static List<String> getFile(String dir, String suffix, List<String> files) {
    	if (null != dir) {
            File file = new File(dir);
            if (file.exists()) {
                File[] list = file.listFiles();
                if(null != list){
                    for (File child : list) {
                        if (child.isFile()) {
                            String temp = child.getAbsolutePath();
                            if (suffix != null) {
                            	if(temp.substring(temp.length() - suffix.length()).equals(suffix))
                            		files.add(child.getAbsolutePath());
                            } else {
                            	files.add(child.getAbsolutePath());
                            }
                        } else if (child.isDirectory()) {
                        	getFile(child.getAbsolutePath(), suffix, files);
                        }
                    }
                }
            }
        }
        return files;
	}
    
    /** 
     * read file content to string 
     * @param filePath input file path        
     * @return string of file content
     */
    public static String readFileToString(String filePath) {
		StringBuilder fileData = new StringBuilder(1000);
		try {
			BufferedReader reader = new BufferedReader(new FileReader(filePath));
			char[] buf = new char[10];
			int numRead = 0;
			while ((numRead = reader.read(buf)) != -1) {
				String readData = String.valueOf(buf, 0, numRead);
				fileData.append(readData);
				buf = new char[1024];
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return  fileData.toString();	
	}
    
    
    /** 
     * write string to given file path
     */
	public static void writeStringToFile(String str, String filePath) {
		File file = new File(filePath);
		// if file exists, delete if
		if (file.exists()) {
			file.delete();
		}
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file.getAbsoluteFile()));
			bw.write(str);
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * read file content to string list
	 * @param filePath input file path
	 * @return a string list, and each element is corresponded to a line in given file
	 */
	public static List<String> readFileToList(String filePath) {
		List<String> stringList = new ArrayList<String>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(filePath));
			String line = null;
			while ((line = reader.readLine()) != null) {
				stringList.add(line);
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return stringList;
	}
	
	/**
	 * write a string into given file path, and each line corresponds to an element
	 */
	public static void writeListToFile(List<String> stringList, String filePath) throws IOException {
		File file = new File(filePath);
		// if file doesn't exists, then create it
		if (file.exists()) {
			file.delete();
		}
		file.createNewFile();
		
		FileWriter fw = new FileWriter(file.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);
		for (String str : stringList) {
			bw.write(str);
			bw.newLine();
		}
		bw.close();
	}
	
	/**
	 * merge two file into a new file
	 * @param filePath1  the input first file path
	 * @param filePath2  the input second file path
	 * @param destFilePath  the destination file path
	 */
	public static void mergeFile(String filePath1, String filePath2, String destFilePath) throws IOException{
		BufferedWriter writer = new BufferedWriter(new FileWriter(destFilePath));
		BufferedReader reader = new BufferedReader(new FileReader(filePath1));
		String line;
		while ((line = reader.readLine()) != null) {
			writer.write(line);
			writer.newLine();
		}
		reader.close();
		reader = new BufferedReader(new FileReader(filePath2));
		while ((line = reader.readLine()) != null) {
			writer.write(line);
			writer.newLine();
		}
		reader.close();
		writer.close();
	}
	
	
	/**
	 * split documents 
	 * @param inputPath input file path
	 * @param cores number of documents to split 
	 * @return a list of string list 
	 */
	public static List<List<String>> spiltDocuments(String inputPath, int cores) {
		List<List<String>> splitDocument = new ArrayList<List<String>>();
		try {
			List<String> document = FileUtil.readFileToList(inputPath);
			splitDocument = splitStringList(document, cores);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return splitDocument;
	}
	
	/**
	 * split string list
	 * @param stringList input string list
	 * @param cores number of list to split 
	 * @return a list of list 
	 */
	public static List<List<String>> splitStringList(List<String> stringList, int cores) {
		List<List<String>> result = new ArrayList<List<String>>();
		int averN = stringList.size() / cores;
		for (int i = 0; i < cores; i++) {
			List<String> texts = new ArrayList<String>();
			result.add(texts);
		}
		int n = 0;
		int core = 0;
		List<String> texts = result.get(core);
		for (int i = 0; i < stringList.size(); i++) {
			texts.add(stringList.get(i));
			n++;
			if (n > averN && core < cores) {
				n = 0;
				core++;
				texts = result.get(core);
			}
		}
		return result;
	}

	
}
