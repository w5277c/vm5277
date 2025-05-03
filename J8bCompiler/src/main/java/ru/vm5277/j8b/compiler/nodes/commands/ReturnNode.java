/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes.commands;

import ru.vm5277.j8b.compiler.nodes.*;
import ru.vm5277.j8b.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.j8b.compiler.enums.Delimiter;
import ru.vm5277.j8b.compiler.exceptions.ParseException;

public class ReturnNode extends AstNode {
	private	ExpressionNode	expression;
	
	public ReturnNode(TokenBuffer tb) {
		super(tb);
		
		consumeToken(tb); // Потребляем "return"
		
		try {this.expression = tb.match(Delimiter.SEMICOLON) ? null : new ExpressionNode(tb).parse();} catch(ParseException e) {markFirstError(e);}
        
        // Обязательно потребляем точку с запятой
        try {consumeToken(tb, Delimiter.SEMICOLON);}catch(ParseException e) {markFirstError(e);}
    }

    public ExpressionNode getExpression() {
        return expression;
    }

    public boolean returnsValue() {
        return expression != null;
    }
	
	@Override
	public String toString() {
		return "return" + (null != expression ? " " + expression : "");
	}
}
