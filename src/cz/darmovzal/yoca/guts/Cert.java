package cz.darmovzal.yoca.guts;

import java.io.*;
import java.math.BigInteger;
import java.util.Date;
import java.security.cert.*;
import javax.security.auth.x500.X500Principal;
import org.spongycastle.operator.ContentVerifierProvider;
import org.spongycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.cert.jcajce.JcaX509CertificateConverter;
import org.spongycastle.asn1.ASN1Primitive;
import android.util.Log;

public class Cert extends Item {
	private static final String LTAG = "YOCA:Cert";
	
	private X509Certificate cert;
	private Boolean ca;
	private Crl crl;
	
	Cert(){}
	
	Cert(X509Certificate cert){
		this.cert = cert;
	}
	
	public boolean isCa(){
		if(this.ca != null) return this.ca.booleanValue();
		Exts exts = new Exts();
		exts.getFrom(this.cert);
		this.ca = new Boolean(exts.isCa());
		return this.ca.booleanValue();
	}
	
	public Name subject(){
		return Name.fromPrincipal(this.cert.getSubjectDN());
	}
	
	public Name issuer(){
		return Name.fromPrincipal(this.cert.getIssuerDN());
	}
	
	public BigInteger serial(){
		return this.cert.getSerialNumber();
	}
	
	public Date notBefore(){
		return this.cert.getNotBefore();
	}
	
	public Date notAfter(){
		return this.cert.getNotAfter();
	}
	
	public boolean valid(){
		Date now = new Date();
		return !now.before(this.notBefore()) && !now.after(this.notAfter());
	}
	
	public String signatureType(){
		return this.cert.getSigAlgName();
	}
	
	public byte[] signature(){
		return this.cert.getSignature();
	}
	
	public void load(InputStream is, char[] pass) throws IOException {
		this.cert = (X509Certificate) this.readPem(is, pass);
	}
	
	public void save(OutputStream os, char[] pass) throws IOException {
		this.writePem(os, this.cert, pass);
	}
	
	protected String getSuffix(){
		return "crt";
	}
	
	public static Cert generate(Name subject, Keys keys, char[] pass, BigInteger serial, Date notBefore, Date notAfter, Sig sig, Exts exts){
		Csr csr = Csr.generate(subject, keys, pass, sig, exts);
		return csr.sign(subject, keys, pass, serial, notBefore, notAfter, sig);
	}
	
	public boolean verify(Cert cert){
		try {
			ContentVerifierProvider cvp = (new JcaContentVerifierProviderBuilder()).build(this.cert);
			return (new X509CertificateHolder(cert.cert.getEncoded())).isSignatureValid(cvp);
		} catch (Exception e){
			throw new GutsException("Error while verifying certificate", e);
		}
	}
	
	public Exts getExts(){
		Exts exts = new Exts();
		exts.getFrom(this.cert);
		return exts.size() == 0 ? null : exts;
	}
	
	public X509Certificate cert(){
		return this.cert;
	}
	
	private void importDerCert(byte[] der){
		try {
			org.spongycastle.asn1.x509.Certificate c = org.spongycastle.asn1.x509.Certificate.getInstance(ASN1Primitive.fromByteArray(der));
			this.cert = (new JcaX509CertificateConverter())
				.setProvider( "SC" )
				.getCertificate(new X509CertificateHolder(c));
		} catch (Exception e2){
			throw new GutsException("Failed to load DER X.509 certificate", e2);
		}
	}
	
	public void importFrom(File file){
		try {
			FileInputStream fis = new FileInputStream(file);
			try {
				this.cert = (X509Certificate) this.readPem(fis, null);
			} finally {
				fis.close();
			}
			if(this.cert == null) throw new NullPointerException("Empty certificate");
		} catch (Exception e){
			Log.w(LTAG, "Failed to load PEM X.509 certificate", e);
			try {
				this.importDerCert(this.readFileContent(file));
			} catch (Exception e2){
				Log.w(LTAG, "Failed to load DER X.509 certificate", e2);
				throw new GutsException("Unable to load PEM or DER X.509 certificate");
			}
		}
	}
	
	public Crl crl(){
		if(!this.isCa()) return null;
		if(!this.slot.keys().hasPrivate()) return null;
		if(this.crl == null){
			this.crl = new Crl(this);
			if(this.crl.exists()) this.crl.load();
		}
		return this.crl;
	}
	
	@Override
	public int hashCode(){
		return this.cert.hashCode();
	}
	
	@Override
	public boolean equals(Object o){
		if(o == null) return false;
		if(!(o instanceof Cert)) return false;
		Cert c = (Cert) o;
		return ((Cert) o).cert.equals(this.cert);
	}
	
	@Override
	public String toString(){
		return String.valueOf(this.cert);
	}
}

