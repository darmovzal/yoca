package cz.darmovzal.yoca.guts;

import java.util.*;
import java.security.*;
import javax.security.auth.x500.X500Principal;
import org.spongycastle.asn1.*;
import org.spongycastle.asn1.x509.X509ObjectIdentifiers;
import org.spongycastle.asn1.x500.RDN;
import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.jce.X509Principal;

public class Name {
	private List<RDN> rdns;
	
	public Name(){
		this.rdns = new ArrayList<RDN>();
	}
	
	public static Name fromAsn1(byte[] asn1){
		try {
			return fromX500Name(X500Name.getInstance(ASN1Primitive.fromByteArray(asn1)));
		} catch (Exception e){
			throw new GutsException("Error while converting bytes to X500Name", e);
		}
	}
	
	public static Name fromX500Name(X500Name x500){
		Name name = new Name();
		for(RDN rdn : x500.getRDNs()) name.rdns.add(rdn);
		return name;
	}
	
	public static Name fromPrincipal(Principal p){
		if(p instanceof X500Principal){
			return Name.fromAsn1(((X500Principal) p).getEncoded());
		} else if(p instanceof X509Principal){
			return Name.fromAsn1(((X509Principal) p).getEncoded());
		} else {
			throw new GutsException("Unknown principal type: " + p.getClass().getName());
		}
	}
	
	public static Name create(String ... params){
		if(params.length % 2 != 0) throw new GutsException("Odd number of parameters");
		Name name = new Name();
		for(int i = 0; i < params.length / 2; i++){
			String oid = params[i * 2];
			String value = params[i * 2 + 1];
			if(value == null) continue;
			value = value.trim();
			if(value.length() == 0) continue;
			name.add(oid, value);
		}
		return name;
	}
	
	public Name add(String oid, String value){
		ASN1ObjectIdentifier _oid;
		ASN1Encodable _value;
		if("CN".equals(oid)){
			_oid = X509ObjectIdentifiers.commonName;
			_value = new DERUTF8String(value);
		} else if("OU".equals(oid)){
			_oid = X509ObjectIdentifiers.organizationalUnitName;
			_value = new DERUTF8String(value);
		} else if("O".equals(oid)){
			_oid = X509ObjectIdentifiers.organization;
			_value = new DERUTF8String(value);
		} else if("L".equals(oid)){
			_oid = X509ObjectIdentifiers.localityName;
			_value = new DERUTF8String(value);
		} else if("ST".equals(oid)){
			_oid = X509ObjectIdentifiers.stateOrProvinceName;
			_value = new DERUTF8String(value);
		} else if("C".equals(oid)){
			_oid = X509ObjectIdentifiers.countryName;
			_value = new DERPrintableString(value);
		} else {
			throw new GutsException("Unknown oid " + oid);
		}
		this.rdns.add(new RDN(_oid, _value));
		return this;
	}
	
	public X500Name get(){
		return new X500Name(this.rdns.toArray(new RDN[rdns.size()]));
	}
	
	public int count(){
		return this.rdns.size();
	}
	
	public String oid(int index){
		RDN rdn = this.rdns.get(index);
		ASN1ObjectIdentifier oid = rdn.getFirst().getType();
		if(oid.equals(X509ObjectIdentifiers.commonName)){
			return "CN";
		} else if(oid.equals(X509ObjectIdentifiers.organizationalUnitName)){
			return "OU";
		} else if(oid.equals(X509ObjectIdentifiers.organization)){
			return "O";
		} else if(oid.equals(X509ObjectIdentifiers.localityName)){
			return "L";
		} else if(oid.equals(X509ObjectIdentifiers.stateOrProvinceName)){
			return "ST";
		} else if(oid.equals(X509ObjectIdentifiers.countryName)){
			return "C";
		} else {
			return oid.toString();
		}
	}
	
	public String value(int index){
		RDN rdn = this.rdns.get(index);
		ASN1Encodable value = rdn.getFirst().getValue();
		return value.toString();
	}
	
	public String all(String oid){
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < this.count(); i++){
			if(!oid.equals(this.oid(i))) continue;
			if(sb.length()  > 0) sb.append(", ");
			sb.append(this.value(i));
		}
		return sb.toString();
	}
	
	public String toShortString(){
		String cn = this.all("CN");
		return cn.length() > 0 ? cn : this.toString();
	}
	
	@Override
	public boolean equals(Object o){
		if(o == null) return false;
		if(!(o instanceof Name)) return false;
		Name n = (Name) o;
		return n.rdns.equals(this.rdns);
	}
	
	@Override
	public String toString(){
		return this.get().toString();
	}
}

