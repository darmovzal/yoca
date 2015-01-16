package cz.darmovzal.yoca.guts;

import java.io.*;
import java.math.*;
import java.util.*;
import java.security.PublicKey;
import org.spongycastle.pkcs.PKCS10CertificationRequest;
import org.spongycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.asn1.ASN1Primitive;
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder;
import org.spongycastle.operator.ContentSigner;
import org.spongycastle.cert.X509v3CertificateBuilder;
import org.spongycastle.cert.jcajce.JcaX509CertificateConverter;
import org.spongycastle.operator.ContentVerifierProvider;
import org.spongycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import android.util.Log;

public class Csr extends Item {
	private static final String LTAG = "YOCA:Csr";
	
	private PKCS10CertificationRequest csr;
	
	Csr(){}
	
	private Csr(PKCS10CertificationRequest csr){
		this.csr = csr;
	}
	
	protected String getSuffix(){
		return "csr";
	}
	
	public Name subject(){
		return Name.fromX500Name(this.csr.getSubject());
	}
	
	public static Csr generate(Name subject, Keys keys, char[] pass, Sig sig, Exts exts){
		try {
			SubjectPublicKeyInfo spki = new SubjectPublicKeyInfo((ASN1Sequence) ASN1Primitive.fromByteArray(keys.pub().key().getEncoded()));
			ContentSigner cs = new JcaContentSignerBuilder(sig.code() + "with" + keys.type()).setProvider("SC").build(keys.priv().key(pass));
			PKCS10CertificationRequestBuilder b = new PKCS10CertificationRequestBuilder(subject.get(), spki);
			if(exts != null) exts.addTo(b);
			return new Csr(b.build(cs));
		} catch (Exception e){
			throw new GutsException("Error while generating CSR for " + subject, e);
		}
	}
	
	public Cert sign(Name issuer, Keys keys, char[] pass, BigInteger serial, Date notBefore, Date notAfter, Sig sig){
		try {
			X509v3CertificateBuilder cb = new X509v3CertificateBuilder(
				issuer.get(),
				serial,
				notBefore,
				notAfter,
				csr.getSubject(),
				csr.getSubjectPublicKeyInfo());
			//cb.addExtension(org.spongycastle.asn1.x509.X509Extension.subjectKeyIdentifier, false, csr.getSubjectPublicKeyInfo());
			Exts exts = this.getExts();
			if(exts != null) exts.addTo(cb);
			ContentSigner cs = new JcaContentSignerBuilder(sig.code() + "with" + keys.type()).setProvider("SC").build(keys.priv().key(pass));
			return new Cert((new JcaX509CertificateConverter()).setProvider("SC").getCertificate(cb.build(cs)));
		} catch (PassphraseException e){
			throw e;
		} catch (Exception e){
			throw new GutsException("Error while signing CSR", e);
		}
	}
	
	public void load(InputStream is, char[] pass) throws IOException {
		this.csr = new PKCS10CertificationRequest(((org.spongycastle.jce.PKCS10CertificationRequest) this.readPem(is, pass)).getEncoded());
	}
	
	public void save(OutputStream os, char[] pass) throws IOException {
		if(pass != null) throw new GutsException("Saving CSR with password not supported");
		// this.writePem(os, this.csr, pass);
		this.writePem2(os, this.csr.getEncoded(), "CERTIFICATE REQUEST");
	}
	
	public String signatureType(){
		return Sig.getAlgName(this.csr.getSignatureAlgorithm().getAlgorithm().toString());
	}
	
	public byte[] signature(){
		return this.csr.getSignature();
	}
	
	public Exts getExts(){
		Exts exts = new Exts();
		exts.getFrom(this.csr);
		return exts.size() == 0 ? null : exts;
	}
	
	public String toShortString(){
		return this.subject().toString();
	}
	
	@Override
	public boolean equals(Object o){
		if(o == null) return false;
		if(!(o instanceof Csr)) return false;
		Csr c = (Csr) o;
		return c.csr.equals(this.csr);
	}
	
	public PublicKey getPublicKey(){
		return Keys.convert(this.csr.getSubjectPublicKeyInfo());
	}
	
	public boolean verify(){
		try {
			ContentVerifierProvider cvp = (new JcaContentVerifierProviderBuilder()).build(this.getPublicKey());
			return this.csr.isSignatureValid(cvp);
		} catch (Exception e){
			throw new GutsException("Error while verifying certificate", e);
		}
	}
	
	public void importFrom(File file){
		try {
			FileInputStream fis = new FileInputStream(file);
			try {
				this.load(file, null);
			} finally {
				fis.close();
			}
			if(this.csr == null) throw new NullPointerException("Empty CSR");
		} catch (Exception e){
			Log.w(LTAG, "Failed to load PEM X.509 CSR", e);
			throw new GutsException("Unable to load PEM or DER X.509 certificate");
		}
	}
}

