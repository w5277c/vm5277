/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
31.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm.semantic;

import ru.vm5277.avr_asm.TokenBuffer;
import ru.vm5277.avr_asm.nodes.Node;
import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.common.AsmKeyword;
import ru.vm5277.common.Delimiter;
import ru.vm5277.common.Operator;
import ru.vm5277.common.TokenType;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.common.tokens.Token;

public class Expression extends Node {
	protected Expression() {
	}

	public static Expression parse(TokenBuffer tb, Scope scope, MessageContainer mc) throws ParseException {
		return parseBinary(tb, scope, mc, 0);
	}
	
	private static Expression parseBinary(TokenBuffer tb, Scope scope, MessageContainer mc, int minPrecedence) throws ParseException {
        Expression left = parseUnary(tb, scope, mc);

		while (tb.match(TokenType.OPERATOR)) {
            Operator operator = ((Operator)tb.current().getValue());

			Integer precedence = Operator.PRECEDENCE.get(operator);
            if (precedence == null || precedence<minPrecedence) {
				break;
			}
            tb.consume();
	
			//TODO перейти на long
			Expression right = parseBinary(tb, scope, mc, precedence + (operator.isAssignment() ? 0 : 1));
			Long leftVal = null;
			Long rightVal = null;
			try {leftVal = Node.getValue(left, null);} catch(ParseException e) {}
			try {rightVal = Node.getValue(right, null);} catch(ParseException e) {}

			if(null != leftVal && null != rightVal) {
				switch (operator) {
					case MULT:		left = new LiteralExpression(leftVal *	rightVal); break;
					case DIV:		left = new LiteralExpression(leftVal /	rightVal); break;
					case SHL:		left = new LiteralExpression(leftVal <<	rightVal); break;
					case BIT_OR:	left = new LiteralExpression(leftVal |	rightVal); break;
					case PLUS:		left = new LiteralExpression(leftVal +	rightVal); break;
					case MINUS:		left = new LiteralExpression(leftVal -	rightVal); break;
					case EQ:		left = new LiteralExpression(leftVal.longValue() == rightVal); break;
					case NEQ:		left = new LiteralExpression(leftVal.longValue() !=	rightVal); break;
					case LT:		left = new LiteralExpression(leftVal <	rightVal); break;
					case LTE:		left = new LiteralExpression(leftVal <=	rightVal); break;
					case GT:		left = new LiteralExpression(leftVal >	rightVal); break;
					case GTE:		left = new LiteralExpression(leftVal >=	rightVal); break;
					default:		throw new ParseException("TODO не поддердживаемый тип операции:" + operator, null);
				}
			}
			else {
				return new BinaryExpression(tb, scope, mc,
											null == leftVal ? left : new LiteralExpression(leftVal),
											operator,
											null == rightVal ? right : new LiteralExpression(rightVal));
			}
		}
        return left;
    }
	
	private static Expression parseUnary(TokenBuffer tb, Scope scope, MessageContainer mc) throws ParseException {
		if (tb.match(TokenType.OPERATOR)) {
/*TODO			Operator operator = ((Operator)tb.current().getValue()); //TODO check it
			
			if(operator.isUnary()) {
				consumeToken(tb);
				return new UnaryExpression(tb, mc, operator, parseUnary());
			}
			else if (operator == Operator.MINUS && tb.match(TokenType.NUMBER)) {
				// Схлопываем "-число" в LiteralExpression с отрицательным значением
				consumeToken(tb); // Потребляем минус
				Token numberToken = consumeToken(tb, TokenType.NUMBER);
				Number value = (Number) numberToken.getValue();
				// Меняем знак
				if (value instanceof Integer) {
					return new LiteralExpression(tb, mc, -value.intValue());
				} else if (value instanceof Long) {
					return new LiteralExpression(tb, mc, -value.longValue());
				} else if (value instanceof Double) {
					return new LiteralExpression(tb, mc, -value.doubleValue());
				}
			}*/
		}
		return parsePrimary(tb, scope, mc);
	}

	private static Expression parsePrimary(TokenBuffer tb, Scope scope, MessageContainer mc) throws ParseException {
		Token token = tb.current();
		
		if(tb.match(TokenType.MACRO_PARAM)) {
			if(scope.isMacroMode()) {
				tb.consume();
				Integer index = (Integer)(consumeToken(tb, TokenType.NUMBER)).getValue();
				Expression result = scope.getMacroParam(index);
				return result;
			}
			else {
				throw new ParseException("Got macro param without macro: " + token, tb.current().getSP());
			}
		}
		else if(tb.match(TokenType.NUMBER) || tb.match(TokenType.STRING) || tb.match(TokenType.CHAR) || tb.match(TokenType.LITERAL)) {
			tb.consume();
			return new LiteralExpression(token.getValue());
		}
		else if(tb.match(Delimiter.LEFT_PAREN)) {
			tb.consume();
			Expression expr = parseBinary(tb, scope, mc, 0);
			Node.consumeToken(tb, Delimiter.RIGHT_PAREN);
			return expr;
		}
		else if(tb.match(TokenType.ID)) {
			String name = ((String)tb.consume().getValue()).toLowerCase();
			if(tb.match(Delimiter.LEFT_PAREN)) {
				tb.consume();
				Expression result = parseFunction(tb, scope, mc, name);
				consumeToken(tb, Delimiter.RIGHT_PAREN);
				return result;
			}
			return new VariableExpression(tb, scope, mc, name);
		}
		else {
			throw new ParseException("Unexpected token in expression: " + token, tb.current().getSP());
        }
    }
	
	private static Expression parseFunction(TokenBuffer tb, Scope scope, MessageContainer mc, String name) throws ParseException {
		Expression expr = Expression.parse(tb, scope, mc);
		
		Long value = null;
		try {value = Node.getValue(expr, null);} catch(ParseException e) {}
		if(null != value) {
			if(AsmKeyword.LOW.getName().equals(name)) {
				return new LiteralExpression(value & 0xff);
			}
			else if(AsmKeyword.HIGH.getName().equals(name)) {
				return new LiteralExpression((value >> 8) & 0xff);
			}
			else {
				throw new ParseException("TODO не поддерживаемая функция:" + name, tb.getSP());
			}
		}
		else {
			if(AsmKeyword.LOW.getName().equals(name)) {
				expr = new BinaryExpression(tb, scope, mc, expr, Operator.BIT_AND, new LiteralExpression(0xff));
			}
			else if(AsmKeyword.HIGH.getName().equals(name)) {
				expr = new BinaryExpression(tb, scope, mc, expr, Operator.SHR, new LiteralExpression(8));
				expr = new BinaryExpression(tb, scope, mc, expr, Operator.BIT_AND, new LiteralExpression(0xff));
			}
			else {
				throw new ParseException("TODO не поддерживаемая функция:" + name, tb.getSP());
			}
		}
		return expr;
	}
}
