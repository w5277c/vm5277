/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
31.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm.semantic;

import ru.vm5277.avr_asm.TokenBuffer;
import ru.vm5277.avr_asm.nodes.Node;
import ru.vm5277.avr_asm.scope.MacroCallSymbol;
import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.avr_asm.Keyword;
import ru.vm5277.avr_asm.Delimiter;
import ru.vm5277.avr_asm.TokenType;
import ru.vm5277.common.Operator;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.avr_asm.tokens.Token;
import static ru.vm5277.common.Operator.AND;
import static ru.vm5277.common.Operator.BIT_AND;
import static ru.vm5277.common.Operator.BIT_NOT;
import static ru.vm5277.common.Operator.BIT_OR;
import static ru.vm5277.common.Operator.BIT_XOR;
import static ru.vm5277.common.Operator.DIV;
import static ru.vm5277.common.Operator.EQ;
import static ru.vm5277.common.Operator.GT;
import static ru.vm5277.common.Operator.GTE;
import static ru.vm5277.common.Operator.LT;
import static ru.vm5277.common.Operator.LTE;
import static ru.vm5277.common.Operator.MINUS;
import static ru.vm5277.common.Operator.MULT;
import static ru.vm5277.common.Operator.NEQ;
import static ru.vm5277.common.Operator.NOT;
import static ru.vm5277.common.Operator.OR;
import static ru.vm5277.common.Operator.PLUS;
import static ru.vm5277.common.Operator.SHL;
import static ru.vm5277.common.Operator.SHR;
import ru.vm5277.common.SourcePosition;

public class Expression extends Node {
	protected Expression() {
	}

	public static Expression parse(TokenBuffer tb, Scope scope, MessageContainer mc) throws ParseException {
		return parseBinary(tb, scope, mc, 0);
	}
	
	// TODO свертывание не выполняется для BinaryExpression из parseFunction
	private static Expression parseBinary(TokenBuffer tb, Scope scope, MessageContainer mc, int minPrecedence) throws ParseException {
        Expression left = parseUnary(tb, scope, mc);

		while (tb.match(TokenType.OPERATOR)) {
            Operator operator = ((Operator)tb.current().getValue());

			Integer precedence = Operator.PRECEDENCE.get(operator);
            if (precedence == null || precedence<minPrecedence) {
				break;
			}
            tb.consume();
	
			Expression right = parseBinary(tb, scope, mc, precedence + (operator.isAssignment() ? 0 : 1));
	
			BinaryExpression binaryExpr = new BinaryExpression(tb, scope, mc, left, operator, right);
			Expression folded = fold(binaryExpr);
        
			left = (null != folded ? folded : binaryExpr);
		}
        return left;
    }
	
	private static Expression parseUnary(TokenBuffer tb, Scope scope, MessageContainer mc) throws ParseException {
		if (tb.match(TokenType.OPERATOR)) {
			Operator operator = ((Operator)tb.current().getValue()); //TODO check it
			if(operator.isUnary()) {
				tb.consume();

				Expression expr = parsePrimary(tb, scope, mc);
				UnaryExpression unaryExpr = new UnaryExpression(tb, scope, mc, operator, expr);
				Expression folded = fold(unaryExpr);
				return (null != folded ? folded : unaryExpr);
//				return new UnaryExpression(tb, scope, mc, operator, parseUnary(tb, scope, mc));
			}
		}
		return parsePrimary(tb, scope, mc);
	}

	private static Expression parsePrimary(TokenBuffer tb, Scope scope, MessageContainer mc) throws ParseException {
		Token token = tb.current();
		
		if(tb.match(TokenType.MACRO_PARAM)) {
			if(scope.isMacroDeploy()) {
				tb.consume();
				Integer index = (Integer)(consumeToken(tb, TokenType.NUMBER)).getValue();
				MacroCallSymbol marcoCall = scope.getMarcoCall();
				Expression result = marcoCall.getParams().get(index);
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
		else if(tb.match(TokenType.INDEX_REG)) {
			IRegExpression expr = new IRegExpression((String)tb.consume().getValue());
			if((!tb.match(TokenType.ID) && !tb.match(TokenType.NUMBER)) || (!expr.isInc() && !expr.isDec())) {
				return expr;
			}
			return new BinaryExpression(tb, scope, mc, new IRegExpression(expr.getId(), false, false),
										expr.isDec() ? Operator.MINUS : Operator.PLUS, parseBinary(tb, scope, mc, 0));
		}
		else if(tb.match(TokenType.ID)) {
			String name = ((String)tb.consume().getValue()).toLowerCase();
			if(tb.match(Delimiter.LEFT_PAREN)) {
				tb.consume();
				Expression result = parseFunction(tb, scope, mc, name);
				consumeToken(tb, Delimiter.RIGHT_PAREN);
				return result;
			}
			return new IdExpression(tb, scope, mc, name);
		}
		else {
			throw new ParseException("Unexpected token in expression: " + token, tb.current().getSP());
        }
    }
	
	private static Expression parseFunction(TokenBuffer tb, Scope scope, MessageContainer mc, String name) throws ParseException {
		Expression expr = Expression.parse(tb, scope, mc);
		
		Long value = null;
		try {value = getLong(expr, null);} catch(ParseException e) {}
		if(null != value) {
			if(Keyword.LOW.getName().equals(name) || Keyword.BYTE1.getName().equals(name)) return new LiteralExpression(value & 0xff);
			else if(Keyword.HIGH.getName().equals(name) || Keyword.BYTE2.getName().equals(name)) return new LiteralExpression((value >> 8) & 0xff);
			else if(Keyword.BYTE3.getName().equals(name) || Keyword.PAGE.getName().equals(name)) return new LiteralExpression((value >> 16) & 0xff);
			else if(Keyword.BYTE4.getName().equals(name)) return new LiteralExpression((value >> 24) & 0xff);
			else if(Keyword.LWRD.getName().equals(name)) return new LiteralExpression(value & 0xffff);
			else if(Keyword.HWRD.getName().equals(name)) return new LiteralExpression((value >> 16) & 0xffff);
			else if(Keyword.EXP2.getName().equals(name)) return new LiteralExpression(1 << value);
			else if(Keyword.LOG.getName().equals(name)) {
				long result = 0;
				while (0x01 <= value) {
					value = value >> 0x01;
					result++;
				}
				return new LiteralExpression(result);
			}
			else {
				throw new ParseException("TODO не поддерживаемая функция:" + name, tb.getSP());
			}
		}
		else {
			if(Keyword.LOW.getName().equals(name)) {
				expr = new BinaryExpression(tb, scope, mc, expr, Operator.BIT_AND, new LiteralExpression(0xff));
			}
			else if(Keyword.HIGH.getName().equals(name)) {
				expr = new BinaryExpression(tb, scope, mc, expr, Operator.SHR, new LiteralExpression(8));
				expr = new BinaryExpression(tb, scope, mc, expr, Operator.BIT_AND, new LiteralExpression(0xff));
			}
			else {
				throw new ParseException("TODO не поддерживаемая функция:" + name, tb.getSP());
			}
		}
		return expr;
	}
	
	private static long getLong(Object obj) throws ParseException {
		if(obj instanceof Integer) {
			return ((Integer)obj).longValue();
		}
		if(obj instanceof Long) {
			return (Long)obj;
		}
		if(obj instanceof Boolean) {
			return ((Boolean)obj) ? 0x01l : 0x00l;
		}
		if(obj instanceof String && 0x01==((String)obj).length()) {
			return ((String)obj).charAt(0);
		}
		throw new ParseException("TODO не поддерживаемый тип значения:" + obj, null);
	}

	public static Long getLong(Expression expr, SourcePosition sp) throws ParseException {
		if(expr instanceof IdExpression) {
			return ((IdExpression)expr).getNumericValue();
		}
		if(expr instanceof LiteralExpression) {
			return Expression.getLong(((LiteralExpression)expr).getValue());
		}
		if (expr instanceof BinaryExpression || expr instanceof UnaryExpression) {
			Expression folded = Expression.fold(expr);
			if (null != folded && folded instanceof LiteralExpression) return Expression.getLong(((LiteralExpression)folded).getValue());
		}
		throw new ParseException("TODO не поддерживаемое выражение:" + expr, sp);
	}

	
	public static Expression fold(Expression expr) throws ParseException {
		if (expr instanceof BinaryExpression) {
			BinaryExpression binaryExpr = (BinaryExpression)expr;
			Operator operator = binaryExpr.getOp();
			Expression folded = fold(binaryExpr.getLeftExpr());
			Expression leftExpr = (null != folded ? folded : binaryExpr.getLeftExpr());
			folded = fold(binaryExpr.getRightExpr());
			Expression rightExpr = (null != folded ? folded : binaryExpr.getRightExpr());
			if(null == leftExpr || null == rightExpr) return null;
			try {
				Long leftVal = getLong(leftExpr, null);
				Long rightVal = getLong(rightExpr, null);
				switch (operator) {
					case MULT:		return new LiteralExpression(leftVal * rightVal);
					case DIV:		return new LiteralExpression(leftVal / rightVal);
					case SHL:		return new LiteralExpression(leftVal << rightVal);
					case SHR:		return new LiteralExpression(leftVal >>> rightVal);
					case BIT_OR:	return new LiteralExpression(leftVal | rightVal);
					case BIT_AND:	return new LiteralExpression(leftVal & rightVal);
					case BIT_XOR:	return new LiteralExpression(leftVal ^ rightVal);
					case PLUS:		return new LiteralExpression(leftVal + rightVal);
					case MINUS:		return new LiteralExpression(leftVal - rightVal);
					case EQ:		return new LiteralExpression(leftVal.longValue() == rightVal);
					case NEQ:		return new LiteralExpression(leftVal.longValue() != rightVal);
					case LT:		return new LiteralExpression(leftVal < rightVal);
					case LTE:		return new LiteralExpression(leftVal <= rightVal);
					case GT:		return new LiteralExpression(leftVal > rightVal);
					case GTE:		return new LiteralExpression(leftVal >= rightVal);
					case OR:		return new LiteralExpression((leftVal != 0) || (rightVal != 0));
					case AND:		return new LiteralExpression((leftVal != 0) && (rightVal != 0));
					case NOT:		return new LiteralExpression(leftVal != rightVal);
				}
			}
			catch (ParseException e) {}
			return null;
		}
		
		if (expr instanceof UnaryExpression) {
			UnaryExpression unaryExpr = (UnaryExpression)expr;
			Expression folded = fold(unaryExpr.getOperand());
			Expression operandExpr = (null != folded ? folded : unaryExpr.getOperand());
			if(null == operandExpr) return null;
			if(operandExpr instanceof LiteralExpression) {
				Object value =  ((LiteralExpression)operandExpr).getValue();
				Operator operator = unaryExpr.getOperator();

				if (value instanceof Number) {
					long num = ((Number)value).longValue();
					switch (operator) {
						case BIT_NOT:	return new LiteralExpression((~num) & 0xffffffffL);
						case MINUS:		return new LiteralExpression(-num);
						case NOT:		return new LiteralExpression(num == 0 ? 1L : 0L);
					}
				}
				else if (value instanceof Boolean && Operator.NOT == operator) {
					return new LiteralExpression(!(boolean)value);
				}
			}
			return null;
		}
		
		if (expr instanceof IdExpression) {
			try {
				Long value = ((IdExpression)expr).getNumericValue();
				return new LiteralExpression(value);
			}
			catch (ParseException e) {}
		}
		return null;
	}
}
