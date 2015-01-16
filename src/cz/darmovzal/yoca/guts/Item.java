package cz.darmovzal.yoca.guts;

import java.io.*;
import java.security.*;
import org.spongycastle.openssl.PEMReader;
import org.spongycastle.openssl.PasswordFinder;
import org.spongycastle.openssl.PEMWriter;
import org.spongycastle.openssl.EncryptionException;
import org.spongycastle.util.io.pem.PemObjectGenerator;
import org.spongycastle.util.io.pem.PemObject;

public abstract class Item {
	static {
		Storage.sinit();
	}
	
	protected Slot slot;
	protected int id = -1;
	
	void setSlot(Slot slot){
		this.slot = slot;
	}
	
	public Slot getSlot(){
		return this.slot;
	}
	
	void setId(int id){
		this.id = id;
	}
	
	public int getId(){
		if(this.id == -1) throw new GutsException("ID has not been set on item");
		return this.id;
	}
	
	static Object readPem(InputStream is, final char[] pass) throws IOException {
		try {
			PEMReader r = new PEMReader(new InputStreamReader(is, "UTF-8"), new PasswordFinder(){
				public char[] getPassword(){
					return pass;
				}
			});
			return r.readObject();
		} catch (EncryptionException e){
			throw new PassphraseException("Unable to decrypt PEM object", e);
		} catch (IOException e){
			throw e;
		} catch (Exception e){
			throw new GutsException("Error while reading PEM format", e);
		}
	}
	
	protected void writePem(OutputStream os, Object o, char[] pass) throws IOException {
		try {
			PEMWriter w = new PEMWriter(new OutputStreamWriter(os, "UTF-8"));
			if(pass != null){
				w.writeObject(o, "DESEDE", pass, new SecureRandom());
			} else {
				w.writeObject(o);
			}
			w.flush();
		} catch (IOException e){
			throw e;
		} catch (Exception e){
			throw new GutsException("Error while writing PEM format", e);
		}
	}
	
	protected void writePem2(OutputStream os, final byte[] data, final String type) throws IOException {
		try {
			PEMWriter w = new PEMWriter(new OutputStreamWriter(os, "UTF-8"));
			w.writeObject(new PemObjectGenerator(){
				public PemObject generate(){
					return new PemObject(type, data);
				}
			});
			w.flush();
		} catch (IOException e){
			throw e;
		} catch (Exception e){
			throw new GutsException("Error while writing PEM2 format", e);
		}
	}
	
	public void load(){
		this.load(null);
	}
	
	public boolean exists(){
		String name = this.getFileName();
		return this.slot.storage().exists(name);
	}
	
	public void load(char[] pass){
		if(this.slot == null) throw new GutsException("Not connected to slot");
		String name = this.getFileName();
		try {
			InputStream is = this.slot.storage().read(name);
			try {
				this.load(is, pass);
			} finally {
				is.close();
			}
		} catch (IOException e){
			throw new GutsException("Error while loading item", e);
		}
	}
	
	public void load(File file, char[] pass) throws IOException {
		FileInputStream fis = new FileInputStream(file);
		try {
			this.load(fis, pass);
		} finally {
			fis.close();
		}
	}
	
	public void save(){
		this.save(null);
	}
	
	public void save(char[] pass){
		if(this.slot == null) throw new GutsException("Not connected to slot: " + this);
		String name = this.getFileName();
		try {
			OutputStream os = this.slot.storage().write(name);
			try {
				this.save(os, pass);
				os.flush();
			} finally {
				os.close();
			}
		} catch (IOException e){
			throw new GutsException("Error while saving item", e);
		}
	}
	
	public void save(File file, char[] pass) throws IOException {
		FileOutputStream fos = new FileOutputStream(file);
		try {
			this.save(fos, pass);
		} finally {
			fos.close();
		}
	}
	
	public String getPemString(char[] pass) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		this.save(baos, pass);
		return new String(baos.toByteArray(), "ASCII");
	}
	
	public void remove(){
		if(this.slot == null) throw new GutsException("Not connected to slot: " + this);
		this.slot.storage().remove(this.getFileName());
	}
	
	protected String getFileName(){
		if(this.slot == null) throw new GutsException("Cannot create filename - not bound to slot");
		StringBuilder sb = new StringBuilder();
		sb.append(this.slot.id());
		if(this.id >= 0) sb.append('_').append(this.id);
		sb.append('.').append(this.getSuffix());
		return sb.toString();
	}
	
	protected abstract String getSuffix();
	protected abstract void load(InputStream is, char[] pass) throws IOException;
	protected abstract void save(OutputStream os, char[] pass) throws IOException;
	
	protected byte[] readFileContent(File file){
		try {
			FileInputStream fis = new FileInputStream(file);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				Storage.pump(fis, baos);
			} finally {
				fis.close();
			}
			return baos.toByteArray();
		} catch (Exception e){
			throw new GutsException("Failed to read content from " + file.getAbsolutePath(), e);
		}
	}
	
	@Override
	public String toString(){
		return this.getClass().getName() + "(slot=" + (this.slot != null ? this.slot.id() : "NULL") + ", id=" + this.id + ")";
	}
}

