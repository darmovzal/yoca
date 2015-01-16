package cz.darmovzal.yoca.guts;

import java.io.*;
import java.util.*;
import java.math.BigInteger;
import org.spongycastle.cert.X509CRLHolder;
import org.spongycastle.cert.X509CRLEntryHolder;
import org.spongycastle.cert.X509v2CRLBuilder;
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder;
import org.spongycastle.operator.ContentSigner;
import org.spongycastle.asn1.x509.CRLReason;
import org.spongycastle.asn1.x509.Extension;
import org.spongycastle.asn1.x509.Extensions;
import org.spongycastle.asn1.DEROctetString;

public class Crl extends Item {
	public enum Reason {
		UNSPECIFIED(CRLReason.unspecified),
		KEY_COMPROMISE(CRLReason.keyCompromise),
		CA_COMPROMISE(CRLReason.cACompromise),
		AFFILIATION_CHANGED(CRLReason.affiliationChanged),
		SUPERSEDED(CRLReason.superseded),
		CESSATION_OF_OPERATION(CRLReason.cessationOfOperation),
		CERTIFICATE_HOLD(CRLReason.certificateHold),
		REMOVE_FROM_CRL(CRLReason.removeFromCRL),
		PRIVILEGE_WITHDRAWN(CRLReason.privilegeWithdrawn),
		AA_COMPROMISE(CRLReason.aACompromise);
		
		private int reason;
		
		Reason(int reason){
			this.reason = reason;
		}
		
		public CRLReason reason(){
			return CRLReason.lookup(this.reason);
		}
		
		public String title(){
			return this.name().toLowerCase().replace('_', ' ');
		}
		
		public static String[] titles(){
			Reason[] values = values();
			String[] ret = new String[values.length];
			for(int i = 0; i < values.length; i++) ret[i] = values[i].title();
			return ret;
		}
		
		public static Reason byReason(int reason){
			for(Reason r : values()){
				if(r.reason == reason) return r;
			}
			return null;
		}
	}
	
	private static class Entry {
		public Reason reason;
		public Date since;
	}
	
	private X509CRLHolder crl;
	private SortedMap<BigInteger, Entry> entries;
	private Cert cert;
	
	Crl(Cert cert){
		this.entries = new TreeMap<BigInteger, Entry>();
		this.cert = cert;
		this.id = cert.id;
		this.slot = cert.slot;
	}
	
	public void save(OutputStream os, char[] pass) throws IOException {
		if(pass != null) throw new GutsException("Saving CLR with password not supported");
		this.writePem2(os, crl.getEncoded(), "X509 CRL");
	}
	
	protected void load(InputStream is, char[] pass) throws IOException {
		try {
			byte[] enc = ((org.spongycastle.jce.provider.X509CRLObject) this.readPem(is, pass)).getEncoded();
			this.crl = new X509CRLHolder(new ByteArrayInputStream(enc));
			this.restore();
		} catch (Exception e){
			throw new GutsException("Error while loading CRL", e);
		}
	}
	
	public void restore(){
		this.entries.clear();
		if(this.crl == null) return;
		for(Object o : this.crl.getRevokedCertificates()){
			X509CRLEntryHolder eh = (X509CRLEntryHolder) o;
			BigInteger serial = eh.getSerialNumber();
			Entry entry = new Entry();
			entry.since = eh.getRevocationDate();
			Extension rext = eh.getExtension(Extension.reasonCode);
			if(rext != null){
				CRLReason r = CRLReason.getInstance(rext.getParsedValue());
				entry.reason = Reason.byReason(r.getValue().intValue());
			} else {
				entry.reason = Reason.UNSPECIFIED;
			}
			this.entries.put(serial, entry);
		}
	}
	
	protected String getSuffix(){
		return "crl";
	}
	
	public void add(BigInteger serial, Date since, Reason reason){
		Entry entry = new Entry();
		entry.since = since;
		entry.reason = reason;
		this.entries.put(serial, entry);
	}
	
	public boolean remove(BigInteger serial){
		return this.entries.remove(serial) != null;
	}
	
	public void generate(char[] pass){
		this.generate(this.cert, this.slot.keys(), pass, new Date(), null, Sig.SHA1);
	}
	
	public void generate(Cert cert, Keys keys, char[] pass, Date update, Date nextUpdate, Sig sig){
		try {
			ContentSigner cs = new JcaContentSignerBuilder(sig.code() + "with" + keys.type()).setProvider("SC").build(keys.priv().key(pass));
			X509v2CRLBuilder b = new X509v2CRLBuilder(cert.subject().get(), update);
			if(nextUpdate != null) b.setNextUpdate(nextUpdate);
			for(BigInteger serial : this.entries.keySet()){
				Entry entry = this.entries.get(serial);
				Extension ext = new Extension(Extension.reasonCode, false, new DEROctetString(entry.reason.reason()));
				b.addCRLEntry(serial, entry.since, new Extensions(new Extension[]{ ext }));
			}
			this.crl = b.build(cs);
		} catch (Exception e){
			throw new GutsException("Error while creating CRL", e);
		}
	}
	
	public Set<BigInteger> serials(){
		return this.entries.keySet();
	}
	
	public Date getSince(BigInteger serial){
		return this.entries.get(serial).since;
	}
	
	public Reason getReason(BigInteger serial){
		return this.entries.get(serial).reason;
	}
	
	public int size(){
		return this.entries.size();
	}
	
	public boolean isRevoked(Cert cert){
		return this.entries.containsKey(cert.serial());
	}
}

