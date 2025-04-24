/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes.commands;

import ru.vm5277.j8b.compiler.nodes.*;
import ru.vm5277.j8b.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.j8b.compiler.nodes.expressions.ExpressionParser;
import ru.vm5277.j8b.compiler.enums.Delimiter;

public class ReturnNode extends AstNode {
	private	final	ExpressionNode	expression;
	
	public ReturnNode(TokenBuffer tb) {
		super(tb);
		
		tb.consume(); // Пропускаем "return"
        
		this.expression = tb.match(Delimiter.SEMICOLON) ? null : new ExpressionParser(tb).parse();
        
        // Обязательно потребляем точку с запятой
        tb.consume(Delimiter.SEMICOLON);
    }

    public ExpressionNode getExpression() {
        return expression;
    }

    public boolean returnsValue() {
        return expression != null;
    }
}
