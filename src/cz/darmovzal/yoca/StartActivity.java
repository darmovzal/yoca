package cz.darmovzal.yoca;

import cz.darmovzal.yoca.ui.*;
import java.io.*;
import java.util.*;
import java.math.BigInteger;
import java.text.*;
import android.view.*;
import android.os.Bundle;
import cz.darmovzal.yoca.guts.*;
import android.util.Log;

public class StartActivity extends CommonActivity {
	private static final String LTAG = "YOCA:StartActivity";
	
	public void processParams(){}
	
	protected String getTitleString(){
		return r(R.string.main);
	}
	
	public void createUi(final UIBuilder b){
		Calendar c = Calendar.getInstance();
		c.setTime(new Date());
		c.add(Calendar.DAY_OF_MONTH, SettingsActivity.getPrefillValidity(this));
		Date notAfter = c.getTime();
		
		b.image(R.drawable.yoca);
		
		int authCount = 0;
		for(Slot slot : this.storage().slots()){
			if(slot.isCa()) authCount++;
		}
		int recCount = this.storage().slots().size() - authCount;
		
		b.title(R.string.archive_and_restore).icon(R.drawable.icon_archive).groupBegin("archive");
		if(this.storage().slots().size() > 0){
			b.button(R.string.archive_all_records, new UIBuilder.ClickListener(){
					public void click(){
						String dirPath = SettingsActivity.getArchiveDir(StartActivity.this);
						File dir = new File(dirPath);
						if(!dir.exists()) dir.mkdirs();
						if(!dir.exists()){
							showErrorDialog(R.string.please_set_archive_directory);
							return;
						}
						String pass = SettingsActivity.getArchivePass(StartActivity.this);
						if(pass.length() == 0){
							showErrorDialog(R.string.please_set_archive_passphrase);
							return;
						}
						archiveTask.execute(dir, pass);
					}
				});
		}
		b.id("restore_file").title(R.string.select_file_to_restore_from).file(SettingsActivity.getArchiveDir(this), new UIBuilder.ClickListener(){
				public void click(){
					showFileDialog("restore_file", R.string.select_file_to_restore_from, b.getFileValue("restore_file"), true, false, new String[]{ "yoca" });
				}
			})
			.button(R.string.restore_all_records, new UIBuilder.ClickListener(){
				public void click(){
					String path = b.getFileValue("restore_file");
					if(!checkImportPath(path)) return;
					String pass = SettingsActivity.getArchivePass(StartActivity.this);
					if(pass.length() == 0){
						showErrorDialog(R.string.please_set_archive_passphrase);
						return;
					}
					File dir = new File(SettingsActivity.getArchiveDir(StartActivity.this));
					if(!dir.exists()){
						showErrorDialog(R.string.please_set_archive_directory);
						return;
					}
					restoreTask.execute(new File(path), dir, pass);
				}
			});
		this.note(R.string.note_archive);
		b.groupEnd();
		
		b.title(this.r(R.string.x_authorities, authCount)).icon(R.drawable.icon_ca).link(authCount > 0 ? new UIBuilder.ClickListener(){
				public void click(){
					start(RecordListActivity.class, "ca", true);
				}
			} : null);
		
		b.title(this.r(R.string.x_records, recCount)).icon(R.drawable.icon_record).link(recCount > 0 ? new UIBuilder.ClickListener(){
				public void click(){
					start(RecordListActivity.class, "ca", false);
				}
			} : null);
		
		b.title(R.string.create_new_authority).icon(R.drawable.icon_new_ca).groupBegin("create_new_authority")
			.id("ca_name").title(R.string.name).edit("")
			.id("ca_key_type").title(R.string.key_type).select(true, Keys.Type.titles(), new UIBuilder.ClickListener(){
				public void click(){
					updateKeySizes("ca_key_type", "ca_key_size");
				}
			})
			.id("ca_key_size").title(R.string.key_size).select(true, Keys.Type.allSizeTitles())
			.id("ca_passphrase").title(R.string.key_passphrase).password()
			.id("ca_common_name").title(R.string.common_name).edit("")
			.id("ca_organization").title(R.string.organization).edit(SettingsActivity.getPrefillOrganization(this))
			.id("ca_organizational_unit").title(R.string.organizational_unit).edit(SettingsActivity.getPrefillOrganizationalUnit(this))
			.id("ca_locality").title(R.string.locality).edit(SettingsActivity.getPrefillLocality(this))
			.id("ca_state").title(R.string.state).edit(SettingsActivity.getPrefillState(this))
			.id("ca_country").title(R.string.country).edit(SettingsActivity.getPrefillCountry(this))
			.id("ca_signature_type").title(R.string.signature_digest_type).select(true, Sig.titles())
			.id("ca_not_before").title(R.string.valid_not_before).date(new Date(), new UIBuilder.ClickListener(){
				public void click(){
					showDateDialog("ca_not_before", R.string.valid_not_before, b.getDateValue("ca_not_before"));
				}
			})
			.id("ca_not_after").title(R.string.valid_not_after).date(notAfter, new UIBuilder.ClickListener(){
				public void click(){
					showDateDialog("ca_not_after", R.string.valid_not_after, b.getDateValue("ca_not_after"));
				}
			});
		
		this.createExtensionEditUi(b);
		
		b.button(R.string.create_new_authority, new UIBuilder.ClickListener(){
				public void click(){
					generateCa.execute();
				}
			});
		this.note(R.string.note_new_authority);
		b.groupEnd();
		
		b.title(R.string.generate_new_keypair).icon(R.drawable.icon_new_record).groupBegin("generate_new_keypair")
			.id("name").title(R.string.name).edit("")
			.id("key_type").title(R.string.key_type).select(true, Keys.Type.titles(), new UIBuilder.ClickListener(){
				public void click(){
					updateKeySizes("key_type", "key_size");
				}
			})
			.id("key_size").title(R.string.key_size).select(true, Keys.Type.allSizeTitles())
			.id("passphrase").title(R.string.key_passphrase).password()
			.button(R.string.generate_keypair, new UIBuilder.ClickListener(){
				public void click(){
					generateKeypair.execute();
				}
			});
		this.note(R.string.note_new_record);
		b.groupEnd();
		
		b.title(R.string.import_security_objects).icon(R.drawable.icon_import).link(new UIBuilder.ClickListener(){
			public void click(){
				start(ImportActivity.class);
			}
		});
		
		if(Storage.TRIAL) this.note(R.string.note_trial);
		this.note(R.string.note_start);
		
		this.updateKeySizes("ca_key_type", "ca_key_size");
		this.updateKeySizes("key_type", "key_size");
	}
	
	protected void updateKeySizes(String typeid, String sizeid){
		Keys.Type type = Keys.Type.get(this.builder.getSelectValue(typeid));
		int[] sizes = Keys.Type.allSizes();
		for(int i = 0; i < sizes.length; i++)
			this.builder.setSelectEnabled(sizeid, i, type.supports(sizes[i]));
	}
	
	private Task generateKeypair = new Task(){
		public void pre(){
			showProgressDialog(R.string.generating_keypair);
		}
		
		public Object work(Object ... args){
			String name = builder.getEditValue("name");
			if(!checkName(name)) return null;
			char[] pass = builder.getPasswordValue("passphrase");
			if(!checkPassphrase(pass)) return null;
			Keys.Type type = Keys.Type.get(builder.getSelectValue("key_type"));
			int size = Keys.Type.allSizes()[builder.getSelectValue("key_size")];
			
			Slot slot = storage().newSlot();
			slot.setCa(false);
			slot.setName(name);
			slot.keys().generate(type, size);
			slot.keys().priv().save(pass);
			slot.keys().priv().clear();
			slot.keys().pub().save();
			slot.save();
			
			return slot.id();
		}
		
		public void fail(Throwable t){
			dismissProgressDialog();
			showErrorDialog(R.string.failed_to_generate_keypair);
		}
		
		public void success(Object result){
			dismissProgressDialog();
			if(result == null) return;
			start(RecordActivity.class, "slot", result);
		}
	};
	
	private Task generateCa = new Task(){
		public void pre(){
			showProgressDialog(R.string.generating_keypair);
		}
		
		public Object work(Object ... args){
			String name = builder.getEditValue("ca_name");
			if(!checkName(name)) return null;
			
			int size = Keys.Type.allSizes()[builder.getSelectValue("ca_key_size")];
			char[] pass = builder.getPasswordValue("ca_passphrase");
			if(!checkPassphrase(pass)) return null;
			Keys.Type type = Keys.Type.get(builder.getSelectValue("ca_key_type"));
			int isig = builder.getSelectValue("ca_signature_type");
			Date notBefore = builder.getDateValue("ca_not_before");
			Date notAfter = builder.getDateValue("ca_not_after");
			if(!checkValidity(notBefore, notAfter)) return null;
			notAfter = Storage.trialNotAfter(notBefore, notAfter);
			Sig sig = Sig.get(builder.getSelectValue("ca_signature_type"));
			
			String cn = builder.getEditValue("ca_common_name");
			if(!checkCommonName(cn)) return null;
			Name subject = Name.create(
				"C", builder.getEditValue("ca_country"),
				"ST", builder.getEditValue("ca_state"),
				"L", builder.getEditValue("ca_locality"),
				"O", Storage.trialOrganization(builder.getEditValue("ca_organization")),
				"OU", builder.getEditValue("ca_organizational_unit"),
				"CN", cn
			);
			
			Exts exts = new Exts();
			exts.addBasicConstraints(true, true, 0);
			try {
				processExtensionEditUi(exts);
			} catch (Exception e){
				Log.e(LTAG, "Processing CA extensions failed", e);
				showErrorDialog(R.string.wrong_extension_value_format);
				return null;
			}
			
			Slot slot = storage().newSlot();
			slot.setCa(true);
			slot.setName(name);
			slot.keys().generate(type, size);
			Csr csr = Csr.generate(subject, slot.keys(), pass, sig, exts);
			Cert cert = csr.sign(subject, slot.keys(), pass, slot.freshSerial(), notBefore, notAfter, sig);
			slot.incSerial();
			slot.add(cert);
			
			slot.keys().priv().save(pass);
			slot.keys().priv().clear();
			slot.keys().pub().save();
			cert.save();
			slot.save();
			
			return slot.id();
		}
		
		public void fail(Throwable t){
			dismissProgressDialog();
			showErrorDialog(R.string.failed_to_create_ca);
		}
		
		public void success(Object result){
			dismissProgressDialog();
			if(result == null) return;
			start(RecordActivity.class, "slot", result);
		}
	};
	
	private File archive(File dir, String pass, boolean auto) throws Exception {
		String name = (auto ? "auto" : "manual") + (new SimpleDateFormat("'_'yyyy-MM-dd_HH-mm-ss'.yoca'")).format(new Date());
		File file = new File(dir, name);
		Log.i(LTAG, "Archiving into file " + file.getAbsolutePath());
		this.storage().archive(file, pass);
		Log.i(LTAG, "Archiving done");
		return file;
	}
	
	private void restore(File file, File dir, String pass) throws Exception {
		Log.i(LTAG, "Dry run");
		this.storage().unarchive(file, pass, true);
		if(this.storage().slots().size() > 0){
			Log.i(LTAG, "Auto-archiving");
			this.archive(dir, pass, true);
		}
		Log.i(LTAG, "Clearing data");
		this.storage().mrproper();
		Log.i(LTAG, "Restoring data from file " + file.getAbsolutePath());
		this.storage().unarchive(file, pass, false);
		Log.i(LTAG, "Loading data");
		this.storage().load();
	}
	
	private Task archiveTask = new Task(){
		public void pre(){
			showProgressDialog(R.string.archiving);
		}
		
		public Object work(Object ... args) throws Exception {
			File dir = (File) args[0];
			String pass = (String) args[1];
			File file = archive(dir, pass, false);
			return file.getAbsolutePath();
		}
		
		public void success(Object result){
			dismissProgressDialog();
			showInfoDialog(r(R.string.successfully_archived_into_x, result));
		}
		
		public void fail(Throwable t){
			dismissProgressDialog();
			showErrorDialog(R.string.creating_archive_failed);
		}
	};
	
	private Task restoreTask = new Task(){
		public void pre(){
			showProgressDialog(R.string.archiving_and_restoring);
		}
		
		public Object work(Object ... args) throws Exception {
			File file = (File) args[0];
			File dir = (File) args[1];
			String pass = (String) args[2];
			restore(file, dir, pass);
			return null;
		}
		
		public void success(Object result){
			updateUi();
			dismissProgressDialog();
			showInfoDialog(R.string.successfully_restored);
		}
		
		public void fail(Throwable t){
			updateUi();
			dismissProgressDialog();
			if(t instanceof PassphraseException){
				showErrorDialog(R.string.wrong_passphrase);
			} else {
				showErrorDialog(R.string.restoring_failed);
			}
		}
	};
}

