/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
28.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes.commands;

import ru.vm5277.j8b.compiler.nodes.*;
import ru.vm5277.j8b.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.j8b.compiler.enums.Delimiter;
import ru.vm5277.j8b.compiler.enums.Keyword;
import ru.vm5277.j8b.compiler.enums.TokenType;
import ru.vm5277.j8b.compiler.enums.VarType;
import ru.vm5277.j8b.compiler.exceptions.ParseException;

public class ForNode extends AstNode {
    private AstNode initialization;
    private ExpressionNode condition;
    private ExpressionNode iteration;
    
    public ForNode(TokenBuffer tb) {
        super(tb);
        
        consumeToken(tb); // Потребляем "for"
        try {consumeToken(tb, Delimiter.LEFT_PAREN);} catch(ParseException e) {markFirstError(e);}
        
        // Инициализация
        if(!tb.match(Delimiter.SEMICOLON)) {
			try {
				VarType type = checkPrimtiveType();
				if(null == type) type = checkClassType();
				if(null != type) {
					String name = null;
					try {name = consumeToken(tb, TokenType.ID).getStringValue();} catch(ParseException e) {markFirstError(e);}
					this.initialization = new FieldNode(tb, null, type, name);
				}
				else {
					this.initialization = new ExpressionNode(tb).parse();
				}
			}
			catch(ParseException e) {
				markFirstError(e);
			}
		}
		else {
			this.initialization = null;
			try {consumeToken(tb, Delimiter.SEMICOLON);} catch(ParseException e) {markFirstError(e);}
		}
        
        // Условие
        try {this.condition = tb.match(Delimiter.SEMICOLON) ? null : new ExpressionNode(tb).parse();} catch(ParseException e) {markFirstError(e);}
        try {consumeToken(tb, Delimiter.SEMICOLON);} catch(ParseException e) {markFirstError(e);}
        
        // Итерация
        try {this.iteration = tb.match(Delimiter.RIGHT_PAREN) ? null : new ExpressionNode(tb).parse();} catch(ParseException e) {markFirstError(e);}
        try {consumeToken(tb, Delimiter.SEMICOLON);} catch(ParseException e) {markFirstError(e);}
        
		try {consumeToken(tb, Delimiter.RIGHT_PAREN);} catch(ParseException e) {markFirstError(e);}
		
        // Основной блок
		tb.getLoopStack().add(this);
		try {blocks.add(tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb) : new BlockNode(tb, parseStatement()));}
		catch(ParseException e) {markFirstError(e);}
		tb.getLoopStack().remove(this);
       
        // Блок else (если есть)
        if (tb.match(Keyword.ELSE)) {
			consumeToken(tb);
			tb.getLoopStack().add(this);
			try {blocks.add(tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb) : new BlockNode(tb, parseStatement()));}
			catch(ParseException e) {markFirstError(e);}
			tb.getLoopStack().remove(this);
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
        return blocks.isEmpty() ? null : blocks.get(0);
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