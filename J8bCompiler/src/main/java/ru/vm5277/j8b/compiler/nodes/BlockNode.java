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
import ru.vm5277.j8b.compiler.exceptions.ParseException;

public class BlockNode extends AstNode {
	protected	List<AstNode>			declarations	= new ArrayList<>();
	protected	Map<String, LabelNode>	labels			= new HashMap<>();
	
	public BlockNode() {
	}
	
	public BlockNode(TokenBuffer tb, AstNode singleStatement) {
		super(tb);
		
		declarations.add(singleStatement);
	}

	public BlockNode(TokenBuffer tb) throws ParseException {
        super(tb);
        
		consumeToken(tb, Delimiter.LEFT_BRACE); //Наличие токена должно быть гарантировано вызывающим

		while (!tb.match(TokenType.EOF) && !tb.match(Delimiter.RIGHT_BRACE)) {
			if(tb.match(TokenType.LABEL)) {
				LabelNode label = new LabelNode(tb);
				labels.put(label.getName(), label);
				declarations.add(label);
			}
			if (tb.match(TokenType.COMMAND)) {
				try {
					declarations.add(parseCommand());
				}
				catch(ParseException e) {
					tb.addMessage(e.getErrorMessage());
					markFirstError(e);
				} // Фиксируем ошибку(Unexpected command token)
				continue;
			}
			if(tb.match(Keyword.FREE)) {
				declarations.add(new FreeNode(tb));
				continue;
			}
			
			Set<Keyword> modifiers = collectModifiers(tb);

			// Обработка классов с модификаторами
			if (tb.match(TokenType.OOP) && Keyword.CLASS == tb.current().getValue()) {
				declarations.add(new ClassNode(tb, modifiers, null));
				continue;
			}

			try {
				// Определение типа (примитив или класс)
				VarType type = checkPrimtiveType();
				if (null == type) type = checkClassType();


				if(null != type) {
					// Получаем имя метода/конструктора
					String name = null;
					try {name = consumeToken(tb, TokenType.ID).getStringValue();}catch(ParseException e) {markFirstError(e);} // Нет имени сущности, пытаемся парсить дальше

					if (tb.match(Delimiter.LEFT_BRACKET)) { // Это объявление массива
						ArrayDeclarationNode node = new ArrayDeclarationNode(tb, modifiers, type, name);
						if(null != name) declarations.add(node);
					}
					else { // Переменная
						FieldNode node = new FieldNode(tb, modifiers, type, name);
						if(null != name) declarations.add(node);
					}
					continue;
				}
			}
			catch(ParseException e) {
				markFirstError(e);
			}

			// 4. Обработка остальных statement (if, while, вызовы и т.д.)
			try {
				AstNode statement = parseStatement();
				declarations.add(statement);
				if(statement instanceof BlockNode) {
					blocks.add((BlockNode)statement);
				}
			}
			catch(ParseException e) {
				markFirstError(e);
			}
		}
		consumeToken(tb, Delimiter.RIGHT_BRACE);
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