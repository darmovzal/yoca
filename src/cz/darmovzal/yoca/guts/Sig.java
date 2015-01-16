package cz.darmovzal.yoca.guts;

import java.util.*;

public enum Sig {
	SHA1("SHA-1", "SHA1"),
	SHA256("SHA-256", "SHA256");
	
	static {
		algNames = new HashMap<String, String>();
		initAlgNames();
	}
	
	private static Map<String, String> algNames;
	
	private String title;
	private String code;
	
	Sig(String title, String code){
		this.title = title;
		this.code = code;
	}
	
	public String title(){
		return this.title;
	}
	
	public String code(){
		return this.code;
	}
	
	public static String[] titles(){
		int count = values().length;
		String[] ret = new String[count];
		for(int i = 0; i < count; i++)
			ret[i] = values()[i].title();
		return ret;
	}
	
	public static Sig get(int index){
		return values()[index];
	}
	
	public static String getAlgName(String oid){
		return algNames.containsKey(oid) ? algNames.get(oid) : oid;
	}
	
	private static void initAlgNames(){
		algNames.put(org.spongycastle.asn1.x9.X9ObjectIdentifiers.id_dsa_with_sha1.getId(), "SHA1withDSA");
		algNames.put(org.spongycastle.asn1.nist.NISTObjectIdentifiers.dsa_with_sha224.getId(), "SHA224withDSA");
		algNames.put(org.spongycastle.asn1.nist.NISTObjectIdentifiers.dsa_with_sha256.getId(), "SHA256withDSA");
		algNames.put(org.spongycastle.asn1.nist.NISTObjectIdentifiers.dsa_with_sha384.getId(), "SHA384withDSA");
		algNames.put(org.spongycastle.asn1.nist.NISTObjectIdentifiers.dsa_with_sha512.getId(), "SHA512withDSA");
		algNames.put(org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers.sha1WithRSAEncryption.getId(), "SHA1withRSA");
		algNames.put(org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers.sha224WithRSAEncryption.getId(), "SHA224withRSA");
		algNames.put(org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers.sha256WithRSAEncryption.getId(), "SHA256withRSA");
		algNames.put(org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers.sha384WithRSAEncryption.getId(), "SHA384withRSA");
		algNames.put(org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers.sha512WithRSAEncryption.getId(), "SHA512withRSA");
	}
}

