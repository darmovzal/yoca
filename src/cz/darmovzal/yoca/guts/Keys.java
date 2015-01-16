package cz.darmovzal.yoca.guts;

import java.io.*;
import java.util.*;
import java.security.*;
import java.security.interfaces.*;
import java.security.spec.*;
import org.spongycastle.crypto.params.AsymmetricKeyParameter;
import org.spongycastle.crypto.params.RSAKeyParameters;
import org.spongycastle.crypto.params.DSAPublicKeyParameters;
import org.spongycastle.crypto.util.PublicKeyFactory;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;

public class Keys {
	public enum Type {
		RSA(1024, 2048, 4096),
		DSA(512, 1024);
		
		private int[] sizes;
		
		Type(int ... sizes){
			this.sizes = sizes;
		}
		
		public static String[] titles(){
			int count = values().length;
			String[] ret = new String[count];
			for(int i = 0; i < count; i++)
				ret[i] = values()[i].name();
			return ret;
		}
		
		public static Type get(int index){
			return values()[index];
		}
		
		public static int[] allSizes(){
			SortedSet<Integer> set = new TreeSet<Integer>();
			for(Type type : values()){
				for(int size : type.sizes){
					set.add(size);
				}
			}
			int[] ret = new int[set.size()];
			Iterator<Integer> iter = set.iterator();
			for(int i = 0; i < ret.length; i++)
				ret[i] = iter.next();
			return ret;
		}
		
		public static String[] allSizeTitles(){
			int[] sizes = allSizes();
			String[] ret = new String[sizes.length];
			for(int i = 0; i < sizes.length; i++)
				ret[i] = String.valueOf(sizes[i]);
			return ret;
		}
		
		public boolean supports(int size){
			for(int _size : this.sizes){
				if(size == _size) return true;
			}
			return false;
		}
		
		public static boolean supported(Key key){
			if(key instanceof RSAKey){
				return true;
			} else if(key instanceof DSAKey){
				return true;
			} else {
				return false;
			}
		}
	}
	
	public class Private extends Item {
		private PrivateKey key;
		
		public Private(PrivateKey key){
			this.key = key;
		}
		
		void set(PrivateKey key){
			this.key = key;
		}
		
		public void clear(){
			this.key = null;
		}
		
		public boolean isLoaded(){
			return this.key != null;
		}
		
		protected String getSuffix(){
			return "pri";
		}
		
		public PrivateKey key(char[] pass){
			if(this.key != null) return this.key;
			try {
				InputStream is = this.slot.storage().read(this.getFileName());
				Object key;
				try {
					key = this.readPem(is, pass);
				} finally {
					is.close();
				}
				return ((KeyPair) key).getPrivate();
			} catch (PassphraseException e){
				throw e;
			} catch (Exception e){
				throw new GutsException("Error while loading private key", e);
			}
		}
		
		public byte[] sign(InputStream is, char[] pass, Sig sig) throws IOException {
			try {
				Signature signer = Signature.getInstance(sig.code() + "with" + Keys.this.type(), "SC");
				signer.initSign(this.key(pass));
				byte[] buffer = new byte[1024];
				int count;
				while((count = is.read(buffer, 0, buffer.length)) > 0)
					signer.update(buffer, 0, count);
				return signer.sign();
			} catch (IOException e){
				throw e;
			} catch (Exception e){
				throw new GutsException("Error while counting signature", e);
			}
		}
		
		public void load(InputStream is, char[] pass) throws IOException {
			Object key = this.readPem(is, pass);
			if(!(key instanceof KeyPair)) throw new GutsException("Loaded private key is not a KeyPair instance: " + key);
			this.key = ((KeyPair) key).getPrivate();
		}
		
		public void save(OutputStream os, char[] pass) throws IOException {
			if(this.key == null) return;
			this.writePem(os, this.key, pass);
		}
		
		public void changePassphrase(char[] orgpass, char[] newpass){
			this.load(orgpass);
			this.save(newpass);
			this.clear();
		}
	}
	
	public class Public extends Item {
		private PublicKey key;
		
		public Public(PublicKey key){
			this.key = key;
		}
		
		void set(PublicKey key){
			this.key = key;
		}
		
		public PublicKey key(){
			return this.key;
		}
		
		protected String getSuffix(){
			return "pub";
		}
		
		Type getType(){
			if(this.key instanceof RSAPublicKey){
				return Type.RSA;
			} else if(this.key instanceof DSAPublicKey){
				return Type.DSA;
			} else {
				throw new GutsException("Unknown public key type: " + this.key.getClass().getName());
			}
		}
		
		public boolean verify(InputStream is, Sig sig, byte[] signature) throws IOException {
			try {
				Signature signer = Signature.getInstance(sig.code() + "with" + Keys.this.type(), "SC");
				signer.initVerify(this.key);
				byte[] buffer = new byte[1024];
				int count;
				while((count = is.read(buffer, 0, buffer.length)) > 0)
					signer.update(buffer, 0, count);
				return signer.verify(signature);
			} catch (IOException e){
				throw e;
			} catch (Exception e){
				throw new GutsException("Error while verifying signature", e);
			}
		}
		
		public void load(InputStream is, char[] pass) throws IOException {
			Object key = this.readPem(is, pass);
			if(!(key instanceof PublicKey)) throw new GutsException("Loaded public key is not a PublicKey instance: " + key);
			this.key = (PublicKey) key;
		}
		
		public void save(OutputStream os, char[] pass) throws IOException {
			this.writePem(os, this.key, null);
		}
		
		@Override
		public boolean equals(Object o){
			if(!(o instanceof Public)) return false;
			return this.key.equals(((Public) o).key);
		}
	}
	
	private Slot slot;
	private boolean hasPrivate;
	private Private priv;
	private Public pub;
	
	Keys(Slot slot){
		this.slot = slot;
		this.priv = new Private(null);
		this.priv.setSlot(slot);
		this.pub = new Public(null);
		this.pub.setSlot(slot);
	}
	
	public Type type(){
		return this.pub.getType();
	}
	
	public void setPrivate(PrivateKey priv){
		this.priv.set(priv);
		this.hasPrivate = priv != null;
	}
	
	public void setPrivate(boolean has){
		this.hasPrivate = has;
	}
	
	public boolean hasPrivate(){
		return this.hasPrivate;
	}
	
	public Public pub(){
		return this.pub;
	}
	
	public Private priv(){
		if(!this.hasPrivate) throw new GutsException("This Keys does not contain private part");
		return this.priv;
	}
	
	public void generate(Type type, int size){
		try {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance(type.name(), "SC");
			kpg.initialize(size);
			KeyPair kp = kpg.generateKeyPair();
			this.priv.set(kp.getPrivate());
			this.pub.set(kp.getPublic());
			this.hasPrivate = true;
		} catch (Exception e){
			throw new GutsException("Error while generating " + type + " keys of size " + size, e);
		}
	}
	
	public void setSlot(Slot slot){
		this.slot = slot;
	}
	
	public int size(){
		PublicKey pub = this.pub.key();
		if(pub instanceof RSAPublicKey){
			return ((RSAPublicKey) pub).getModulus().bitLength();
		} else if(pub instanceof DSAPublicKey){
			return ((DSAPublicKey) pub).getParams().getP().bitLength();
		} else {
			throw new GutsException("Unsupported key type: " + pub.getClass().getName());
		}
	}
	
	public static PublicKey convert(SubjectPublicKeyInfo spki){
		try {
			AsymmetricKeyParameter params = PublicKeyFactory.createKey(spki);
			KeySpec spec;
			String type;
			if(params instanceof RSAKeyParameters){
				RSAKeyParameters rsa = (RSAKeyParameters) params;
				spec = new RSAPublicKeySpec(rsa.getModulus(), rsa.getExponent());
				type = "RSA";
			} else if(params instanceof DSAPublicKeyParameters){
				DSAPublicKeyParameters dsa = (DSAPublicKeyParameters) params;
				spec = new DSAPublicKeySpec(dsa.getY(), dsa.getParameters().getP(), dsa.getParameters().getQ(), dsa.getParameters().getG());
				type = "DSA";
			} else {
				throw new GutsException("Unknown key parameters class: " + params.getClass());
			}
			KeyFactory kf = KeyFactory.getInstance(type, "SC");
			return kf.generatePublic(spec);
		} catch (Exception e){
			throw new GutsException("Failed to convert public key", e);
		}
	}
	
	public static Object importFrom(File file, char[] pass){
		try {
			FileInputStream fis = new FileInputStream(file);
			try {
				return Item.readPem(fis, pass);
			} finally {
				fis.close();
			}
		} catch (PassphraseException e){
			throw e;
		} catch (Exception e){
			throw new GutsException("Unable to import keys from file " + file.getAbsolutePath(), e);
		}
	}
}

