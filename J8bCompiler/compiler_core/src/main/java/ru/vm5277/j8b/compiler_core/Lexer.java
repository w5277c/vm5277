/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
22.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler_core;

import ru.vm5277.j8b.compiler.common.SourcePosition;
import ru.vm5277.j8b.compiler_core.messages.MessageContainer;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import ru.vm5277.j8b.compiler_core.enums.Keyword;
import ru.vm5277.j8b.compiler_core.tokens.TChar;
import ru.vm5277.j8b.compiler_core.tokens.TDelimiter;
import ru.vm5277.j8b.compiler_core.tokens.TKeyword;
import ru.vm5277.j8b.compiler_core.tokens.TNumber;
import ru.vm5277.j8b.compiler_core.tokens.TOpearator;
import ru.vm5277.j8b.compiler_core.tokens.TString;
import ru.vm5277.j8b.compiler_core.tokens.Token;
import ru.vm5277.j8b.compiler_core.enums.TokenType;
import ru.vm5277.j8b.compiler.common.messages.ErrorMessage;
import ru.vm5277.j8b.compiler.common.messages.MessageOwner;
import ru.vm5277.j8b.compiler_core.tokens.TLabel;
import ru.vm5277.j8b.compiler_core.tokens.TNote;

public class Lexer {
	private	final	MessageContainer	mc;
	private			SourceBuffer		sb;
	private final	List<Token>			tokens	= new ArrayList<>();
	
	public Lexer(Reader reader, MessageContainer mc) throws IOException {
		mc.setOwner(MessageOwner.LEXER);
		
		StringBuilder stringBuilder = new StringBuilder();
		char[] buffer = new char[4*1024];
		for (int length; (length = reader.read(buffer)) != -1;) {
			stringBuilder.append(buffer, 0, length);
		}
		sb = new SourceBuffer(stringBuilder.toString());

		this.mc = mc;

		while (sb.hasNext()) {
			// Пропускаем пробелы
			if(skipWhiteSpaces(sb)) continue;
			// Пропускаем комментарии
			if(skipComment(sb, mc)) continue;
			
			char ch = sb.getChar();			
			//Блок данных
			if(sb.hasNext(1) && '#'==ch) {
				sb.next();
				SourcePosition sp = sb.snapSP();
				char type = sb.getChar();
				sb.next();
				
				switch(type) {
					case 'p':
						tokens.add(new TNote(sb, mc));
						continue;
					case ';':
						mc.add(new ErrorMessage("Empty #block", sp));
						continue;
					default: {
						mc.add(new ErrorMessage("Unsupported #block: '" + type + "'", sp));
						TNote.skipToken(sb, mc);
					}
				}
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
            
			// Операторы
			Token token = TOpearator.parse(sb);
            if (null != token) {
                tokens.add(token);
                continue;
            }

			// Идентификаторы и ключевые слова
			if (Character.isLetter(ch) || '_'==ch) {
				token = new TKeyword(sb);

				// Добавляем проверку на метку
				if (!(token.getValue() instanceof Keyword) && sb.hasNext() && ':'==sb.getChar()) {
					token = new TLabel(token.getStringValue(), sb);
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
