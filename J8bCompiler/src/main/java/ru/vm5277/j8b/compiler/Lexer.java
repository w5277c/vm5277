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
	private	final	String			src;
	private			int				pos				= 0;
	private			int				line			= 1;
	private			int				column			= 1;
	private final	List<Token>		tokens			= new ArrayList<>();
	
	public Lexer(Reader reader) throws IOException {
		StringBuilder sb = new StringBuilder();
		char[] buffer = new char[4*1024];
		for (int length; (length = reader.read(buffer)) != -1;) {
			sb.append(buffer, 0, length);
		}
		src = sb.toString();

		while (pos<src.length()) {
            char ch = src.charAt(pos);
            
			// Пропускаем пробелы (кроме \n)
			if (Character.isWhitespace(ch)) {
				if ('\n'==ch) {
					//tokens.add(new Token(TokenType.NEWLINE, "\\n", line, column));
					line++;
					column = 1;
				}
				else if ('\r'==ch) {
					if ((pos+1)<src.length() && '\n'==src.charAt(pos+1)) {
						pos++;
					}
					line++;						
					column = 1;
		        }
				else {
					column++;
				}
				pos++;
				continue;
			}
            
			// 2. Пропускаем комментарии
			if ('/'==ch && pos+1 < src.length()) {
				if ('/'==src.charAt(pos+1)) {
				    while (pos<src.length() && '\n'!=src.charAt(pos)) {
						pos++;
						column++;
					}
					continue;
				}
				else if ('*'==src.charAt(pos+1)) {
					pos += 2;
					column += 2;

					while (pos < src.length()) {
						ch = src.charAt(pos);

						if ('*'==ch && pos+1 < src.length() && '/'==src.charAt(pos + 1)) {
							// Конец комментария
							pos += 2;
							column += 2;
							continue;
						}

						if ('\n'==ch) {
							line++;
							column = 1;
						}
						else {
							column++;
						}
						pos++;
					}

					//add new ParseError("Unterminated block comment", startLine, startColumn);
					continue;
				}
			}
			
			//Блок данных
			if((pos+1)<src.length() && '#'==ch) {
				pos++;
				column++;
				char type = src.charAt(pos);
				pos++;
				column++;
				
				switch(type) {
					case 'p':
						Token token = new TNote(src, pos, line, column);
						tokens.add(token);
						column += (token.getEndPos()-pos);
						pos=token.getEndPos();
						continue;
					default: throw new ParseError("Unsupported #block: '" + type + "'", line, column);		
				}
			}
			
			// Символ
			if ('\''==ch) {
				try {
					Token token = new TChar(src, pos, line, column);
					tokens.add(token);
					column += (token.getEndPos()-pos);
					pos=token.getEndPos();
				}
				catch(ParseError e) {
					tokens.add(new Token(TokenType.CHAR, "?", e));
					pos += (e.getColumn()-column)-1;
					column = e.getColumn()-1;
				}
				continue;
			}
			
			// Строки
			if ('"'==ch) {
				try {
					Token token = new TString(src, pos, line, column);
					tokens.add(token);
					column += (token.getEndPos()-pos);
					pos=token.getEndPos();
				}
				catch(ParseError e) {
					tokens.add(new Token(TokenType.STRING, "????", e));
					pos += e.getColumn()-column;
					column = e.getColumn(); // TODO check it
				}
				continue;
			}
			
            // Числа
            if (Character.isDigit(ch)) {
                try {
					Token token = new TNumber(src, pos, line, column);
					tokens.add(token);
					column += (token.getEndPos()-pos);
					pos=token.getEndPos();
				}
				catch(ParseError e) {
					tokens.add(new Token(TokenType.NUMBER, 0, e));
					pos += e.getColumn()-column;
					column = e.getColumn();  // TODO check it
				}
				continue;
            }
            
			// Идентификаторы и ключевые слова
			if (Character.isLetter(ch) || '_'==ch) {
				Token token = new TKeyword(src, pos, line, column);
				column += (token.getEndPos()-pos);
				pos=token.getEndPos();

				// Добавляем проверку на метку
				if (!(token.getValue() instanceof Keyword) && pos < src.length() && src.charAt(pos) == ':') {
					token = new TLabel(token.getStringValue(), token.getEndPos(), token.getLine(), token.getColumn());
					column += (token.getEndPos()-pos);
					pos=token.getEndPos();
				}
				tokens.add(token);
				continue;
			}
			
			// Операторы
			Token token = TOpearator.parse(src, pos, line, column);
            if (null != token) {
                tokens.add(token);
				column += (token.getEndPos()-pos);
				pos=token.getEndPos();
                continue;
            }
                    
			// Разделители
			token = TDelimiter.parse(src, pos, line, column);
            if (null != token) {
                tokens.add(token);
				column += (token.getEndPos()-pos);
				pos=token.getEndPos();
                continue;
            }
			throw new ParseError("Unexpected character: '" + ch + "'", line, column);
        }
        tokens.add(new Token(TokenType.EOF, "", line, column));
		
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
