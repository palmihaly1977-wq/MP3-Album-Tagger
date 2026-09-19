package hu.mp3albumtagger;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
 private WebView web;
 private ValueCallback<Uri[]> files;
 private static final int FILES=7001;

 @Override public void onCreate(Bundle b){
  super.onCreate(b);
  setContentView(R.layout.activity_main);
  web=findViewById(R.id.web);
  WebSettings s=web.getSettings();
  s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setDatabaseEnabled(true);
  s.setAllowFileAccess(true); s.setAllowContentAccess(true);
  s.setBuiltInZoomControls(true); s.setDisplayZoomControls(false); s.setSupportZoom(true);
  s.setUseWideViewPort(true); s.setLoadWithOverviewMode(false); s.setTextZoom(100);
  web.addJavascriptInterface(new MetalArchivesBridge(),"MetalArchivesNative");
  web.setWebViewClient(new WebViewClient(){
   @Override public void onPageFinished(WebView v,String url){
    super.onPageFinished(v,url);
    String js="(function(){window.fetchMetalArchivesLyrics=async function(artist,album,title){try{if(!window.MetalArchivesNative)return '';var r=JSON.parse(MetalArchivesNative.lyrics(String(artist||''),String(title||'')));if(!r.ok)return '';var d=document.createElement('textarea');d.innerHTML=String(r.lyricsHtml||'').replace(/<br\\s*\\/?>/gi,'\\n');return d.value.replace(/<[^>]+>/g,'').trim();}catch(e){return '';}};window.METAL_ARCHIVES_NATIVE=true;})();";
    v.evaluateJavascript(js,null);
   }
  });
  web.setWebChromeClient(new WebChromeClient(){
   @Override public boolean onShowFileChooser(WebView v,ValueCallback<Uri[]> cb,FileChooserParams p){
    if(files!=null)files.onReceiveValue(null); files=cb;
    Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION|Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
    try{startActivityForResult(i,FILES);}catch(Exception e){files=null;return false;} return true;
   }
  });
  web.loadUrl("file:///android_asset/tagger_v3_18.html");
 }
 private static String read(InputStream in)throws Exception{
  BufferedReader br=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8)); StringBuilder b=new StringBuilder(); String l;
  while((l=br.readLine())!=null)b.append(l).append('\n'); return b.toString();
 }
 private static String get(String u)throws Exception{
  HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();
  c.setConnectTimeout(12000); c.setReadTimeout(15000); c.setInstanceFollowRedirects(true);
  c.setRequestProperty("User-Agent","Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 Chrome/124 Mobile Safari/537.36");
  c.setRequestProperty("Accept","application/json,text/html,*/*");
  int code=c.getResponseCode(); if(code<200||code>=300)throw new Exception("HTTP "+code);
  try(InputStream in=c.getInputStream()){return read(in);} finally{c.disconnect();}
 }
 public class MetalArchivesBridge {
  @JavascriptInterface public String lyrics(String artist,String title){
   JSONObject out=new JSONObject();
   try{
    String q="https://www.metal-archives.com/search/ajax-advanced/searching/songs?bandName="+URLEncoder.encode(artist,"UTF-8")+"&songTitle="+URLEncoder.encode(title,"UTF-8");
    String body=get(q); JSONObject json=new JSONObject(body); JSONArray rows=json.optJSONArray("aaData");
    if(rows==null||rows.length()==0){out.put("ok",false);out.put("reason","no-result");return out.toString();}
    Pattern p=Pattern.compile("lyricsLink_(\\d+)",Pattern.CASE_INSENSITIVE);
    for(int i=0;i<rows.length();i++){
     JSONArray row=rows.optJSONArray(i); if(row==null)continue;
     Matcher m=p.matcher(row.toString()); if(!m.find())continue;
     String id=m.group(1);
     String lyr=get("https://www.metal-archives.com/release/ajax-view-lyrics/id/"+id);
     if(lyr!=null&&lyr.trim().length()>10){
      out.put("ok",true);out.put("id",id);out.put("lyricsHtml",lyr);return out.toString();
     }
    }
    out.put("ok",false);out.put("reason","no-lyrics-id");
   }catch(Exception e){try{out.put("ok",false);out.put("reason",e.getClass().getSimpleName()+": "+e.getMessage());}catch(Exception ignored){}}
   return out.toString();
  }
  @JavascriptInterface public String fetchPublic(String url){
   JSONObject out=new JSONObject();
   try{
    if(url==null||!url.startsWith("https://www.metal-archives.com/"))throw new SecurityException("Metal Archives only");
    out.put("ok",true);out.put("body",get(url));
   }catch(Exception e){try{out.put("ok",false);out.put("reason",e.getClass().getSimpleName()+": "+e.getMessage());}catch(Exception ignored){}}
   return out.toString();
  }
 }
 @Override public void onBackPressed(){if(web!=null&&web.canGoBack())web.goBack();else super.onBackPressed();}
 @Override protected void onActivityResult(int request,int result,Intent data){
  super.onActivityResult(request,result,data); if(request!=FILES||files==null)return; Uri[] out=null;
  if(result==RESULT_OK&&data!=null&&data.getData()!=null){Uri u=data.getData();try{getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);}catch(Exception ignored){}out=new Uri[]{u};}
  files.onReceiveValue(out);files=null;
 }
}