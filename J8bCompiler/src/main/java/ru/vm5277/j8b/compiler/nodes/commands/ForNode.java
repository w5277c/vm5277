/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
28.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes.commands;

import ru.vm5277.j8b.compiler.nodes.*;
import ru.vm5277.j8b.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.j8b.compiler.nodes.expressions.ExpressionParser;
import ru.vm5277.j8b.compiler.enums.Delimiter;
import ru.vm5277.j8b.compiler.enums.Keyword;
import ru.vm5277.j8b.compiler.enums.TokenType;
import ru.vm5277.j8b.compiler.enums.VarType;

public class ForNode extends AstNode {
    private final AstNode initialization;
    private final ExpressionNode condition;
    private final ExpressionNode iteration;
    
    public ForNode(TokenBuffer tb) {
        super(tb);
        
        tb.consume(); // Пропускаем "for"
        tb.consume(Delimiter.LEFT_PAREN);
        
        // Инициализация
        if(!tb.match(Delimiter.SEMICOLON)) {
			VarType type = checkPrimtiveType();
			if(null == type) type = checkClassType();
			if(null != type) {
				String name = tb.consume(TokenType.ID).getStringValue();
				this.initialization = new FieldNode(tb, null, type, name);
			}
			else {
				this.initialization = new ExpressionParser(tb).parse();
			}
		}
		else {
			this.initialization = null;
			tb.consume(Delimiter.SEMICOLON);
		}
        
        // Условие
        this.condition = tb.match(Delimiter.SEMICOLON) ? null : new ExpressionParser(tb).parse();
        tb.consume(Delimiter.SEMICOLON);
        
        // Итерация
        this.iteration = tb.match(Delimiter.RIGHT_PAREN) ? null : new ExpressionParser(tb).parse();
        tb.consume(Delimiter.RIGHT_PAREN);
        
        // Основной блок
		if(tb.match(Delimiter.LEFT_BRACE)) {
			try {
				tb.getLoopStack().add(this);
				blocks.add(new BlockNode(tb));
			}
			finally {
				tb.getLoopStack().remove(this);
			}
		}
		else blocks.add(new BlockNode(tb, parseStatement()));
       
        // Блок else (если есть)
        if (tb.match(Keyword.ELSE)) {
			tb.consume();
            blocks.add(tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb) : new BlockNode(tb, parseStatement()));
        }
    }
    
    // Геттеры
    public AstNode getInitialization() {
        return initialization;
    }
    
    public ExpressionNode getCondition() {
        return condition;
    }
    
    public ExpressionNode getIteration() {
        return iteration;
    }
    
    public BlockNode getBody() {
        return blocks.get(0);
    }
    
    public BlockNode getElseBlock() {
        return blocks.size() > 1 ? blocks.get(1) : null;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("for (");
        sb.append(initialization != null ? initialization : ";");
        sb.append(condition != null ? condition : ";");
        sb.append(iteration != null ? iteration : "");
        sb.append(") ");
        sb.append(getBody());
        
        if (getElseBlock() != null) {
            sb.append(" else ").append(getElseBlock());
        }
        
        return sb.toString();
    }
}