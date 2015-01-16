package cz.darmovzal.yoca;

import java.io.*;
import java.util.*;
import android.util.Log;
import cz.darmovzal.yoca.guts.*;
import cz.darmovzal.yoca.ui.*;

public class ImportActivity extends CommonActivity {
	private static final String LTAG = "YOCA:ImportActivity";
	
	protected String getTitleString(){
		return r(R.string.main) + " > " + r(R.string.imports);
	}
	
	public void createUi(final UIBuilder b){
		b.title(R.string.import_record_from_pkcs12_file).icon(R.drawable.icon_import).groupBegin("import_pkcs12")
			.id("import_pkcs12_file").title(R.string.select_pkcs12_file).file(this.getImportPath(), new UIBuilder.ClickListener(){
				public void click(){
					showFileDialog("import_pkcs12_file", R.string.select_pkcs12_file, b.getFileValue("import_pkcs12_file"), true, false, new String[]{ "p12", "pfx" });
				}
			})
			.id("import_pkcs12_pass").title(R.string.pkcs12_passphrase).password()
			.button(R.string.import_record_from_pkcs12_file, new UIBuilder.ClickListener(){
				public void click(){
					importPkcs12();
				}
			})
			.groupEnd();
		b.title(R.string.import_x509_certificate).icon(R.drawable.icon_import).groupBegin("import_certificate")
			.id("import_certificate").title(R.string.select_certificate_file).file(this.getImportPath(), new UIBuilder.ClickListener(){
				public void click(){
					showFileDialog("import_certificate", R.string.select_certificate_file, b.getFileValue("import_certificate"), true, false, new String[]{ "cer", "crt", "pem" });
				}
			})
			.button(R.string.import_x509_certificate, new UIBuilder.ClickListener(){
				public void click(){
					importCertificate();
				}
			})
			.groupEnd();
		b.title(R.string.import_key).icon(R.drawable.icon_import).groupBegin("import_keys")
			.id("import_keys").title(R.string.select_key_file).file(this.getImportPath(), new UIBuilder.ClickListener(){
				public void click(){
					showFileDialog("import_keys", R.string.select_key_file, b.getFileValue("import_keys"), true, false, new String[]{ "key", "pem" });
				}
			})
			.id("import_keys_pass").title(R.string.key_passphrase).password()
			.button(R.string.import_key, new UIBuilder.ClickListener(){
				public void click(){
					importKeys();
				}
			})
			.groupEnd();
		b.title(R.string.import_x509_signing_request).icon(R.drawable.icon_import).groupBegin("import_csr")
			.id("import_csr").title(R.string.select_signing_request_file).file(this.getImportPath(), new UIBuilder.ClickListener(){
				public void click(){
					showFileDialog("import_csr", R.string.select_signing_request_file, b.getFileValue("import_csr"), true, false, new String[]{ "csr", "pem" });
				}
			})
			.button(R.string.import_x509_signing_request, new UIBuilder.ClickListener(){
				public void click(){
					importCsr();
				}
			})
			.groupEnd();
		b.title(R.string.import_pem_object_from_clipboard).icon(R.drawable.icon_import).groupBegin("import_from_clipboard")
			.id("import_from_clipboard_pass").title(R.string.pem_passphrase_optional).password()
			.button(R.string.import_pem_object_from_clipboard, new UIBuilder.ClickListener(){
				public void click(){
					importFromClipboard();
				}
			})
			.groupEnd();
		this.note(R.string.note_import);
	}
	
	public void processParams(){}
	
	private void importPkcs12(){
		String path = this.builder.getFileValue("import_pkcs12_file");
		char[] pass = this.builder.getPasswordValue("import_pkcs12_pass");
		if(!this.checkImportPath(path)) return;
		int count;
		try {
			count = this.storage().importPkcs12(new File(path), pass);
		} catch (PassphraseException e){
			Log.e(LTAG, "Error importing PKCS#12 file - wrong passphrase", e);
			this.showErrorDialog(R.string.wrong_passphrase);
			return;
		} catch (Exception e){
			Log.e(LTAG, "Error importing PKCS#12 file", e);
			this.showErrorDialog(R.string.import_from_pkcs12_file_failed);
			return;
		}
		this.showInfoDialog(this.r(R.string.x_records_imported_or_affected, count));
	}
	
	private String getDefaultImportTitle(File file){
		return r(R.string.imported_from_x_on_y, file.getName(), new Date());
	}
	
	private void importCertificate(){
		String path = this.builder.getFileValue("import_certificate");
		if(!this.checkImportPath(path)) return;
		File file = new File(path);
		Slot slot;
		try {
			slot = this.storage().importCertificate(file, this.getDefaultImportTitle(file));
		} catch (Exception e){
			Log.e(LTAG, "Error importing certificate", e);
			this.showErrorDialog(R.string.certificate_import_failed);
			return;
		}
		this.start(RecordActivity.class, "slot", slot.id());
		this.showToast(r(R.string.object_successfully_imported));
	}
	
	private void importKeys(){
		String path = this.builder.getFileValue("import_keys");
		if(!this.checkImportPath(path)) return;
		File file = new File(path);
		char[] pass = this.builder.getPasswordValue("import_keys_pass");
		Slot slot;
		try {
			slot = this.storage().importKeys(file, pass, this.getDefaultImportTitle(file));
		} catch (PassphraseException e){
			Log.e(LTAG, "Error importing keys - wrong passphrase", e);
			this.showErrorDialog(R.string.wrong_passphrase);
			return;
		} catch (Exception e){
			Log.e(LTAG, "Error importing keys", e);
			this.showErrorDialog(R.string.key_import_failed);
			return;
		}
		this.start(RecordActivity.class, "slot", slot.id());
		this.showToast(r(R.string.object_successfully_imported));
	}
	
	private void importCsr(){
		String path = this.builder.getFileValue("import_csr");
		if(!this.checkImportPath(path)) return;
		File file = new File(path);
		Slot slot;
		try {
			slot = this.storage().importCsr(file, this.getDefaultImportTitle(file));
		} catch (Exception e){
			Log.e(LTAG, "Error importing CSR", e);
			this.showErrorDialog(R.string.signing_request_import_failed);
			return;
		}
		this.start(RecordActivity.class, "slot", slot.id());
		this.showToast(r(R.string.object_successfully_imported));
	}
	
	private void importFromClipboard(){
		String pem = this.getClipboardText();
		if(pem == null){
			this.showErrorDialog(R.string.clipboard_is_empty);
			return;
		}
		char[] pass = this.builder.getPasswordValue("import_from_clipboard_pass");
		String name = r(R.string.imported_from_clipboard_on_x, new Date());
		Slot slot;
		try {
			slot = this.storage().importFromPemString(pem, pass, name);
		} catch (PassphraseException e){
			Log.e(LTAG, "Error importing from clipboard - wrong passphrase", e);
			this.showErrorDialog(R.string.wrong_passphrase);
			return;
		} catch (Exception e){
			Log.e(LTAG, "Error importing from clipboard", e);
			this.showErrorDialog(R.string.import_from_clipboard_failed);
			return;
		}
		this.start(RecordActivity.class, "slot", slot.id());
		this.showToast(r(R.string.object_successfully_imported));
	}
}

