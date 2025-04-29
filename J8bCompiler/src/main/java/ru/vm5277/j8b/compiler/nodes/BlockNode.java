/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ru.vm5277.j8b.compiler.enums.Delimiter;
import ru.vm5277.j8b.compiler.enums.Keyword;
import ru.vm5277.j8b.compiler.enums.TokenType;
import ru.vm5277.j8b.compiler.enums.VarType;

public class BlockNode extends AstNode {
	protected	List<AstNode>			declarations	= new ArrayList<>();
	protected	Map<String, LabelNode>	labels			= new HashMap<>();
	public BlockNode() {
	}
	
	public BlockNode(TokenBuffer tb, AstNode singleStatement) {
		super(tb);
		
		declarations.add(singleStatement);
	}

	public BlockNode(TokenBuffer tb) {
        super(tb);
        
		tb.consume(Delimiter.LEFT_BRACE);

		while (!tb.match(TokenType.EOF) && !tb.match(Delimiter.RIGHT_BRACE)) {
			if(tb.match(TokenType.LABEL)) {
				LabelNode label = new LabelNode(tb);
				labels.put(label.getName(), label);
			}
			if (tb.match(TokenType.COMMAND)) {
				declarations.add(parseCommand());
				continue;
			}
			
			Set<Keyword> modifiers = collectModifiers(tb);

			// Обработка классов с модификаторами
			if (tb.match(TokenType.OOP) && Keyword.CLASS == tb.current().getValue()) {
				declarations.add(new ClassNode(tb, modifiers, null));
				continue;
			}

			// Определение типа (примитив или класс)
			VarType type = checkPrimtiveType();
			if (null == type) type = checkClassType();


			if(null != type) {
				// Получаем имя метода/конструктора
				String name = tb.consume(TokenType.ID).getStringValue();

				if (tb.match(Delimiter.LEFT_BRACKET)) { // Это объявление массива
					declarations.add(new ArrayDeclarationNode(tb, modifiers, type, name));
				}
				else { // Переменная
					declarations.add(new FieldNode(tb, modifiers, type, name));
				}
				continue;
			}

			// 4. Обработка остальных statement (if, while, вызовы и т.д.)
			AstNode statement = parseStatement();
			declarations.add(statement);
			if(statement instanceof BlockNode) {
				blocks.add((BlockNode)statement);
			}
		}
		tb.consume(Delimiter.RIGHT_BRACE);
    }
	
	public List<AstNode> getDeclarations() {
		return declarations;
	}
	
	public Map<String, LabelNode> getLabels() {
		return labels;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}