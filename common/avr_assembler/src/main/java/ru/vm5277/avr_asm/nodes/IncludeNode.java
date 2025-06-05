/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
30.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm.nodes;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import ru.vm5277.avr_asm.AsmLexer;
import ru.vm5277.avr_asm.Parser;
import ru.vm5277.avr_asm.TokenBuffer;
import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.common.Lexer;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.common.TokenType;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.common.messages.WarningMessage;
import ru.vm5277.common.tokens.Token;

public class IncludeNode {
	public static void parse(TokenBuffer tb, Scope scope, MessageContainer mc, String rtosPath, String basePath) throws ParseException {
		String importPath = (String)Node.consumeToken(tb, TokenType.STRING).getValue();

		String _basePath = rtosPath;
		File file = new File(_basePath + File.separator + importPath);
		if (!file.exists()) {
			_basePath = basePath;
			file = new File(_basePath + File.separator + importPath);
			if(!file.exists()) {
				throw new ParseException("TODO Imported file not found: " + importPath, tb.getSP());
			}
		}

		if(!scope.addImport(_basePath, importPath)) {
			mc.add(new WarningMessage("File '" + importPath + "' already imported", tb.getSP()));
		}
		else {
			SourcePosition sp = tb.getSP();

			try (FileReader reader = new FileReader(file)) {
				mc.setFile(importPath, sp);
				Lexer lexer = new AsmLexer(reader, scope, mc);
				new Parser(lexer.getTokens(), scope, mc, rtosPath, file.getParent());
			}
			catch(IOException e) {
				throw new ParseException("TODO " + e.getMessage(), sp);
			}
			mc.releaseFile(sp);
		}
		Node.consumeToken(tb, TokenType.NEWLINE);
	}
}
