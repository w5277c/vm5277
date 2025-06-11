/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
30.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm.nodes;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import ru.vm5277.avr_asm.Lexer;
import ru.vm5277.avr_asm.Parser;
import ru.vm5277.avr_asm.TokenBuffer;
import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.avr_asm.TokenType;
import ru.vm5277.common.exceptions.CriticalParseException;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.common.messages.WarningMessage;

public class IncludeNode {
	public static void parse(TokenBuffer tb, Scope scope, MessageContainer mc, Map<String, SourceType> sourcePaths)
																												throws ParseException, CriticalParseException {
		String importPath = (String)Node.consumeToken(tb, TokenType.STRING).getValue();


		File sourceFile = null;
		for(String path : sourcePaths.keySet()) {
			File file = new File(path + File.separator + importPath);
			if (file.exists()) sourceFile = file;
		}
		if(null == sourceFile) throw new ParseException("TODO Imported file not found: " + importPath, tb.getSP());

		if(!scope.addImport(sourceFile.getAbsolutePath() + File.separator + importPath)) {
			mc.add(new WarningMessage("File '" + importPath + "' already imported", tb.getSP()));
		}
		else {
			SourcePosition sp = tb.getSP();

			scope.list(".INCLUDE " + sourceFile.getPath());
			
			try {
				Lexer lexer = new Lexer(sourceFile, scope, mc);
				Map<String, SourceType> innerSourcePaths = new HashMap<>(sourcePaths);
				innerSourcePaths.put(sourceFile.getParent(), SourceType.LIB);
				new Parser(lexer.getTokens(), scope, mc, innerSourcePaths);
			}
			catch(IOException e) {
				throw new ParseException("TODO " + e.getMessage(), sp);
			}
			try {scope.leaveImport();}
			catch(ParseException e) {mc.add(e.getErrorMessage());}
			catch(CriticalParseException e) {mc.add(e.getErrorMessage()); throw e;}
		}
		Node.consumeToken(tb, TokenType.NEWLINE);
	}
}
