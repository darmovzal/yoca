package cz.darmovzal.yoca;

import java.util.*;
import java.io.FileOutputStream;
import java.text.DateFormat;
import android.util.Log;
import cz.darmovzal.yoca.guts.*;
import cz.darmovzal.yoca.ui.*;

public class CertActivity extends CommonActivity {
	private static final String LTAG = "YOCA:CertActivity";
	
	private int slotid, certid;
	
	public void processParams(){
		this.slotid = this.getIntent().getIntExtra("slot", -1);
		this.certid = this.getIntent().getIntExtra("cert", -1);
	}
	
	protected String getTitleString(){
		Slot slot = this.storage().getSlotById(this.slotid);
		return r(R.string.main) + " > " + r(slot.isCa() ? R.string.authorities : R.string.records) + " > " + r(R.string.record) + " > " + r(R.string.certificate);
	}
	
	public void createUi(final UIBuilder b){
		DateFormat df = DateFormat.getDateInstance(DateFormat.LONG);
		Slot slot = this.storage().getSlotById(this.slotid);
		final Cert cert = slot.getCertById(this.certid);
		Exts exts = cert.getExts();
		Cert vercert = this.storage().getVerificationCert(cert);
		boolean selfsigned = cert.equals(vercert);
		Crl vercrl = vercert != null ? vercert.crl() : null;
		boolean revoked = (vercrl != null) && vercrl.isRevoked(cert);
		
		b.title(R.string.subject).groupBegin("subject", cert.subject().toShortString());
		this.createNameUi(b, cert.subject());
		b.groupEnd();
		
		b.title(R.string.issuer).groupBegin("issuer", cert.issuer().toShortString());
		this.createNameUi(b, cert.issuer());
		b.groupEnd();
		
		b.title(R.string.serial_number).text(this.toHex(cert.serial(), 8));
		b.title(R.string.validity).text(r(revoked ? R.string.revoked : (cert.valid() ? R.string.valid : R.string.not_valid)));
		b.title(R.string.valid_not_before).text(df.format(cert.notBefore()));
		b.title(R.string.valid_not_after).text(df.format(cert.notAfter()));
		
		b.title(r(R.string.signature) + " (" + r(vercert == null ? R.string.unable_to_verify : R.string.verified) + ")").icon(R.drawable.icon_sign).groupBegin("signature");
		if(vercert != null)
			b.title(R.string.verified_by).text(selfsigned ? r(R.string.self_signed) : this.toCertString(vercert));
		b.title(R.string.signature_type).text(cert.signatureType())
			.title(R.string.signature_data).text(this.toHex(cert.signature(), 8, false));
		this.note(R.string.note_signature);
		b.groupEnd();
		
		this.createExtensionPresentationUi(b, exts);
		
		final Crl crl = cert.crl();
		if(crl != null){
			b.title(r(R.string.x_revoked_certificates, crl.size())).icon(R.drawable.icon_revoke).link(new UIBuilder.ClickListener(){
				public void click(){
					if(crl.size() == 0) return;
					start(CrlActivity.class, "slot", slotid, "cert", certid);
				}
			});
		}
		
		if((vercrl != null) && !selfsigned){
			b.title(revoked ? R.string.revocation_info : R.string.revoke_certificate).icon(R.drawable.icon_revoke).groupBegin("revocation");
			if(revoked){
				Date since = vercrl.getSince(cert.serial());
				b.title(R.string.since).text(df.format(since));
				Crl.Reason reason = vercrl.getReason(cert.serial());
				b.title(R.string.reason).text(reason.title());
				b.id("unrevoke_pass").title(R.string.authority_key_passphrase).password();
				b.button(R.string.unrevoke_certificate, new UIBuilder.ClickListener(){
					public void click(){
						showConfirmDialog("unrevoke", r(R.string.do_you_really_want_to_unrevoke_certificate_x, toCertString(cert)));
					}
				});
			} else {
				b.id("revoke_since").title(R.string.since).date(new Date(), new UIBuilder.ClickListener(){
					public void click(){
						showDateDialog("revoke_since", R.string.since, b.getDateValue("revoke_since"));
					}
				});
				b.id("revoke_reason").title(R.string.reason).select(true, Crl.Reason.titles());
				b.id("revoke_pass").title(R.string.authority_key_passphrase).password();
				b.button(R.string.revoke_certificate, new UIBuilder.ClickListener(){
					public void click(){
						showConfirmDialog("revoke", r(R.string.do_you_really_want_to_revoke_certificate_x, toCertString(cert)));
					}
				});
			}
			b.groupEnd(revoked);
		}
		
		b.title(R.string.export_certificate).icon(R.drawable.icon_export_cert).groupBegin("export")
			.button(R.string.export_certificate_to_clipboard, new UIBuilder.ClickListener(){
				public void click(){
					exportToClipboard();
				}
			})
			.id("export_file").title(R.string.select_export_path).file(this.getExportPath(), new UIBuilder.ClickListener(){
				public void click(){
					showFileDialog("export_file", R.string.select_export_path, b.getFileValue("export_file"), false, false, null);
				}
			})
			.button(R.string.export_certificate_to_file, new UIBuilder.ClickListener(){
				public void click(){
					exportToFile();
				}
			})
			.groupEnd();
		
		b.title(R.string.removal).icon(R.drawable.icon_remove_cert).groupBegin("removal")
			.button(R.string.remove_certificate, new UIBuilder.ClickListener(){
				public void click(){
					showConfirmDialog("remove", r(R.string.do_you_really_want_to_remove_certificate_x, toCertString(cert)));
				}
			})
			.groupEnd();
		
		this.note(R.string.note_cert);
	}
	
	@Override
	public void onConfirm(String id, boolean ok){
		Slot slot = this.storage().getSlotById(this.slotid);
		final Cert cert = slot.getCertById(this.certid);
		if("remove".equals(id) && ok){
			slot.removeCert(cert);
			slot.save();
			cert.remove();
			this.finish();
		} else if("revoke".equals(id) && ok){
			this.revokeCertificate();
		} else if("unrevoke".equals(id) & ok){
			this.unrevokeCertificate();
		}
	}
	
	private void exportToFile(){
		Slot slot = this.storage().getSlotById(this.slotid);
		Cert cert = slot.getCertById(this.certid);
		String path = builder.getFileValue("export_file");
		if(!this.checkExportPath(path)) return;
		try {
			FileOutputStream fos = new FileOutputStream(path);
			try {
				cert.save(fos, null);
			} finally {
				fos.close();
			}
		} catch (Exception e){
			Log.e(LTAG, "Failed to export certificate to path \"" + path + "\"", e);
			this.showErrorDialog(R.string.certificate_export_failed);
			return;
		}
		this.showInfoDialog(R.string.certificate_exported);
	}
	
	private void exportToClipboard(){
		Cert cert = this.storage().getSlotById(this.slotid).getCertById(this.certid);
		try {
			this.setClipboardText(cert.getPemString(null));
		} catch (Exception e){
			Log.e(LTAG, "Failed to export certificate to clipboard", e);
			this.showErrorDialog(R.string.certificate_export_failed);
			return;
		}
		this.showInfoDialog(R.string.certificate_exported);
	}
	
	private void revokeCertificate(){
		Date since = builder.getDateValue("revoke_since");
		Crl.Reason reason = Crl.Reason.values()[builder.getSelectValue("revoke_reason")];
		char[] pass = builder.getPasswordValue("revoke_pass");
		
		Slot slot = this.storage().getSlotById(this.slotid);
		Cert cert = slot.getCertById(this.certid);
		Crl crl = this.storage().getVerificationCert(cert).crl();
		
		crl.add(cert.serial(), since, reason);
		try {
			crl.generate(pass);
		} catch (Exception e){
			Log.w(LTAG, "Failed to generate CRL", e);
			this.showErrorDialog(R.string.wrong_passphrase);
			crl.restore();
			return;
		}
		crl.save();
		updateUi();
	}
	
	private void unrevokeCertificate(){
		char[] pass = builder.getPasswordValue("unrevoke_pass");
		
		Slot slot = this.storage().getSlotById(this.slotid);
		Cert cert = slot.getCertById(this.certid);
		Crl crl = this.storage().getVerificationCert(cert).crl();
		
		crl.remove(cert.serial());
		try {
			crl.generate(pass);
		} catch (Exception e){
			Log.w(LTAG, "Failed to generate CRL", e);
			this.showErrorDialog(R.string.wrong_passphrase);
			crl.restore();
			return;
		}
		crl.save();
		updateUi();
	}
}

