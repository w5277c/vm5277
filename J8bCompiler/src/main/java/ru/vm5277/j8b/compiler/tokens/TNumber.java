/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
22.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.tokens;

import ru.vm5277.j8b.compiler.enums.TokenType;
import java.math.BigInteger;
import ru.vm5277.j8b.compiler.ParseError;
import ru.vm5277.j8b.compiler.SourceBuffer;
import ru.vm5277.j8b.compiler.messages.MessageContainer;

public class TNumber extends Token {
	public TNumber(SourceBuffer sb, MessageContainer mc) {
		super(sb);
		type = TokenType.NUMBER;
		
		if (sb.hasNext(1) && '0'==sb.getChar() && 'x'==(sb.getChar(1)|0x20)) {
			// Шестнадцатеричные числа (0x...)
			sb.next(2);
			StringBuilder hex = new StringBuilder();
			while (sb.hasNext() && isHexDigit(sb.getChar())) {
				hex.append(sb.getChar());
				sb.next();
			}
			if (0==hex.length()) {
				setError("Invalid hexadecimal number", mc);
				value = 0;
			}
			else {
				try {
					value = Integer.parseInt(hex.toString(), 0x10);
				}
				catch (NumberFormatException e) {
					setError("Hexadecimal number too large", mc);
					value = 0;
				}
			}
		}
		else if (sb.hasNext(1) && '0'==sb.getChar() && 'b'==(sb.getChar(1)|0x20)) {
			// Двоичные числа (0b...)
			sb.next(2);
			StringBuilder bin = new StringBuilder();
			while (sb.hasNext() && isBinaryDigit(sb.getChar())) {
				bin.append(isBinaryTrue(sb.getChar()) ? '1' : '0');
				sb.next();
			}
			if (0==bin.length()) {
				setError("Invalid binary number", mc);
				value = 0;
			}
			else {
				try {
					value = Integer.parseInt(bin.toString(), 0b10);
				}
				catch (NumberFormatException e) {
					setError("Binary number too large", mc);
					value = 0;
				}
			}
		}
		else if (sb.hasNext() && '0'==sb.getChar() && (sb.hasNext(1) && '.'!=sb.getChar(1))) {
            // Восьмеричные числа (0...)
			StringBuilder oct = new StringBuilder();
			oct.append(sb.getChar());
			sb.next();
			while (sb.hasNext() && isOctalDigit(sb.getChar())) {
				oct.append(sb.getChar());
				sb.next();
			}
			// Если после 0 нет цифр, это просто ноль
			if (oct.length() == 1) value = 0;
			else {
				try {
					value = Integer.parseInt(oct.toString(), 010);
				}
				catch (NumberFormatException e) {
					setError("Octal number too large", mc);
					value = 0;
				}
			}
		}
		else {
			// Десятичные числа
			StringBuilder dec = new StringBuilder();
			boolean hasDecimalPoint = false;
    
			while (sb.hasNext()) {
				char ch = sb.getChar();
				if (Character.isDigit(ch)) {
					dec.append(ch);
					sb.next();
				}
				else if (sb.hasNext(1) && '.'==ch && '.'==sb.getChar(1)) {
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
			if (sb.hasNext()) {
				if ('f'==(sb.getChar()|0x20)) {
					isFloat = true;
					sb.next();
				}
				else if ('d'==(sb.getChar()|0x20)) {
					sb.next();
				}
			}
    
			try {
				if (hasDecimalPoint || isFloat) {
					value = new Double(dec.toString());
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
				setError("Invalid number format", mc);
			}
		}    
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
