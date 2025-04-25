/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes.expressions;

import java.util.ArrayList;
import java.util.List;
import ru.vm5277.j8b.compiler.ParseError;
import ru.vm5277.j8b.compiler.nodes.TokenBuffer;
import ru.vm5277.j8b.compiler.tokens.Token;
import ru.vm5277.j8b.compiler.enums.Delimiter;
import ru.vm5277.j8b.compiler.enums.Operator;
import ru.vm5277.j8b.compiler.enums.TokenType;

public class ExpressionParser {
    private final TokenBuffer tb;
    
    public ExpressionParser(TokenBuffer tokens) {
        this.tb = tokens;
    }
    
    public ExpressionNode parse() {
        return parseAssignment();
    }
    
	private ExpressionNode parseAssignment() {
		ExpressionNode left = parseBinary(0);
        
        if (tb.match(TokenType.OPERATOR) && isAssignmentOperator((Operator)tb.current().getValue())) {
            Operator operator = ((Operator)tb.consume().getValue());
            ExpressionNode right = parseAssignment();
            return new BinaryExpression(tb, left, operator, right);
        }
        
        return left;
    }    
	
	private ExpressionNode parseBinary(int minPrecedence) {
        ExpressionNode left = parseUnary();
        
        while (tb.match(TokenType.OPERATOR)) {
            Operator operator = ((Operator)tb.current().getValue());
            Integer precedence = Operator.PRECEDENCE.get(operator);
            
            if (precedence == null || precedence<minPrecedence) {
				break;
			}
            tb.consume();
			
            ExpressionNode right = parseBinary(precedence+1);
            left = new BinaryExpression(tb, left, operator, right);
        }
        
        return left;
    }
	
	
	private ExpressionNode parseUnary() {
		if (tb.match(TokenType.OPERATOR) && isUnaryOperator((Operator)tb.current().getValue())) {
			Operator operator = ((Operator)tb.consume().getValue()); //TODO check it
			ExpressionNode operand = parseUnary();
			return new UnaryExpression(tb, operator, operand);
		}

		return parsePostfix();
	}
	
	private ExpressionNode parsePostfix() {
		ExpressionNode expr = parsePrimary();
        
		while (true) {
//			if (tb.match(Delimiter.LEFT_PAREN)) {
//				expr = parseMethodCall(expr);
			//}
//			else
			if (tb.match(Delimiter.LEFT_BRACKET)) {
				expr = new ArrayExpression(tb, expr);
			}
			else {
				break;
			}
		}

		return expr;
    }

	
	private ExpressionNode parsePrimary() {
		Token token = tb.current();
        
		if(tb.match(TokenType.NUMBER) || tb.match(TokenType.STRING)) {
			tb.consume();
			return new LiteralExpression(tb, token.getValue());
		}
		else if(tb.match(Delimiter.LEFT_PAREN)) {
			tb.consume();
			ExpressionNode expr = parse();
			tb.consume(Delimiter.RIGHT_PAREN);
			return expr;
		}
		else if(tb.match(TokenType.ID)) {
			tb.consume();
			if(tb.match(Delimiter.LEFT_PAREN)) {
				tb.consume();
				List<ExpressionNode> args = new ArrayList<>();
				if(!tb.match(Delimiter.RIGHT_PAREN)) {
					//TODO parse args
				}
				tb.consume();
				return new MethodCallExpression(tb, new VariableExpression(tb, "this"), token.getValue().toString(), args);
			}
			return new VariableExpression(tb, token.getValue().toString());
		}
		else {
			throw new ParseError("Unexpected token in expression: " + token, tb.current().getLine(), tb.current().getColumn());
        }
    }
	
	private MethodCallExpression parseMethodCall(ExpressionNode target) {
        tb.consume(Delimiter.LEFT_PAREN);
        List<ExpressionNode> args = new ArrayList<>();
        
        // Если сразу идет закрывающая скобка ')', значит аргументов нет
		if (!tb.match(Delimiter.RIGHT_PAREN)) {
			// Парсим аргументы через запятую
			while(true) {
				// Парсим выражение-аргумент
				args.add(parse());
				
				// Если после выражения нет запятой - выходим из цикла
				if (!tb.match(Delimiter.COMMA)) break;
            
	            // Пропускаем запятую
		        tb.consume(Delimiter.COMMA);
			} 
        }
        
        tb.consume(Delimiter.RIGHT_PAREN);
		String methodName = (target instanceof VariableExpression) ? ((VariableExpression)target).getName() : "";
        return new MethodCallExpression(tb, target, methodName, args);
    }
	
	private boolean isAssignmentOperator(Operator op) {
		return	Operator.AND_ASSIGN == op || Operator.ASSIGN == op || Operator.DIV_ASSIGN == op || Operator.MINUS_ASSIGN == op ||
				Operator.MOD_ASSIGN == op || Operator.MULT_ASSIGN == op || Operator.OR_ASSIGN == op || Operator.PLUS_ASSIGN ==op ||
				Operator.SHL_ASSIGN == op || Operator.SHR_ASSIGN == op || Operator.XOR_ASSIGN == op;
    }
	
	private boolean isUnaryOperator(Operator op) {
		return Operator.NOT == op || Operator.BIT_NOT == op || Operator.PLUS == op || Operator.MINUS == op;
    }
}