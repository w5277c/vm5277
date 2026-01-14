/*
 * Copyright 2025 konstantin@5277.ru
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.vm5277.avr_asm.semantic;

import ru.vm5277.avr_asm.TokenBuffer;
import ru.vm5277.avr_asm.nodes.Node;
import ru.vm5277.avr_asm.scope.MacroCallSymbol;
import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.common.lexer.ASMKeyword;
import ru.vm5277.common.lexer.Delimiter;
import ru.vm5277.common.lexer.TokenType;
import ru.vm5277.common.lexer.Operator;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.lexer.Keyword;
import ru.vm5277.common.messages.MessageContainer;
import static ru.vm5277.common.lexer.Operator.AND;
import static ru.vm5277.common.lexer.Operator.BIT_AND;
import static ru.vm5277.common.lexer.Operator.BIT_NOT;
import static ru.vm5277.common.lexer.Operator.BIT_OR;
import static ru.vm5277.common.lexer.Operator.BIT_XOR;
import static ru.vm5277.common.lexer.Operator.DIV;
import static ru.vm5277.common.lexer.Operator.EQ;
import static ru.vm5277.common.lexer.Operator.GT;
import static ru.vm5277.common.lexer.Operator.GTE;
import static ru.vm5277.common.lexer.Operator.LT;
import static ru.vm5277.common.lexer.Operator.LTE;
import static ru.vm5277.common.lexer.Operator.MINUS;
import static ru.vm5277.common.lexer.Operator.MULT;
import static ru.vm5277.common.lexer.Operator.NEQ;
import static ru.vm5277.common.lexer.Operator.NOT;
import static ru.vm5277.common.lexer.Operator.OR;
import static ru.vm5277.common.lexer.Operator.PLUS;
import static ru.vm5277.common.lexer.Operator.SHL;
import static ru.vm5277.common.lexer.Operator.SHR;
import ru.vm5277.common.lexer.SourcePosition;
import ru.vm5277.common.lexer.tokens.Token;

public class Expression extends Node {
	protected Expression() {
	}

	public static Expression parse(TokenBuffer tb, Scope scope, MessageContainer mc) throws CompileException {
		return parseBinary(tb, scope, mc, 0);
	}
	
	// TODO свертывание не выполняется для BinaryExpression из parseFunction
	private static Expression parseBinary(TokenBuffer tb, Scope scope, MessageContainer mc, int minPrecedence) throws CompileException {
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
	
	private static Expression parseUnary(TokenBuffer tb, Scope scope, MessageContainer mc) throws CompileException {
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

	private static Expression parsePrimary(TokenBuffer tb, Scope scope, MessageContainer mc) throws CompileException {
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
				throw new CompileException("Got macro param without macro: " + token, tb.current().getSP());
			}
		}
		else if(tb.match(TokenType.NUMBER) || tb.match(TokenType.STRING) || tb.match(TokenType.CHARACTER) || tb.match(TokenType.LITERAL)) {
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
			if((!tb.match(TokenType.IDENTIFIER) && !tb.match(TokenType.NUMBER)) || (!expr.isInc() && !expr.isDec())) {
				return expr;
			}
			return new BinaryExpression(tb, scope, mc, new IRegExpression(expr.getId(), false, false),
										expr.isDec() ? Operator.MINUS : Operator.PLUS, parseBinary(tb, scope, mc, 0));
		}
		else if(tb.match(TokenType.COMMAND)) {
			String name = ((Keyword)tb.consume().getValue()).getName();
			if(tb.match(Delimiter.LEFT_PAREN)) {
				tb.consume();
				Expression result = parseFunction(tb, scope, mc, name);
				consumeToken(tb, Delimiter.RIGHT_PAREN);
				return result;
			}
			throw new CompileException("Missing opening parenthesis '(' for command '" + name + "'", tb.current().getSP());
		}
		else if(tb.match(TokenType.IDENTIFIER)) {
			return new IdExpression(tb, scope, mc, ((String)tb.consume().getValue()).toLowerCase());
		}
		else {
			throw new CompileException("Unexpected token in expression: " + token, tb.current().getSP());
        }
    }
	
	private static Expression parseFunction(TokenBuffer tb, Scope scope, MessageContainer mc, String name) throws CompileException {
		Expression expr = Expression.parse(tb, scope, mc);
		
		Long value = null;
		try {value = getLong(expr, null);} catch(CompileException e) {}
		if(null != value) {
			if(ASMKeyword.LOW.getName().equals(name) || ASMKeyword.BYTE1.getName().equals(name)) return new LiteralExpression(value & 0xff);
			else if(ASMKeyword.HIGH.getName().equals(name) || ASMKeyword.BYTE2.getName().equals(name)) return new LiteralExpression((value >> 8) & 0xff);
			else if(ASMKeyword.BYTE3.getName().equals(name) || ASMKeyword.PAGE.getName().equals(name)) return new LiteralExpression((value >> 16) & 0xff);
			else if(ASMKeyword.BYTE4.getName().equals(name)) return new LiteralExpression((value >> 24) & 0xff);
			else if(ASMKeyword.LWRD.getName().equals(name)) return new LiteralExpression(value & 0xffff);
			else if(ASMKeyword.HWRD.getName().equals(name)) return new LiteralExpression((value >> 16) & 0xffff);
			else if(ASMKeyword.EXP2.getName().equals(name)) return new LiteralExpression(1 << value);
			else if(ASMKeyword.LOG.getName().equals(name)) {
				long result = 0;
				while (0x01 <= value) {
					value = value >> 0x01;
					result++;
				}
				return new LiteralExpression(result);
			}
			else {
				throw new CompileException("Unsupported function: " + name, tb.getSP());
			}
		}
		else {
			if(ASMKeyword.LOW.getName().equals(name)) {
				expr = new BinaryExpression(tb, scope, mc, expr, Operator.BIT_AND, new LiteralExpression(0xff));
			}
			else if(ASMKeyword.HIGH.getName().equals(name)) {
				expr = new BinaryExpression(tb, scope, mc, expr, Operator.SHR, new LiteralExpression(8));
				expr = new BinaryExpression(tb, scope, mc, expr, Operator.BIT_AND, new LiteralExpression(0xff));
			}
			else if(ASMKeyword.EXP2.getName().equals(name)) {
				expr = new BinaryExpression(tb, scope, mc, new LiteralExpression(1), Operator.SHL, expr);
			}
			else {
				throw new CompileException("Unsupported function: " + name, tb.getSP());
			}
		}
		return expr;
	}
	
	private static long getLong(Object obj) throws CompileException {
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
		if(obj instanceof Character) {
			return (char)obj;
		}
		throw new CompileException("Unsupported value type:" + obj, null);
	}

	public static Long getLong(Expression expr, SourcePosition sp) throws CompileException {
		if(expr instanceof IdExpression) {
			return ((IdExpression)expr).getNumericValue();
		}
		if(expr instanceof LiteralExpression) {
			return Expression.getLong(((LiteralExpression)expr).getValue());
		}
		if (expr instanceof BinaryExpression || expr instanceof UnaryExpression) {
			Expression folded = Expression.fold(expr);
			if (null != folded && folded instanceof LiteralExpression) {
				return Expression.getLong(((LiteralExpression)folded).getValue());
			}
		}
		throw new CompileException("Expression '" + expr + "' with type '" + expr.getClass().getSimpleName() + "' is not supported", sp);
	}

	
	public static Expression fold(Expression expr) throws CompileException {
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
			catch (CompileException e) {}
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
		
		if(expr instanceof IdExpression) {
			Long value = ((IdExpression)expr).getNumericValue();
			return new LiteralExpression(value);
		}
		return null;
	}
}
