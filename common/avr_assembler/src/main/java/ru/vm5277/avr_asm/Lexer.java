/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
30.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm;

import java.io.File;
import java.io.FileInputStream;
import ru.vm5277.avr_asm.tokens.TDirective;
import ru.vm5277.common.messages.MessageContainer;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.avr_asm.tokens.TChar;
import ru.vm5277.avr_asm.tokens.TDelimiter;
import ru.vm5277.avr_asm.tokens.TNumber;
import ru.vm5277.avr_asm.tokens.TOpearator;
import ru.vm5277.avr_asm.tokens.TString;
import ru.vm5277.avr_asm.tokens.Token;
import ru.vm5277.common.SourceBuffer;
import ru.vm5277.common.messages.ErrorMessage;
import ru.vm5277.common.messages.MessageOwner;

public class Lexer {
	protected	final	MessageContainer	mc;
	protected			SourceBuffer		sb;
	protected	final	List<Token>			tokens	= new ArrayList<>();
	
	public Lexer(File sourceFile, Scope scope, MessageContainer mc) throws IOException {
		this.mc = mc;
		mc.setOwner(MessageOwner.LEXER);
		
		try (InputStreamReader isr = new InputStreamReader(new FileInputStream(sourceFile))) {
			StringBuilder stringBuilder = new StringBuilder();
			char[] buffer = new char[4*1024];
			for (int length; (length = isr.read(buffer)) != -1;) {
				stringBuilder.append(buffer, 0, length);
			}
			sb = new SourceBuffer(sourceFile, stringBuilder.toString());
		}

		while (sb.hasNext()) {
			// Пропускаем комментарий
			if(';'==sb.getChar() || '#'==sb.getChar()) {
				while (sb.hasNext() && '\n'!=sb.getChar()) {
					sb.next();
				}
				continue;
			}

			// Пропускаем комментарии
			if(skipComment(sb, mc)) continue;
			
			char ch = sb.getChar();

			//Пропускаем табуляцию с корректным счетом позиции
			if('\t'==ch) {
				sb.nextTab(Scope.getTabSize());
				continue;
			}
			
			//Новая строка
			if ('\n'==ch) {
				sb.incLine();
				sb.incPos();
				tokens.add(new Token(sb, TokenType.NEWLINE, null));
				continue;
			}
			if ('\r'==ch) {
				if (sb.hasNext(1) && '\n'==sb.getChar(1)) {
					sb.next();
				}
				sb.incLine();
				sb.next();
				tokens.add(new Token(sb, TokenType.NEWLINE, null));
				continue;
			}

			if (Character.isWhitespace(ch)) {
				sb.next();
				continue;
			}
			
			// Макро параметер
			if ('@'==ch) {
				sb.next();
				tokens.add(new Token(sb, TokenType.MACRO_PARAM, null));
				continue;
			}

			// Символ
			if ('\''==ch) {
				tokens.add(new TChar(sb, mc));
				continue;
			}
			
			// Строки
			if ('"'==ch) {
				tokens.add(new TString(sb, mc));
				continue;
			}
			
            // Числа
            if (Character.isDigit(ch)) {
				tokens.add(new TNumber(sb, mc));
				continue;
            }

			// Проверка на индексные регистры -X,-Y,-Z
			if ('-'==ch && sb.hasNext()) {
				ch = (char)(sb.getChar(1)|0x20);
				if ('x'==ch || 'y'==ch || 'z'==ch) {
					sb.next(2);
					Token token = new Token(sb, TokenType.INDEX_REG, "-"+ch);
					tokens.add(token);
					continue;
				}
			}
			
			// Операторы
			Token token = TOpearator.parse(sb);
            if (null != token) {
                tokens.add(token);
                continue;
            }

			if('.'==ch && sb.hasNext()) {
				sb.next();
				token = new TDirective(sb, mc);
				tokens.add(token);
				continue;
			}


			
			// Идентификаторы и ключевые слова
			if (Character.isLetter(ch) || '_'==ch) {
				StringBuilder stringBuilder = new StringBuilder();
				while (sb.hasNext() && (Character.isLetterOrDigit(sb.getChar()) || '_'==sb.getChar())) {
					stringBuilder.append(sb.getChar());
					sb.next();
				}
				
				String str = stringBuilder.toString().toLowerCase();
				if(null != scope.getInstrReader().getInstrById().get(str) || null != scope.getInstrReader().getInstrByMn().get(str)) {
					token = new Token(sb, TokenType.MNEMONIC, str.toLowerCase());
				}
				else if (sb.hasNext() && ':'==sb.getChar()) {
					token = new Token(sb, TokenType.LABEL, str);
					sb.next();
				}
				// Проверка на индексные регистры X+,Y+,Z+
				else if (str.equals("x") || str.equals("y") || str.equals("z")) {
					if (sb.hasNext() && '+'==sb.getChar()) {
						sb.next();
						token = new Token(sb, TokenType.INDEX_REG, str+"+");
					}
					else {
						token = new Token(sb, TokenType.INDEX_REG, str);
					}
				}
				else {
					token = new Token(sb, TokenType.ID, str);
				}
				tokens.add(token);
				continue;
			}
			
			// Разделители
			token = TDelimiter.parse(sb);
            if (null != token) {
                tokens.add(token);
                continue;
            }
			SourcePosition sp = sb.snapSP();
			sb.next();
			mc.add(new ErrorMessage("Unexpected character: '" + ch + "'", sp));
        }
        tokens.add(new Token(sb, TokenType.NEWLINE, null));
		tokens.add(new Token(sb, TokenType.EOF, null));
	}
	
	public List<Token> getTokens() {
		return tokens;
	}
	
	public void print() {
		for(Token token : tokens) {
			System.out.print(token.toString());
			if(TokenType.NEWLINE == token.getType()) {
				System.out.println();
			}
		}
		System.out.println();
	}

	public static boolean skipWhiteSpaces(SourceBuffer sb) {
		char ch = sb.getChar();
		if (Character.isWhitespace(ch)) {
			if ('\n'==ch) {
				sb.incLine();
			}
			else if ('\r'==ch) {
				if (sb.hasNext(1) && '\n'==sb.getChar(1)) {
					sb.next();
				}
				sb.incLine();
			}
			else {
				sb.incColumn();
			}
			sb.incPos();
			return true;
		}
		return false;
	}
	
	public static boolean skipComment(SourceBuffer sb, MessageContainer mc) {
		char ch = sb.getChar();
		if ('/'==ch && sb.hasNext(1)) {
			if ('/'==sb.getChar(1)) {
				while (sb.hasNext() && '\n'!=sb.getChar()) {
					sb.next();
				}
				return true;
			}
			else if ('*'==sb.getChar(1)) {
				sb.next(2);
				while (sb.hasNext()) {
					ch = sb.getChar();
					if ('*'==ch && sb.hasNext(1) && '/'==sb.getChar(1)) {
						// Конец комментария
						sb.next(2);
						return true;
					}

					if ('\n'==ch) {
						sb.incLine();
					}
					else {
						sb.incColumn();
					}
					sb.incPos();
				}

				mc.add(new ErrorMessage("Unterminated block comment", sb.snapSP()));
				return true;
			}
		}
		return false;
	}
}
