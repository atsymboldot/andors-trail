package com.gpl.rpg.AndorsTrail.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;
import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;
import com.gpl.rpg.AndorsTrail.R;
import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.controller.WorldMapController;
import com.gpl.rpg.AndorsTrail.model.map.WorldMapSegment;
import com.gpl.rpg.AndorsTrail.model.map.WorldMapSegment.WorldMapSegmentMap;
import com.gpl.rpg.AndorsTrail.util.L;

import java.io.File;

public final class DisplayWorldMapActivity extends Activity {
	private WorldContext world;

	private WebView displayworldmap_webview;
	private String worldMapSegmentName;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		AndorsTrailApplication app = AndorsTrailApplication.getApplicationFromActivity(this);
		if (!app.isInitialized()) { finish(); return; }
		this.world = app.getWorld();

		app.setWindowParameters(this);

		setContentView(R.layout.displayworldmap);

		displayworldmap_webview = (WebView) findViewById(R.id.displayworldmap_webview);
//		displayworldmap_webview.setBackgroundColor(getResources().getColor(R.color.displayworldmap_background));
		displayworldmap_webview.getSettings().setBuiltInZoomControls(true);
		displayworldmap_webview.getSettings().setUseWideViewPort(true);
		enableJavascript();

		Button b = (Button) findViewById(R.id.displayworldmap_close);
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				DisplayWorldMapActivity.this.finish();
			}
		});

		worldMapSegmentName = getIntent().getStringExtra("worldMapSegmentName");
	}

	@SuppressLint("SetJavaScriptEnabled")
	public void enableJavascript() {
		displayworldmap_webview.getSettings().setJavaScriptEnabled(true);
	}

	@Override
	public void onResume() {
		super.onResume();
		update();
	}

	@SuppressLint("NewApi")
	private void update() {
		File worldmap = WorldMapController.getCombinedWorldMapFile(worldMapSegmentName);

		if (!worldmap.exists()) {
			Toast.makeText(this, getResources().getString(R.string.menu_button_worldmap_failed), Toast.LENGTH_LONG).show();
			this.finish();
		}

		WorldMapSegment segment = world.maps.worldMapSegments.get(worldMapSegmentName);
		WorldMapSegmentMap map = segment.maps.get(world.model.currentMap.name);
		if (map == null) {
			this.finish();
			return;
		}

		String url = "file://" + worldmap.getAbsolutePath() + '?'
				+ (world.model.player.position.x + map.worldPosition.x) * WorldMapController.WORLDMAP_DISPLAY_TILESIZE
				+ ','
				+ (world.model.player.position.y + map.worldPosition.y-1) * WorldMapController.WORLDMAP_DISPLAY_TILESIZE;
		L.log("Showing " + url);
		displayworldmap_webview.loadUrl(url);
		displayworldmap_webview.setBackgroundColor(getResources().getColor(android.R.color.transparent));
		
		if (Build.VERSION.SDK_INT >= 11) displayworldmap_webview.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);

		displayworldmap_webview.setWebViewClient(new WebViewClient()
		{
		    @SuppressLint("NewApi")
			@Override
		    public void onPageFinished(WebView view, String url)
		    {
		    	displayworldmap_webview.setBackgroundColor(0x00000000);
		        if (Build.VERSION.SDK_INT >= 11) displayworldmap_webview.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
		    }
		});
	}
}
