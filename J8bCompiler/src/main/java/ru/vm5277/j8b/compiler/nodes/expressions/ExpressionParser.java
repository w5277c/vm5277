/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes.expressions;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import ru.vm5277.j8b.compiler.ParseError;
import ru.vm5277.j8b.compiler.nodes.TokenBuffer;
import ru.vm5277.j8b.compiler.tokens.Token;
import ru.vm5277.j8b.compiler.enums.Delimiter;
import ru.vm5277.j8b.compiler.enums.Operator;
import ru.vm5277.j8b.compiler.enums.TokenType;
import ru.vm5277.j8b.compiler.nodes.AstNode;

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
        
        if (tb.match(TokenType.OPERATOR) && ((Operator)tb.current().getValue()).isAssignment()) {
            Operator operator = ((Operator)tb.consume().getValue());
            ExpressionNode right = parseAssignment();
			
			return optimizeExpression(new BinaryExpression(tb, left, operator, right));
        }
        
        return optimizeExpression(left);
    }    
	
	private ExpressionNode parseBinary(int minPrecedence) {
        ExpressionNode left = parseUnary();

		while (tb.match(TokenType.OPERATOR)) {
            Operator operator = ((Operator)tb.current().getValue());

			if (operator == Operator.TERNARY) {
				if (minPrecedence > Operator.PRECEDENCE.get(operator)) {
					break;
				}
				tb.consume();

				ExpressionNode trueExpr = parseBinary(Operator.PRECEDENCE.get(operator));
				tb.consume(Delimiter.COLON);
				ExpressionNode falseExpr = parseBinary(Operator.PRECEDENCE.get(operator));
				if(left instanceof LiteralExpression) {
					Object val = ((LiteralExpression)left).getValue();
					if (val instanceof Boolean) {
						return (Boolean)val ? trueExpr : falseExpr;
					}
				}
				return new TernaryExpression(tb, left, trueExpr, falseExpr);
			}
			
			Integer precedence = Operator.PRECEDENCE.get(operator);
            if (precedence == null || precedence<minPrecedence) {
				break;
			}
            tb.consume();
			
			ExpressionNode right = parseBinary(precedence + (operator.isAssignment() ? 0 : 1));
			left = optimizeOperationChain(left, operator, right, precedence);
		}
        return left;
    }
	
	// Агрессивная оптимизация
	private ExpressionNode optimizeOperationChain(ExpressionNode left, Operator op, ExpressionNode right, int precedence) {
		// Оптимизация унарных операций (если левая часть - унарный оператор)
		if (right instanceof UnaryExpression) {
			UnaryExpression unary = (UnaryExpression)right;
			right = optimizeUnary(unary.getOperator(), unary.getOperand());
		}
		
		if (left instanceof LiteralExpression && right instanceof LiteralExpression) {
			Object leftVal = ((LiteralExpression)left).getValue();
			Object rightVal = ((LiteralExpression)right).getValue();

			if ((op.isLogical() || op.isComparison()) && leftVal instanceof Boolean && rightVal instanceof Boolean) {
				switch(op) {
					case AND:	return new LiteralExpression(tb, ((Boolean)leftVal) && ((Boolean)rightVal));
					case OR:	return new LiteralExpression(tb, ((Boolean)leftVal) || ((Boolean)rightVal));
					case EQ:	return new LiteralExpression(tb, ((Boolean)leftVal) == ((Boolean)rightVal));
					case NEQ:	return new LiteralExpression(tb, ((Boolean)leftVal) != ((Boolean)rightVal));
					default:	return new BinaryExpression(tb, new LiteralExpression(tb, left), op, new LiteralExpression(tb, right));
				}
			}
			else if (op.isComparison() && leftVal instanceof Number && rightVal instanceof Number) {
				double delta = ((Number)leftVal).doubleValue() - ((Number)rightVal).doubleValue();
				switch(op) {
					case EQ:	return new LiteralExpression(tb, 0d == delta);
					case NEQ:	return new LiteralExpression(tb, 0d != delta);
					case LT:	return new LiteralExpression(tb, 0d > delta);
					case GT:	return new LiteralExpression(tb, 0d < delta);
					case LTE:	return new LiteralExpression(tb, 0d <= delta);
					case GTE:	return new LiteralExpression(tb, 0d >= delta);
					default: throw new ParseError("Invalid comparision operator: " + op, tb.current().getLine(), tb.current().getColumn());
				}
			}
		}

		if (op.isAssignment() && right instanceof BinaryExpression) {
			BinaryExpression bRight = (BinaryExpression)right;
			if (left instanceof VariableExpression && (Operator.PLUS==bRight.getOperator() || Operator.MINUS==bRight.getOperator())) {
				if(	(bRight.getLeft() instanceof VariableExpression && bRight.getRight() instanceof LiteralExpression) ||
					(bRight.getLeft() instanceof LiteralExpression && bRight.getRight() instanceof VariableExpression)) {
					
					VariableExpression ve = (VariableExpression)(bRight.getLeft() instanceof VariableExpression ? bRight.getLeft() : bRight.getRight());
					LiteralExpression le = (LiteralExpression)(bRight.getLeft() instanceof VariableExpression ? bRight.getRight(): bRight.getLeft());
					if(	ve.getValue().equals(((VariableExpression)left).getValue()) && le.isInteger()) {
						long num = le.toLong();
						if(-1 == num || 1 == num) {
							return new UnaryExpression(tb, (Operator.PLUS==bRight.getOperator()^(-1==num)) ? Operator.PRE_INC : Operator.PRE_DEC, left);
						}					
					}
				}
			}
		}
		
		switch(op) {
			case PLUS:
			case MINUS:
				return optimizeAdditiveChain(left, op, right);
			case MULT:
			case DIV:
			case MOD:
				return optimizeMultiplicativeChain(left, op, right);
			case BIT_OR:
			case BIT_AND:
			case BIT_XOR:
				return optimizeBitwiseChain(left, op, right);

			default:
				return new BinaryExpression(tb, left, op, right);
		}
	}

	private ExpressionNode optimizeAdditiveChain(ExpressionNode left, Operator op, ExpressionNode right) {
		if (Operator.PLUS==op) {
			boolean leftIsString = (left instanceof LiteralExpression && ((LiteralExpression)left).getValue() instanceof String);
			boolean rightIsString = (right instanceof LiteralExpression && ((LiteralExpression)right).getValue() instanceof String);

			if (leftIsString || rightIsString) {
				return optimizeStringConcat(left, right);
			}
		}
		
		if (left instanceof LiteralExpression && right instanceof LiteralExpression) {
			Object leftVal = ((LiteralExpression)left).getValue();
			Object rightVal = ((LiteralExpression)right).getValue();

			if (leftVal instanceof Number && rightVal instanceof Number) {
				if (leftVal instanceof Double || rightVal instanceof Double) {
					double a = ((Number)leftVal).doubleValue();
					double b = ((Number)rightVal).doubleValue();
					double result = op == Operator.PLUS ? a + b : a - b;
					return new LiteralExpression(tb, result);
				}
				else {
					long a = ((Number)leftVal).longValue();
					long b = ((Number)rightVal).longValue();
					long result = op == Operator.PLUS ? a + b : a - b;
					return new LiteralExpression(tb, result);
				}
			}
		}

		List<Term> terms = new ArrayList<>();
		collectAdditiveTerms(left, terms, true);
		terms.add(new Term(op, right, op != Operator.MINUS));

		long constValueLong = 0;
		double constValueDouble = 0;
		boolean hasDoubles = false;
		boolean hasLongs = false;
		List<Term> varTerms = new ArrayList<>();
		for (Term term : terms) {
			if (term.isNumber()) {
				Number num = (Number)((LiteralExpression)term.getNode()).getValue();
				if (num instanceof Double) {
					hasDoubles = true;
					double val = num.doubleValue();
					constValueDouble = term.isPositive() ? constValueDouble + val : constValueDouble - val;
				}
				else {
					hasLongs = true;
					long val = num.longValue();
					constValueLong = term.isPositive() ? constValueLong + val : constValueLong - val;
				}
			}
			else {
				varTerms.add(term);
			}
		}

		ExpressionNode result = null;
		if (hasDoubles) {
			double totalConst = constValueDouble + constValueLong;
			if (totalConst != 0) {
				result = new LiteralExpression(tb, totalConst);
			}
		}
		else if (constValueLong != 0) {
			result = new LiteralExpression(tb, constValueLong);
		}

		for (Term term : varTerms) {
			ExpressionNode node = term.getNode();
			if(null == result) {
				result = term.isPositive() ? node : new UnaryExpression(tb, Operator.MINUS, node);
			}
			else {
				result = new BinaryExpression(tb, result, term.isPositive() ? Operator.PLUS : Operator.MINUS, node);
			}
		}
		return result != null ? result : new LiteralExpression(tb, hasDoubles ? 0.0 : 0L);
	}

	private ExpressionNode optimizeExpression(ExpressionNode node) {
		// Оптимизация унарных операций
		if (node instanceof UnaryExpression) {
			UnaryExpression ue = (UnaryExpression)node;
			return optimizeUnary(ue.getOperator(), ue.getOperand());
		}

		// Оптимизация бинарных операций
		if (node instanceof BinaryExpression) {
			BinaryExpression bin = (BinaryExpression)node;
			ExpressionNode left = optimizeExpression(bin.getLeft());
			ExpressionNode right = optimizeExpression(bin.getRight());
			return optimizeOperationChain(left, bin.getOperator(), right, 0);
		}

		return node;
	}
	private ExpressionNode optimizeMultiplicativeChain(ExpressionNode left, Operator op, ExpressionNode right) {
		if (left instanceof LiteralExpression && right instanceof LiteralExpression) {
			Object leftVal = ((LiteralExpression)left).getValue();
			Object rightVal = ((LiteralExpression)right).getValue();

			if (leftVal instanceof Number && rightVal instanceof Number) {
				if (leftVal instanceof Double || rightVal instanceof Double) {
					double a = ((Number)leftVal).doubleValue();
					double b = ((Number)rightVal).doubleValue();
					
					switch (op) {
						case MULT: return new LiteralExpression(tb, a * b);
						case DIV:  return new LiteralExpression(tb, a / b);
						case MOD:  return new LiteralExpression(tb, a % b);
						default: throw new IllegalArgumentException();
					}
				}
				else {
					long a = ((Number)leftVal).longValue();
					long b = ((Number)rightVal).longValue();
					switch (op) {
						case MULT: return new LiteralExpression(tb, a * b);
						case DIV:  return new LiteralExpression(tb, a / b);
						case MOD:  return new LiteralExpression(tb, a % b);
						default: throw new IllegalArgumentException();
					}
				}
			}
		}

		if (Operator.MOD == op) {
			return new BinaryExpression(tb, left, op, right);
		}
		
		// Оптимизация цепочек
		List<Term> terms = new ArrayList<>();
		collectMultiplicativeTerms(left, terms, true);
		terms.add(new Term(op, right, Operator.MULT==op || Operator.MOD==op));

		long constValueLong = 1;
		double constValueDouble = 1;
		boolean hasDoubles = false;
		boolean hasDivision = false;
		List<Term> varTerms = new ArrayList<>();

		for (Term term : terms) {
			if (term.isNumber()) {
				Number num = (Number)((LiteralExpression)term.getNode()).getValue();
				if (num instanceof Double) {
					hasDoubles = true;
					double val = num.doubleValue();
					switch (term.getOperator()) {
						case MULT: constValueDouble *= val; break;
						case DIV:  constValueDouble /= val; hasDivision = true; break;
					}
				} else {
					long val = num.longValue();
					switch (term.getOperator()) {
						case MULT: constValueLong *= val; break;
						case DIV:  constValueLong /= val; hasDivision = true; break;
					}
				}
			}
			else {
				varTerms.add(term);
			}
		}

		ExpressionNode result = null;
		if (hasDoubles) {
			double totalConst = constValueDouble * constValueLong;
			if (totalConst != 1 || hasDivision) {
				result = new LiteralExpression(tb, totalConst);
			}
		}
		else if (constValueLong != 1 || hasDivision) {
			result = new LiteralExpression(tb, constValueLong);
		}

		for (Term term : varTerms) {
			ExpressionNode node = term.getNode();
			result = result == null ? node : new BinaryExpression(tb, result, term.getOperator(), node);
		}
		return result != null ? result : new LiteralExpression(tb, hasDoubles ? 1.0 : 1L);
	}

	private ExpressionNode optimizeBitwiseChain(ExpressionNode left, Operator op, ExpressionNode right) {
		// Константное свертывание
		if (left instanceof LiteralExpression && right instanceof LiteralExpression) {
			Object leftVal = ((LiteralExpression)left).getValue();
			Object rightVal = ((LiteralExpression)right).getValue();

			if (leftVal instanceof Number && rightVal instanceof Number) {
				long a = ((Number)leftVal).longValue();
				long b = ((Number)rightVal).longValue();
				long result;
				switch (op) {
					case BIT_AND: result = a & b; break;
					case BIT_OR:  result = a | b; break;
					case BIT_XOR: result = a ^ b; break;
					default: throw new IllegalArgumentException();
				}
				return new LiteralExpression(tb, result);
			}
		}
		return new BinaryExpression(tb, left, op, right);
	}
	
	private ExpressionNode optimizeStringConcat(ExpressionNode left, ExpressionNode right) {
		if (left instanceof LiteralExpression && right instanceof LiteralExpression) {
			Object leftVal = ((LiteralExpression)left).getValue();
			Object rightVal = ((LiteralExpression)right).getValue();

			if (leftVal instanceof String && rightVal instanceof String) {
				return new LiteralExpression(tb, (String)leftVal + (String)rightVal);
			}
		}
		if (left instanceof LiteralExpression && ((LiteralExpression)left).getValue() instanceof String) {
			String leftStr = (String)((LiteralExpression)left).getValue();
			if (right instanceof LiteralExpression) {
				Object rightVal = ((LiteralExpression)right).getValue();
				return new LiteralExpression(tb, leftStr + rightVal.toString());
			}
			return new BinaryExpression(tb, left, Operator.PLUS, right);
		}
		if (right instanceof LiteralExpression && ((LiteralExpression)right).getValue() instanceof String) {
			String rightStr = (String)((LiteralExpression)right).getValue();
			if (left instanceof LiteralExpression) {
				Object leftVal = ((LiteralExpression)left).getValue();
				return new LiteralExpression(tb, leftVal.toString() + rightStr);
			}
		}
		return new BinaryExpression(tb, left, Operator.PLUS, right);
	}	
	
	ExpressionNode optimizeUnary(Operator op, ExpressionNode operand) {
		if (operand instanceof LiteralExpression) {
			Object value = ((LiteralExpression)operand).getValue();
			if (Operator.MINUS == op  && value instanceof Number) {
				if(value instanceof Integer) return new LiteralExpression(tb, -(Integer)value);
				else if(value instanceof Long) return new LiteralExpression(tb, -(Long)value);
				else if(value instanceof BigInteger) return new LiteralExpression(tb, ((BigInteger)value).negate());
				else if(value instanceof Double) return new LiteralExpression(tb, -(Double)value);
			}
			else if (Operator.NOT == op && value instanceof Boolean) {
				if(value instanceof Boolean) return new LiteralExpression(tb, !(Boolean)value);
			}
			else if (Operator.PLUS == op) {
				return operand;
			}
		}
		return new UnaryExpression(tb, op, operand);
	}
	
	private void collectAdditiveTerms(ExpressionNode node, List<Term> terms, boolean isPositive) {
		if (node instanceof BinaryExpression) {
			BinaryExpression binExpr = (BinaryExpression)node;
			Operator op = binExpr.getOperator();
			if (Operator.PLUS==op || Operator.MINUS==op) {
				collectAdditiveTerms(binExpr.getLeft(), terms, isPositive);
				boolean rightPositive = Operator.PLUS==op ? isPositive : !isPositive;
				terms.add(new Term(op, binExpr.getRight(), rightPositive));
				return;
			}
		}
		terms.add(new Term(isPositive ? Operator.PLUS : Operator.MINUS, node, isPositive));
	}	

	private void collectMultiplicativeTerms(ExpressionNode node, List<Term> terms, boolean isPositive) {
		if (node instanceof BinaryExpression) {
			BinaryExpression binExpr = (BinaryExpression)node;
			Operator op = binExpr.getOperator();
			// Собираем ТОЛЬКО *, /, %
			if (Operator.MULT==op || Operator.DIV==op || Operator.MOD==op) {
				collectMultiplicativeTerms(binExpr.getLeft(), terms, isPositive);
				boolean rightPositive = Operator.DIV!=op ? isPositive : !isPositive;
				terms.add(new Term(op, binExpr.getRight(), rightPositive));
				return;
			}
		}
		terms.add(new Term(isPositive ? Operator.MULT : Operator.DIV, node, isPositive));
	}
	
	private ExpressionNode parseUnary() {
		if (tb.match(TokenType.OPERATOR)) {
			Operator operator = ((Operator)tb.current().getValue()); //TODO check it
			
			if(operator.isUnary()) {
				tb.consume();
				return new UnaryExpression(tb, operator, parseUnary());
			}
			else if (operator == Operator.INC || operator == Operator.DEC) {
				tb.consume();
				Operator realOp = (operator == Operator.INC) ? Operator.PRE_INC : Operator.PRE_DEC;
				return new UnaryExpression(tb, realOp, parseUnary());
			}
		}
		return parsePostfix();
	}
	
	private ExpressionNode parsePostfix() {
		ExpressionNode expr = parsePrimary();
        
		while (true) {
			if (tb.match(Delimiter.LEFT_BRACKET)) {
				expr = new ArrayExpression(tb, expr);
			}
			else if (tb.match(TokenType.OPERATOR)) { // Обработка постфиксных операторов ++ и --
				Operator operator = (Operator)tb.current().getValue();
				if (operator == Operator.INC || operator == Operator.DEC) {
					tb.consume();
					Operator realOp = (operator == Operator.INC) ? Operator.POST_INC : Operator.POST_DEC;
					expr = new UnaryExpression(tb, realOp, expr);
					continue;
				}
				break;
			}
			else {
				break;
			}
		}

		return expr;
    }

	
	private ExpressionNode parsePrimary() {
		Token token = tb.current();
        
		if(tb.match(TokenType.NUMBER) || tb.match(TokenType.STRING) || tb.match(TokenType.CHAR) || tb.match(TokenType.LITERAL)) {
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
				return new MethodCallExpression(tb, null, token.getValue().toString(), AstNode.parseArguments(tb));
			}
			return new VariableExpression(tb, token.getValue().toString());
		}
		else {
			throw new ParseError("Unexpected token in expression: " + token, tb.current().getLine(), tb.current().getColumn());
        }
    }
}