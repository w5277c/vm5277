/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
22.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.tokens;

import ru.vm5277.j8b.compiler.tokens.enums.TokenType;
import java.math.BigDecimal;
import java.math.BigInteger;
import ru.vm5277.j8b.compiler.ParseError;

public class TNumber extends Token {
	public TNumber(String src, int pos, int line, int column) {
		type = TokenType.NUMBER;
		
		endPos = pos;
		this.line = line;
		this.column = column;
		
		if (endPos+1 < src.length() && '0'==src.charAt(endPos) && ('x'==src.charAt(endPos+1) || 'X'==src.charAt(endPos+1))) {
			// Шестнадцатеричные числа (0x...)
			endPos+=2;
			column+=2;
			StringBuilder hex = new StringBuilder();
			while (endPos<src.length() && isHexDigit(src.charAt(endPos))) {
				hex.append(src.charAt(endPos));
				endPos++;
				column++;
			}
			if (0==hex.length()) {
				throw new ParseError("Invalid hexadecimal number", line, this.column);
			}
			try {
				value = Integer.parseInt(hex.toString(), 0x10);
			}
			catch (NumberFormatException e) {
				throw new ParseError("Hexadecimal number too large", line, this.column);
			}
		}
		else if (endPos+1 < src.length() && '0'==src.charAt(endPos) && ('b'==src.charAt(endPos+1) || 'B'==src.charAt(endPos+1))) {
			// Двоичные числа (0b...)
			endPos+=2;
			column+=2;
			StringBuilder bin = new StringBuilder();
			while (endPos<src.length() && isBinaryDigit(src.charAt(endPos))) {
				bin.append(src.charAt(endPos));
				endPos++;
				column++;
			}
			if (0==bin.length()) {
				throw new ParseError("Invalid binary number", line, this.column);
			}
			try {
				value = Integer.parseInt(bin.toString(), 0b10);
			}
			catch (NumberFormatException e) {
				throw new ParseError("Binary number too large", line, this.column);
			}
		}
		else if (endPos<src.length() && '0'==src.charAt(endPos)) {
            // Восьмеричные числа (0...)
			StringBuilder oct = new StringBuilder();
			oct.append(src.charAt(endPos));
			endPos++;
			column++;
			while (endPos<src.length() && isOctalDigit(src.charAt(endPos))) {
				oct.append(src.charAt(endPos));
				endPos++;
				column++;
			}
			// Если после 0 нет цифр, это просто ноль
			if (oct.length() == 1) {
				value = 0;
			}
			else {
				try {
					value = Integer.parseInt(oct.toString(), 010);
				}
				catch (NumberFormatException e) {
					throw new ParseError("Octal number too large", line, this.column);
				}
			}
		}
		else {
			// Десятичные числа
			StringBuilder dec = new StringBuilder();
			boolean hasDecimalPoint = false;
    
			while (endPos<src.length()) {
				char ch = src.charAt(endPos);
				if (Character.isDigit(ch)) {
					dec.append(ch);
					endPos++;
					column++;
				}
				else if ('.'==ch && !hasDecimalPoint) {
					dec.append(ch);
					hasDecimalPoint = true;
					endPos++;
					column++;
				}
				else {
					break;
				}
			}
    
			// Обработка суффиксов (только F и D для float/double)
			boolean isFloat = false;
			if (endPos<src.length()) {
				char suffix = Character.toUpperCase(src.charAt(endPos));
				if ('F'==suffix) {
					isFloat = true;
					endPos++;
					column++;
				}
				else if ('D'==suffix) {
					endPos++;
					column++;
				}
			}
    
			try {
				if (hasDecimalPoint || isFloat) {
					value = new BigDecimal(dec.toString());
				}
				else {
					String numStr = dec.toString();
		            // Пробуем распарсить как int
				    try {
						value = Integer.parseInt(numStr);
					}
					catch (NumberFormatException e) {
						// Если не влезает в int, пробуем long
						try {
							value = Long.parseLong(numStr);
		                }
						catch (NumberFormatException e2) {
							// Если не влезает даже в long, используем BigInteger
							value =  new BigInteger(numStr);
						}
					}
				}
			}
			catch (NumberFormatException e) {
				throw new ParseError("Invalid number format", line, this.column);
			}
		}    
	}

	private boolean isHexDigit(char ch) {
		return Character.isDigit(ch) || (ch>='a' && ch<='f') || (ch>='A' && ch<='F');
	}
	
	private boolean isBinaryDigit(char ch) {
		return '0'==ch || '1'==ch;
	}

	private boolean isOctalDigit(char ch) {
		return ch>='0' && ch<='7';
	}
}
