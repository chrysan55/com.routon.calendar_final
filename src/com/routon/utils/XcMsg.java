package com.routon.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class XcMsg {
	public static final int MSG_CSSM_RES_INFO_REQ = 3104;
	public static final int MSG_CSSM_RES_INFO_ACK = 3105;
	public static final int MSG_CSSM_IMAGE_REQ = 3106;
	public static final int MSG_CSSM_IMAGE_ACK = 3107;
	public static final int MSG_CSSM_OTHER_GETFILE_COMMON_REQ = 3130;
	public static final int MSG_CSSM_OTHER_GETFILE_COMMON_ACK = 3131;
	public static final int	MSG_CSSM_GET_XML_REQ = 3158;
	public static final int	MSG_CSSM_GET_XML_ACK = 3159;
	public static final int	MSG_ONLINE_EXIT_PLAY = 3401;
	
	public XcMsg(String modname) {
		this(modname, true);
	}
	
	public XcMsg(String modname, boolean has_loop) {
		tnLoopInit(true);
		tnMessageInit(modname, has_loop);
	}
	
	public static int byteToInt(byte[] b) {  
		  
        int mask=0xff;  
        int temp=0;  
        int n=0;  
        for(int i=0;i<4;i++){  
           n<<=8;  
           temp=b[i]&mask;  
           n|=temp;  
       }  
	return n;  
	}
	
	public static String byteToString(byte[] data)
	{
		ByteArrayInputStream in = new ByteArrayInputStream(data);
		int len = in.available();
		byte[] buffin = new byte[len];
		try{
			in.read(buffin);
			in.close();
			return new String(buffin,0,len,"UTF-8");
		}
		catch(IOException e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public static byte[] stringToByte(String str)
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buff = null;
		try {
			out.write(str.getBytes("UTF-8"));
			buff = out.toByteArray();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return buff;
	}
	

	
	
	public static byte[] intToByteArray(int i) {   
		  byte[] result = new byte[4];   
		  result[0] = (byte)((i >> 24) & 0xFF);
		  result[1] = (byte)((i >> 16) & 0xFF);
		  result[2] = (byte)((i >> 8) & 0xFF); 
		  result[3] = (byte)(i & 0xFF);
		  return result;
		 }
	
	public native boolean tnModuleExists(String name);
	public native int tnMessageSend_sync(String target, int code, byte[] data);
	public native int tnMessageSendByte(String target, int code, byte[] data);
	public native int tnMessageSend(String target, int code, String data);
	public native void tnTypeInit();
	public native int tnLoopQuit();
	public native int tnLoopRun(XcMsgInterface callback);
	public native int tnLoopInit(boolean need_multi_thread);
	public native int tnMessageInit(String name, boolean has_loop);
	
	static {
	    System.loadLibrary("dbus-glib-java");
	}

}
