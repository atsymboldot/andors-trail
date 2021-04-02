package com.gpl.rpg.AndorsTrail.activity;

import java.util.Collections;
import java.util.List;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;
import com.gpl.rpg.AndorsTrail.AndorsTrailPreferences;
import com.gpl.rpg.AndorsTrail.R;
import com.gpl.rpg.AndorsTrail.model.ModelContainer;
import com.gpl.rpg.AndorsTrail.resource.tiles.TileManager;
import com.gpl.rpg.AndorsTrail.savegames.Savegames;
import com.gpl.rpg.AndorsTrail.savegames.Savegames.FileHeader;
import com.gpl.rpg.AndorsTrail.util.ThemeHelper;
import com.gpl.rpg.AndorsTrail.view.CustomDialogFactory;

public final class LoadSaveActivity extends AndorsTrailBaseActivity implements OnClickListener {
	private boolean isLoading = true;
	private static final int SLOT_NUMBER_CREATE_NEW_SLOT = -1;
	private static final int SLOT_NUMBER_FIRST_SLOT = 1;
	private ModelContainer model;
	private TileManager tileManager;
	private AndorsTrailPreferences preferences;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setTheme(ThemeHelper.getDialogTheme());
		super.onCreate(savedInstanceState);

		final AndorsTrailApplication app = AndorsTrailApplication.getApplicationFromActivity(this);
		app.setWindowParameters(this);
		this.model = app.getWorld().model;
		this.preferences = app.getPreferences();
		this.tileManager = app.getWorld().tileManager;

		String loadsave = getIntent().getData().getLastPathSegment();
		isLoading = (loadsave.equalsIgnoreCase("load"));

		setContentView(R.layout.loadsave);

		TextView tv = (TextView) findViewById(R.id.loadsave_title);
		if (isLoading) {
			tv.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_search, 0, 0, 0);
			tv.setText(R.string.loadsave_title_load);
		} else {
			tv.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_save, 0, 0, 0);
			tv.setText(R.string.loadsave_title_save);
		}

		ViewGroup slotList = (ViewGroup) findViewById(R.id.loadsave_slot_list);
		Button slotTemplateButton = (Button) findViewById(R.id.loadsave_slot_n);
		LayoutParams params = slotTemplateButton.getLayoutParams();
		slotList.removeView(slotTemplateButton);

		ViewGroup newSlotContainer = (ViewGroup) findViewById(R.id.loadsave_save_to_new_slot_container);
		Button createNewSlot = (Button) findViewById(R.id.loadsave_save_to_new_slot);

		addSavegameSlotButtons(slotList, params, Savegames.getUsedSavegameSlots(this));

		// checkAndRequestPermissions();
		
		if (!isLoading) {
			createNewSlot.setTag(SLOT_NUMBER_CREATE_NEW_SLOT);
			createNewSlot.setOnClickListener(this);
			newSlotContainer.setVisibility(View.VISIBLE);
		} else {
			newSlotContainer.setVisibility(View.GONE);
		}
	}

	private static final int READ_EXTERNAL_STORAGE_REQUEST=1;
	private static final int WRITE_EXTERNAL_STORAGE_REQUEST=2;
	
	@TargetApi(23)
	private void checkAndRequestPermissions() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (getApplicationContext().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
				this.requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE_REQUEST);
			}
			if (getApplicationContext().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
				this.requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE_REQUEST);
			} 
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
			Toast.makeText(this, R.string.storage_permissions_mandatory, Toast.LENGTH_LONG).show();
			((AndorsTrailApplication)getApplication()).discardWorld();
			finish();
		}
	}
	
	private void addSavegameSlotButtons(ViewGroup parent, LayoutParams params, List<Integer> usedSavegameSlots) {
		int unused = 1;
		for (int slot : usedSavegameSlots) {
			final FileHeader header = Savegames.quickload(this, slot);
			if (header == null) continue;

			while (unused < slot){
				Button b = new Button(this);
				b.setLayoutParams(params);
				b.setTag(unused);
				b.setOnClickListener(this);
				b.setText(getString(R.string.loadsave_empty_slot, unused));
				tileManager.setImageViewTileForPlayer(getResources(), b, header.iconID);
				parent.addView(b, params);
				unused++;
			}
			unused++;

			Button b = new Button(this);
			b.setLayoutParams(params);
			b.setTag(slot);
			b.setOnClickListener(this);
			b.setText(slot + ". " + header.describe());
			tileManager.setImageViewTileForPlayer(getResources(), b, header.iconID);
			parent.addView(b, params);
		}
	}

	public void loadsave(int slot) {
		if (slot == SLOT_NUMBER_CREATE_NEW_SLOT) {
			List<Integer> usedSlots = Savegames.getUsedSavegameSlots(this);
			if (usedSlots.isEmpty()) slot = SLOT_NUMBER_FIRST_SLOT;
			else slot = Collections.max(usedSlots) + 1;
		}
		if (slot < SLOT_NUMBER_FIRST_SLOT) slot = SLOT_NUMBER_FIRST_SLOT;

		Intent i = new Intent();
		i.putExtra("slot", slot);
		setResult(Activity.RESULT_OK, i);
		LoadSaveActivity.this.finish();
	}

	private String getConfirmOverwriteQuestion(int slot) {
		if (isLoading) return null;
		if (slot == SLOT_NUMBER_CREATE_NEW_SLOT) return null;					// if we're creating a new slot
        if (!Savegames.getSlotFile(slot, this).exists()) return null;

		if (preferences.displayOverwriteSavegame == AndorsTrailPreferences.CONFIRM_OVERWRITE_SAVEGAME_ALWAYS) {
			return getString(R.string.loadsave_save_overwrite_confirmation_all);
		}
		if (preferences.displayOverwriteSavegame == AndorsTrailPreferences.CONFIRM_OVERWRITE_SAVEGAME_NEVER) {
			return null;
		}

		final String currentPlayerName = model.player.getName();
		final FileHeader header = Savegames.quickload(this, slot);
		if (header == null) return null;

		final String savedPlayerName = header.playerName;
		if (currentPlayerName.equals(savedPlayerName)) return null;			// if the names match

		return getString(R.string.loadsave_save_overwrite_confirmation, savedPlayerName, currentPlayerName);
	}

	@Override
	public void onClick(View view) {
		final int slot = (Integer) view.getTag();

		if (!isLoading && slot != SLOT_NUMBER_CREATE_NEW_SLOT && AndorsTrailApplication.CURRENT_VERSION == AndorsTrailApplication.DEVELOPMENT_INCOMPATIBLE_SAVEGAME_VERSION) {
			final FileHeader header = Savegames.quickload(this, slot);
			if (header != null && header.fileversion != AndorsTrailApplication.DEVELOPMENT_INCOMPATIBLE_SAVEGAME_VERSION) {
				final Dialog d = CustomDialogFactory.createDialog(this,
						"Overwriting not allowed",
						getResources().getDrawable(android.R.drawable.ic_dialog_alert),
						"You are currently using a development version of Andor's trail. Overwriting a regular savegame is not allowed in development mode.",
						null,
						true);
				CustomDialogFactory.addDismissButton(d, android.R.string.ok);
				CustomDialogFactory.show(d);
				return;
			}
		}


		if (isLoading) {
			if(!Savegames.getSlotFile(slot, this).exists()) {
				showErrorLoadingEmptySlot();
			} else {
				final FileHeader header = Savegames.quickload(this, slot);
				if (header != null && !header.hasUnlimitedSaves) {
					showSlotGetsDeletedOnLoadWarning(slot);
				} else {
					loadsave(slot);
				}
			}
		} else {
			final String message = getConfirmOverwriteQuestion(slot);
			if (message != null) {
				showConfirmoverwriteQuestion(slot, message);
			} else {
				loadsave(slot);
			}
		}
	}

	private void showErrorLoadingEmptySlot() {
		final Dialog d = CustomDialogFactory.createDialog(this,
				getString(R.string.startscreen_error_loading_game),
				getResources().getDrawable(android.R.drawable.ic_dialog_alert),
				getString(R.string.startscreen_error_loading_empty_slot),
				null,
				true);
		CustomDialogFactory.addDismissButton(d, android.R.string.ok);
		CustomDialogFactory.show(d);
	}

	private void showSlotGetsDeletedOnLoadWarning(final int slot) {
		final Dialog d = CustomDialogFactory.createDialog(this,
				getString(R.string.startscreen_attention_slot_gets_delete_on_load),
				getResources().getDrawable(android.R.drawable.ic_dialog_alert),
				getString(R.string.startscreen_attention_message_slot_gets_delete_on_load),
				null,
				true);
		CustomDialogFactory.addButton(d, android.R.string.ok, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				loadsave(slot);
			}
		});
		CustomDialogFactory.show(d);
	}

	private void showConfirmoverwriteQuestion(final int slot, String message) {
		final String title =
				getString(R.string.loadsave_save_overwrite_confirmation_title) + ' '
						+ getString(R.string.loadsave_save_overwrite_confirmation_slot, slot);
		final Dialog d = CustomDialogFactory.createDialog(this,
				title,
				getResources().getDrawable(android.R.drawable.ic_dialog_alert),
				message,
				null,
				true);

		CustomDialogFactory.addButton(d, android.R.string.yes, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				loadsave(slot);
			}
		});
		CustomDialogFactory.addDismissButton(d, android.R.string.no);
		CustomDialogFactory.show(d);
	}
}
