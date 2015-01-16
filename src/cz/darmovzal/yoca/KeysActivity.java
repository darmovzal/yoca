package cz.darmovzal.yoca;

import java.io.*;
import java.security.*;
import java.security.interfaces.*;
import android.util.Log;
import cz.darmovzal.yoca.guts.*;
import cz.darmovzal.yoca.ui.*;

public class KeysActivity extends CommonActivity {
	private static final String LTAG = "YOCA:KeysActivity";
	
	private int slotid;
	
	public void processParams(){
		this.slotid = this.getIntent().getIntExtra("slot", -1);
	}
	
	protected String getTitleString(){
		Slot slot = this.storage().getSlotById(this.slotid);
		return r(R.string.main) + " > " + r(slot.isCa() ? R.string.authorities : R.string.records) + " > " + r(R.string.record) + " > " + r(R.string.keys);
	}
	
	public void createUi(final UIBuilder b){
		Slot slot = this.storage().getSlotById(this.slotid);
		Keys keys = slot.keys();
		
		b.title(R.string.key_type).text(keys.type().toString());
		b.title(R.string.key_size).text(String.valueOf(keys.size()));
		b.title(R.string.private_key).text(keys.hasPrivate() ? R.string.present : R.string.missing, false);
		
		b.title(R.string.create_or_verify_file_signatures).icon(R.drawable.icon_sign).groupBegin("signatures")
			.id("signature_file").title(R.string.select_file_to_sign_or_verify).file(this.getImportPath(), new UIBuilder.ClickListener(){
				public void click(){
					showFileDialog("signature_file", R.string.select_file_to_sign_or_verify, b.getFileValue("signature_file"), true, false, null);
				}
			})
			.id("signature_data").title(R.string.signature).edit("")
			.id("signature_type").title(R.string.signature_type).select(true, Sig.titles())
			.button(R.string.verify_file, new UIBuilder.ClickListener(){
				public void click(){
					verifySignature();
				}
			});
		if(keys.hasPrivate()){
			b.id("signature_pass").title(R.string.private_key_passphrase).password()
				.button(R.string.sign_file, new UIBuilder.ClickListener(){
					public void click(){
						createSignature();
					}
				});
		}
		this.note(R.string.note_signature);
		b.groupEnd();
		
		b.title(R.string.export_public_key).icon(R.drawable.icon_export_public).groupBegin("export_public_key")
			.button(R.string.export_public_key_to_clipboard, new UIBuilder.ClickListener(){
				public void click(){
					exportPublicKeyToClipboard();
				}
			})
			.id("export_public_file").title(R.string.select_export_path).file(this.getExportPath(), new UIBuilder.ClickListener(){
				public void click(){
					showFileDialog("export_public_file", R.string.select_export_path, b.getFileValue("export_public_file"), false, false, null);
				}
			})
			.button(R.string.export_public_key_to_file, new UIBuilder.ClickListener(){
				public void click(){
					exportPublicKeyToFile();
				}
			})
			.groupEnd();
		
		if(keys.hasPrivate()){
			b.title(R.string.export_private_key).icon(R.drawable.icon_export_private).groupBegin("export_private_key")
				.id("export_private_pass").title(R.string.private_key_passphrase).password()
				.id("export_private_export_pass").title(R.string.export_passphrase_recommended).password()
				.button(R.string.export_private_key_to_clipboard, new UIBuilder.ClickListener(){
					public void click(){
						exportPrivateKeyToClipboard();
					}
				})
				.id("export_private_file").title(R.string.select_export_path).file(this.getExportPath(), new UIBuilder.ClickListener(){
					public void click(){
						showFileDialog("export_private_file", R.string.select_export_path, b.getFileValue("export_private_file"), false, false, null);
					}
				})
				.button(R.string.export_private_key_to_file, new UIBuilder.ClickListener(){
					public void click(){
						exportPrivateKeyToFile();
					}
				})
				.groupEnd();
		}
		
		b.title(R.string.show_public_key).icon(R.drawable.icon_public).groupBegin("public_key");
		PublicKey pub = keys.pub().key();
		if(pub instanceof RSAPublicKey){
			RSAPublicKey rsa = (RSAPublicKey) pub;
			b.title(R.string.modulus).text(this.toHex(rsa.getModulus(), 8));
			b.title(R.string.public_exponent).text(this.toHex(rsa.getPublicExponent(), 8));
		} else if(pub instanceof DSAPublicKey){
			DSAPublicKey dsa = (DSAPublicKey) pub;
			b.title("Y").text(this.toHex(dsa.getY(), 8));
			b.title("G").text(this.toHex(dsa.getParams().getG(), 8));
			b.title("P").text(this.toHex(dsa.getParams().getP(), 8));
			b.title("Q").text(this.toHex(dsa.getParams().getQ(), 8));
		}
		b.groupEnd();
		
		if(keys.hasPrivate()){
			b.title(R.string.show_private_key).icon(R.drawable.icon_private).groupBegin("private_key")
				.id("key_passphrase").title(R.string.key_passphrase).password()
				.button(R.string.decipher_private_key, new UIBuilder.ClickListener(){
					public void click(){
						decipherKey();
					}
				});
			if(pub instanceof RSAPublicKey){
				b.id("rsa_exponent").title(R.string.private_exponent).text(R.string.encrypted, false);
				b.id("rsa_prime_exponent_p").title(R.string.prime_exponent_p).text(R.string.encrypted, false);
				b.id("rsa_prime_exponent_q").title(R.string.prime_exponent_q).text(R.string.encrypted, false);
				b.id("rsa_prime_p").title(R.string.prime_p).text(R.string.encrypted, false);
				b.id("rsa_prime_q").title(R.string.prime_q).text(R.string.encrypted, false);
				b.id("rsa_crt_coef").title(R.string.crt_coefficient).text(R.string.encrypted, false);
			} else if(pub instanceof DSAPublicKey){
				b.id("dsa_x").title("X").text(R.string.encrypted, false);
			}
			b.groupEnd();
		}
		
		if(keys.hasPrivate()){
			b.title(R.string.change_private_key_passphrase).icon(R.drawable.icon_change_pass).groupBegin("change_passphrase")
				.id("original_pass").title(R.string.original_passphrase).password()
				.id("new_pass").title(R.string.new_passphrase).password()
				.button(R.string.change_private_key_passphrase, new UIBuilder.ClickListener(){
					public void click(){
						changePrivateKeyPassphrase();
					}
				})
				.groupEnd();
		}
	}
	
	private void decipherKey(){
		Slot slot = this.storage().getSlotById(this.slotid);
		char[] pass = this.builder.getPasswordValue("key_passphrase");
		PrivateKey priv;
		try {
			priv = slot.keys().priv().key(pass);
		} catch (Exception e){
			this.showErrorDialog(R.string.wrong_passphrase);
			return;
		}
		if(priv instanceof RSAPrivateKey){
			this.builder.setTextValue("rsa_exponent", this.toHex(((RSAPrivateKey) priv).getPrivateExponent(), 8));
			if(priv instanceof RSAPrivateCrtKey){
				this.builder.setTextValue("rsa_prime_p", this.toHex(((RSAPrivateCrtKey) priv).getPrimeP(), 8));
				this.builder.setTextValue("rsa_prime_q", this.toHex(((RSAPrivateCrtKey) priv).getPrimeQ(), 8));
				this.builder.setTextValue("rsa_prime_exponent_p", this.toHex(((RSAPrivateCrtKey) priv).getPrimeExponentP(), 8));
				this.builder.setTextValue("rsa_prime_exponent_q", this.toHex(((RSAPrivateCrtKey) priv).getPrimeExponentQ(), 8));
				this.builder.setTextValue("rsa_crt_coef", this.toHex(((RSAPrivateCrtKey) priv).getCrtCoefficient(), 8));
			}
		} else if(priv instanceof DSAPrivateKey){
			this.builder.setTextValue("dsa_x", this.toHex(((DSAPrivateKey) priv).getX(), 8));
		}
		this.builder.clearPassword("key_passphrase");
	}
	
	private void exportPublicKeyToFile(){
		Slot slot = this.storage().getSlotById(this.slotid);
		String path = builder.getFileValue("export_public_file");
		if(!this.checkExportPath(path)) return;
		try {
			FileOutputStream fos = new FileOutputStream(path);
			try {
				slot.keys().pub().save(fos, null);
			} finally {
				fos.close();
			}
		} catch (Exception e){
			Log.e(LTAG, "Failed to export public key to path \"" + path + "\"", e);
			this.showErrorDialog(R.string.public_key_export_failed);
			return;
		}
		this.showInfoDialog(R.string.public_key_exported);
	}
	
	private void exportPublicKeyToClipboard(){
		Keys.Public pub = this.storage().getSlotById(this.slotid).keys().pub();
		try {
			this.setClipboardText(pub.getPemString(null));
		} catch (Exception e){
			Log.e(LTAG, "Failed to export public key to clipboard", e);
			this.showErrorDialog(R.string.public_key_export_failed);
			return;
		}
		this.showInfoDialog(R.string.public_key_exported);
	}
	
	private void exportPrivateKeyToFile(){
		Slot slot = this.storage().getSlotById(this.slotid);
		String path = builder.getFileValue("export_private_file");
		if(!this.checkExportPath(path)) return;
		char[] pass = builder.getPasswordValue("export_private_pass");
		char[] exppass = builder.getPasswordValue("export_private_export_pass");
		if(exppass.length == 0) exppass = null;
		//if(!this.checkPassphrase(exppass)) return;
		try {
			slot.keys().priv().load(pass);
		} catch (Exception e){
			this.showErrorDialog(R.string.wrong_passphrase);
			return;
		}
		try {
			FileOutputStream fos = new FileOutputStream(path);
			try {
				slot.keys().priv().save(fos, exppass);
				fos.flush();
			} finally {
				fos.close();
			}
		} catch (Exception e){
			Log.e(LTAG, "Failed to export private key to path \"" + path + "\"", e);
			this.showErrorDialog(R.string.private_key_export_failed);
			return;
		} finally {
			slot.keys().priv().clear();
		}
		this.showInfoDialog(exppass == null ? R.string.private_key_exported_unprotected : R.string.private_key_exported);
		this.builder.clearPassword("export_private_pass");
		this.builder.clearPassword("export_private_export_pass");
	}
	
	private void exportPrivateKeyToClipboard(){
		Keys.Private priv = this.storage().getSlotById(this.slotid).keys().priv();
		char[] pass = builder.getPasswordValue("export_private_pass");
		char[] exppass = builder.getPasswordValue("export_private_export_pass");
		if(exppass.length == 0) exppass = null;
		//if(!this.checkPassphrase(exppass)) return;
		try {
			priv.load(pass);
		} catch (Exception e){
			this.showErrorDialog(R.string.wrong_passphrase);
			return;
		}
		try {
			this.setClipboardText(priv.getPemString(exppass));
		} catch (Exception e){
			Log.e(LTAG, "Failed to export private key to clipboard", e);
			this.showErrorDialog(R.string.private_key_export_failed);
			return;
		} finally {
			priv.clear();
		}
		this.showInfoDialog(exppass == null ? R.string.private_key_exported_unprotected : R.string.private_key_exported);
		this.builder.clearPassword("export_private_pass");
		this.builder.clearPassword("export_private_export_pass");
	}
	
	private void changePrivateKeyPassphrase(){
		Slot slot = this.storage().getSlotById(this.slotid);
		char[] orgpass = builder.getPasswordValue("original_pass");
		char[] newpass = builder.getPasswordValue("new_pass");
		if(!this.checkPassphrase(newpass)) return;
		try {
			slot.keys().priv().changePassphrase(orgpass, newpass);
		} catch (Exception e){
			this.showErrorDialog(R.string.wrong_passphrase);
			return;
		}
		this.showInfoDialog(R.string.passphrase_has_been_changed);
		this.builder.clearPassword("original_pass");
		this.builder.clearPassword("new_pass");
	}
	
	private void createSignature(){
		String path = this.builder.getFileValue("signature_file");
		if(!this.checkImportPath(path)) return;
		char[] pass = this.builder.getPasswordValue("signature_pass");
		Sig sig = Sig.get(this.builder.getSelectValue("signature_type"));
		
		Slot slot = this.storage().getSlotById(this.slotid);
		try {
			FileInputStream fis = new FileInputStream(path);
			byte[] signature;
			try {
				signature = slot.keys().priv().sign(fis, pass, sig);
			} finally {
				fis.close();
			}
			this.builder.setEditValue("signature_data", this.toHex(signature, 8, false));
			this.builder.clearPassword("signature_pass");
		} catch (Exception e){
			this.showErrorDialog(R.string.wrong_passphrase);
		}
	}
	
	private void verifySignature(){
		String path = this.builder.getFileValue("signature_file");
		if(!this.checkImportPath(path)) return;
		Sig sig = Sig.get(this.builder.getSelectValue("signature_type"));
		String ssignature = this.builder.getEditValue("signature_data");
		byte[] signature;
		try {
			signature = this.fromHex(ssignature);
		} catch (Exception e){
			this.showErrorDialog(R.string.wrong_signature_format);
			return;
		}
		
		boolean verified;
		Slot slot = this.storage().getSlotById(this.slotid);
		try {
			FileInputStream fis = new FileInputStream(path);
			try {
				verified = slot.keys().pub().verify(fis, sig, signature);
			} finally {
				fis.close();
			}
		} catch (Exception e){
			this.showErrorDialog(R.string.error_in_verification_process);
			return;
		}
		
		this.showInfoDialog(verified ? R.string.signature_successfully_verified : R.string.signature_is_not_verified);
	}
}

