package cz.darmovzal.yoca;

import java.io.*;
import java.util.*;
import android.os.Bundle;
import android.content.Intent;
import android.util.Log;
import cz.darmovzal.yoca.guts.*;
import cz.darmovzal.yoca.ui.*;

public class RecordActivity extends CommonActivity {
	private static final String LTAG = "YOCA:RecordActivity";
	
	private int id;
	
	public void processParams(){
		Intent i = this.getIntent();
		this.id = i.getIntExtra("slot", -1);
		if(this.id == -1) throw new RuntimeException("Cannot get slot ID from intent");
	}
	
	protected String getTitleString(){
		Slot slot = this.storage().getSlotById(this.id);
		return r(R.string.main) + " > " + r(slot.isCa() ? R.string.authorities : R.string.records) + " > " + r(R.string.record);
	}
	
	public void createUi(final UIBuilder b){
		final Slot slot = this.storage().getSlotById(this.id);
		
		b.id("name").title(R.string.name).edit(slot.getName())
			.button(R.string.change_name, new UIBuilder.ClickListener(){
				public void click(){
					changeName();
				}
			});
		
		b.title(slot.keys().hasPrivate() ? R.string.public_and_private_keys : R.string.public_key)
			.icon(R.drawable.icon_keys).link(new UIBuilder.ClickListener(){
				public void click(){
					start(KeysActivity.class, "slot", slot.id());
				}
			});
		
		b.title(this.r(R.string.x_certificates, slot.certs().size())).icon(R.drawable.icon_certs).groupBegin("certificates");
		for(final Cert cert : slot.certs()){
			boolean revoked = storage().isRevoked(cert);
			boolean valid = cert.valid();
			b.icon(cert.crl() != null ? R.drawable.cert_crl : R.drawable.cert)
				.highlight(!valid || revoked)
				.title(toCertString(cert) + " - " + r(revoked ? R.string.revoked_2 : (valid ? R.string.valid : R.string.not_valid)))
				.link(new UIBuilder.ClickListener(){
					public void click(){
						start(CertActivity.class, "slot", slot.id(), "cert", cert.getId());
					}
			});
		}
		b.groupEnd(slot.certs().size() < 5);
		
		b.title(this.r(R.string.x_certificate_requests, slot.csrs().size())).icon(R.drawable.icon_csrs).groupBegin("certificate_requests");
		for(final Csr csr : slot.csrs()){
			Name subject = csr.subject();
			b.icon(R.drawable.csr).title(subject.toShortString()).link(new UIBuilder.ClickListener(){
				public void click(){
					start(CsrActivity.class, "slot", slot.id(), "csr", csr.getId());
				}
			});
		}
		b.groupEnd(slot.csrs().size() < 5);
		
		if(slot.keys().hasPrivate()){
			b.title(R.string.issue_certificate_request).icon(R.drawable.icon_issue_csr).groupBegin("issue_csr")
				.id("common_name").title(R.string.common_name).edit("")
				.id("organization").title(R.string.organization).edit(SettingsActivity.getPrefillOrganization(this))
				.id("organizational_unit").title(R.string.organizational_unit).edit(SettingsActivity.getPrefillOrganizationalUnit(this))
				.id("locality").title(R.string.locality).edit(SettingsActivity.getPrefillLocality(this))
				.id("state").title(R.string.state).edit(SettingsActivity.getPrefillState(this))
				.id("country").title(R.string.country).edit(SettingsActivity.getPrefillCountry(this))
				.id("signature_type").title(R.string.signature_digest_type).select(true, Sig.titles())
				.id("csr_passphrase").title(R.string.enter_private_key_passphrase).password();
			this.createExtensionEditUi(b);
			b.button(R.string.issue_certificate_request, new UIBuilder.ClickListener(){
					public void click(){
						issueCsr();
					}
				});
			this.note(R.string.note_new_csr);
			b.groupEnd();
		}
		
		if((slot.certs().size() > 0) && slot.keys().hasPrivate()){
			List<String> certtitles = new ArrayList<String>();
			for(Cert cert : slot.certs()) certtitles.add(this.toCertString(cert));
			b.title(R.string.export_as_pkcs12).icon(R.drawable.icon_export).groupBegin("export")
				.id("export_file").title(R.string.select_export_path).file(this.getExportPath(), new UIBuilder.ClickListener(){
					public void click(){
						showFileDialog("export_file", R.string.select_export_path, b.getFileValue("export_file"), false, false, null);
					}
				})
				.id("export_cert").title(R.string.select_certificate_to_export).select(true, certtitles)
				.id("export_key_pass").title(R.string.key_passphrase).password()
				.id("export_exp_pass").title(R.string.export_passphrase).password()
				.button(R.string.export_as_pkcs12, new UIBuilder.ClickListener(){
					public void click(){
						exportPkcs12();
					}
				});
			this.note(R.string.note_export_pkcs12);
			b.groupEnd();
		}
		
		b.title(R.string.removal).icon(R.drawable.icon_remove).groupBegin("removal")
			.button(R.string.remove_record, new UIBuilder.ClickListener(){
				public void click(){
					showConfirmDialog("remove_1", r(R.string.do_you_really_want_to_remove_whole_record));
				}
			})
			.groupEnd();
		
		this.note(R.string.note_record);
	}
	
	@Override
	public void onConfirm(String id, boolean ok){
		if("remove_1".equals(id) && ok){
			showConfirmDialog("remove_2", r(R.string.are_you_100_percent_positive));
		} else if("remove_2".equals(id) && ok){
			Slot slot = this.storage().getSlotById(this.id);
			this.storage().removeSlot(slot);
			slot.remove();
			this.finish();
		}
	}
	
	private void changeName(){
		String name = this.builder.getEditValue("name");
		if(!checkName(name)) return;
		
		Slot slot = this.storage().getSlotById(this.id);
		if(name.equals(slot.getName())) return;
		slot.setName(name);
		slot.save();
		
		this.showInfoDialog(R.string.record_name_has_been_changed);
	}
	
	private void issueCsr(){
		Slot slot = this.storage().getSlotById(this.id);
		
		String cn = builder.getEditValue("common_name");
		if(!checkCommonName(cn)) return;
		Name subject = Name.create(
			"C", builder.getEditValue("country"),
			"ST", builder.getEditValue("state"),
			"L", builder.getEditValue("locality"),
			"O", Storage.trialOrganization(builder.getEditValue("organization")),
			"OU", builder.getEditValue("organizational_unit"),
			"CN", cn
		);
		Sig sig = Sig.get(this.builder.getSelectValue("signature_type"));
		char[] pass = this.builder.getPasswordValue("csr_passphrase");
		//if(!this.checkPassphrase(pass)) return;
		
		Exts exts = new Exts();
		exts.addBasicConstraints(slot.isCa(), slot.isCa(), 0);
		try {
			this.processExtensionEditUi(exts);
		} catch (Exception e){
			Log.e(LTAG, "Processing CSR extensions failed", e);
			this.showErrorDialog(R.string.wrong_extension_value_format);
			return;
		}
		
		Csr csr;
		try {
			csr = Csr.generate(subject, slot.keys(), pass, sig, exts);
		} catch (Exception e){
			this.showErrorDialog(R.string.wrong_passphrase);
			return;
		}
		slot.add(csr);
		csr.save();
		slot.save();
		
		this.start(CsrActivity.class, "slot", slot.id(), "csr", csr.getId());
	}
	
	private void exportPkcs12(){
		Slot slot = this.storage().getSlotById(this.id);
		
		String path = builder.getFileValue("export_file");
		if(!this.checkExportPath(path)) return;
		int index = this.builder.getSelectValue("export_cert");
		Cert cert = slot.certs().get(index);
		char[] keypass = this.builder.getPasswordValue("export_key_pass");
		char[] exppass = this.builder.getPasswordValue("export_exp_pass");
		if(!this.checkPassphrase(exppass)) return;
		
		try {
			FileOutputStream fos = new FileOutputStream(path);
			try {
				slot.exportPkcs12(fos, cert.getId(), keypass, exppass);
			} finally {
				fos.close();
			}
		} catch (PassphraseException e){
			this.showErrorDialog(R.string.wrong_passphrase);
			return;
		} catch (Exception e){
			Log.e(LTAG, "Failed to export PKCS#12 bundle to path \"" + path + "\"", e);
			this.showErrorDialog(R.string.pkcs12_export_failed);
			return;
		}
		
		this.builder.clearPassword("export_key_pass");
		this.builder.clearPassword("export_exp_pass");
		this.showInfoDialog(R.string.pkcs12_exported);
	}
}

