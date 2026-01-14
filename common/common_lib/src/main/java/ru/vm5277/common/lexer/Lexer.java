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

package ru.vm5277.common.lexer;

import ru.vm5277.common.lexer.tokens.Token;
import ru.vm5277.common.lexer.tokens.TDelimiter;
import ru.vm5277.common.lexer.tokens.TOpearator;
import ru.vm5277.common.lexer.tokens.TLabel;
import ru.vm5277.common.lexer.tokens.TChar;
import ru.vm5277.common.lexer.tokens.TNote;
import ru.vm5277.common.lexer.tokens.TNumber;
import ru.vm5277.common.lexer.tokens.TString;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.lexer.tokens.TDirective;
import ru.vm5277.common.lexer.tokens.TKeyword;
import static ru.vm5277.common.lexer.tokens.Token.skipToken;

public class Lexer {
	protected	final	LexerType				type;
	protected			SourceBuffer			sb;
	protected			ExternalTokenProvider	extTokenProvider;
	protected	final	List<Token>				tokens		= new ArrayList<>();
	protected			boolean					allTokens	= true;
	
	public Lexer(LexerType type) {
		this.type = type;
	}

	public Lexer(LexerType type, File sourceFile, ExternalTokenProvider extTokenProvider, int tabSize, boolean allTokens) throws IOException {
		this.type = type;
		this.sb = new SourceBuffer(sourceFile, tabSize);
		this.extTokenProvider = extTokenProvider;
		this.allTokens = allTokens;
		
		parse();
		
		sb.close();
	}
/*
	public Lexer(LexerType type, File sourceFile, int tabSize) throws IOException {
		this.type = type;
		this.sb = new SourceBuffer(new FileInputStream(sourceFile), tabSize);
		
		parse();
		
		sb.close();
	}
*/
	public Lexer(LexerType type, String source, boolean allTokens, int tabSize) throws IOException {
		this.type = type;
		this.sb = new SourceBuffer(source, tabSize);
		this.allTokens = allTokens;
		
		parse();
		
		sb.close();
	}

	private void parse() {
		while(sb.available()) {
			Token token = parseToken(sb);
			if(TokenType.NEWLINE==token.getType() && LexerType.J8B==type) {
				token.setType(TokenType.WHITESPACE);
			}
			if(allTokens || (TokenType.WHITESPACE!=token.getType() && TokenType.COMMENT!=token.getType())) {
				tokens.add(token);
			}
		}
		tokens.add(new Token(sb, sb.snapSP(), TokenType.EOF));
	}
	
	public Token parseToken(SourceBuffer sb) {
		if(!sb.available()) {
			return new Token(sb, sb.snapSP(), TokenType.EOF);
		}
		SourcePosition sp = sb.snapSP();

		try {
			if('\n'==sb.peek()) {
				sb.next();
				Token token = new Token(sb, sp, TokenType.NEWLINE);
				sb.gotNewLine();
				return token;
			}
			if('\r'==sb.peek()) {
				if(sb.available(2) && '\n'==sb.peek(1)) {
					sb.next();
				}
				sb.next();
				Token token = new Token(sb, sp, TokenType.NEWLINE);
				sb.gotNewLine();
				return token;
			}

			// Пропускаем пробелы
			if(skipWhiteSpaces(sb)) {
				return new Token(sb, sp, TokenType.WHITESPACE);
			}
			// Пропускаем комментарии
			if(skipComment(sb, type)) {
				return new Token(sb, sp, TokenType.COMMENT);
			}

			if(LexerType.J8B==type) {
				//Блок данных
				if(sb.available(2) && '#'==sb.peek()) {
					sb.next();
					sp = sb.snapSP();
					char blockType = sb.peek();
					sb.next();

					switch(blockType) {
						case 'p':
							return  new TNote(sb);
						case ';':
							skipToken(sb);
							throw new CompileException("Empty #block");
						default: {
							skipToken(sb);
							throw new CompileException("Unsupported #block: '" + blockType + "'");
						}
					}
				}
			}

			if(sb.available(2) && '*'==sb.peek() && '/'==sb.peek(1)) {
				sb.next(2);
				throw new CompileException("Unexpected end of comment '*/' without start");
			}

			// Макро параметер
			if((LexerType.ALL==type || LexerType.ASM==type) && '@'==sb.peek()) {
				sb.next();
				return  new Token(sb, sp, TokenType.MACRO_PARAM);
			}

			// Символ
			if ('\''==sb.peek()) {
				return new TChar(sb);
			}

			// Строки
			if ('"'==sb.peek()) {
				return  new TString(sb);
			}

			// Числа
			if (Character.isDigit(sb.peek())) {
				return  new TNumber(sb);
			}

			// Проверка на индексные регистры -X,-Y,-Z
			if((LexerType.ALL==type || LexerType.ASM==type) && '-'==sb.peek() && sb.available(2)) {
				char reg = (char)(sb.peek(1)|0x20);
				if('x'==reg || 'y'==reg || 'z'==reg) {
					sb.next(2);
					return new Token(sb, sp, TokenType.INDEX_REG, "-"+reg);
				}
			}

			// Операторы
			Token token = TOpearator.parse(sb);
			if (null != token) {
				return token;
			}

			if((LexerType.ALL==type || LexerType.ASM==type) && '.'==sb.peek() && sb.available()) {
				sb.next();
				return new TDirective(sb, sp);
			}

			// Идентификаторы и ключевые слова
			if(Character.isLetter(sb.peek()) || '_'==sb.peek()) {
				token = new TKeyword(sb, type);

				if(TokenType.IDENTIFIER==token.getType()) {
					// Проверяем наличие во внешнем токен провайдере
					if(null!=extTokenProvider) {
						Token extToken = extTokenProvider.getExternalToken(sb, sp, token.getStringValue().toLowerCase());
						if(null!=extToken) {
							return extToken;
						}
					}
					// Добавляем проверку на метку
					if(sb.available() && ':'==sb.peek()) {
						token = new TLabel(token.getStringValue(), sb);
					}
				}
				return token;
			}

			// Разделители
			token = TDelimiter.parse(sb, type);
			if(null!=token) {
				return token;
			}
			return new Token(sb, sp, TokenType.INVALID, "Unexpected character: '" + sb.next() + "'");
		}
		catch(CompileException ex) {
			return new Token(sb, sp, TokenType.INVALID, ex.getMessage());
		}
	}

	public List<Token> getTokens() {
		return tokens;
	}
	
	public void print() {
		for(Token token : tokens) {
			System.out.print(token.toString());
			if(TokenType.NEWLINE==token.getType()) {
				System.out.println();
			}
		}
		System.out.println();
	}

	public List<Token> getInvalidTokens() {
		List<Token> result = new ArrayList<>();
		for(Token token : tokens) {
			if(TokenType.INVALID==token.getType()) {
				result.add(token);
			}
		}
		return result;
	}
	
	private static boolean skipWhiteSpaces(SourceBuffer sb) {
		boolean result = false;
		while(sb.available() && Character.isWhitespace(sb.peek())) {
			// Обрываем токен, если найден код новой строки (некоторым компиляторам важен перевод строки)
			if('\r'==sb.peek()|| '\n'==sb.peek()) {
				break;
			}
			
			result = true;			
			// Пропускаем табуляцию с корректным счетом позиции			
			if('\t'==sb.peek()) {
				sb.gotTab();
			}
			sb.next();				
		}

		return result;
	}
	
	public static boolean skipComment(SourceBuffer sb, LexerType lexerType) throws CompileException {
		if(LexerType.ASM==lexerType || LexerType.ALL==lexerType) {
			if(';'==sb.peek() || '#'==sb.peek()) {
				while(sb.available() && '\n'!=sb.peek()) {
					sb.next();
				}
				return true;
			}
		}
		if('/'==sb.peek() && sb.available(2)) {
			if('/'==sb.peek(1)) {
				sb.next(2);
				while(sb.available() && '\n'!=sb.peek()) {
					sb.next();
				}
				return true;
			}
			else if('*'==sb.peek(1)) {
				sb.next(2);
				while (sb.available()) {
					if(sb.available(2) && '*'==sb.peek() && '/'==sb.peek(1)) {
						// Конец комментария
						sb.next(2);
						return true;
					}

					if ('\n'==sb.peek()) {
						sb.gotNewLine();
					}

					sb.next();
				}
				while(sb.available() && ('\n'!=sb.peek() || ';'!=sb.peek())) {
					sb.next();
				}
				throw new CompileException("Unterminated block comment", sb.snapSP());
			}
		}
		return false;
	}
}
