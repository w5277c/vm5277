/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
22.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {
    
	public static void main(String[] args) throws IOException {
		try (InputStreamReader isr = new InputStreamReader(new FileInputStream(args[0x00]))) {
			Lexer lexer = new Lexer(isr);
			
			Parser parser = new Parser(lexer.getTokens());
			
			SemanticAnalyzer analyzer = new SemanticAnalyzer(parser.getAst());
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
    }
}
