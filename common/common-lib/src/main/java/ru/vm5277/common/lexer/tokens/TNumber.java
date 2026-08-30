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

package ru.vm5277.common.lexer.tokens;

import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.lexer.TokenType;
import ru.vm5277.common.lexer.SourceBuffer;

public class TNumber extends Token {
	public TNumber(SourceBuffer sb) throws CompileException {
		super(sb);
		type = TokenType.NUMBER;
		
		if(sb.available(2) && '0'==sb.peek() && 'x'==(sb.peek(1)|0x20)) {
			// Шестнадцатеричные числа (0x...)
			sb.next(2);
			StringBuilder hex = new StringBuilder();
			while(sb.available() && isHexDigit(sb.peek())) {
				hex.append(sb.peek());
				sb.next();
			}
			if(0==hex.length()) {
				throw new CompileException("Invalid hexadecimal number");
			}
			else {
				try {
					try {value = Integer.parseInt(hex.toString(), 0x10);} catch (NumberFormatException e) {value = Long.parseLong(hex.toString(), 0x10);}
				}
				catch (NumberFormatException e) {
					throw new CompileException("Hexadecimal number too large");
				}
			}
		}
		else if(sb.available(2) && '0'==sb.peek() && 'b'==(sb.peek(1)|0x20)) {
			// Двоичные числа (0b...)
			sb.next(2);
			StringBuilder bin = new StringBuilder();
			while (sb.available() && isBinaryDigit(sb.peek())) {
				bin.append(isBinaryTrue(sb.peek()) ? '1' : '0');
				sb.next();
			}
			if(0==bin.length()) {
				throw new CompileException("Invalid binary number");
			}
			else {
				try {
					try {value = Integer.parseInt(bin.toString(), 0b10);} catch (NumberFormatException e) {value = Long.parseLong(bin.toString(), 0b10);}
				}
				catch (NumberFormatException e) {
					throw new CompileException("Binary number too large");
				}
			}
		}
		else if(sb.available() && '0'==sb.peek() && (sb.available(2) && '.'!=sb.peek(1))) {
            // Восьмеричные числа (0...)
			StringBuilder oct = new StringBuilder();
			oct.append(sb.peek());
			sb.next();
			while (sb.available() && isOctalDigit(sb.peek())) {
				oct.append(sb.peek());
				sb.next();
			}
			// Если после 0 нет цифр, это просто ноль
			if(oct.length() == 1) value = 0;
			else {
				try {
					try {value = Integer.parseInt(oct.toString(), 010);} catch (NumberFormatException e) {value = Long.parseLong(oct.toString(), 010);}
				}
				catch (NumberFormatException e) {
					throw new CompileException("Octal number too large");
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
				throw new CompileException("Invalid number format");
			}
		}
		
		length = sb.getPos()-sp.getPos();
	}

	private boolean isHexDigit(char ch) {
		return Character.isDigit(ch) || ((ch|0x20)>='a' && (ch|0x20)<='f');
	}
	
	private boolean isBinaryDigit(char ch) {
		return '0'==ch || '1'==ch || '!'==ch || '.'==ch;
	}
	private boolean isBinaryTrue(char ch) {
		return '1'==ch || '!'==ch;
	}

	private boolean isOctalDigit(char ch) {
		return ch>='0' && ch<='7';
	}
}
