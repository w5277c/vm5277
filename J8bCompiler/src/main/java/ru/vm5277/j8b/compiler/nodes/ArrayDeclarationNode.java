/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
25.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes;

import java.util.Set;
import ru.vm5277.j8b.compiler.enums.Delimiter;
import ru.vm5277.j8b.compiler.enums.Keyword;
import ru.vm5277.j8b.compiler.enums.Operator;
import ru.vm5277.j8b.compiler.enums.VarType;
import ru.vm5277.j8b.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.j8b.compiler.nodes.expressions.ExpressionParser;

public class ArrayDeclarationNode extends AstNode {
    private	final	Set<Keyword>	modifiers;
	private	final	VarType			elementType;
    private	final	String			name;
	private	final	ExpressionNode	size;
	private	final	ExpressionNode	initializer;

	public ArrayDeclarationNode(TokenBuffer tb, Set<Keyword> modifiers, VarType type, String name) {
		super(tb);
		
		this.modifiers = modifiers;
		this.elementType = type;
		this.name = name;
		
		tb.consume(); // Пропускаем '['
        
        // Размер массива
        size = new ExpressionParser(tb).parse();
        
        tb.consume(Delimiter.RIGHT_BRACKET); // Пропускаем ']'

        // Инициализация (опционально)
        if (tb.match(Operator.ASSIGN)) {
            tb.consume();
            initializer = new ExpressionParser(tb).parse();
        }
		else {
			initializer = null;
		}
		
		tb.consume(Delimiter.SEMICOLON);
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}