/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b_compiler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.common.messages.MessageOwner;
import ru.vm5277.j8b_compiler.nodes.AstNode;
import ru.vm5277.j8b_compiler.nodes.ClassNode;
import ru.vm5277.j8b_compiler.nodes.ImportNode;
import ru.vm5277.j8b_compiler.nodes.TokenBuffer;
import ru.vm5277.j8b_compiler.tokens.Token;

public class ASTParser extends AstNode {
	private	final	FileImporter		fileImporter;
	private			List<AstNode>		imports			= new ArrayList<>();
	private			List<ClassNode>		importedClasses	= new ArrayList<>();
	private			ClassNode			classNode;

	public ASTParser(File runtimeDir, File baseDir, List<Token> tokens, MessageContainer mc) throws IOException {
		this.fileImporter = new FileImporter(runtimeDir, baseDir, mc);
		this.mc = mc;
		mc.setOwner(MessageOwner.PARSER);
		
		if(tokens.isEmpty()) return;
		
		tb = new TokenBuffer(tokens.iterator());
		// Обработка импортов		
		while (tb.match(Keyword.IMPORT) && !tb.match(TokenType.EOF)) {
			ImportNode importNode = new ImportNode(tb, mc);
			imports.add(importNode);
			
			// Загрузка импортируемого файла
			List<Token> importedTokens = fileImporter.importFile(importNode.getImportPath());
			if (!importedTokens.isEmpty()) {
				// Рекурсивный парсинг импортированного файла
				ASTParser importedParser = new ASTParser(runtimeDir, baseDir, importedTokens, mc);
				if (null != importedParser.getClazz()) {
					importedClasses.add(importedParser.getClazz());
				}
			}
		}
		
		Set<Keyword> modifiers = collectModifiers(tb);
		if(tb.match(TokenType.OOP, Keyword.CLASS)) {
			try {
				classNode = new ClassNode(tb, mc, modifiers, null, importedClasses);
			}
			catch(ParseException e) {
				// Парсинг прерван (дальнейший парсинг файла бессмыслен)
			}
		}
	}
	
	public List<AstNode> getImports() {
		return imports;
	}

	public ClassNode getClazz() {
		return classNode;
	}

	public List<ClassNode> getImportedClasses() {
		return importedClasses;
	}
	
	@Override
	public String getNodeType() {
		return "root";
	}
	
	public TokenBuffer getTB() {
		return tb;
	}
}

