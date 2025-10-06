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
package ru.vm5277.compiler;

import ru.vm5277.compiler.tokens.Token;
import ru.vm5277.compiler.tokens.TDelimiter;
import ru.vm5277.compiler.tokens.TOpearator;
import ru.vm5277.compiler.tokens.TLabel;
import ru.vm5277.compiler.tokens.TChar;
import ru.vm5277.compiler.tokens.TNote;
import ru.vm5277.compiler.tokens.TNumber;
import ru.vm5277.compiler.tokens.TString;
import ru.vm5277.compiler.tokens.TKeyword;
import java.io.File;
import java.io.FileInputStream;
import ru.vm5277.common.messages.MessageContainer;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.common.messages.ErrorMessage;
import ru.vm5277.common.SourceBuffer;

public class Lexer {
	protected	final	MessageContainer	mc;
	protected			SourceBuffer		sb;
	protected	final	List<Token>			tokens	= new ArrayList<>();

	public Lexer(File sourceFile, MessageContainer mc) throws IOException {
		try (InputStreamReader isr = new InputStreamReader(new FileInputStream(sourceFile))) {
			StringBuilder stringBuilder = new StringBuilder();
			char[] buffer = new char[4*1024];
			for (int length; (length = isr.read(buffer)) != -1;) {
				stringBuilder.append(buffer, 0, length);
			}
			sb = new SourceBuffer(sourceFile, stringBuilder.toString());
		}
		this.mc = mc;
		
		parse();
	}

	public Lexer(String source, MessageContainer mc) throws IOException {
		sb = new SourceBuffer(null, source);
		this.mc = mc;
		
		parse();
	}

	private void parse() {
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
			
			if(sb.hasNext(1) && '*'==ch && '/'==sb.getChar(1)) {
				mc.add(new ErrorMessage("Unexpected end of comment '*/' without start", sb.snapSP()));
				sb.next(2);
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
