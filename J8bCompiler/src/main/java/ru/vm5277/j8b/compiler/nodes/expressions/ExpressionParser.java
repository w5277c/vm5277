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
			return new BinaryExpression(tb, left, operator, right);
        }
        
        return left;
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
	
	private ExpressionNode optimizeOperationChain(ExpressionNode left, Operator op, ExpressionNode right, int precedence) {
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
		if (op == Operator.PLUS) {
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
		collectTerms(left, terms, true);
		terms.add(new Term(op, right, op != Operator.MINUS));

		double constValue = 0;
		List<Term> varTerms = new ArrayList<>();
		boolean hasDoubles = false;

		for (Term term : terms) {
			if (term.isNumber()) {
				Number num = (Number)((LiteralExpression)term.getNode()).getValue();
				double val = num.doubleValue();

				if (num instanceof Double) {
					hasDoubles = true;
				}
				constValue = term.isPositive() ? constValue + val : constValue - val;
			}
			else {
				varTerms.add(term);
			}
		}

		ExpressionNode result = null;
		if (constValue != 0) {
			result = new LiteralExpression(tb, hasDoubles ? constValue : (long)constValue);
		}

		for (Term term : varTerms) {
			ExpressionNode node = term.getNode();
//			if (!term.isPositive()) {
//				node = new BinaryExpression(tb, result, Operator.MINUS, node);//new UnaryExpression(tb, Operator.MINUS, node);
//			}
//			result = result == null ? node : new BinaryExpression(tb, result, Operator.PLUS, node);
			result = new BinaryExpression(tb, result, term.isPositive() ? Operator.PLUS : Operator.MINUS, node);
		}
		return result != null ? result : new LiteralExpression(tb, hasDoubles ? 0.0 : 0L);
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

		// Оптимизация цепочек
		List<Term> terms = new ArrayList<>();
		collectTerms(left, terms, true);
		terms.add(new Term(op, right, op == Operator.MULT));

		double constValue = 1;
		List<Term> varTerms = new ArrayList<>();
		boolean hasDoubles = false;
		boolean hasDivision = false;

		for (Term term : terms) {
			if (term.isNumber()) {
				Number num = (Number)((LiteralExpression)term.getNode()).getValue();
				double val = num.doubleValue();

				if (num instanceof Double) {
					hasDoubles = true;
				}

				switch (term.getOperator()) {
					case MULT:
						constValue *= val;
						break;
					case DIV:
						constValue /= val;
						hasDivision = true;
						break;
					case MOD:
						constValue %= val;
						hasDivision = true;
						break;
				}
			}
			else {
				varTerms.add(term);
			}
		}

		ExpressionNode result = null;
		if (constValue != 1 || hasDivision) {
			result = new LiteralExpression(tb, hasDoubles || hasDivision ? constValue : (long)constValue);
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

	private void collectTerms(ExpressionNode node, List<Term> terms, boolean isPositive) {
		if (node instanceof BinaryExpression) {
			BinaryExpression binExpr = (BinaryExpression)node;
			Operator op = binExpr.getOperator();

			if (Operator.PLUS == op || Operator.MINUS == op || Operator.MULT == op || Operator.DIV == op || Operator.MOD == op) {
				// Рекурсивно обрабатываем левую часть с текущим знаком
				collectTerms(binExpr.getLeft(), terms, isPositive);
				// Определяем знак для правой части
				boolean rightPositive = isPositive;
				if (binExpr.getOperator() == Operator.MINUS || binExpr.getOperator() == Operator.DIV) {
					rightPositive = !rightPositive;
				}
				// Добавляем правую часть
				terms.add(new Term(binExpr.getOperator(), binExpr.getRight(), rightPositive));
				return;
			}
		}
		terms.add(new Term(isPositive ? Operator.PLUS : Operator.MINUS, node, isPositive));
	}
	
	private ExpressionNode parseUnary() {
		if (tb.match(TokenType.OPERATOR)) {
			Operator operator = ((Operator)tb.current().getValue()); //TODO check it
			if(operator.isUnary()) {
				tb.consume();
				ExpressionNode operand = parseUnary();							
				
				if (Operator.MINUS == operator  && operand instanceof LiteralExpression) {
					Object value = ((LiteralExpression)operand).getValue();
					if(value instanceof Integer) return new LiteralExpression(tb, -(Integer)value);
					else if(value instanceof Long) return new LiteralExpression(tb, -(Long)value);
					else if(value instanceof BigInteger) return new LiteralExpression(tb, ((BigInteger)value).negate());
					else if(value instanceof Double) return new LiteralExpression(tb, -(Double)value);
				}
				else if (Operator.NOT == operator && operand instanceof LiteralExpression) {
					Object value = ((LiteralExpression)operand).getValue();
					if(value instanceof Boolean) return new LiteralExpression(tb, !(Boolean)value);
				}
				else if (Operator.PLUS == operator) {
					return parseUnary(); // Пропускаем '+' и парсим дальше
				}
			}
			else if (operator == Operator.INC || operator == Operator.DEC) {
				tb.consume();
				
				Operator realOp = (operator == Operator.INC) ? Operator.PRE_INC : Operator.PRE_DEC;
				return new UnaryExpression(tb, realOp, parseUnary());
			}
			tb.consume();
			ExpressionNode operand = parseUnary();							
			return new UnaryExpression(tb, operator, operand);
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
		String methodName = (target instanceof VariableExpression) ? ((VariableExpression)target).getValue() : "";
        return new MethodCallExpression(tb, target, methodName, args);
    }
}