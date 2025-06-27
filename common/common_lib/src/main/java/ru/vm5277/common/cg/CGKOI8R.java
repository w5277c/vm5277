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
package ru.vm5277.common.cg;

public class CGKOI8R {
	public	final	static	String	KOI8_R	=	"─│┌┐└┘├┤┬┴┼▀▄█▌▐░▒▓⌠■∙√≈≤≥ ⌡°²·÷═║╒ё╓╔╕╖╗╘╙╚╛╜╝╞╟╠╡Ё╢╣╤╥╦╧╨╩╪╫╬©" +
												"юабцдефгхийклмнопярстужвьызшэщчъЮАБЦДЕФГХИЙКЛМНОПЯРСТУЖВЬЫЗШЭЩЧЪ";

	
	public static String decode(String input) {
		if(input.trim().isEmpty()) return "";
		
		boolean inQuotes = false;
		int byteCount = 0;
		StringBuilder sb = new StringBuilder(".db ");
		
		for(int i=0; i<input.length(); i++) {
			char ch = input.charAt(i);

			if(ch=='\\') {
				if(i+1 < input.length()) {
					char next = input.charAt(i+1);
					byte escapedChar = 0;
					boolean isEscape = true;

					switch(next) {
						case 'n':  escapedChar = 0x0a; break;
						case 'r':  escapedChar = 0x0d; break;
						case 't':  escapedChar = 0x09; break;
						case '0':  escapedChar = 0x00; break;
						case '\"': escapedChar = 0x22; break;
						case '\\': escapedChar = 0x5c; break;
						default:  isEscape = false;
					}

					if(isEscape) {
						if (inQuotes) {
							sb.append("\"");
							inQuotes = false;
						}

						if(byteCount>0) sb.append(",");
						sb.append(String.format("0x%02x", escapedChar));
						byteCount++;
						i++;
						continue;
					}
				}
			}
			
			if(ch>=0x20 && ch<=0x7f && ch!='\"') {
				if(!inQuotes) {
					if (byteCount>0) sb.append(",");
					sb.append("\"");
					inQuotes = true;
				}
				sb.append(ch);
			}
			else {
				if(inQuotes) {
					sb.append("\"");
					inQuotes = false;
				}

				int b = 0x80+KOI8_R.indexOf(ch);
				if(-1 == b) b = ch;
				if(byteCount>0) sb.append(",");
				sb.append(String.format("0x%02x", b & 0xff));
			}
			byteCount++;
		}

		if(inQuotes) sb.append("\"");
		
		sb.append(",0x00"); byteCount++;
		if(0x01 == (byteCount&0x01)) {
            sb.append(",0x00");
        }

		return sb.toString();
	}
}
