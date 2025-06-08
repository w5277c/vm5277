/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
22.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.common;

import java.io.File;
import java.io.FileInputStream;
import ru.vm5277.common.messages.MessageContainer;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import ru.vm5277.common.tokens.Token;
import ru.vm5277.common.messages.ErrorMessage;
import ru.vm5277.common.messages.MessageOwner;

public class Lexer {
	protected	final	MessageContainer	mc;
	protected			SourceBuffer		sb;
	protected	final	List<Token>			tokens	= new ArrayList<>();
	
	public Lexer(File sourceFile, MessageContainer mc) throws IOException {
		mc.setOwner(MessageOwner.LEXER);
		
		try (InputStreamReader isr = new InputStreamReader(new FileInputStream(sourceFile))) {
			StringBuilder stringBuilder = new StringBuilder();
			char[] buffer = new char[4*1024];
			for (int length; (length = isr.read(buffer)) != -1;) {
				stringBuilder.append(buffer, 0, length);
			}
			sb = new SourceBuffer(sourceFile, stringBuilder.toString());
		}
		this.mc = mc;
	}

	public Lexer(String source, MessageContainer mc) throws IOException {
		mc.setOwner(MessageOwner.LEXER);
		
		sb = new SourceBuffer(null, source);
		this.mc = mc;
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
