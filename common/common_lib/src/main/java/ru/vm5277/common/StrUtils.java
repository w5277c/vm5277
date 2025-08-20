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

import java.util.Collection;

public class StrUtils {
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
	
	public static String toString(int[] array) {
		if(null != array && 0!=array.length) {
			StringBuilder sb = new StringBuilder("");
			for(Object obj : array) {
				if(!obj.toString().isEmpty()) sb.append(obj).append(",");
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
}
