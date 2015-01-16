package cz.darmovzal.yoca.guts;

import java.io.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;

public class Codec {
	private static final byte[] MAGIC = new byte[]{ 'Y', '0', 'C', 'a' };
	
	private Codec(){}
	
	private static Key getKey(String pass){
		try {
			MessageDigest md = MessageDigest.getInstance("MD5", "SC");
			byte[] mdkey = md.digest(pass.getBytes("UTF-8"));
			return new SecretKeySpec(mdkey, "AES");
		} catch (Exception e){
			throw new RuntimeException("Failed to create AES key", e);
		}
	}
	
	public static OutputStream encrypt(OutputStream os, String pass) throws IOException {
		for(byte b : MAGIC) os.write(b);
		Cipher c;
		try {
			c = Cipher.getInstance("AES/CBC/PKCS5Padding", "SC");
			c.init(Cipher.ENCRYPT_MODE, getKey(pass));
		} catch (Exception e){
			throw new RuntimeException("Cannot initialize cipher", e);
		}
		byte[] iv = c.getIV();
		os.write((byte) iv.length);
		os.write(iv);
		return new CipherOutputStream(os, c);
	}
	
	public static InputStream decrypt(InputStream is, String pass) throws IOException {
		for(byte b : MAGIC){
			if(is.read() != b) throw new IOException("Wrong magic number");
		}
		int ivlen = is.read();
		byte[] iv = new byte[ivlen];
		for(int i = 0; i < ivlen; i++) iv[i] = (byte) is.read();
		Cipher c;
		try {
			c = Cipher.getInstance("AES/CBC/PKCS5Padding", "SC");
			c.init(Cipher.DECRYPT_MODE, getKey(pass), new IvParameterSpec(iv));
		} catch (Exception e){
			throw new RuntimeException("Cannot initialize cipher", e);
		}
		return new CipherInputStream(is, c);
	}
}

