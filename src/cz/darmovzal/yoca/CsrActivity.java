package cz.darmovzal.yoca;

import java.io.FileOutputStream;
import java.util.*;
import java.math.BigInteger;
import android.util.Log;
import cz.darmovzal.yoca.guts.*;
import cz.darmovzal.yoca.ui.*;

public class CsrActivity extends CommonActivity {
	private static final String LTAG = "YOCA:CsrActivity";
	
	private int slotid, csrid;
	
	public void processParams(){
		this.slotid = this.getIntent().getIntExtra("slot", -1);
		this.csrid = this.getIntent().getIntExtra("csr", -1);
	}
	
	protected String getTitleString(){
		Slot slot = this.storage().getSlotById(this.slotid);
		return r(R.string.main) + " > " + r(slot.isCa() ? R.string.authorities : R.string.records) + " > " + r(R.string.record) + " > " + r(R.string.csr);
	}
	
	private List<Cert> getCaCerts(){
		List<Cert> ret = new ArrayList<Cert>();
		for(Slot slot : this.storage().slots()){
			if(!slot.isCa()) continue;
			if(!slot.keys().hasPrivate()) continue;
			for(Cert cert : slot.certs()) ret.add(cert);
		}
		return ret;
	}
	
	private Slot getSlotForCert(Cert cert){
		for(Slot slot : this.storage().slots()){
			for(Cert _cert : slot.certs()){
				if(_cert.equals(cert)) return slot;
			}
		}
		throw new RuntimeException("Cannot find slot for certificate: " + cert);
	}
	
	public void createUi(final UIBuilder b){
		Slot slot = this.storage().getSlotById(this.slotid);
		final Csr csr = slot.getCsrById(this.csrid);
		Exts exts = csr.getExts();
		
		Calendar c = Calendar.getInstance();
		c.setTime(new Date());
		c.add(Calendar.DAY_OF_MONTH, SettingsActivity.getPrefillValidity(this));
		Date notAfter = c.getTime();
		
		List<String> canames = new ArrayList<String>();
		for(Cert cert : this.getCaCerts())
			canames.add(this.toCertString(cert));
		
		b.title(R.string.subject).groupBegin("subject", csr.subject().toShortString());
		this.createNameUi(b, csr.subject());
		b.groupEnd();
		
		int verres;
		try {
			verres = csr.verify() ? R.string.valid : R.string.tampered;
		} catch (Exception e){
			Log.e(LTAG, "Unable to verify CSR", e);
			verres = R.string.unable_to_verify;
		}
		b.title(r(R.string.signature) + " (" + r(verres) + ")").icon(R.drawable.icon_sign).groupBegin("signature")
			.title(R.string.signature_type).text(csr.signatureType())
			.title(R.string.signature_data).text(this.toHex(csr.signature(), 8, false))
			.groupEnd();
		
		this.createExtensionPresentationUi(b, exts);
		
		if(canames.size() > 0){
			b.title(R.string.sign_certificate_request).icon(R.drawable.icon_sign).groupBegin("sign_certificate_request")
				.id("signature_type").title(R.string.signature_digest_type).select(true, Sig.titles())
				.id("not_before").title(R.string.valid_not_before).date(new Date(), new UIBuilder.ClickListener(){
					public void click(){
						showDateDialog("not_before", R.string.valid_not_before, b.getDateValue("not_before"));
					}
				})
				.id("not_after").title(R.string.valid_not_after).date(notAfter, new UIBuilder.ClickListener(){
					public void click(){
						showDateDialog("not_after", R.string.valid_not_after, b.getDateValue("not_after"));
					}
				})
				.id("ca_cert").title(R.string.select_authority_certificate).select(true, canames)
				.id("ca_passphrase").title(R.string.authority_key_passphrase).password()
				.button(R.string.sign_certificate_request, new UIBuilder.ClickListener(){
					public void click(){
						signCsr();
					}
				})
				.groupEnd();
		}
		
		b.title(R.string.export_certificate_request).icon(R.drawable.icon_export_csr).groupBegin("export")
			.button(R.string.export_csr_to_clipboard, new UIBuilder.ClickListener(){
				public void click(){
					exportToClipboard();
				}
			})
			.id("export_file").title(R.string.select_export_path).file(this.getExportPath(), new UIBuilder.ClickListener(){
				public void click(){
					showFileDialog("export_file", R.string.select_export_path, b.getFileValue("export_file"), false, false, null);
				}
			})
			.button(R.string.export_csr_to_file, new UIBuilder.ClickListener(){
				public void click(){
					exportToFile();
				}
			})
			.groupEnd();
		
		b.title(R.string.removal).icon(R.drawable.icon_remove_csr).groupBegin("removal")
			.button(R.string.remove_certificate_request, new UIBuilder.ClickListener(){
				public void click(){
					showConfirmDialog("remove", r(R.string.do_you_really_want_to_remove_certificate_request_x, csr.toShortString()));
				}
			})
			.groupEnd();
		
		this.note(R.string.note_csr);
	}
	
	private void signCsr(){
		try {
			Slot slot = this.storage().getSlotById(this.slotid);
			Date notBefore = builder.getDateValue("not_before");
			Date notAfter = builder.getDateValue("not_after");
			if(!checkValidity(notBefore, notAfter)) return;
			notAfter = Storage.trialNotAfter(notBefore, notAfter);
			char[] caPass = builder.getPasswordValue("ca_passphrase");
			Cert caCert = this.getCaCerts().get(builder.getSelectValue("ca_cert"));
			Slot caSlot = this.getSlotForCert(caCert);
			BigInteger serial = caSlot.freshSerial();
			Sig sig = Sig.get(builder.getSelectValue("signature_type"));
			
			Csr csr = slot.getCsrById(this.csrid);
			Cert cert = csr.sign(caCert.subject(), caSlot.keys(), caPass, serial, notBefore, notAfter, sig);
			slot.add(cert);
			cert.save();
			slot.save();
			caSlot.incSerial();
			caSlot.save();
			
			start(CertActivity.class, "slot", slot.id(), "cert", cert.getId());
		} catch (PassphraseException e){
			this.showErrorDialog(R.string.wrong_passphrase);
		} catch (Exception e){
			Log.e(LTAG, "Signing CSR failed", e);
			this.showErrorDialog(R.string.signing_certificate_request_failed);
		}
	}
	
	private void exportToFile(){
		Slot slot = this.storage().getSlotById(this.slotid);
		Csr csr = slot.getCsrById(this.csrid);
		String path = builder.getFileValue("export_file");
		if(!this.checkExportPath(path)) return;
		try {
			FileOutputStream fos = new FileOutputStream(path);
			try {
				csr.save(fos, null);
			} finally {
				fos.close();
			}
		} catch (Exception e){
			Log.e(LTAG, "Failed to export CSR to path \"" + path + "\"", e);
			this.showErrorDialog(R.string.certificate_request_export_failed);
			return;
		}
		this.showInfoDialog(R.string.certificate_request_exported);
	}
	
	private void exportToClipboard(){
		Csr csr = this.storage().getSlotById(this.slotid).getCsrById(this.csrid);
		try {
			this.setClipboardText(csr.getPemString(null));
		} catch (Exception e){
			Log.e(LTAG, "Failed to export CSR to clipboard", e);
			this.showErrorDialog(R.string.certificate_request_export_failed);
			return;
		}
		this.showInfoDialog(R.string.certificate_request_exported);
	}
	
	@Override
	public void onConfirm(String id, boolean ok){
		Slot slot = this.storage().getSlotById(this.slotid);
		final Csr csr = slot.getCsrById(this.csrid);
		if("remove".equals(id) && ok){
			slot.removeCsr(csr);
			slot.save();
			csr.remove();
			this.finish();
		}
	}
}

