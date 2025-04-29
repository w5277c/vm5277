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
import ru.vm5277.j8b.compiler.enums.Keyword;
import ru.vm5277.j8b.compiler.enums.TokenType;

public class IfNode extends AstNode {
    private	final	ExpressionNode	condition;
	
	public IfNode(TokenBuffer tb) {
		super(tb);
		
        tb.consume(); // Пропускаем "if"
        tb.consume(Delimiter.LEFT_PAREN);
        
		// Условие
		this.condition = new ExpressionParser(tb).parse();
        tb.consume(Delimiter.RIGHT_PAREN);

		// Then блок
		blocks.add(tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb) : new BlockNode(tb, parseStatement()));

		// Else блок
        if (tb.match(Keyword.ELSE)) {
			tb.consume();
        
			if (tb.match(TokenType.COMMAND, Keyword.IF)) {
				// Обработка else if
				blocks.add(new BlockNode(tb, new IfNode(tb)));
			}
			else {
				blocks.add(tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb) : new BlockNode(tb, parseStatement()));
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
}
