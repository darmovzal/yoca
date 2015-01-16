package cz.darmovzal.yoca.guts;

import java.util.*;
import java.security.cert.X509Certificate;
import java.security.cert.X509Extension;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.ASN1Encodable;
import org.spongycastle.asn1.DEROctetString;
import org.spongycastle.asn1.DERBitString;
import org.spongycastle.asn1.ASN1Primitive;
import org.spongycastle.asn1.DLSequence;
import org.spongycastle.asn1.x509.Extension;
import org.spongycastle.asn1.x509.Extensions;
import org.spongycastle.asn1.x509.BasicConstraints;
import org.spongycastle.asn1.x509.KeyUsage;
import org.spongycastle.asn1.x509.ExtendedKeyUsage;
import org.spongycastle.asn1.x509.KeyPurposeId;
import org.spongycastle.asn1.x509.GeneralName;
import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.asn1.pkcs.Attribute;
import org.spongycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.spongycastle.pkcs.PKCS10CertificationRequest;
import org.spongycastle.cert.X509v3CertificateBuilder;
import android.util.Log;

public class Exts {
	private static final String LTAG = "YOCA:Exts";
	
	public static enum BasicKeyUsage {
		CRL_SIGN(KeyUsage.cRLSign),
		DATA_ENCIPHERMENT(KeyUsage.dataEncipherment),
		DECIPHER_ONLY(KeyUsage.decipherOnly),
		DIGITAL_SIGNATURE(KeyUsage.digitalSignature),
		ENCIPHER_ONLY(KeyUsage.encipherOnly),
		KEY_AGREEMENT(KeyUsage.keyAgreement),
		KEY_CERT_SIGN(KeyUsage.keyCertSign),
		KEY_ENCIPHERMENT(KeyUsage.keyEncipherment),
		KEY_NON_REPUDIATION(KeyUsage.nonRepudiation);
		
		private int flag;
		
		BasicKeyUsage(int flag){
			this.flag = flag;
		}
		
		public int flag(){
			return this.flag;
		}
		
		public String title(){
			return this.name().replace('_', ' ').toLowerCase();
		}
	}
	
	public static enum ExtKeyUsage {
		SERVER_AUTH(KeyPurposeId.id_kp_serverAuth),
		CLIENT_AUTH(KeyPurposeId.id_kp_clientAuth),
		CODE_SIGNING(KeyPurposeId.id_kp_codeSigning),
		EMAIL_PROTECTION(KeyPurposeId.id_kp_emailProtection),
		TIME_STAMPING(KeyPurposeId.id_kp_timeStamping),
		OCSP_SIGNING(KeyPurposeId.id_kp_OCSPSigning),
		IPSEC_END_SYSTEM(KeyPurposeId.id_kp_ipsecEndSystem),
		IPSEC_TUNNEL(KeyPurposeId.id_kp_ipsecTunnel),
		IPSEC_USER(KeyPurposeId.id_kp_ipsecUser);
		
		private ASN1ObjectIdentifier oid;
		
		ExtKeyUsage(ASN1ObjectIdentifier oid){
			this.oid = oid;
		}
		
		public ASN1ObjectIdentifier oid(){
			return this.oid;
		}
		
		public String title(){
			return this.name().replace('_', ' ').toLowerCase();
		}
	}
	
	public static enum SubjectAltNameTag {
		EMAIL(GeneralName.rfc822Name, "Email (RFC 822)"),
		DIR(GeneralName.directoryName, "Directory name"),
		DNS(GeneralName.dNSName, "DNS"),
		EDI(GeneralName.ediPartyName, "EDI party"),
		IP(GeneralName.iPAddress, "IP address"),
		OTHER(GeneralName.otherName, "Other"),
		REG_ID(GeneralName.registeredID, "Registered ID"),
		URI(GeneralName.uniformResourceIdentifier, "URI"),
		X400(GeneralName.x400Address, "X.400");
		
		private int code;
		private String title;
		
		SubjectAltNameTag(int code, String title){
			this.code = code;
			this.title = title;
		}
		
		public String title(){
			return this.title;
		}
		
		public int code(){
			return this.code;
		}
		
		public static SubjectAltNameTag byCode(int code){
			for(SubjectAltNameTag tag : values()){
				if(tag.code() == code) return tag;
			}
			throw new RuntimeException("Cannot find subject alternative name tag by code " + code);
		}
	}
	
	public static class SubjectAltName {
		public static final int RFC822NAME = GeneralName.rfc822Name;
		
		private GeneralName gn;
		
		private SubjectAltName(GeneralName gn){
			this.gn = gn;
		}
		
		public SubjectAltName(SubjectAltNameTag tag, String name){
			this.gn = new GeneralName(tag.code(), name);
		}
		
		public SubjectAltNameTag tag(){
			int tag = this.gn.getTagNo();
			return SubjectAltNameTag.byCode(tag);
		}
		
		public String name(){
			return this.gn.getName().toString();
		}
		
		public static SubjectAltName[] parse(ASN1Encodable asn1){
			try {
				DLSequence seq = (DLSequence) asn1;
				int size = seq.size();
				SubjectAltName[] ret = new SubjectAltName[size];
				for(int i = 0; i < size; i++){
					GeneralName gn = GeneralName.getInstance(seq.getObjectAt(i));
					SubjectAltName san = new SubjectAltName(gn);
					ret[i] = san;
				}
				return ret;
			} catch (Exception e){
				Log.i(LTAG, "Error while parsing subject alternative names", e);
				return null;
			}
		}
		
		public static ASN1Encodable format(SubjectAltName[] names){
			ASN1Encodable[] items = new ASN1Encodable[names.length];
			for(int i = 0; i < names.length; i++) items[i] = names[i].gn;
			return new DLSequence(items);
		}
	}
	
	public interface Listener {
		public void basicConstraints(boolean critical, boolean ca, int pathlen);
		public void keyUsage(boolean critical, BasicKeyUsage[] values);
		public void extKeyUsage(boolean critical, ExtKeyUsage[] values);
		public void subjectAltNames(boolean critical, SubjectAltName[] names);
		public void unknown(boolean critical, String oid);
	}
	
	private List<Extension> exts;
	
	public Exts(){
		this.exts = new ArrayList<Extension>();
	}
	
	public void addBasicConstraints(boolean critical, boolean ca, int pathlen){
		this.add(Extension.basicConstraints, critical, ca ? new BasicConstraints(pathlen) : new BasicConstraints(false));
	}
	
	public void addKeyUsage(boolean critical, BasicKeyUsage ... values){
		int flags = 0;
		for(BasicKeyUsage value : values) flags |= value.flag();
		this.add(Extension.keyUsage, critical, new KeyUsage(flags));
	}
	
	public void addExtKeyUsage(boolean critical, ExtKeyUsage ... values){
		Vector<ASN1ObjectIdentifier> v = new Vector<ASN1ObjectIdentifier>();
		for(ExtKeyUsage value : values) v.add(value.oid());
		ExtendedKeyUsage eku = new ExtendedKeyUsage(v);
		this.add(Extension.extendedKeyUsage, critical, eku);
	}
	
	public void addSubjectAltNames(boolean critical, SubjectAltName[] names){
		this.add(Extension.subjectAlternativeName, critical, SubjectAltName.format(names));
	}
	
	private void add(ASN1ObjectIdentifier oid, boolean critical, ASN1Encodable value){
		try {
			this.exts.add(new Extension(oid, critical, new DEROctetString(value)));
		} catch (Exception e){
			throw new GutsException("Error while creating extension", e);
		}
	}
	
	public void addTo(PKCS10CertificationRequestBuilder b){
		Extensions e = new Extensions(this.exts.toArray(new Extension[this.exts.size()]));
		b.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, e);
	}
	
	public void addTo(X509v3CertificateBuilder cb){
		try {
			for(Extension ext : this.exts)
				cb.addExtension(ext.getExtnId(), ext.isCritical(), ext.getParsedValue());
		} catch (Exception e){
			throw new GutsException("Adding extension to certificate failed", e);
		}
	}
	
	public void getFrom(PKCS10CertificationRequest csr){
		for(Attribute att : csr.getAttributes()){
			ASN1ObjectIdentifier oid = att.getAttrType();
			if(!PKCSObjectIdentifiers.pkcs_9_at_extensionRequest.equals(oid)) continue;
			for(ASN1Encodable enc : att.getAttributeValues()){
				Extensions ext = Extensions.getInstance(enc);
				Enumeration en = ext.oids();
				while(en.hasMoreElements()){
					ASN1ObjectIdentifier oid2 = (ASN1ObjectIdentifier) en.nextElement();
					this.exts.add(ext.getExtension(oid2));
				}
			}
		}
	}
	
	public void getFrom(X509Certificate cert){
		if(cert == null) return;
		if(!(cert instanceof X509Extension)) return;
		X509Extension xe = (X509Extension) cert;
		Set<String> coids = xe.getCriticalExtensionOIDs();
		if(coids != null){
			for(String oid : coids)
				this.add(oid, true, xe.getExtensionValue(oid));
		}
		Set<String> ncoids = xe.getNonCriticalExtensionOIDs();
		if(ncoids != null){
			for(String oid : ncoids)
				this.add(oid, false, xe.getExtensionValue(oid));
		}
	}
	
	private void add(String oid, boolean critical, byte[] value){
		try {
			ASN1Primitive prim = ASN1Primitive.fromByteArray(value);
			byte[] octets = ((DEROctetString) prim).getOctets();
			ASN1Encodable enc = ASN1Primitive.fromByteArray(octets);
			this.add(new ASN1ObjectIdentifier(oid), critical, enc);
		} catch (Exception e){
			throw new GutsException("Adding extension " + oid + " failed", e);
		}
	}
	
	public int size(){
		return this.exts.size();
	}
	
	public void traverse(Listener l){
		for(Extension ext : this.exts){
			ASN1ObjectIdentifier oid = ext.getExtnId();
			boolean critical = ext.isCritical();
			if(Extension.basicConstraints.equals(oid)){
				BasicConstraints bc = BasicConstraints.getInstance(ext.getParsedValue());
				int pathlen = bc.getPathLenConstraint() != null ? bc.getPathLenConstraint().intValue() : 0;
				l.basicConstraints(critical, bc.isCA(), bc.isCA() ? pathlen : 0);
			} else if(Extension.keyUsage.equals(oid)){
				int flags = ((DERBitString) KeyUsage.getInstance(ext.getParsedValue())).intValue();
				List<BasicKeyUsage> list = new ArrayList<BasicKeyUsage>();
				for(BasicKeyUsage bku : BasicKeyUsage.values()){
					if((bku.flag() & flags) != 0) list.add(bku);
				}
				l.keyUsage(critical, list.toArray(new BasicKeyUsage[list.size()]));
			} else if(Extension.extendedKeyUsage.equals(oid)){
				ExtendedKeyUsage eku = ExtendedKeyUsage.getInstance(ext.getParsedValue());
				List<ExtKeyUsage> list = new ArrayList<ExtKeyUsage>();
				for(Object _oid : eku.getUsages()){
					for(ExtKeyUsage _eku : ExtKeyUsage.values()){
						if(_eku.oid().equals(_oid)) list.add(_eku);
					}
				}
				l.extKeyUsage(critical, list.toArray(new ExtKeyUsage[list.size()]));
			} else if(Extension.subjectAlternativeName.equals(oid)){
				SubjectAltName[] names = SubjectAltName.parse(ext.getParsedValue());
				l.subjectAltNames(critical, names);
			} else {
				l.unknown(critical, oid.toString());
			}
		}
	}
	
	public boolean isCa(){
		final boolean[] ret = new boolean[]{ false };
		this.traverse(new Listener(){
			public void basicConstraints(boolean critical, boolean ca, int pathlen){
				ret[0] = ca;
			}
			public void keyUsage(boolean critical, BasicKeyUsage[] values){}
			public void extKeyUsage(boolean critical, ExtKeyUsage[] values){}
			public void subjectAltNames(boolean critical, SubjectAltName[] names){}
			public void unknown(boolean critical, String oid){}
		});
		return ret[0];
	}
}

