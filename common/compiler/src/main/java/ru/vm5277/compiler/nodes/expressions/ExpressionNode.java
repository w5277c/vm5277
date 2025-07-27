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
package ru.vm5277.compiler.nodes.expressions;

import java.util.ArrayList;
import java.util.List;
import ru.vm5277.common.cg.Operand;
import ru.vm5277.common.cg.OperandType;
import ru.vm5277.compiler.Delimiter;
import ru.vm5277.compiler.Keyword;
import ru.vm5277.common.Operator;
import static ru.vm5277.common.Operator.MINUS;
import static ru.vm5277.common.Operator.PLUS;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.compiler.TokenType;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.FieldNode;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.nodes.VarNode;
import ru.vm5277.compiler.semantic.AstHolder;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.semantic.Symbol;
import ru.vm5277.compiler.tokens.Token;

public class ExpressionNode extends AstNode {
	protected	CGScope	cgScope;
	
	public ExpressionNode(TokenBuffer tb, MessageContainer mc) {
		super(tb, mc);
	}
	
    public ExpressionNode parse() throws CompileException {
        return parseAssignment();
    }
   
	public ExpressionNode optimizeWithScope() throws CompileException {
		return this;
	};
	
	private ExpressionNode parseAssignment() throws CompileException {
		ExpressionNode left = parseBinary(0);

		if (tb.match(TokenType.OPERATOR) && ((Operator)tb.current().getValue()).isAssignment()) {
			Operator operator = ((Operator)consumeToken(tb).getValue());
			ExpressionNode right = parseAssignment();

			return optimizeExpression(new BinaryExpression(tb, mc, left, operator, right));
		}

		return optimizeExpression(left);
	}
	
	
	private ExpressionNode parseBinary(int minPrecedence) throws CompileException {
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
				return new TernaryExpression(tb, mc, left, trueExpr, falseExpr);
			}

			if (operator == Operator.IS) {
				consumeToken(tb); // Пропускаем 'is'

				// Запрещаем литералы слева
				if (left instanceof LiteralExpression) throw new CompileException("Literals cannot be used with 'instanceof'", left.getSP());

				if (minPrecedence > Operator.PRECEDENCE.get(operator)) {
					break;
				}
				
				ExpressionNode typeExpr = null;
				if(tb.match(TokenType.TYPE)) {
					typeExpr = new LiteralExpression(tb, mc, checkPrimtiveType());
				}
				else {
					typeExpr = parseTypeReference(); // Разбираем выражение типа
				}
				String varName = null;
				if (tb.match(Keyword.AS)) {
					tb.consume(); // Потребляем "as"
					// Проверяем, что после типа идет идентификатор
					if (tb.match(TokenType.ID)) {
						varName = consumeToken(tb).getStringValue();
					}
					else {
						markError("TODO Expected variable name after type in pattern matching");
					}
				}
				return optimizeExpression(new InstanceOfExpression(tb, mc, left, typeExpr, varName));
			}
			
			Integer precedence = Operator.PRECEDENCE.get(operator);
            if (precedence == null || precedence<minPrecedence) {
				break;
			}
            consumeToken(tb);
			
			ExpressionNode right = parseBinary(precedence + (operator.isAssignment() ? 0 : 1));
			left = optimizeOperationChain(left, operator, right, precedence);
			//left = new BinaryExpression(tb, mc, left, operator, right);
		}
        return left;
    }
	
	// Агрессивная оптимизация
	private ExpressionNode optimizeOperationChain(ExpressionNode left, Operator op, ExpressionNode right, int precedence) throws CompileException {
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
					case AND:	return new LiteralExpression(tb, mc, ((Boolean)leftVal) && ((Boolean)rightVal));
					case OR:	return new LiteralExpression(tb, mc, ((Boolean)leftVal) || ((Boolean)rightVal));
					case EQ:	return new LiteralExpression(tb, mc, ((Boolean)leftVal) == ((Boolean)rightVal));
					case NEQ:	return new LiteralExpression(tb, mc, ((Boolean)leftVal) != ((Boolean)rightVal));
					default:	return new BinaryExpression(tb, mc, new LiteralExpression(tb, mc, left), op, new LiteralExpression(tb, mc, right));
				}
			}
			else if (op.isComparison() && leftVal instanceof Number && rightVal instanceof Number) {
				double delta = ((Number)leftVal).doubleValue() - ((Number)rightVal).doubleValue();
				switch(op) {
					case EQ:	return new LiteralExpression(tb, mc, 0d == delta);
					case NEQ:	return new LiteralExpression(tb, mc, 0d != delta);
					case LT:	return new LiteralExpression(tb, mc, 0d > delta);
					case GT:	return new LiteralExpression(tb, mc, 0d < delta);
					case LTE:	return new LiteralExpression(tb, mc, 0d <= delta);
					case GTE:	return new LiteralExpression(tb, mc, 0d >= delta);
					default: throw parserError("Invalid comparision operator: " + op);
				}
			}
		}

		if (op.isAssignment() && right instanceof BinaryExpression) {
			BinaryExpression bRight = (BinaryExpression)right;
			if (left instanceof VarFieldExpression && (Operator.PLUS==bRight.getOperator() || Operator.MINUS==bRight.getOperator())) {
				if(	(bRight.getLeft() instanceof VarFieldExpression && bRight.getRight() instanceof LiteralExpression) ||
					(bRight.getLeft() instanceof LiteralExpression && bRight.getRight() instanceof VarFieldExpression)) {
					
					VarFieldExpression ve = (VarFieldExpression)(bRight.getLeft() instanceof VarFieldExpression ? bRight.getLeft() : bRight.getRight());
					LiteralExpression le = (LiteralExpression)(bRight.getLeft() instanceof VarFieldExpression ? bRight.getRight(): bRight.getLeft());
					if(	ve.getValue().equals(((VarFieldExpression)left).getValue()) && le.isInteger()) {
						long num = le.getNumValue();
						if(-1 == num || 1 == num) {
							return new UnaryExpression(tb, mc, (Operator.PLUS==bRight.getOperator()^(-1==num)) ? Operator.PRE_INC : Operator.PRE_DEC, left);
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
				return new BinaryExpression(tb, mc, left, op, right);
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
					return new LiteralExpression(tb, mc, result);
				}
				else {
					long a = ((Number)leftVal).longValue();
					long b = ((Number)rightVal).longValue();
					long result = op == Operator.PLUS ? a + b : a - b;
					return new LiteralExpression(tb, mc, result);
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
				result = new LiteralExpression(tb, mc, totalConst);
			}
		}
		else if (constValueLong != 0) {
			result = new LiteralExpression(tb, mc, constValueLong);
		}

		for (Term term : varTerms) {
			ExpressionNode node = term.getNode();
			if(null == result) {
				result = term.isPositive() ? node : new UnaryExpression(tb, mc, Operator.MINUS, node);
			}
			else {
				//Перестановка операндов местами ломает сворачивание констант
				result = new BinaryExpression(tb, mc, result, term.isPositive() ? Operator.PLUS : Operator.MINUS, node);
			}
		}
		return result != null ? result : new LiteralExpression(tb, mc, hasDoubles ? 0.0 : 0L);
	}

	private ExpressionNode optimizeExpression(ExpressionNode node) throws CompileException {
		// Оптимизация цепочек примитивных приведений типов
		if (node instanceof CastExpression) {
			CastExpression leftCast = (CastExpression)node;
			// Если операнд - тоже приведение типа
			ExpressionNode operand = optimizeExpression(leftCast.getOperand());
			CastExpression result = new CastExpression(tb, mc, leftCast.getType(), operand);
			
			if(operand instanceof CastExpression) {
				CastExpression rightCast = (CastExpression)operand;
				// Если оба типа примитивные
				if (leftCast.getType().isPrimitive() && rightCast.getType().isPrimitive()) {
					// Если внутреннее приведение - расширяющее, его можно убрать
					if (leftCast.getType().getSize() <= rightCast.getType().getSize()) {
						return optimizeExpression(new CastExpression(tb, mc, leftCast.getType(), rightCast.getOperand()));
					}
				}
			}
			else if(operand instanceof LiteralExpression) {
				LiteralExpression le = (LiteralExpression) operand;
				VarType newType = leftCast.getType();
				
				Object value = null;
				if(VarType.BOOL == newType) {
					value = (byte)(le.getNumValue() & 0x01);
				}
				else if(newType.isInteger()) {
					value = (long)(le.getNumValue() & ((1l<<(newType.getSize()*8))-1));
				}
				else if(VarType.FIXED == newType) {
					double tmp = le.getValue() instanceof Double ? (Double)le.getValue() : (double)le.getNumValue();
					// Ограничиваем целую часть диапазоном FIXED (Q7.8)
					value = Math.max(-128.0, Math.min(127.99609375, tmp));
				}
				if(null != value) {
					return new LiteralExpression(tb, mc, value);
				}
			}
			return result;
		}
		
		if (node instanceof InstanceOfExpression) {
			InstanceOfExpression ioe = (InstanceOfExpression)node;

			// Проверка Object справа (все объекты - экземпляры Object)
			if (ioe.getRightType() != null && ioe.getRightType().isClassType() && "Object".equals(ioe.getRightType().getClassName()))
				return new LiteralExpression(tb, mc, true);
			
			return ioe;
		}
		
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
	
	
	// Оптимизация выражения с использованием Scope (поздний этап).
	// Осторожно с новыми инстансами выражений, теряем проинициализированные при семантическом анализе поля.
	public ExpressionNode optimizeWithScope(Scope scope) throws CompileException {
		if (this instanceof CastExpression) {
			CastExpression cast = (CastExpression)this;
			ExpressionNode operand = cast.getOperand().optimizeWithScope(scope);
			if(null == operand) operand = cast.getOperand();

			VarType operandType = operand.getType(scope);
			
			// Если внутреннее приведение - расширяющее, его можно убрать
			if (null != operandType && cast.getType().isPrimitive() && operandType.isPrimitive() && cast.getType().getSize() >= operandType.getSize()) {
				return operand;
			}
			return null;
		}

		// Заменяем переменные на их значения из Scope (если они final и известны)
		if (this instanceof VarFieldExpression) {
			VarFieldExpression vfe = (VarFieldExpression)this;
			if(vfe.getSymbol() instanceof AstHolder) {
				AstNode node = ((AstHolder)vfe.getSymbol()).getNode();
				if(node instanceof VarNode) {
					VarNode vNode = (VarNode)node;
					if(vNode.isFinal() && VarType.CSTR != vNode.getType() && vNode.getInitializer() instanceof LiteralExpression) {
						return vNode.getInitializer();
					}
				}
				else if(node instanceof FieldNode) {
					FieldNode fNode = (FieldNode)node;
					if (fNode.isStatic() && fNode.isFinal() && VarType.CSTR != fNode.getType() && fNode.getInitializer() instanceof LiteralExpression) {
						return fNode.getInitializer();
					}
				}
			}
		}
		
		if (this instanceof InstanceOfExpression) {
			InstanceOfExpression ioe = (InstanceOfExpression)this;

			// Оптимизация через флаг из postAnalyze
			if (ioe.isFulfillsContract()) return new LiteralExpression(tb, mc, true);

			// Оптимизация для final объектов
			if (ioe.getLeft() instanceof VarFieldExpression) {
				if(ioe.getLeftType().isPrimitive() && ioe.getLeftType() == ioe.getRightType()) {
					return new LiteralExpression(tb, mc, true);
				}

				VarFieldExpression varExpr = (VarFieldExpression)ioe.getLeft();
				Symbol varSymbol = varExpr.getSymbol();

				if (null != varSymbol && varSymbol.isFinal() && ioe.getLeftType() != null && ioe.getRightType() != null) {
					VarType leftType = ioe.getLeftType();
					Operand op = null;//varSymbol.getConstantOperand();
					//TODO
					if(null != op && OperandType.TYPE == op.getOperandType()) {
						varSymbol = scope.resolve((String)op.getValue());
						if(null != varSymbol) leftType = varSymbol.getType();
					}
					// Точное совпадение типов
					if (leftType == ioe.getRightType()) return new LiteralExpression(tb, mc, true);

					// Массивы одинаковой размерности
					if (leftType.isArray() && ioe.getRightType().isArray()) {
						// Совпадает размерность?
						if (leftType.getArrayDepth() != ioe.getRightType().getArrayDepth())  return new LiteralExpression(tb, mc, false);

						// Object[] совместим с любым массивом
						if ("Object".equals(ioe.getRightType().getClassName())) return new LiteralExpression(tb, mc, true);

						// Проверка типа элементов
						if (leftType.getClassName().equals(ioe.getRightType().getClassName())) return new LiteralExpression(tb, mc, true);
						
						// Все остальные случаи -> false
						return new LiteralExpression(tb, mc, false);
					}
				}
			}
			return ioe;
		}

		if (this instanceof FieldAccessExpression) {
			FieldAccessExpression fae = (FieldAccessExpression)this;
			FieldNode fNode = (FieldNode)((AstHolder)fae.getSymbol()).getNode();
			if (fNode.isStatic() && fNode.isFinal() && fNode.getInitializer() instanceof LiteralExpression) {
				return fNode.getInitializer();
			}
		}
		
		if (this instanceof BinaryExpression) {
			BinaryExpression bin = (BinaryExpression) this;
			ExpressionNode left = bin.getLeft().optimizeWithScope(scope);
			ExpressionNode right = bin.getRight().optimizeWithScope(scope);
			return optimizeOperationChain(left, bin.getOperator(), right, 0);
		}
		return null;
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
						case MULT: return new LiteralExpression(tb, mc, a * b);
						case DIV:  return new LiteralExpression(tb, mc, a / b);
						case MOD:  return new LiteralExpression(tb, mc, a % b);
						default: throw new IllegalArgumentException();
					}
				}
				else {
					long a = ((Number)leftVal).longValue();
					long b = ((Number)rightVal).longValue();
					switch (op) {
						case MULT: return new LiteralExpression(tb, mc, a * b);
						case DIV:  return new LiteralExpression(tb, mc, a / b);
						case MOD:  return new LiteralExpression(tb, mc, a % b);
						default: throw new IllegalArgumentException();
					}
				}
			}
		}

		if (Operator.MOD == op) {
			return new BinaryExpression(tb, mc, left, op, right);
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
				result = new LiteralExpression(tb, mc, totalConst);
			}
		}
		else if (constValueLong != 1 || hasDivision) {
			result = new LiteralExpression(tb, mc, constValueLong);
		}

		for (Term term : varTerms) {
			ExpressionNode node = term.getNode();
			result = result == null ? node : new BinaryExpression(tb, mc, result, term.getOperator(), node);
		}
		return result != null ? result : new LiteralExpression(tb, mc, hasDoubles ? 1.0 : 1L);
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
				return new LiteralExpression(tb, mc, result);
			}
		}
		return new BinaryExpression(tb, mc, left, op, right);
	}
	
	private ExpressionNode optimizeStringConcat(ExpressionNode left, ExpressionNode right) {
		if (left instanceof LiteralExpression && right instanceof LiteralExpression) {
			Object leftVal = ((LiteralExpression)left).getValue();
			Object rightVal = ((LiteralExpression)right).getValue();

			if (leftVal instanceof String && rightVal instanceof String) {
				return new LiteralExpression(tb, mc, (String)leftVal + (String)rightVal);
			}
		}
		if (left instanceof LiteralExpression && ((LiteralExpression)left).getValue() instanceof String) {
			String leftStr = (String)((LiteralExpression)left).getValue();
			if (right instanceof LiteralExpression) {
				Object rightVal = ((LiteralExpression)right).getValue();
				return new LiteralExpression(tb, mc, leftStr + rightVal.toString());
			}
			return new BinaryExpression(tb, mc, left, Operator.PLUS, right);
		}
		if (right instanceof LiteralExpression && ((LiteralExpression)right).getValue() instanceof String) {
			String rightStr = (String)((LiteralExpression)right).getValue();
			if (left instanceof LiteralExpression) {
				Object leftVal = ((LiteralExpression)left).getValue();
				return new LiteralExpression(tb, mc, leftVal.toString() + rightStr);
			}
		}
		return new BinaryExpression(tb, mc, left, Operator.PLUS, right);
	}	
	
	ExpressionNode optimizeUnary(Operator op, ExpressionNode operand) {
		if (operand instanceof LiteralExpression) {
			Object value = ((LiteralExpression)operand).getValue();
			if (Operator.MINUS == op  && value instanceof Number) {
				if(value instanceof Integer) return new LiteralExpression(tb, mc, -(Integer)value);
				else if(value instanceof Long) return new LiteralExpression(tb, mc, -(Long)value);
				else if(value instanceof Double) return new LiteralExpression(tb, mc, -(Double)value);
			}
			else if (Operator.NOT == op && value instanceof Boolean) {
				if(value instanceof Boolean) return new LiteralExpression(tb, mc, !(Boolean)value);
			}
			else if (Operator.PLUS == op) {
				return operand;
			}
		}
		return new UnaryExpression(tb, mc, op, operand);
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
	
	private ExpressionNode parseUnary() throws CompileException {
		// Обработка приведения типов (type)expr
		if (tb.match(Delimiter.LEFT_PAREN)) {
			consumeToken(tb);
			VarType type = checkPrimtiveType();
			if (type == null) {
				type = checkClassType();
			}

			if (type != null) {
				consumeToken(tb, Delimiter.RIGHT_PAREN);
				ExpressionNode expr = parse();
				return new CastExpression(tb, mc, type, expr);
			}
			else {
				tb.back();
			}
		}
		
		if (tb.match(TokenType.OPERATOR)) {
			Operator operator = ((Operator)tb.current().getValue()); //TODO check it
			
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
			}
			else if (operator == Operator.INC || operator == Operator.DEC) {
				consumeToken(tb);
				Operator realOp = (operator == Operator.INC) ? Operator.PRE_INC : Operator.PRE_DEC;
				return new UnaryExpression(tb, mc, realOp, parseUnary());
			}
		}
		return parsePostfix();
	}
	
	private ExpressionNode parsePostfix() throws CompileException {
		ExpressionNode expr = parsePrimary();
        
		while (true) {
			if (tb.match(Delimiter.LEFT_BRACKET)) {
				expr = new ArrayExpression(tb, mc, expr);
			}
			else if (tb.match(TokenType.OPERATOR)) { // Обработка постфиксных операторов ++ и --
				Operator operator = (Operator)tb.current().getValue();
				if (operator == Operator.INC || operator == Operator.DEC) {
					consumeToken(tb);
					Operator realOp = (operator == Operator.INC) ? Operator.POST_INC : Operator.POST_DEC;
					expr = new UnaryExpression(tb, mc, realOp, expr);
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

	
	private ExpressionNode parsePrimary() throws CompileException {
		Token token = tb.current();
        
		if(tb.match(Keyword.NEW)) {
			consumeToken(tb); // Потребляем 'new'

			// Парсим имя класса
			String className = consumeToken(tb, TokenType.ID).getValue().toString();

			// Парсим аргументы конструктора
			List<ExpressionNode> args = parseArguments(tb);
			return new NewExpression(tb, mc, className, args);
		}
		if(tb.match(TokenType.NUMBER) || tb.match(TokenType.STRING) || tb.match(TokenType.CHAR) || tb.match(TokenType.LITERAL)) {
			consumeToken(tb);
			return new LiteralExpression(tb, mc, token.getValue());
		}
		else if(tb.match(Delimiter.LEFT_PAREN)) {
			consumeToken(tb);
			ExpressionNode expr = parse();
			consumeToken(tb, Delimiter.RIGHT_PAREN);
			return expr;
		}
		else if(tb.match(TokenType.ID)) {
			ExpressionNode expr = parseFullQualifiedExpression(tb);

			// Парсим цепочку идентификаторов через точки
//			ExpressionNode expr = parseQualifiedName();
			
			// Если после цепочки идет '(', то это вызов метода
			if(tb.match(Delimiter.LEFT_PAREN)) {
				return parseMethodCall(expr);
			}
			return expr;
		}
		else {
			throw new CompileException("Unexpected token in expression: " + token, tb.current().getSP());
        }
    }
	
	private ExpressionNode parseMethodCall(ExpressionNode target) throws CompileException {
		// Проверяем, является ли target MemberAccessExpression или VariableExpression
		String methodName;
		if(target instanceof FieldAccessExpression) {
			methodName = ((FieldAccessExpression)target).getFieldName();
			target = ((FieldAccessExpression)target).getTarget();
		}
		else if(target instanceof VarFieldExpression) {
			methodName = ((VarFieldExpression)target).getValue();
			target = null; // Статический вызов
		}
		else {
			throw new CompileException("Invalid method call target", tb.current().getSP());
		}

		List<ExpressionNode> args = parseArguments(tb);
		return new MethodCallExpression(tb, mc, target, methodName, args);
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
	
	public VarType getType(Scope scope) throws CompileException {
		throw new CompileException("Not supported here.");
	}
//	public abstract VarType getType(Scope scope) throws CompileException;
	
	@Override
	public boolean preAnalyze() {
		return false;
	}

	@Override
	public boolean declare(Scope scope) {
		return true;
	}

	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		return false;
	}

	@Override
	public List<AstNode> getChildren() {
		return null;
	}
	
	public void setSymbol(Symbol symbol) {
		this.symbol = symbol;
	}
}
