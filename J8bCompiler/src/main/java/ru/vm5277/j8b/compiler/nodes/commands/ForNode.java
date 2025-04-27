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

public class ForNode extends AstNode {
    private final ExpressionNode initialization;
    private final ExpressionNode condition;
    private final ExpressionNode iteration;
    
    public ForNode(TokenBuffer tb) {
        super(tb);
        
        tb.consume(); // Пропускаем "for"
        tb.consume(Delimiter.LEFT_PAREN);
        
        // Инициализация
        this.initialization = tb.match(Delimiter.SEMICOLON) ? null : new ExpressionParser(tb).parse();
        tb.consume(Delimiter.SEMICOLON);
        
        // Условие
        this.condition = tb.match(Delimiter.SEMICOLON) ? null : new ExpressionParser(tb).parse();
        tb.consume(Delimiter.SEMICOLON);
        
        // Итерация
        this.iteration = tb.match(Delimiter.RIGHT_PAREN) ? null : new ExpressionParser(tb).parse();
        tb.consume(Delimiter.RIGHT_PAREN);
        
        // Основной блок
        blocks.add(tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb, "") : new BlockNode(tb, parseStatement()));
        
        // Блок else (если есть)
        if (tb.match(TokenType.COMMAND, Keyword.ELSE)) {
			tb.consume();
            blocks.add(tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb, "") : new BlockNode(tb, parseStatement()));
        }
    }
    
    // Геттеры
    public ExpressionNode getInitialization() {
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