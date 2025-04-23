/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes.commands;

import java.util.List;
import ru.vm5277.j8b.compiler.nodes.*;
import ru.vm5277.j8b.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.j8b.compiler.nodes.expressions.ExpressionParser;
import ru.vm5277.j8b.compiler.tokens.enums.Delimiter;
import ru.vm5277.j8b.compiler.tokens.enums.Keyword;
import ru.vm5277.j8b.compiler.tokens.enums.TokenType;

public class IfNode extends AstNode {
    private	final	ExpressionNode	condition;
    private	final	BlockNode		thenBranch;
    private	final	BlockNode		elseBranch;
	
	public IfNode(TokenBuffer tb) {
		super(tb);
		
        tb.consume(); // Пропускаем "if"
        tb.consume(Delimiter.LEFT_PAREN);
        
		// Условие
		this.condition = new ExpressionParser(tb).parse();
        tb.consume(Delimiter.RIGHT_PAREN);

		// Then блок
		thenBranch = tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb) : new BlockNode(tb, parseStatement());

		// Else блок
        if (tb.match(TokenType.COMMAND, Keyword.ELSE)) {
			tb.consume();
        
			if (tb.match(TokenType.COMMAND, Keyword.IF)) {
				// Обработка else if
				elseBranch = new BlockNode(tb, new IfNode(tb));
			}
			else {
				elseBranch = tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb) : new BlockNode(tb, parseStatement());
			}
		}
		else {
			elseBranch = null;
		}
	}

    // Геттеры
    public ExpressionNode getCondition() {
        return condition;
    }

    public BlockNode getThenBranch() {
        return thenBranch;
    }

    public BlockNode getElseBranch() {
        return elseBranch;
    }
}
