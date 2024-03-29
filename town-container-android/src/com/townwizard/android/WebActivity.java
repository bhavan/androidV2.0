package com.townwizard.android;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.MailTo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;

import com.google.analytics.tracking.android.EasyTracker;
import com.townwizard.android.category.CategoriesAdapter;
import com.townwizard.android.category.Category;
import com.townwizard.android.config.Config;
import com.townwizard.android.config.Constants;
import com.townwizard.android.partner.Partner;
import com.townwizard.android.utils.CurrentLocation;
import com.townwizard.android.utils.Utils;

@SuppressLint("SetJavaScriptEnabled")
public class WebActivity extends Activity {

    private static final String sUpload = "components/com_shines/iuploadphoto.php";
    private String mUrlSite;
    private WebView mWebView;
    private static final int sCAMERA_RESULT = 1;
    private static final int sGALLERY = 2;
    private static Uri sImagePath;
    private Header header;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        
        
        Bundle extras = getIntent().getExtras();
        if(extras != null && extras.getBoolean(Constants.OVERRIDE_TRANSITION)) {
            overridePendingTransition(R.anim.slide_from_left, R.anim.slide_from_left);
        }
        
        boolean online = Utils.checkConnectivity(this);
        Partner partner = Config.getConfig(this).getPartner();
        Category category = Config.getConfig(this).getCategory();
        
        String categoryUrl = null;
        if(category != null && online) {
            categoryUrl = getFullCategoryUrl(category);        
            Log.d("Category url", categoryUrl);            
        }        
        
        boolean contentViewSet = false;
        
        if(partner != null && category != null) {
            mUrlSite = partner.getUrl();
            if (category.getSectionName().contains(Constants.PHOTOS) && isUploadScriptExist(mUrlSite + sUpload)) {
                Log.d("WebActivity", "File exist");
                setContentView(R.layout.web_with_upload);
                contentViewSet = true;
                Button uploadButton = (Button) findViewById(R.id.bt_upload);
                uploadButton.setOnClickListener(new View.OnClickListener() {        
                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(WebActivity.this);
                        alertDialog.setPositiveButton("Take photo", new OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        startCameraIntent();
                                    }
                                });
                        alertDialog.setNeutralButton("Choose from library",  new OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        startChooseFromLibraryIntent();
                                    }
                                });
                        alertDialog.setNegativeButton("Cancel", new OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                    }
                                });
                        alertDialog.show();
                    }
                });
            }
        }

        if(!contentViewSet) {
            if(categoryUrl != null && categoryUrl.contains(Config.CONTENT_PARTNER_CONTENT_FOLDER)) {
                setContentView(R.layout.web_with_banner);
                ImageView ownThisTownBanner = (ImageView)findViewById(R.id.own_this_town);
                if(ownThisTownBanner != null) {
                    ownThisTownBanner.setOnTouchListener(new OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            Intent i = new Intent(Intent.ACTION_DIAL, null);
                            i.setData(Uri.parse("tel:" + Config.TOWNWIZARD_PHONE));
                            startActivity(i);
                            return true;
                        }                        
                    });
                }
            } else {
                setContentView(R.layout.web);
            }
        }

        mWebView = (WebView) findViewById(R.id.webview);
        mWebView.setWebViewClient(new TownWizardWebViewClient());
        mWebView.getSettings().setJavaScriptEnabled(true);           
        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.getSettings().setSupportZoom(true);
        mWebView.getSettings().setBuiltInZoomControls(true);
        Utils.makeZoomControlsInvisible(mWebView);

        header = Header.build(this, mWebView);
    }
    
    @Override
    public void onStart() {
        super.onStart();
        EasyTracker.getInstance().activityStart(this);
        
        if(mWebView.getUrl() == null && Utils.isOnline(this)) {
            Category c = Config.getConfig(this).getCategory();
            if(c == null) {
                c = new CategoriesAdapter(this).getHomeCategory();
            }
            if(c != null) {
                String categoryUrl = getFullCategoryUrl(c);
                mWebView.loadUrl(categoryUrl);
            }
        }
    }
    
    @Override
    public void onStop() {
        super.onStop();
        EasyTracker.getInstance().activityStop(this);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Intent uploadPhoto = new Intent(this, UploadPhotoActivity.class);
        uploadPhoto.putExtra(Constants.URL, mUrlSite + sUpload);

        if (requestCode == sGALLERY) {
            Log.d("result is ", Integer.toString(resultCode));

            if (resultCode == RESULT_OK) {
                Uri imageUri = data.getData();

                uploadPhoto.putExtra(Constants.IMAGE_URI, imageUri);
                uploadPhoto.putExtra(Constants.SOURCE, "GALLERY");
                startActivity(uploadPhoto);

            } else {
                WebActivity.this.finish();
            }
        }
        if (requestCode == sCAMERA_RESULT) {

            if (resultCode == RESULT_OK) {
                Log.d("camera result", "start upload activity");
                Log.d("imagePath", sImagePath.toString());
                uploadPhoto.putExtra(Constants.IMAGE_URI, sImagePath);
                uploadPhoto.putExtra(Constants.SOURCE, "CAMERA");
                startActivity(uploadPhoto);
            } else {
                WebActivity.this.finish();
            }
        }
    }
    
    @Override
    public void onBackPressed() {
        header.goBack();
    }

    private boolean isUploadScriptExist(String URLName) {
        try {
            HttpURLConnection con = (HttpURLConnection) new URL(URLName).openConnection();
            con.setRequestMethod("HEAD");
            return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private class TownWizardWebViewClient extends WebViewClient {
        @Override
        @SuppressLint("DefaultLocale")
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            String upperUrl = url.toUpperCase();
            if (upperUrl.contains("YOUTUBE.COM/WATCH")) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); 
            } else if (upperUrl.startsWith("HTTP")) {            
                header.addToBreadCrumb(view.getUrl(), url);                
                view.loadUrl(url);
            } else if (upperUrl.startsWith("MAILTO:")) {
                sendMail(url);
            } else if (upperUrl.startsWith("TEL")) {
                Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
                startActivity(dialIntent);
            } else if (upperUrl.startsWith("APP30A:")) {
                if(upperUrl.contains("SHOWMAP")) {            
                    showMap(url);
                } else if (upperUrl.contains("FBCHECKIN")) {
                    facebookCheckin();
                }
            }
            
            return true;
        }

        @Override
        public void onPageStarted (WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            view.clearHistory();
            header.drawBackButton();
        }
        
        @Override
        public void onPageFinished (WebView view, String url) {
            super.onPageFinished(view, url);
            header.drawBackButton();            
        }
    }
    
    private void startCameraIntent() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photo = new File(Environment.getExternalStorageDirectory(),
                Long.toString(System.currentTimeMillis()));
        sImagePath = Uri.fromFile(photo);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photo));
        startActivityForResult(cameraIntent, sCAMERA_RESULT);
    }

    private void startChooseFromLibraryIntent() {
        Intent intent = new Intent();
        intent.setType("image/jpg");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"),
                sGALLERY);
    }

    private void showMap(String url) {
        String[] urlParts = url.split(":");
        if(urlParts.length == 4) {
            //String latitude = "42.18794250";
            //String longitude = "-79.83222961";
            String latitude = urlParts[2];
            String longitude = urlParts[3];
            Intent i = new Intent(WebActivity.this, MapViewActivity.class);
            i.putExtra(Constants.LATITUDE, latitude);
            i.putExtra(Constants.LONGITUDE, longitude);
            i.putExtra(Constants.FROM_ACTIVITY, getClass());
            startActivity(i);
       }
    }

    private void facebookCheckin() {
        Intent i = new Intent(WebActivity.this, FacebookPlacesActivity.class);        
        i.putExtra(Constants.FROM_ACTIVITY, getClass());
        startActivity(i);
    }

    private void sendMail(String url) {
        MailTo mailTo = MailTo.parse(url);
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_EMAIL, new String[]{mailTo.getTo()});
        i.putExtra(Intent.EXTRA_TEXT, mailTo.getBody());
        i.putExtra(Intent.EXTRA_SUBJECT, mailTo.getSubject());
        startActivity(i);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    
    private String getFullCategoryUrl(Category category) {
        String url = category.getUrl();
        // String categoryName = category.getName(); //bhavan: this is section name
        String categoryName = category.getSectionName();
        Partner partner = Config.getConfig(this).getPartner();
        
        if(!url.startsWith("http")) {
            if(partner != null) {
                url = partner.getUrl() + url;
            }
        }
        
        boolean zipSet = false;
        if(partner != null && (
                Constants.CONTENT_PARTNER_EVENTS.equals(partner.getName()) ||
                Constants.CONTENT_PARTNER_PLACES.equals(partner.getName()) ||
                Constants.CONTENT_PARTNER_RESTAURANTS.equals(partner.getName()))) {
            String zip = Config.getConfig(this).getZip();
            if(zip != null) {
                url = addParameterToUrl(url, "zip", zip);
                zipSet = true;
            }
        }
        
        // bhavan: let's add lattitude/longitude for section name "star" as well.
        //         star can be used for any general category
        if(!zipSet) {
            if(Constants.RESTAURANTS.equals(categoryName) ||
               Constants.PLACES.equals(categoryName) ||               
               Constants.EVENTS.equals(categoryName) ||
               Constants.STAR.equals(categoryName)) {            
                url = addParameterToUrl(url, "lat", Double.valueOf(CurrentLocation.latitude()).toString());
                url = addParameterToUrl(url, "lon", Double.valueOf(CurrentLocation.longitude()).toString());
            }
        }
        
        return url;
    }
    
    private String addParameterToUrl(String url, String key, String value) {        
        if(url.contains(("?"))) return url + ("&" + key + "=" + value);
        return url + ("?" + key + "=" + value);
    }
}
