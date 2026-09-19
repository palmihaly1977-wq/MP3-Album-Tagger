package hu.mp3albumtagger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Id3v24Writer {
 static byte[] synchsafe(int n){
  if(n<0||n>0x0FFFFFFF)throw new IllegalArgumentException("ID3 size out of range");
  return new byte[]{(byte)((n>>21)&0x7f),(byte)((n>>14)&0x7f),(byte)((n>>7)&0x7f),(byte)(n&0x7f)};
 }
 static byte[] concat(byte[]... xs)throws IOException{
  ByteArrayOutputStream o=new ByteArrayOutputStream();for(byte[] x:xs)if(x!=null)o.write(x);return o.toByteArray();
 }
 static byte[] frame(String id,byte[] payload)throws IOException{
  if(payload==null||payload.length==0)return new byte[0];
  return concat(id.getBytes(StandardCharsets.ISO_8859_1),synchsafe(payload.length),new byte[]{0,0},payload);
 }
 static byte[] utf8(String s){return s==null?new byte[0]:s.getBytes(StandardCharsets.UTF_8);}
 static byte[] text(String s)throws IOException{return concat(new byte[]{3},utf8(s));}
 static byte[] textList(List<String> xs)throws IOException{
  ByteArrayOutputStream o=new ByteArrayOutputStream();o.write(3);
  boolean first=true;for(String s:xs){if(!first)o.write(0);o.write(utf8(s));first=false;}return o.toByteArray();
 }
 static byte[] txxx(String desc,String value)throws IOException{
  return concat(new byte[]{3},utf8(desc),new byte[]{0},utf8(value));
 }
 static byte[] uslt(String lyrics)throws IOException{
  return concat(new byte[]{3},new byte[]{'e','n','g'},new byte[]{0},utf8(lyrics));
 }
 static byte[] comm(String value)throws IOException{
  return concat(new byte[]{3},new byte[]{'e','n','g'},utf8("Album information"),new byte[]{0},utf8(value));
 }
 static byte[] apic(byte[] image,String mime)throws IOException{
  if(image==null||image.length==0)return new byte[0];
  return concat(new byte[]{3},mime.getBytes(StandardCharsets.ISO_8859_1),new byte[]{0},new byte[]{3},new byte[]{0},image);
 }
 public static byte[] build(Id3Plan p,byte[] cover,String mime)throws IOException{
  ByteArrayOutputStream frames=new ByteArrayOutputStream();
  for(Map.Entry<String,List<String>> e:p.frames.entrySet()){
   String k=e.getKey();List<String> v=e.getValue();if(v==null||v.isEmpty())continue;
   if(k.startsWith("TXXX:"))frames.write(frame("TXXX",txxx(k.substring(5),v.get(0))));
   else if(k.equals("USLT"))frames.write(frame("USLT",uslt(v.get(0))));
   else if(k.equals("COMM"))frames.write(frame("COMM",comm(v.get(0))));
   else if(k.equals("APIC:SOURCE")){} // actual bytes below
   else if(k.equals("TMCL")||k.equals("TIPL"))frames.write(frame(k,textList(v)));
   else if(k.equals("TCOM")||k.equals("TEXT"))frames.write(frame(k,textList(v)));
   else frames.write(frame(k,text(v.get(0))));
  }
  if(cover!=null&&cover.length>0)frames.write(frame("APIC",apic(cover,mime==null?"image/jpeg":mime)));
  byte[] body=frames.toByteArray();
  return concat(new byte[]{'I','D','3',4,0,0},synchsafe(body.length),body);
 }

 // Copy source audio while replacing/removing only the leading ID3v2 tag.
 public static void writeTaggedCopy(InputStream src,OutputStream dst,byte[] newTag)throws IOException{
  Id3BinaryInspector.requireValid(newTag);
  BufferedInputStream in=new BufferedInputStream(src,65536);
  in.mark(10);
  byte[] h=new byte[10];int got=in.read(h);
  if(got==10&&h[0]=='I'&&h[1]=='D'&&h[2]=='3'){
   int major=h[3]&0xff;
   if(major<2||major>4)throw new IOException("Unsupported source ID3 version: 2."+major);
   for(int i=6;i<10;i++)if((h[i]&0x80)!=0)throw new IOException("Malformed source ID3: non-synchsafe size");
   int old=((h[6]&0x7f)<<21)|((h[7]&0x7f)<<14)|((h[8]&0x7f)<<7)|(h[9]&0x7f);
   long skip=old;
   while(skip>0){
    long n=in.skip(skip);
    if(n>0){skip-=n;continue;}
    if(in.read()<0)throw new EOFException("Truncated source ID3 body");
    skip--;
   }
   // ID3v2.4 may declare a 10-byte footer. It must be complete before any output is written.
   if(major==4&&(h[5]&0x10)!=0){
    for(int i=0;i<10;i++)if(in.read()<0)throw new EOFException("Truncated source ID3 footer");
   }
  }else{
   in.reset();
  }
  dst.write(newTag);
  byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)dst.write(b,0,n);
  dst.flush();
 }
}
