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
    Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
    i.addCategory(Intent.CATEGORY_OPENABLE);
    i.setType("*/*");
    i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);
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
   if(data.getClipData()!=null){
    int n=data.getClipData().getItemCount();out=new Uri[n];
    for(int i=0;i<n;i++)out[i]=data.getClipData().getItemAt(i).getUri();
   }else if(data.getData()!=null)out=new Uri[]{data.getData()};
  }
  files.onReceiveValue(out);files=null;
 }
}
