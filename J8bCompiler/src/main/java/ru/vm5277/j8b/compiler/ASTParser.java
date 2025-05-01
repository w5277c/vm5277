/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import ru.vm5277.j8b.compiler.enums.Keyword;
import ru.vm5277.j8b.compiler.enums.TokenType;
import ru.vm5277.j8b.compiler.messages.MessageContainer;
import ru.vm5277.j8b.compiler.nodes.AstNode;
import ru.vm5277.j8b.compiler.nodes.ClassNode;
import ru.vm5277.j8b.compiler.nodes.ImportNode;
import ru.vm5277.j8b.compiler.nodes.TokenBuffer;
import ru.vm5277.j8b.compiler.tokens.Token;

public class ASTParser {
	private	final	MessageContainer	mc;
	private final	List<Token>			tokens;
	private			List<AstNode>		imports		= new ArrayList<>();
	private			ClassNode			classNode;

	public ASTParser(List<Token> tokens, MessageContainer mc) throws IOException {
		this.mc = mc;
		this.tokens = tokens;
		
		if(tokens.isEmpty()) return;
		
		TokenBuffer tb = new TokenBuffer(tokens.iterator()); // Создаём корень AST
		// Обработка импортов		
		while (tb.match(Keyword.IMPORT) && !tb.match(TokenType.EOF)) {
			imports.add(new ImportNode(tb));
		}
		
		Set<Keyword> modifiers = AstNode.collectModifiers(tb);
		if(tb.match(TokenType.OOP, Keyword.CLASS)) {
			classNode = new ClassNode(tb, modifiers, null);
		}
	}
	
	public List<AstNode> getImports() {
		return imports;
	}
	
	public ClassNode getClazz() {
		return classNode;
	}
}

