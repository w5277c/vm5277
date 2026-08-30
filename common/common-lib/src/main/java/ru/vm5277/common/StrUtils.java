/*
 * Copyright 2025 konstantin@5277.ru
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.vm5277.common;

import ru.vm5277.common.lexer.SourceBuffer;
import ru.vm5277.common.lexer.SourcePosition;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Properties;
import ru.vm5277.common.exceptions.CompileException;

public class StrUtils {
	private	final	static	char[]	ALPHABET	= "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();
	private			static	int[]	toInt		= new int[128];
	static {
		for(int i=0; i< ALPHABET.length; i++){
			toInt[ALPHABET[i]]= i;
		}
	}

    public static String readVersion(Class<?> clazz) {
		String packagePath = clazz.getPackage().getName().replace('.', '/');
		String resourcePath = packagePath + "/version.properties";
		try (InputStream input = clazz.getClassLoader().getResourceAsStream(resourcePath)) {
			if(input != null) {
				Properties prop = new Properties();
				prop.load(input);
				return prop.getProperty("version");
			}
		}
		catch (IOException e) {
		}
		return " UNKNOWN";
    }

	public static String toString(Collection collection) {
		if(null != collection && !collection.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			for(Object obj : collection) {
				if(!obj.toString().isEmpty()) sb.append(obj).append(",");
			}
			if(0!=sb.length()) sb.deleteCharAt(sb.length()-1);
			return sb.toString();
		}
		return "";
	}
	public static String toString(Object[] array) {
		if(null != array && 0!=array.length) {
			StringBuilder sb = new StringBuilder();
			for(Object obj : array) {
				if(!obj.toString().isEmpty()) sb.append(obj).append(",");
			}
			if(0!=sb.length()) sb.deleteCharAt(sb.length()-1);
			return sb.toString();
		}
		return "";
	}
	
	public static String toString(byte[] array) {
		if(null != array && 0!=array.length) {
			StringBuilder sb = new StringBuilder("");
			for(byte b : array) {
				sb.append(b&0xff).append(",");
			}
			if(0!=sb.length()) sb.deleteCharAt(sb.length()-1);
			return sb.toString();
		}
		return "";
	}

	public static String toString(int[] array) {
		if(null != array && 0!=array.length) {
			StringBuilder sb = new StringBuilder("");
			for(int i : array) {
				sb.append(i).append(",");
			}
			if(0!=sb.length()) sb.deleteCharAt(sb.length()-1);
			return sb.toString();
		}
		return "";
	}

	public static String toString(long[] array) {
		if(null != array && 0!=array.length) {
			StringBuilder sb = new StringBuilder("");
			for(long l : array) {
				sb.append(l).append(",");
			}
			if(0!=sb.length()) sb.deleteCharAt(sb.length()-1);
			return sb.toString();
		}
		return "";
	}

	public static String escapeChar(char c) {
		switch (c) {
			case '\n': return "\\n";
			case '\t': return "\\t";
			case '\r': return "\\r";
			case '\b': return "\\b";
			case '\f': return "\\f";
			case '\'': return "\\'";
			case '\"': return "\\\"";
			case '\\': return "\\\\";
			default:
				return Character.isISOControl(c) ? String.format("\\u%04x", (int) c) : String.valueOf(c);
		}
	}
	
	public static Object parseNum(SourceBuffer sb, SourcePosition sp) throws CompileException {
		Object value = null;
		
		if (sb.available(2) && '0'==sb.peek() && 'x'==(sb.peek(1)|0x20)) {
			// Шестнадцатеричные числа (0x...)
			sb.next(2);
			StringBuilder hex = new StringBuilder();
			while (sb.available() && (Character.isDigit(sb.peek()) || ((sb.peek()|0x20)>='a' && (sb.peek()|0x20)<='f'))) {
				hex.append(sb.peek());
				sb.next();
			}
			if (0==hex.length()) {
				throw new CompileException("Invalid hexadecimal number", sp);
			}
			else {
				try {
					try {value = Integer.parseInt(hex.toString(), 0x10);} catch (NumberFormatException e) {value = Long.parseLong(hex.toString(), 0x10);}
				}
				catch (NumberFormatException e) {
					throw new CompileException("Hexadecimal number too large", sp);
				}
			}
		}
		else if (sb.available(2) && '0'==sb.peek() && 'b'==(sb.peek(1)|0x20)) {
			// Двоичные числа (0b...)
			sb.next(2);
			StringBuilder bin = new StringBuilder();
			while (sb.available() &&  ('0'==sb.peek() || '1'==sb.peek() || '!'==sb.peek() || '.'==sb.peek())) {
				bin.append(('1'==sb.peek() || '!'==sb.peek()) ? '1' : '0');
				sb.next();
			}
			if (0==bin.length()) {
				throw new CompileException("Invalid binary number", sp);
			}
			else {
				try {
					try {value = Integer.parseInt(bin.toString(), 0b10);} catch (NumberFormatException e) {value = Long.parseLong(bin.toString(), 0b10);}
				}
				catch (NumberFormatException e) {
					throw new CompileException("Binary number too large", sp);
				}
			}
		}
		else if (sb.available() && '0'==sb.peek() && (sb.available(2) && '.'!=sb.peek(1))) {
            // Восьмеричные числа (0...)
			StringBuilder oct = new StringBuilder();
			oct.append(sb.peek());
			sb.next();
			while (sb.available() && (sb.peek()>='0' && sb.peek()<='7')) {
				oct.append(sb.peek());
				sb.next();
			}
			// Если после 0 нет цифр, это просто ноль
			if (oct.length() == 1) value = 0;
			else {
				try {
					try {value = Integer.parseInt(oct.toString(), 010);} catch (NumberFormatException e) {value = Long.parseLong(oct.toString(), 010);}
				}
				catch (NumberFormatException e) {
					throw new CompileException("Octal number too large", sp);
				}
			}
		}
		else {
			// Десятичные числа
			StringBuilder dec = new StringBuilder();
			boolean hasDecimalPoint = false;
    
			while (sb.available()) {
				char ch = sb.peek();
				if (Character.isDigit(ch)) {
					dec.append(ch);
					sb.next();
				}
				else if (sb.available(2) && '.'==ch && '.'==sb.peek(1)) {
					break;	//Это Demimiter.RANGE
				}
				else if ('.'==ch && !hasDecimalPoint) {
					dec.append(ch);
					hasDecimalPoint = true;
					sb.next();
				}
				else {
					break;
				}
			}
    
			// Обработка суффиксов (только F и D для float/double)
			boolean isFloat = false;
			if (sb.available()) {
				if ('f'==(sb.peek()|0x20)) {
					isFloat = true;
					sb.next();
				}
				else if ('d'==(sb.peek()|0x20)) {
					sb.next();
				}
			}
    
			try {
				if (hasDecimalPoint || isFloat) {
					value = new Double(dec.toString());
				}
				else {
					String numStr = dec.toString();
		            try {value = Integer.parseInt(numStr);} catch (NumberFormatException e) {value = Long.parseLong(numStr);}
				}
			}
			catch (NumberFormatException e) {
				throw new CompileException("Invalid number format", sp);
			}
		}    
		return value;
	}
	
	public static byte[] parseBase64Binary(String _base64String) {
		int delta = _base64String.endsWith( "==" ) ? 2 : _base64String.endsWith( "=" ) ? 1 : 0;
		byte[] result = new byte[_base64String.length()*3/4 - delta];
		int mask=0xff;
		int index=0x00;
		for(int pos=0; pos<_base64String.length(); pos+=4){
			int c0 = toInt[_base64String.charAt(pos)];
			int c1 = toInt[_base64String.charAt(pos+1)];
			result[index++]=(byte)(((c0 << 2) | (c1 >> 4)) & mask);
			if(index>=result.length){
				return result;
			}
			int c2 = toInt[_base64String.charAt(pos+2)];
			result[index++]=(byte)(((c1 << 4) | (c2 >> 2)) & mask);
			if(index>=result.length){
				return result;
			}
			int c3 = toInt[_base64String.charAt(pos+3)];
			result[index++]=(byte)(((c2 << 6) | c3) & mask);
		}
		return result;
	}

	public static String printBase64Binary(byte _bytes[]) {
		int size = _bytes.length;
		char[] buffer = new char[((size+2)/3)*4];
		int index=0;
		int pos=0;
		while(pos<size){
			byte b0 = _bytes[pos++];
			byte b1 = (pos < size) ? _bytes[pos++] : 0;
			byte b2 = (pos < size) ? _bytes[pos++] : 0;

			int mask = 0x3F;
			buffer[index++] = ALPHABET[(b0 >> 2) & mask];
			buffer[index++] = ALPHABET[((b0 << 4) | ((b1 & 0xFF) >> 4)) & mask];
			buffer[index++] = ALPHABET[((b1 << 2) | ((b2 & 0xFF) >> 6)) & mask];
			buffer[index++] = ALPHABET[b2 & mask];
		}
		switch(size % 3){
			case 1: buffer[--index]  = '=';
			case 2: buffer[--index]  = '=';
		}
		return new String(buffer);
	}
	
	public static byte[] parseHexBinary(String _hexString) {
		if(null==_hexString || _hexString.isEmpty()) {
			return new byte[0x00];
		}

		byte[] result = new byte[_hexString.length() / 0x02];
		for(int pos = 0; pos < result.length; pos++) {
			String substr = _hexString.substring(pos * 0x02, pos * 0x02 + 0x02).toLowerCase();
			result[pos] = (byte)Integer.parseInt(substr, 16);
		}
		return result;
	}

	public static String printHexBinary(byte[] _bytes) {
		return printHexBinary(_bytes, 0x00, _bytes.length);
	}
	public static String printHexBinary(byte[] _bytes, int _offset, int _len) {
		if(null==_bytes) {
			return "";
		}

		StringBuilder result = new StringBuilder();
		for(int pos = 0; pos<_len && (pos+_offset)<_bytes.length; pos++) {
			String num = Integer.toHexString(_bytes[pos+_offset] & 0xff).toLowerCase();
			if(num.length()<0x02) {
				result.append("0");
			}
			result.append(num);
		}
		return result.toString();
	}
}
