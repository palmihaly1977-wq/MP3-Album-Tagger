package hu.mp3albumtagger;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
 private WebView web;
 private ValueCallback<Uri[]> files;
 private static final int FILES=7001;

 @Override public void onCreate(Bundle b){
  super.onCreate(b);
  setContentView(R.layout.activity_main);
  web=findViewById(R.id.web);
  WebSettings s=web.getSettings();
  s.setJavaScriptEnabled(true);
  s.setDomStorageEnabled(true);
  s.setDatabaseEnabled(true);
  s.setAllowFileAccess(true);
  s.setAllowContentAccess(true);
  s.setBuiltInZoomControls(true);
  s.setDisplayZoomControls(false);
  s.setSupportZoom(true);
  s.setUseWideViewPort(true);
  s.setLoadWithOverviewMode(false);
  s.setTextZoom(100);
  web.setWebViewClient(new WebViewClient());
  web.setWebChromeClient(new WebChromeClient(){
   @Override public boolean onShowFileChooser(WebView v,ValueCallback<Uri[]> cb,FileChooserParams p){
    if(files!=null)files.onReceiveValue(null);
    files=cb;
    Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
    try{startActivityForResult(i,FILES);}catch(Exception e){files=null;return false;}
    return true;
   }
  });
  web.loadUrl("file:///android_asset/tagger_v3_18.html");
 }
 @Override public void onBackPressed(){
  if(web!=null&&web.canGoBack())web.goBack(); else super.onBackPressed();
 }
 @Override protected void onActivityResult(int request,int result,Intent data){
  super.onActivityResult(request,result,data);
  if(request!=FILES||files==null)return;
  Uri[] out=null;
  if(result==RESULT_OK&&data!=null){
   if(data.getData()!=null){
    Uri u=data.getData();
    try{getContentResolver().takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);}catch(Exception ignored){}
    out=new Uri[]{u};
   }
  }
  files.onReceiveValue(out);files=null;
 }
}
