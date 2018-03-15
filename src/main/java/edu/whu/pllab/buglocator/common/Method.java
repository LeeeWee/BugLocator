package edu.whu.pllab.buglocator.common;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Method {

    private String name;
    private String returnType;
    private String params;
    private String content;
	private String hashKey;

    public Method() {
    	this.setName("");
    	this.setReturnType("");
    	this.setParams("");
    	this.setContent("");
    	this.setHashKey("");
    }
    
	public Method(String name, String returnType, String params) {
    	this.name = name;
    	this.returnType = returnType;
    	this.params = params;
    	this.setContent("");
    	this.setHashKey(calculateMD5(name + " " + returnType + " " + params));
    }
	
	public Method(String sourceCodeName, String name, String returnType, String params) {
		this.name = name;
		this.returnType = returnType;
		this.params = params;
    	this.setContent("");
		this.setHashKey(calculateMD5(name + " " + returnType + " " + params));
	}
	
    private String calculateMD5(String str){
    	String MD5 = ""; 
    	try{
    		MessageDigest md = MessageDigest.getInstance("MD5"); 
    		md.update(str.getBytes()); 
    		byte byteData[] = md.digest();
    		StringBuffer sb = new StringBuffer(); 
    		for(int i = 0 ; i < byteData.length ; i++){
    			sb.append(Integer.toString((byteData[i]&0xff) + 0x100, 16).substring(1));
    		}
    		MD5 = sb.toString();
    		
    	}catch(NoSuchAlgorithmException e){
    		e.printStackTrace(); 
    		MD5 = null; 
    	}
    	return MD5;
    }
	
    
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getReturnType() {
		return returnType;
	}

	public void setReturnType(String returnType) {
		this.returnType = returnType;
	}

	public String getParams() {
		return params;
	}

	public void setParams(String params) {
		this.params = params;
	}
    
    public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}
	
	public String getHashKey() {
		return hashKey;
	}

	public void setHashKey(String hashKey) {
		this.hashKey = hashKey;
	}
}
