/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
22.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler_core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import ru.vm5277.j8b.compiler.common.CodeGenerator;
import ru.vm5277.j8b.compiler_core.codegen.PlatformLoader;
import ru.vm5277.j8b.compiler_core.messages.MessageContainer;
import ru.vm5277.j8b.compiler_core.nodes.ClassNode;

public class Main {
    public	final	static	String	VERSION	= "0.0.22";
	
	public static void main(String[] args) throws IOException, Exception {
		MessageContainer mc = new MessageContainer(8, true, false);
		String runtimePath = args[0];
		File inputFile = new File(args[1]);
		String basePath = inputFile.getParent();
		
		RegisterMapLoader rml = new RegisterMapLoader(runtimePath, mc);
		
		CodeGenerator cg = PlatformLoader.loadGenerator("avr", rml.getMap(), null);
		
		try (InputStreamReader isr = new InputStreamReader(new FileInputStream(inputFile))) {
			Lexer lexer = new Lexer(isr, mc);
			ASTParser parser = new ASTParser(runtimePath, basePath, lexer.getTokens(), mc);
			ClassNode clazz = parser.getClazz();
//			new ASTPrinter(parser.getClazz());
			new SemanticAnalyzer(runtimePath, parser.getClazz());
			new ASTPrinter(parser.getClazz());
			
			if(!mc.hasErrors()) {
				clazz.codeGen(cg);
			}
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
    }
	
	private static Map<String, String> parseArgs(String[] args) {
		Map<String, String> params = new HashMap<>();
		for (String arg : args) {
			String[] parts = arg.split("=");
			if (0x02 == parts.length) {
				params.put(parts[0].trim(), parts[1].trim());
			}
		}
		return params;
	}
}
