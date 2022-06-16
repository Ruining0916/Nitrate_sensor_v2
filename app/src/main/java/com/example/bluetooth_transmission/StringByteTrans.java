package com.example.bluetooth_transmission;

import java.io.UnsupportedEncodingException;

public class StringByteTrans {

	public static String str2HexStr(String str)    
	{      
	  
	    char[] chars = "0123456789ABCDEF".toCharArray();      
	    StringBuilder sb = new StringBuilder("");    
	    byte[] bs = str.getBytes();      
	    int bit;      
	        
	    for (int i = 0; i < bs.length; i++)    
	    {      
	        bit = (bs[i] & 0x0f0) >> 4;      
	        sb.append(chars[bit]);      
	        bit = bs[i] & 0x0f;      
	        sb.append(chars[bit]);    
	        sb.append(' ');    
	    }      
	    return sb.toString().trim();      
	}    
	    

	public static String hexStr2Str(String hexStr)    
	{      
	    String str = "0123456789ABCDEF";      
	    hexStr = hexStr.toUpperCase();
	    char[] hexs = hexStr.toCharArray();
	    
	    byte[] bytes = new byte[hexStr.length() / 2];      
	    // judge the input string good or not
        if (hexStr.length() % 2 == 1) {
        	return new String();
        }
        // judge the input string good or not
	    for(int i = 0; i < hexStr.length(); i++) {
	    	if((hexs[i] >= '0' && hexs[i] <= '9') || (hexs[i] >= 'A' && hexs[i] <= 'F')) {
            } else {
            	return new String();
            }
	    }
	    int n;      
	  
	    for (int i = 0; i < bytes.length; i++)    
	    {      
	        n = str.indexOf(hexs[2 * i]) * 16;      
	        n += str.indexOf(hexs[2 * i + 1]);
	        bytes[i] = (byte) (n & 0xff);
	    }      
	    return new String(bytes);      
	}    
	    
	

	public static String byte2HexStr(byte[] b)    
	{    
	    String stmp="";    
	    StringBuilder sb = new StringBuilder("");    
	    for (int n=0;n<b.length;n++)    
	    {    
	        stmp = Integer.toHexString(b[n] & 0xFF);    
	        sb.append((stmp.length()==1)? "0"+stmp : stmp);    
	        sb.append(" ");    
	    }    
	    return sb.toString().toUpperCase().trim();    
	}    
	    

	public static byte[] hexStr2Bytes(String src)    
	{    
	    int m=0,n=0;    
	    int l=src.length()/2;   
	    src = src.toUpperCase();
	    char[] hexs = src.toCharArray();
	    // judge the input string good or not
        if (src.length() % 2 == 1) {
        	return null;
        }
        // judge the input string good or not
	    for(int i = 0; i < src.length(); i++) {
	    	if((hexs[i] >= '0' && hexs[i] <= '9') || (hexs[i] >= 'A' && hexs[i] <= 'F')) {
            } else {
            	return null;
            }
	    }
	    
	    byte[] ret = new byte[l];    
	    for (int i = 0; i < l; i++)    
	    {    
	        m=i*2+1;    
	        n=m+1;    
	        ret[i] = Byte.decode("0x" + src.substring(i*2, m) + src.substring(m,n));    
	    }    
	    return ret;    
	}    
	

	public static byte[] Str2Bytes(String str)    
	{    
		if (str == null) {
            throw new IllegalArgumentException(
                    "Argument str ( String ) is null! ");
        }
        byte[] b = new byte[str.length() / 2];
        
        try {
			b = str.getBytes("US-ASCII");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return b; 
	}    
	  

    public static String Byte2String(byte[] bytearray) {
        String result = "";
        char temp;

        int length = bytearray.length;
        for (int i = 0; i < length; i++) {
            temp = (char) bytearray[i];
            result += temp;
        }
        return result;
    }
    

	public static String strToUnicode(String strText)    
	    throws Exception    
	{    
	    char c;    
	    StringBuilder str = new StringBuilder();    
	    int intAsc;    
	    String strHex;    
	    for (int i = 0; i < strText.length(); i++)    
	    {    
	        c = strText.charAt(i);    
	        intAsc = (int) c;    
	        strHex = Integer.toHexString(intAsc);    
	        if (intAsc > 128)    
	            str.append("\\u" + strHex);    
	        else // ��λ��ǰ�油00    
	            str.append("\\u00" + strHex);    
	    }    
	    return str.toString();    
	}    
	    

	public static String unicodeToString(String hex)    
	{    
	    int t = hex.length() / 6;    
	    StringBuilder str = new StringBuilder();    
	    for (int i = 0; i < t; i++)    
	    {    
	        String s = hex.substring(i * 6, (i + 1) * 6);    

	        String s1 = s.substring(2, 4) + "00";    

	        String s2 = s.substring(4);    

	        int n = Integer.valueOf(s1, 16) + Integer.valueOf(s2, 16);    

	        char[] chars = Character.toChars(n);    
	        str.append(new String(chars));    
	    }    
	    return str.toString();    
	}   
}
