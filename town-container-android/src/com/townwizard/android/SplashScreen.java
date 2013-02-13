package com.townwizard.android;

import java.net.URL;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;

import com.townwizard.android.category.CategoriesAdapter;
import com.townwizard.android.category.CategoriesLoadTask;
import com.townwizard.android.config.Config;
import com.townwizard.android.config.Constants;
import com.townwizard.android.partner.Partner;
import com.townwizard.android.utils.CurrentLocation;
import com.townwizard.android.utils.ServerConnector;

public class SplashScreen extends Activity{

    private Handler handler;
    private Runnable runnable;
    private boolean isTownWizard;
    private Partner partner;
    private CategoriesAdapter categoriesAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String partnerId = Config.getConfig(this).getPartnerId();
        isTownWizard = Config.getConfig(this).isContainerApp();

        if (!isTownWizard) {
            partner = loadPartner(partnerId);
            CategoriesLoadTask categoriesLoader = new CategoriesLoadTask(this);
            categoriesLoader.execute(new String[] {partnerId});
            categoriesAdapter = categoriesLoader.getCategoriesAdapter();
        }
        
        setContentView(R.layout.splash);
        new CurrentLocation(this).getLocation();

        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                startNextActivity();
            }
        };
        handler.postDelayed(runnable, Config.SPLASH_TIME);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            handler.removeCallbacks(runnable);
            startNextActivity();
        }
        return true;
    }
    
    private Partner loadPartner(String partnerId) {
        try {
            URL url = new URL(Config.PARTNER_API + partnerId);
            Log.d("Search URL = ", url.toString());

            String response = ServerConnector.getServerResponse(url);
            Log.d("JSON = ", response);

            JSONObject mMainJsonObject = new JSONObject(response);
            int status = mMainJsonObject.getInt("status");

            if (status == 1) {
                JSONObject jsObj = mMainJsonObject.getJSONObject("data");
                int id = jsObj.getInt("id");
                String name = jsObj.getString("name");                
                String androidAppId = jsObj.getString("android_app_id");
                String imageUrl = jsObj.getString("image");
                String siteUrl = jsObj.getString("website_url");                
                if (siteUrl.charAt(siteUrl.length() - 1) != '/') {
                    siteUrl += "/";
                }
                
                Partner p = new Partner(name, siteUrl, androidAppId, id, imageUrl);
                Log.d("partner", p.toString());
                return p;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }    
    
    private void startNextActivity() {
        if (isTownWizard) {
            startTownWizardActivity();
        } else {
            startWebActivity(categoriesAdapter);
        }
        finish();
    }    
    
    private void startTownWizardActivity() {        
        startActivity(new Intent(this, TownWizardActivity.class));        
    }
    
    private void startWebActivity(CategoriesAdapter categoriesAdapter) {
        Intent web = new Intent(this, WebActivity.class);
        web.putExtra(Constants.URL_SITE, partner.getUrl());
        String categoryUrl = getFullCategoryUrl(partner.getUrl(), categoriesAdapter.getHomeUrl());
        web.putExtra(Constants.URL_SECTION, categoryUrl);
        web.putExtra(Constants.CATEGORY_NAME, Constants.HOME);
        web.putExtra(Constants.PARTNER_NAME, partner.getName());
        web.putExtra(Constants.PARTNER_ID, Integer.valueOf(partner.getId()).toString());
        web.putExtra(Constants.IMAGE_URL, partner.getImageUrl());
        startActivity(web);
    }
    
    private String getFullCategoryUrl(String siteUrl, String url) {
        return url.startsWith("http") ? url : siteUrl + url;        
    }
}
