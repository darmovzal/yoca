package cz.darmovzal.yoca.guts;

import java.io.*;
import java.security.*;
import java.security.cert.*;
import java.util.*;
import java.util.zip.*;
import org.spongycastle.openssl.PEMReader;
import org.spongycastle.openssl.PasswordFinder;
import org.spongycastle.openssl.PEMWriter;
import org.spongycastle.util.io.pem.PemObjectGenerator;
import org.spongycastle.util.io.pem.PemObject;
import android.util.Log;

public class Storage {
	public static final boolean TRIAL = true;
	private static final String LTAG = "YOCA:Storage";
	
	private static boolean initialized = false;
	
	static {
		sinit();
	}
	
	private FileHandler fh;
	private List<Slot> slots;
	
	static void sinit(){
		if(initialized) return;
		Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
		initialized = true;
	}
		
	public void setFileHandler(FileHandler fh){
		this.fh = fh;
		this.slots = new ArrayList<Slot>();
	}
	
	InputStream read(String name) throws IOException {
		return this.fh.read(name);
	}
	
	OutputStream write(String name) throws IOException {
		return this.fh.write(name);
	}
	
	void remove(String name){
		this.fh.remove(name);
	}
	
	boolean exists(String name){
		for(String _name : this.fh.list()){
			if(_name.equals(name)) return true;
		}
		return false;
	}
	
	Object readPem(String name, final char[] pass){
		try {
			InputStream is = this.fh.read(name);
			try {
				PEMReader r = new PEMReader(new InputStreamReader(is, "UTF-8"), new PasswordFinder(){
					public char[] getPassword(){
						return pass;
					}
				});
				return r.readObject();
			} finally {
				is.close();
			}
		} catch (Exception e){
			throw new GutsException("Error while reading PEM file \"" + name + "\"", e);
		}
	}
	
	void writePem(String name, Object o, char[] pass){
		try {
			OutputStream os = this.fh.write(name);
			PEMWriter w = new PEMWriter(new OutputStreamWriter(os, "UTF-8"));
			try {
				if(pass != null){
					w.writeObject(o, "DESEDE", pass, new SecureRandom());
				} else {
					w.writeObject(o);
				}
				w.flush();
			} finally {
				w.close();
			}
		} catch (Exception e){
			throw new GutsException("Error while writing PEM file \"" + name + "\"", e);
		}
	}
	
	void writePem2(final byte[] data, final String type, String name){
		try {
			OutputStream os = this.fh.write(name);
			PEMWriter w = new PEMWriter(new OutputStreamWriter(os, "UTF-8"));
			try {
				w.writeObject(new PemObjectGenerator(){
					public PemObject generate(){
						return new PemObject(type, data);
					}
				});
				w.flush();
			} finally {
				w.close();
			}
		} catch (Exception e){
			throw new GutsException("Error while writing PEM2 file \"" + name + "\"", e);
		}
	}
	
	public void load(){
		Log.i(LTAG, "Loading storage data");
		try {
			for(String name : this.fh.list()){
				try {
					if(!name.endsWith(".properties")) continue;
					int id = Integer.parseInt(name.substring(0, name.length() - 11));
					Log.i(LTAG, "Loading slot " + id);
					Slot slot = new Slot(this, id);
					slot.load();
					this.slots.add(slot);
				} catch (Exception e){
					Log.e(LTAG, "Unable to load slot", e);
				}
			}
			Collections.sort(this.slots, new Comparator<Slot>(){
				public int compare(Slot a, Slot b){
					return (new Integer(a.id())).compareTo(b.id());
				}
			});
		} catch (Exception e){
			throw new GutsException("Error while loading storage", e);
		}
		Log.i(LTAG, "Loading storage data done");
	}
	
	public Slot getSlotById(int id){
		for(Slot slot : this.slots){
			if(slot.id() == id) return slot;
		}
		throw new GutsException("Cannot find slot by ID: " + id);
	}
	
	public Slot newSlot(){
		int maxid = -1;
		for(Slot slot : this.slots){
			if(slot.id() > maxid) maxid = slot.id();
		}
		Slot slot = new Slot(this, maxid + 1);
		this.slots.add(slot);
		return slot;
	}
	
	public List<Slot> slots(){
		return this.slots;
	}
	
	public Slot getSlotByKey(PublicKey pub){
		for(Slot slot : this.slots){
			if(slot.keys().pub().key().equals(pub)) return slot;
		}
		return null;
	}
	
	public int importPkcs12(File file, char[] pass){
		try {
			KeyStore ks = KeyStore.getInstance("PKCS12", "SC");
			if(!file.canRead()) throw new GutsException("Cannot read file \"" + file.getAbsolutePath() + "\"");
			FileInputStream fis = new FileInputStream(file);
			try {
				ks.load(fis, pass);
			} catch (Exception e){
				throw new PassphraseException("Unable to load PKCS#12 bundle", e);
			} finally {
				fis.close();
			}
			List<String> aliases = Collections.list(ks.aliases());
			int total = 0;
			for(String alias : aliases){
				java.security.cert.Certificate cert = ks.getCertificate(alias);
				PrivateKey priv = (PrivateKey) ks.getKey(alias, pass);
				if(cert == null) continue;
				PublicKey pub = cert.getPublicKey();
				Slot slot = this.getSlotByKey(pub);
				boolean imported = false;
				Cert _cert = null;
				if(cert instanceof X509Certificate)
					_cert = new Cert((X509Certificate) cert);
				if(slot != null){
					if(!slot.has(_cert)){
						slot.add(_cert);
						_cert.save();
						imported = true;
					}
					if(!slot.keys().hasPrivate() && (priv != null)){
						slot.keys().setPrivate(priv);
						slot.keys().priv().save(pass);
						slot.keys().priv().clear();
						imported = true;
					}
					slot.save();
				} else {
					imported = true;
					slot = this.newSlot();
					slot.setName(alias);
					slot.keys().pub().set(pub);
					slot.keys().pub().save();
					if(priv != null){
						slot.keys().setPrivate(priv);
						slot.keys().priv().save(pass);
						slot.keys().priv().clear();
					}
					if(_cert != null){
						slot.add(_cert);
						_cert.save();
					}
					slot.save();
				}
				if(imported) total++;
			}
			return total;
		} catch (UnrecoverableKeyException e){
			throw new PassphraseException("Wrong PKCS#12 key passphrase", e);
		} catch (PassphraseException e){
			throw e;
		} catch (Exception e){
			throw new GutsException("Failed to import PKCS#12 bundle from \"" + file + "\"", e);
		}
	}
	
	public Slot importCertificate(File file, String name){
		try {
			Cert cert = new Cert();
			cert.importFrom(file);
			return this.importCertificate(cert, name);
		} catch (Exception e){
			throw new GutsException("Failed to import certificate from " + file.getAbsolutePath(), e);
		}
	}
	
	private Slot importCertificate(Cert cert, String name){
		PublicKey pub = cert.cert().getPublicKey();
		Slot slot = this.getSlotByKey(pub);
		if(slot == null){
			slot = this.newSlot();
			slot.setName(name);
			slot.keys().pub().set(pub);
			slot.keys().pub().save();
		}
		for(Cert _cert : slot.certs()){
			if(_cert.equals(cert)) return slot;
		}
		slot.add(cert);
		cert.save();
		slot.save();
		return slot;
	}
	
	public Slot importKeys(File file, char[] pass, String name){
		try {
			Object o = Keys.importFrom(file, pass);
			if(o == null) throw new NullPointerException("Empty keys");
			return this.importKeys(o, pass, name);
		} catch (PassphraseException e){
			throw e;
		} catch (Exception e){
			throw new GutsException("Failed to import keys from " + file.getAbsolutePath(), e);
		}
	}
	
	private Slot importKeys(Object o, char[] pass, String name){
		PublicKey pub;
		PrivateKey priv = null;
		if(o instanceof PublicKey){
			pub = (PublicKey) o;
		} else if(o instanceof KeyPair){
			pub = ((KeyPair) o).getPublic();
			priv = ((KeyPair) o).getPrivate();
		} else {
			throw new GutsException("Unknown imported keys class: " + o.getClass().getName());
		}
		if(!Keys.Type.supported(pub)) throw new GutsException("Unsupported key type: " + pub.getClass().getName());
		Slot slot = this.getSlotByKey(pub);
		if(slot == null){
			slot = this.newSlot();
			slot.setName(name);
			slot.keys().pub().set(pub);
			slot.keys().pub().save();
		}
		if((priv != null) && !slot.keys().hasPrivate()){
			slot.keys().setPrivate(priv);
			slot.keys().priv().save(pass);
			slot.keys().priv().clear();
		}
		slot.save();
		return slot;
	}
	
	public Slot importCsr(File file, String name){
		try {
			Csr csr = new Csr();
			csr.importFrom(file);
			return this.importCsr(csr, name);
		} catch (Exception e){
			throw new GutsException("Failed to import CSR from " + file.getAbsolutePath(), e);
		}
	}
	
	private Slot importCsr(Csr csr, String name){
		PublicKey pub = csr.getPublicKey();
		Slot slot = this.getSlotByKey(pub);
		if(slot == null){
			slot = this.newSlot();
			slot.setName(name);
			slot.keys().pub().set(pub);
			slot.keys().pub().save();
		}
		for(Csr _csr: slot.csrs()){
			if(_csr.equals(csr)) return slot;
		}
		slot.add(csr);
		csr.save();
		slot.save();
		return slot;
	}
	
	public Slot importFromPemString(String pem, char[] pass, String name){
		if(pem == null) throw new NullPointerException("PEM string cannot be null");
		byte[] data;
		try {
			data = pem.getBytes("ASCII");
		} catch (IOException e){
			throw new GutsException("Cannot convert PEM to ASCII bytes");
		}
		try {
			boolean imported = false;
			Csr csr = new Csr();
			try {
				csr.load(new ByteArrayInputStream(data), null);
				imported = true;
			} catch (Exception e){
				Log.w(LTAG, "Error trying to import CSR from PEM: " + e);
			}
			try {
				if(imported) return this.importCsr(csr, name);
			} catch (Exception e){
				throw new GutsException("Error importing CSR into records", e);
			}
			Cert cert = new Cert();
			try {
				cert.load(new ByteArrayInputStream(data), null);
				imported = true;
			} catch (Exception e){
				Log.w(LTAG, "Error trying to import Cert from PEM: " + e);
			}
			try {
				if(imported) return this.importCertificate(cert, name);
			} catch (Exception e){
				throw new GutsException("Error importing certificate into records", e);
			}
			Object keys = null;
			try {
				keys = Item.readPem(new ByteArrayInputStream(data), pass);
				imported = keys != null;
			} catch (PassphraseException e){
				throw e;
			} catch (Exception e){
				Log.w(LTAG, "Error trying to import keys from PEM: " + e);
			}
			try {
				if(imported) return this.importKeys(keys, pass, name);
			} catch (Exception e){
				throw new GutsException("Error importing keys into records", e);
			}
			throw new GutsException("No type of object succeeded to load");
		} catch (PassphraseException e){
			throw e;
		} catch (Exception e){
			throw new GutsException("Failed to import object from PEM string", e);
		}
	}
	
	public void removeSlot(Slot slot){
		this.slots.remove(slot);
	}
	
	public void mrproper(){
		this.slots.clear();
		for(String name : this.fh.list())
			this.fh.remove(name);
	}
	
	public void archive(File file, String pass) throws IOException {
		ZipOutputStream zos = new ZipOutputStream(Codec.encrypt(new FileOutputStream(file), pass));
		try {
			for(String name : this.fh.list()){
				Log.i(LTAG, "Archiving item " + name);
				zos.putNextEntry(new ZipEntry(name));
				InputStream is = this.read(name);
				try {
					pump(is, zos);
				} finally {
					is.close();
				}
			}
			zos.flush();
		} finally {
			zos.close();
		}
	}
	
	public void unarchive(File file, String pass, boolean dry) throws IOException {
		ZipInputStream zis = new ZipInputStream(Codec.decrypt(new FileInputStream(file), pass));
		try {
			ZipEntry ze;
			int count = 0;
			while((ze = zis.getNextEntry()) != null){
				count++;
				String name = ze.getName();
				Log.i(LTAG, "Restoring item " + name);
				OutputStream os = dry ? new ByteArrayOutputStream() : this.write(name);
				try {
					pump(zis, os);
				} finally {
					os.close();
				}
			}
			if(count == 0) throw new PassphraseException("Empty ZIP stream - decryption failed?");
		} finally {
			zis.close();
		}
	}
	
	static void pump(InputStream is, OutputStream os) throws IOException {
		byte[] buffer = new byte[1024];
		int count;
		while((count = is.read(buffer)) > 0)
			os.write(buffer, 0, count);
		os.flush();
	}
	
	public static Date trialNotAfter(Date notBefore, Date notAfter){
		if(!TRIAL) return notAfter;
		if((notBefore == null) || (notAfter == null)) return notAfter;
		long b = notBefore.getTime();
		long a = notAfter.getTime();
		long max = 7 * 24 * 60 * 60 * 1000;
		return new Date(a - b < max ? a : b + max);
	}
	
	public static String trialOrganization(String org){
		if(!TRIAL) return org;
		return "Generated with YOCA Trial";
	}
	
	public Cert getVerificationCert(Cert cert){
		for(Slot _slot : this.slots()){
			if(!_slot.isCa()) continue;
			for(Cert _cert : _slot.certs()){
				try {
					if(_cert.verify(cert)) return _cert;
				} catch (Exception e){
					Log.e(LTAG, "Certificate verification failed", e);
				}
			}
		}
		return null;
	}
	
	public boolean isRevoked(Cert cert){
		Cert vercert = this.getVerificationCert(cert);
		if(vercert == null) return false;
		Crl vercrl = vercert.crl();
		if(vercrl == null) return false;
		return vercrl.isRevoked(cert);
	}
}

