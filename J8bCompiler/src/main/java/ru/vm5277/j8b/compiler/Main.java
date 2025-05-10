/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
22.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import ru.vm5277.j8b.compiler.messages.MessageContainer;
import ru.vm5277.j8b.compiler.messages.MessageOwner;
import ru.vm5277.j8b.compiler.semantic.Scope;

public class Main {
    public	final	static	String	VERSION	= "0.0.9";
	
	public static void main(String[] args) throws IOException {
		MessageContainer mc = new MessageContainer(8, true, false);
		
		try (InputStreamReader isr = new InputStreamReader(new FileInputStream(args[0x00]))) {
			
			mc.setOwner(MessageOwner.LEXER);
			Lexer lexer = new Lexer(isr, mc);
			
			mc.setOwner(MessageOwner.AST);
			ASTParser parser = new ASTParser(lexer.getTokens(), mc);
			
			new ASTPrinter(parser.getClazz());
			
			mc.setOwner(MessageOwner.SEMANTIC);
			
			new SemanticAnalyzer(parser.getClazz()) {
				@Override
				public boolean preAnalyze() {return true;}
				@Override
				public boolean declare(Scope scope) {return true;}
				@Override
				public boolean postAnalyze(Scope scope) {return true;}
			};
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
    }
}
