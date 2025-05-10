/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes.expressions;

import java.util.ArrayList;
import java.util.List;
import ru.vm5277.j8b.compiler.enums.Delimiter;
import ru.vm5277.j8b.compiler.enums.Keyword;
import ru.vm5277.j8b.compiler.enums.Operator;
import static ru.vm5277.j8b.compiler.enums.Operator.AND;
import static ru.vm5277.j8b.compiler.enums.Operator.BIT_AND;
import static ru.vm5277.j8b.compiler.enums.Operator.BIT_OR;
import static ru.vm5277.j8b.compiler.enums.Operator.BIT_XOR;
import static ru.vm5277.j8b.compiler.enums.Operator.DIV;
import static ru.vm5277.j8b.compiler.enums.Operator.EQ;
import static ru.vm5277.j8b.compiler.enums.Operator.GT;
import static ru.vm5277.j8b.compiler.enums.Operator.GTE;
import static ru.vm5277.j8b.compiler.enums.Operator.LT;
import static ru.vm5277.j8b.compiler.enums.Operator.LTE;
import static ru.vm5277.j8b.compiler.enums.Operator.MINUS;
import static ru.vm5277.j8b.compiler.enums.Operator.MOD;
import static ru.vm5277.j8b.compiler.enums.Operator.MULT;
import static ru.vm5277.j8b.compiler.enums.Operator.NEQ;
import static ru.vm5277.j8b.compiler.enums.Operator.OR;
import static ru.vm5277.j8b.compiler.enums.Operator.PLUS;
import ru.vm5277.j8b.compiler.enums.TokenType;
import ru.vm5277.j8b.compiler.enums.VarType;
import ru.vm5277.j8b.compiler.exceptions.ParseException;
import ru.vm5277.j8b.compiler.exceptions.SemanticException;
import ru.vm5277.j8b.compiler.nodes.AstNode;
import ru.vm5277.j8b.compiler.nodes.TokenBuffer;
import ru.vm5277.j8b.compiler.semantic.Scope;
import ru.vm5277.j8b.compiler.tokens.Token;

public class ExpressionNode extends AstNode {
	public ExpressionNode(TokenBuffer tb) {
		super(tb);
	}
	
    public ExpressionNode parse() throws ParseException {
        return parseAssignment();
    }
   
	private ExpressionNode parseAssignment() throws ParseException{
		ExpressionNode left = parseBinary(0);
        
        if (tb.match(TokenType.OPERATOR) && ((Operator)tb.current().getValue()).isAssignment()) {
            Operator operator = ((Operator)consumeToken(tb).getValue());
            ExpressionNode right = parseAssignment();
			
			return new BinaryExpression(tb, left, operator, right);
        }
        
        return left;
    }    
	
	private ExpressionNode parseBinary(int minPrecedence) throws ParseException {
        ExpressionNode left = parseUnary();

		while (tb.match(TokenType.OPERATOR)) {
            Operator operator = ((Operator)tb.current().getValue());

			if (operator == Operator.TERNARY) {
				if (minPrecedence > Operator.PRECEDENCE.get(operator)) {
					break;
				}
				consumeToken(tb);

				ExpressionNode trueExpr = parseBinary(Operator.PRECEDENCE.get(operator));
				consumeToken(tb, Delimiter.COLON);
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
            consumeToken(tb);
			
			ExpressionNode right = parseBinary(precedence + (operator.isAssignment() ? 0 : 1));
			left = new BinaryExpression(tb, left, operator, right);
		}
        return left;
    }
	
/*	// Агрессивная оптимизация
	private ExpressionNode optimizeOperationChain(ExpressionNode left, Operator op, ExpressionNode right, int precedence) throws ParseException {
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
					default: throw tb.parseError("Invalid comparision operator: " + op);
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

	private ExpressionNode optimizeExpression(ExpressionNode node) throws ParseException {
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
	*/
	private ExpressionNode parseUnary() throws ParseException {
		if (tb.match(TokenType.OPERATOR)) {
			Operator operator = ((Operator)tb.current().getValue()); //TODO check it
			
			if(operator.isUnary()) {
				consumeToken(tb);
				return new UnaryExpression(tb, operator, parseUnary());
			}
			else if (operator == Operator.INC || operator == Operator.DEC) {
				consumeToken(tb);
				Operator realOp = (operator == Operator.INC) ? Operator.PRE_INC : Operator.PRE_DEC;
				return new UnaryExpression(tb, realOp, parseUnary());
			}
		}
		return parsePostfix();
	}
	
	private ExpressionNode parsePostfix() throws ParseException {
		ExpressionNode expr = parsePrimary();
        
		while (true) {
			if (tb.match(Delimiter.LEFT_BRACKET)) {
				expr = new ArrayExpression(tb, expr);
			}
			else if (tb.match(TokenType.OPERATOR)) { // Обработка постфиксных операторов ++ и --
				Operator operator = (Operator)tb.current().getValue();
				if (operator == Operator.INC || operator == Operator.DEC) {
					consumeToken(tb);
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

	
	private ExpressionNode parsePrimary() throws ParseException {
		Token token = tb.current();
        
		if(tb.match(Keyword.NEW)) {
			consumeToken(tb); // Потребляем 'new'

			// Парсим имя класса
			String className = consumeToken(tb, TokenType.ID).getValue().toString();

			// Парсим аргументы конструктора
			List<ExpressionNode> args = parseArguments(tb);
			return new NewExpression(tb, className, args);
		}
		if(tb.match(TokenType.NUMBER) || tb.match(TokenType.STRING) || tb.match(TokenType.CHAR) || tb.match(TokenType.LITERAL)) {
			consumeToken(tb);
			return new LiteralExpression(tb, token.getValue());
		}
		else if(tb.match(Delimiter.LEFT_PAREN)) {
			consumeToken(tb);
			ExpressionNode expr = parse();
			consumeToken(tb, Delimiter.RIGHT_PAREN);
			return expr;
		}
		else if(tb.match(TokenType.ID)) {
			consumeToken(tb);
			if(tb.match(Delimiter.LEFT_PAREN)) {
				return new MethodCallExpression(tb, null, token.getValue().toString(), parseArguments(tb));
			}
			return new VariableExpression(tb, token.getValue().toString());
		}
		else {
			throw new ParseException("Unexpected token in expression: " + token, tb.current().getSP());
        }
    }

	protected boolean isUnaryOperationValid(VarType type, Operator op) {
		switch (op) {
			case NOT: return VarType.BOOL == type;
			case BIT_NOT: return type.isInteger();
			case PLUS:
			case MINUS: return type.isNumeric();
			default: return false;
		}
	}

	@Override
	public String getNodeType() {
		return "expression";
	}

	public VarType getType(Scope scope) throws SemanticException {
		throw new SemanticException("Not supported here.");
	}
//	public abstract VarType getType(Scope scope) throws SemanticException;
	
	@Override
	public boolean preAnalyze() {
		return false;
	}

	@Override
	public boolean declare(Scope scope) {
		return true;
	}

	@Override
	public boolean postAnalyze(Scope scope) {
		return false;
	}
}
