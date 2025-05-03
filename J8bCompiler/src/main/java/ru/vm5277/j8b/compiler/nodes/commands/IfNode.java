/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes.commands;

import ru.vm5277.j8b.compiler.nodes.*;
import ru.vm5277.j8b.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.j8b.compiler.enums.Delimiter;
import ru.vm5277.j8b.compiler.enums.Keyword;
import ru.vm5277.j8b.compiler.enums.TokenType;
import ru.vm5277.j8b.compiler.exceptions.ParseException;

public class IfNode extends AstNode {
    private	ExpressionNode	condition;
	
	public IfNode(TokenBuffer tb) {
		super(tb);
		
        consumeToken(tb); // Потребляем "if"
		// Условие
		try {consumeToken(tb, Delimiter.LEFT_PAREN);} catch(ParseException e){markFirstError(e);}
		try {this.condition = new ExpressionNode(tb).parse();} catch(ParseException e) {markFirstError(e);}
		try {consumeToken(tb, Delimiter.RIGHT_PAREN);} catch(ParseException e){markFirstError(e);}

		// Then блок
		tb.getLoopStack().add(this);
		try {blocks.add(tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb) : new BlockNode(tb, parseStatement()));}
		catch(ParseException e) {markFirstError(e);}
		tb.getLoopStack().remove(this);

		// Else блок
        if (tb.match(Keyword.ELSE)) {
			consumeToken(tb);
        
			if (tb.match(TokenType.COMMAND, Keyword.IF)) {
				// Обработка else if
				tb.getLoopStack().add(this);
				blocks.add(new BlockNode(tb, new IfNode(tb)));
				tb.getLoopStack().remove(this);
			}
			else {
				tb.getLoopStack().add(this);
				try {blocks.add(tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb) : new BlockNode(tb, parseStatement()));}
				catch(ParseException e) {markFirstError(e);}
				tb.getLoopStack().remove(this);
			}
		}
	}

    // Геттеры
    public ExpressionNode getCondition() {
        return condition;
    }

    public BlockNode getThenBlock() {
        return blocks.get(0);
    }

    public BlockNode getElseBlock() {
        return (0x02 == blocks.size() ? blocks.get(1) : null);
    }
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("if (").append(condition.toString()).append(") ");
		sb.append(getThenBlock().toString());

		if (getElseBlock() != null) {
			sb.append(" else ");
			sb.append(getElseBlock().toString());
		}

		return sb.toString();
	}
}
