/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
22.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import ru.vm5277.j8b.compiler.enums.Keyword;
import ru.vm5277.j8b.compiler.tokens.TChar;
import ru.vm5277.j8b.compiler.tokens.TDelimiter;
import ru.vm5277.j8b.compiler.tokens.TKeyword;
import ru.vm5277.j8b.compiler.tokens.TNumber;
import ru.vm5277.j8b.compiler.tokens.TOpearator;
import ru.vm5277.j8b.compiler.tokens.TString;
import ru.vm5277.j8b.compiler.tokens.Token;
import ru.vm5277.j8b.compiler.enums.TokenType;
import ru.vm5277.j8b.compiler.tokens.TLabel;
import ru.vm5277.j8b.compiler.tokens.TNote;

public class Lexer {
	private			SourceBuffer	sb;
	private final	List<Token>		tokens			= new ArrayList<>();
	
	public Lexer(Reader reader) throws IOException {
		StringBuilder stringBuilder = new StringBuilder();
		char[] buffer = new char[4*1024];
		for (int length; (length = reader.read(buffer)) != -1;) {
			stringBuilder.append(buffer, 0, length);
		}
		sb = new SourceBuffer(stringBuilder.toString());

		while (sb.hasNext()) {
            char ch = sb.getChar();
			// Пропускаем пробелы (кроме \n)
			if (Character.isWhitespace(ch)) {
				if ('\n'==ch) {
					sb.nextLine();
				}
				else if ('\r'==ch) {
					if (sb.hasNext(1) && '\n'==sb.getChar(1)) {
						sb.next();
					}
					sb.nextLine();
		        }
				else {
					sb.incColumn();
				}
				sb.incPos();
				continue;
			}
            
			// 2. Пропускаем комментарии
			if ('/'==ch && sb.hasNext(1)) {
				if ('/'==sb.getChar(1)) {
				    while (sb.hasNext() && '\n'!=sb.getChar()) {
						sb.next();
					}
					continue;
				}
				else if ('*'==sb.getChar(1)) {
					sb.next(2);
					while (sb.hasNext()) {
						ch = sb.getChar();
						if ('*'==ch && sb.hasNext(1) && '/'==sb.getChar(1)) {
							// Конец комментария
							sb.next(2);
							continue;
						}

						if ('\n'==ch) {
							sb.nextLine();
						}
						else {
							sb.incColumn();
						}
						sb.incPos();
					}

					//add new ParseError("Unterminated block comment", startLine, startColumn);
					continue;
				}
			}
			
			//Блок данных
			if(sb.hasNext(1) && '#'==ch) {
				sb.next();
				char type = sb.getChar();
				sb.next();
				
				switch(type) {
					case 'p':
						Token token = new TNote(sb);
						tokens.add(token);
						continue;
					default: throw new ParseError("Unsupported #block: '" + type + "'", sb);
				}
			}
			
			// Символ
			if ('\''==ch) {
				try {
					Token token = new TChar(sb);
					tokens.add(token);
				}
				catch(ParseError e) {
					if(sb.hasNext()) tokens.add(new Token(TokenType.CHAR, "?", e));
					else throw e;
				}
				continue;
			}
			
			// Строки
			if ('"'==ch) {
				try {
					Token token = new TString(sb);
					tokens.add(token);
				}
				catch(ParseError e) {
					if(sb.hasNext()) tokens.add(new Token(TokenType.STRING, "????", e));
					else throw e;
				}
				continue;
			}
			
            // Числа
            if (Character.isDigit(ch)) {
                try {
					Token token = new TNumber(sb);
					tokens.add(token);
				}
				catch(ParseError e) {
					tokens.add(new Token(TokenType.NUMBER, 0, e));
				}
				continue;
            }
            
			// Идентификаторы и ключевые слова
			if (Character.isLetter(ch) || '_'==ch) {
				Token token = new TKeyword(sb);

				// Добавляем проверку на метку
				if (!(token.getValue() instanceof Keyword) && sb.hasNext() && ':'==sb.getChar()) {
					token = new TLabel(token.getStringValue(), sb);
				}
				tokens.add(token);
				continue;
			}
			
			// Операторы
			Token token = TOpearator.parse(sb);
            if (null != token) {
                tokens.add(token);
                continue;
            }
                    
			// Разделители
			token = TDelimiter.parse(sb);
            if (null != token) {
                tokens.add(token);
                continue;
            }
			throw new ParseError("Unexpected character: '" + ch + "'", sb);
        }
        tokens.add(new Token(sb, TokenType.EOF, null));
		
		for(Token token : tokens) {
			System.out.print(token.toString());
			if(TokenType.NEWLINE == token.getType()) {
				System.out.println();
			}
		}
		System.out.println();
	}

	public List<Token> getTokens() {
		return tokens;
	}
}
