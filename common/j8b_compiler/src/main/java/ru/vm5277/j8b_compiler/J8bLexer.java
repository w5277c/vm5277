/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
22.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b_compiler;

import java.io.File;
import ru.vm5277.common.messages.MessageContainer;
import java.io.IOException;
import java.io.Reader;
import ru.vm5277.common.Keyword;
import ru.vm5277.common.Lexer;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.common.TokenType;
import ru.vm5277.common.tokens.TChar;
import ru.vm5277.common.tokens.TDelimiter;
import ru.vm5277.common.tokens.TKeyword;
import ru.vm5277.common.tokens.TNumber;
import ru.vm5277.common.tokens.TOpearator;
import ru.vm5277.common.tokens.TString;
import ru.vm5277.common.tokens.Token;
import ru.vm5277.common.messages.ErrorMessage;
import ru.vm5277.common.tokens.TLabel;
import ru.vm5277.common.tokens.TNote;

public class J8bLexer extends Lexer {
	public J8bLexer(File sourceFile, MessageContainer mc) throws IOException {
		super(sourceFile, mc);
	
		parse();
	}
	
	public J8bLexer(String source, MessageContainer mc) throws IOException {
		super(source, mc);
		
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
}
