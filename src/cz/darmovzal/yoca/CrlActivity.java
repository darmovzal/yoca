package cz.darmovzal.yoca;

import java.math.BigInteger;
import java.io.*;
import java.text.*;
import java.util.*;
import cz.darmovzal.yoca.guts.*;
import cz.darmovzal.yoca.ui.*;
import android.util.Log;

public class CrlActivity extends CommonActivity {
	private static final String LTAG = "YOCA:CrlActivity";
	
	private int slotid, certid;
	
	public void processParams(){
		this.slotid = this.getIntent().getIntExtra("slot", -1);
		this.certid = this.getIntent().getIntExtra("cert", -1);
	}
	
	protected String getTitleString(){
		Slot slot = this.storage().getSlotById(this.slotid);
		return r(R.string.main) + " > " + r(R.string.authorities) + " > " + r(R.string.record) + " > " + r(R.string.revocation_list);
	}
	
	public void createUi(final UIBuilder b){
		Crl crl = this.storage().getSlotById(this.slotid).getCertById(this.certid).crl();
		if(crl == null) throw new IllegalStateException("CRL is null");
		DateFormat ldf = DateFormat.getDateInstance(DateFormat.LONG);
		DateFormat sdf = DateFormat.getDateInstance(DateFormat.SHORT);
		
		if(crl.serials().isEmpty()){
			b.text(r(R.string.empty));
		} else {
			for(final BigInteger serial : crl.serials()){
				Date since = crl.getSince(serial);
				Crl.Reason reason = crl.getReason(serial);
				b.icon(R.drawable.cert).title("#" + serial + " - " + sdf.format(since)).groupBegin(serial.toString())
					.title(R.string.serial_number).text(serial.toString(10) + " (0x" + serial.toString(16) + ")")
					.title(R.string.since).text(ldf.format(since))
					.title(R.string.reason).text(reason.title())
					.id("unrevoke_pass_" + serial).title(R.string.authority_key_passphrase).password()
					.button(R.string.unrevoke_certificate, new UIBuilder.ClickListener(){
						public void click(){
							unrevokeCertificate(serial);
						}
					})
					.groupEnd(false);
			}
			
			b.title(R.string.export_crl).icon(R.drawable.icon_export_cert).groupBegin("export")
				.id("export_file").title(R.string.select_export_path).file(this.getExportPath(), new UIBuilder.ClickListener(){
					public void click(){
						showFileDialog("export_file", R.string.select_export_path, b.getFileValue("export_file"), false, false, null);
					}
				})
				.button(R.string.export_crl, new UIBuilder.ClickListener(){
					public void click(){
						exportCrl();
					}
				})
				.groupEnd();
		}
	}
	
	private void unrevokeCertificate(BigInteger serial){
		char[] pass = builder.getPasswordValue("unrevoke_pass_" + serial);
		Crl crl = this.storage().getSlotById(this.slotid).getCertById(this.certid).crl();
		crl.remove(serial);
		try {
			crl.generate(pass);
		} catch (Exception e){
			Log.w(LTAG, "Failed to generate CRL", e);
			crl.restore();
			this.showErrorDialog(R.string.wrong_passphrase);
			return;
		}
		crl.save();
		this.updateUi();
	}
	
	private void exportCrl(){
		Crl crl = this.storage().getSlotById(this.slotid).getCertById(this.certid).crl();
		String path = builder.getFileValue("export_file");
		if(!this.checkExportPath(path)) return;
		try {
			FileOutputStream fos = new FileOutputStream(path);
			try {
				crl.save(fos, null);
			} finally {
				fos.close();
			}
		} catch (Exception e){
			Log.e(LTAG, "Failed to export CRL to path \"" + path + "\"", e);
			this.showErrorDialog(R.string.crl_export_failed);
			return;
		}
		this.showInfoDialog(R.string.crl_exported);
	}
}

