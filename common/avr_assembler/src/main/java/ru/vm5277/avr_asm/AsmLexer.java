/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
30.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm;

import ru.vm5277.avr_asm.tokens.TDirective;
import ru.vm5277.common.messages.MessageContainer;
import java.io.IOException;
import java.io.Reader;
import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.common.Lexer;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.common.TokenType;
import ru.vm5277.common.tokens.TChar;
import ru.vm5277.common.tokens.TDelimiter;
import ru.vm5277.common.tokens.TNumber;
import ru.vm5277.common.tokens.TOpearator;
import ru.vm5277.common.tokens.TString;
import ru.vm5277.common.tokens.Token;
import ru.vm5277.common.messages.ErrorMessage;

public class AsmLexer extends Lexer {
	
	public AsmLexer(Reader reader, Scope scope, MessageContainer mc) throws IOException {
		super(reader, mc);

		while (sb.hasNext()) {
			// Пропускаем комментарий
			if(';'==sb.getChar()) {
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

			// TODO добавить токены вида -W,W+,-X,X+,-Y,Y+,-Z,Z+
			
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
        tokens.add(new Token(sb, TokenType.EOF, null));
	}
}
