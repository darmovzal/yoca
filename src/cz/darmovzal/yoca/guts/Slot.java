package cz.darmovzal.yoca.guts;

import java.io.*;
import java.util.*;
import java.security.*;
import java.math.BigInteger;
import android.util.Log;

public class Slot {
	private static final String LTAG = "YOCA:Slot";
	
	private Storage storage;
	private Properties properties;
	
	private int id;
	private boolean ca;
	private BigInteger serial;
	private String name;
	
	private Keys keys;
	private List<Cert> certs;
	private List<Csr> csrs;
	//private List<Crl> crls;
	
	Slot(Storage storage, int id){
		this.storage = storage;
		this.id = id;
		this.properties = new Properties();
		this.ca = false;
		this.serial = BigInteger.ONE;
		this.certs = new ArrayList<Cert>();
		this.csrs = new ArrayList<Csr>();
		this.keys = new Keys(this);
	}
	
	public int id(){
		return this.id;
	}
	
	Storage storage(){
		return this.storage;
	}
	
	public String getName(){
		return this.name;
	}
	
	public void setName(String name){
		this.name = name;
	}
	
	private int[] getIntArray(String name){
		String s = this.properties.getProperty(name);
		if(s.trim().length() == 0) return new int[0];
		String[] sa = s.split(",");
		int[] ia = new int[sa.length];
		for(int i = 0; i < sa.length; i++)
			ia[i] = Integer.parseInt(sa[i]);
		return ia;
	}
	
	private void setIntArray(String name, int[] ia){
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < ia.length; i++){
			if(i > 0) sb.append(",");
			sb.append(ia[i]);
		}
		this.properties.setProperty(name, sb.toString());
	}
	
	public void load(){
		this.loadProperties();
		
		this.keys.pub().load();
		
		this.certs.clear();
		for(int id : this.getIntArray("certs")){
			try {
				Cert cert = new Cert();
				cert.setSlot(this);
				cert.setId(id);
				cert.load();
				this.certs.add(cert);
			} catch (Exception e){
				Log.e(LTAG, "Loading certificate failed", e);
			}
		}
		Collections.sort(this.certs, new Comparator<Cert>(){
			public int compare(Cert a, Cert b){
				return (new Integer(a.getId())).compareTo(b.getId());
			}
		});
		
		this.csrs.clear();
		for(int id : this.getIntArray("csrs")){
			try {
				Csr csr = new Csr();
				csr.setSlot(this);
				csr.setId(id);
				csr.load();
				this.csrs.add(csr);
			} catch (Exception e){
				Log.e(LTAG, "Loading csr failed", e);
			}
		}
		Collections.sort(this.csrs, new Comparator<Csr>(){
			public int compare(Csr a, Csr b){
				return (new Integer(a.getId())).compareTo(b.getId());
			}
		});
		
		this.ca = "true".equals(this.properties.getProperty("ca"));
		this.serial = new BigInteger(this.properties.getProperty("serial", "0"));
		this.keys.setPrivate("true".equals(this.properties.getProperty("private")));
		this.name = this.properties.getProperty("name", "");
	}
	
	private void loadProperties(){
		try {
			InputStream is = this.storage().read(this.id + ".properties");
			try {
				this.properties.load(is);
			} finally {
				is.close();
			}
		} catch (Exception e){
			throw new GutsException("Error while loading slot " + this.id, e);
		}
	}
	
	private void removeProperties(){
		this.storage().remove(this.id + ".properties");
	}
	
	public void save(){
		int[] certids = new int[this.certs.size()];
		for(int i = 0; i < this.certs.size(); i++)
			certids[i] = this.certs.get(i).getId();
		this.setIntArray("certs", certids);
		
		int[] csrids = new int[this.csrs.size()];
		for(int i = 0; i < this.csrs.size(); i++)
			csrids[i] = this.csrs.get(i).getId();
		this.setIntArray("csrs", csrids);
		
		this.properties.setProperty("ca", String.valueOf(this.isCa()));
		this.properties.setProperty("serial", this.serial.toString());
		this.properties.setProperty("private", String.valueOf(this.keys.hasPrivate()));
		this.properties.setProperty("name", this.name != null ? this.name : "");
		
		this.saveProperties();
	}
	
	private void saveProperties(){
		try {
			OutputStream os = this.storage().write(this.id + ".properties");
			try {
				this.properties.store(os, "YOCA PKI slot");
				os.flush();
			} finally {
				os.close();
			}
		} catch (Exception e){
			throw new GutsException("Error while saving slot " + this.id, e);
		}
	}
	
	public Keys keys(){
		return this.keys;
	}
	
	public List<Cert> certs(){
		return this.certs;
	}
	
	public Cert getCertById(int id){
		for(Cert cert : this.certs){
			if(cert.getId() == id) return cert;
		}
		throw new GutsException("Cannot find Cert by id " + id + " in " + this);
	}
	
	public List<Csr> csrs(){
		return this.csrs;
	}
	
	public Csr getCsrById(int id){
		for(Csr csr : this.csrs){
			if(csr.getId() == id) return csr;
		}
		throw new GutsException("Cannot find CSR by id " + id + " in " + this);
	}
	
	public boolean isCa(){
		for(Cert cert : this.certs){
			if(cert.isCa()) return true;
		}
		return this.ca;
	}
	
	public void setCa(boolean ca){
		this.ca = ca;
	}
	
	public void add(Csr csr){
		int maxid = -1;
		for(Csr _csr : this.csrs){
			if(_csr.getId() > maxid) maxid = _csr.getId();
		}
		csr.setId(maxid + 1);
		csr.setSlot(this);
		this.csrs.add(csr);
	}
	
	public void add(Cert cert){
		int maxid = -1;
		for(Cert _cert : this.certs){
			if(_cert.getId() > maxid) maxid = _cert.getId();
		}
		cert.setId(maxid + 1);
		cert.setSlot(this);
		this.certs.add(cert);
	}
	
	public boolean has(Cert cert){
		for(Cert _cert : this.certs){
			if(_cert.equals(cert)) return true;
		}
		return false;
	}
	
	public BigInteger freshSerial(){
		return this.serial;
	}
	
	public void incSerial(){
		this.serial = this.serial.add(BigInteger.ONE);
	}
	
	public void removeCert(Cert cert){
		this.certs.remove(cert);
	}
	
	public void removeCsr(Csr csr){
		this.csrs.remove(csr);
	}
	
	public void remove(){
		for(Cert cert : this.certs) cert.remove();
		for(Csr csr : this.csrs) csr.remove();
		this.keys().pub().remove();
		if(this.keys().hasPrivate()) this.keys.priv().remove();
		this.removeProperties();
	}
	
	public void exportPkcs12(OutputStream os, int certid, char[] keypass, char[] exppass){
		if(!this.keys().hasPrivate()) throw new GutsException("Record does not have private key");
		Cert cert = this.getCertById(certid);
		PrivateKey pk = this.keys().priv().key(keypass);
		try {
			KeyStore ks = KeyStore.getInstance("PKCS12", "SC");
			ks.load(null, null);
			ks.setKeyEntry(this.name, pk, exppass, new java.security.cert.Certificate[]{ cert.cert() });
			ks.store(os, exppass);
			os.flush();
		} catch (Exception e){
			throw new GutsException("Error creating PKCS#12 bundle", e);
		}
	}
	
	@Override
	public String toString(){
		return "Slot(" + this.id + " ca=" + this.ca + " name=" + this.name + " serial=" + this.serial + " keys=" + this.keys + " certs=" + this.certs + " csrs=" + this.csrs + ")";
	}
}

