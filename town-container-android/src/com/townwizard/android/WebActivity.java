package com.townwizard.android;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.MailTo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.townwizard.android.R;
import com.townwizard.android.utils.TownWizardConstants;

public class WebActivity extends Activity {

    private static final String sUpload = "components/com_shines/iuploadphoto.php";
    private String mUrlSite;
    private WebView mWebView;
    private ImageView mImageView;
    private TextView mTextView;
    private Button mUploadButton;
    private static final int sCAMERA_RESULT = 1;
    private static final int sGALLERY = 2;
    private static Uri sImagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        mUrlSite = extras.getString(TownWizardConstants.URL_SITE);
        String urlSection = extras.getString(TownWizardConstants.URL_SECTION);
        String partnerName = extras.getString(TownWizardConstants.PARTNER_NAME);
        if (partnerName.indexOf("Photos") != -1) {
            Log.d("photos", "photos");
            if (isUploadScriptExist(mUrlSite + sUpload)) {
                Log.d("WebActivity", "File exist");
                setContentView(R.layout.web_with_upload);
                mUploadButton = (Button) findViewById(R.id.bt_upload);

                mUploadButton.setOnClickListener(new View.OnClickListener() {

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
            } else {
                setContentView(R.layout.web);
            }

        } else {
            setContentView(R.layout.web);
        }
        mWebView = (WebView) findViewById(R.id.webview);
        mWebView.setWebViewClient(new TownWizardWebViewClient());
        mWebView.getSettings().setJavaScriptEnabled(true);
        if (extras.getParcelable(TownWizardConstants.HEADER_IMAGE) != null) {
            Bitmap bitmap = extras.getParcelable(TownWizardConstants.HEADER_IMAGE);
            mImageView = (ImageView) findViewById(R.id.iv_header_web);
            mImageView.setImageBitmap(bitmap);
            mTextView = (TextView) findViewById(R.id.tv_header_web);
            mTextView.setText(extras.getString(TownWizardConstants.PARTNER_NAME));
        }

        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setUseWideViewPort(true);

        if (urlSection.startsWith("http")) {
            mWebView.loadUrl(urlSection);
        } else {
            mWebView.loadUrl(mUrlSite + urlSection);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Intent uploadPhoto = new Intent(this, UploadPhotoActivity.class);
        uploadPhoto.putExtra(TownWizardConstants.URL, mUrlSite + sUpload);

        if (requestCode == sGALLERY) {
            Log.d("result is ", Integer.toString(resultCode));

            if (resultCode == RESULT_OK) {
                Uri imageUri = data.getData();

                uploadPhoto.putExtra(TownWizardConstants.IMAGE_URI, imageUri);
                uploadPhoto.putExtra(TownWizardConstants.SOURCE, "GALLERY");
                startActivity(uploadPhoto);

            } else {
                WebActivity.this.finish();
            }
        }
        if (requestCode == sCAMERA_RESULT) {

            if (resultCode == RESULT_OK) {
                Log.d("camera result", "start upload activity");
                Log.d("imagePath", sImagePath.toString());
                uploadPhoto.putExtra(TownWizardConstants.IMAGE_URI, sImagePath);
                uploadPhoto.putExtra(TownWizardConstants.SOURCE, "CAMERA");
                startActivity(uploadPhoto);
            } else {
                WebActivity.this.finish();
            }
        }
    }

    public static boolean isUploadScriptExist(String URLName) {
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
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d("URL", url);
            if (url.startsWith("http")) {
                view.loadUrl(url);
            } else {
                if (url.startsWith("mailto:")) {
                    mailSend(url);
                    return true;
                }
                if (url.startsWith("tel")) {
                    Intent dialIntent = new Intent(Intent.ACTION_DIAL,
                            Uri.parse(url));
                    startActivity(dialIntent);
                } else if (url.startsWith("APP30A:")) {
                    if (url.indexOf("SHOWMAP") != -1) {
                        showMap(url);
                    } else if (url.indexOf("FBCHECKIN") != -1) {
                        facebookCheckin();
                    }
                }
            }
            return true;
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
        String latlong = url.substring("APP30A:SHOWMAP:".length());
        Log.d("latlong", latlong);
        String latitude = latlong.substring(0, latlong.indexOf(":"));
        String longitude = latlong.substring(latlong.indexOf(":") + 1);
        Intent i = new Intent(WebActivity.this, MapViewActivity.class);
        i.putExtra(TownWizardConstants.LATITUDE, latitude);
        i.putExtra(TownWizardConstants.LONGITUDE, longitude);

        Drawable drawable = mImageView.getDrawable();
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();

        i.putExtra(TownWizardConstants.HEADER_IMAGE, bitmap);
        i.putExtra(TownWizardConstants.PARTNER_NAME, mTextView.getText().toString());
        startActivity(i);
    }

    private void facebookCheckin() {
        Intent i = new Intent(WebActivity.this, FacebookPlaceActivity.class);
        Drawable drawable = mImageView.getDrawable();
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
        i.putExtra(TownWizardConstants.HEADER_IMAGE, bitmap);
        i.putExtra(TownWizardConstants.PARTNER_NAME, mTextView.getText().toString());
        startActivity(i);
    }

    private void mailSend(String url) {
        String mt = "mailto:?";
        String u = url;
        u = u.substring("mailto:?".length());
        u = u.replaceAll(":", "%3A");
        u = u.replaceAll("/", "%2F");
        u = u.replaceAll(" ", "%20");
        MailTo mailTo = MailTo.parse(mt + u);
        Log.d("mailTo", mailTo.toString());

        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_EMAIL, mailTo.getTo());
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

}