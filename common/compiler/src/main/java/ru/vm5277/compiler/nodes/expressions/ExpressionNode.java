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

import ru.vm5277.compiler.nodes.expressions.bin.BinaryExpression;
import java.util.ArrayList;
import java.util.List;
import ru.vm5277.common.NumUtils;
import ru.vm5277.compiler.Delimiter;
import ru.vm5277.compiler.Keyword;
import ru.vm5277.common.Operator;
import static ru.vm5277.common.Operator.MINUS;
import static ru.vm5277.common.Operator.PLUS;
import ru.vm5277.common.Property;
import static ru.vm5277.common.SemanticAnalyzePhase.DECLARE;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.compiler.TokenType;
import ru.vm5277.common.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import static ru.vm5277.compiler.Main.debugAST;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.CatchBlock;
import ru.vm5277.compiler.nodes.FieldNode;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.nodes.VarNode;
import ru.vm5277.compiler.nodes.expressions.bin.ComparisonExpression;
import ru.vm5277.compiler.semantic.AstHolder;
import ru.vm5277.compiler.semantic.EnumScope;
import ru.vm5277.compiler.semantic.ExceptionScope;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.semantic.Symbol;
import ru.vm5277.compiler.semantic.VarSymbol;
import ru.vm5277.compiler.tokens.Token;

public class ExpressionNode extends AstNode {
	protected	VarType type;
	
	public ExpressionNode(TokenBuffer tb, MessageContainer mc) {
		super(tb, mc);
	}
	
	public ExpressionNode(TokenBuffer tb, MessageContainer mc, SourcePosition sp) {
		super(tb, mc, sp);
	}

	public ExpressionNode parse() throws CompileException {
        return parseAssignment();
    }
   
	private ExpressionNode parseAssignment() throws CompileException {
		ExpressionNode left = parseBinary(0);

		if (tb.match(TokenType.OPERATOR) && ((Operator)tb.current().getValue()).isAssignment()) {
			Operator operator = ((Operator)consumeToken(tb).getValue());
			ExpressionNode right = parseAssignment();

			return optimizeExpression(BinaryExpression.create(tb, mc, tb.getSP(), left, operator, right));
		}

		return optimizeExpression(left);
	}
	
	
	private ExpressionNode parseBinary(int minPrecedence) throws CompileException {
        ExpressionNode left = parseUnary();

		while(tb.match(TokenType.OPERATOR)) {
            Operator operator = ((Operator)tb.current().getValue());

			if(operator==Operator.TERNARY) {
				if(minPrecedence>Operator.PRECEDENCE.get(operator)) {
					break;
				}
				consumeToken(tb);

				ExpressionNode trueExpr = parseBinary(Operator.PRECEDENCE.get(operator));
				consumeToken(tb, Delimiter.COLON);
				ExpressionNode falseExpr = parseBinary(Operator.PRECEDENCE.get(operator));
				if(left instanceof LiteralExpression) {
					Object val = ((LiteralExpression)left).getValue();
					if(val instanceof Boolean) {
						return (Boolean)val ? trueExpr : falseExpr;
					}
				}
				return new TernaryExpression(tb, mc, sp, left, trueExpr, falseExpr);
			}

			if(operator==Operator.IS) {
				consumeToken(tb); // Пропускаем 'is'

				// Запрещаем литералы слева
				if(left instanceof LiteralExpression) {
					throw new CompileException("Literals cannot be used with 'instanceof'", left.getSP());
				}

				if(minPrecedence>Operator.PRECEDENCE.get(operator)) {
					break;
				}
				
				ExpressionNode typeExpr = null;
				if(tb.match(TokenType.TYPE)) {
					VarType type = checkPrimtiveType();
					if(null == type) type = checkClassType();
					if(null != type) type = checkArrayType(type);
					typeExpr = new LiteralExpression(tb, mc, type);
				}
				else {
					typeExpr = parseTypeReference(); // Разбираем выражение типа
				}
				String varName = null;
				if(tb.match(Keyword.AS)) {
					tb.consume(); // Потребляем "as"
					// Проверяем, что после типа идет идентификатор
					if(tb.match(TokenType.ID)) {
						varName = consumeToken(tb).getStringValue();
					}
					else {
						markError("TODO Expected variable name after type in pattern matching");
					}
				}
				return optimizeExpression(new InstanceOfExpression(tb, mc, left, typeExpr, varName));
			}
			
			Integer precedence = Operator.PRECEDENCE.get(operator);
            if(precedence == null || precedence<minPrecedence) {
				break;
			}
            consumeToken(tb);
			
			ExpressionNode right = parseBinary(precedence + (operator.isAssignment() ? 0 : 1));
			left = optimizeOperationChain(left, operator, right);
			//left = new BinaryExpression(tb, mc, left, operator, right);
		}
        return left;
    }
	
	// Агрессивная оптимизация
	private ExpressionNode optimizeOperationChain(ExpressionNode left, Operator op, ExpressionNode right) throws CompileException {
		// Оптимизация унарных операций (если левая часть - унарный оператор)
		if (right instanceof UnaryExpression) {
			UnaryExpression unary = (UnaryExpression)right;
			right = optimizeUnary(unary);
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
					default:	return BinaryExpression.create(	tb, mc, left.getSP(), new LiteralExpression(tb, mc, left), op,
																new LiteralExpression(tb, mc, right));
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
		else if(Operator.AND == op &&
				left instanceof LiteralExpression && ((LiteralExpression)left).isBoolean() && 1==((LiteralExpression)left).getNumValue()) {
			return right;
		}
		else if(Operator.AND == op &&
				right instanceof LiteralExpression && ((LiteralExpression)right).isBoolean() && 1==((LiteralExpression)right).getNumValue()) {
			return left;
		}

		
		if (op.isAssignment() && right instanceof BinaryExpression) {
			BinaryExpression bRight = (BinaryExpression)right;
			if (left instanceof VarFieldExpression && (Operator.PLUS==bRight.getOperator() || Operator.MINUS==bRight.getOperator())) {
				if(	(bRight.getLeft() instanceof VarFieldExpression && bRight.getRight() instanceof LiteralExpression) ||
					(bRight.getLeft() instanceof LiteralExpression && bRight.getRight() instanceof VarFieldExpression)) {
					
					VarFieldExpression ve = (VarFieldExpression)(bRight.getLeft() instanceof VarFieldExpression ? bRight.getLeft() : bRight.getRight());
					LiteralExpression le = (LiteralExpression)(bRight.getLeft() instanceof VarFieldExpression ? bRight.getRight(): bRight.getLeft());
					if(	ve.getName().equals(((VarFieldExpression)left).getName()) && le.isInteger()) {
						long num = le.getNumValue();
						if(-1 == num || 1 == num) {
							return new UnaryExpression(	tb, mc, left.getSP(),
														(Operator.PLUS==bRight.getOperator()^(-1==num)) ? Operator.PRE_INC : Operator.PRE_DEC, left);
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
				return BinaryExpression.create(tb, mc, left.getSP(), left, op, right);
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

		// Собираем все термы additive цепочки
		List<ExpressionNode> positiveTerms = new ArrayList<>();   // Положительные слагаемые
		List<ExpressionNode> negativeTerms = new ArrayList<>();   // Отрицательные слагаемые
		// Собираем термы из левой части
		collectAdditiveTerms(left, positiveTerms, negativeTerms);
		
		// Добавляем правую часть в соответствующую группу
		if (op == Operator.PLUS) positiveTerms.add(right);
		else negativeTerms.add(right);
		

		// Разделяем на константы и переменные
		double totalConstant = 0.0;
		List<ExpressionNode> varPositiveTerms = new ArrayList<>();
		List<ExpressionNode> varNegativeTerms = new ArrayList<>();
		
		// Обрабатываем положительные слагаемые
		for(ExpressionNode term : positiveTerms) {
			if(term instanceof LiteralExpression && ((LiteralExpression)term).getValue() instanceof Number) {
				Number num = (Number)((LiteralExpression)term).getValue();
				totalConstant += num.doubleValue();
			}
			else {
				varPositiveTerms.add(term);
			}
		}
		
		// Обрабатываем отрицательные слагаемые
		for(ExpressionNode term : negativeTerms) {
			if(term instanceof LiteralExpression && ((LiteralExpression)term).getValue() instanceof Number) {
				Number num = (Number)((LiteralExpression)term).getValue();
				totalConstant -= num.doubleValue();
			}
			else {
				varNegativeTerms.add(term);
			}
		}
		
		// Строим результирующее выражение
		ExpressionNode result = null;
		
		// Добавляем положительные переменные слагаемые
		for(ExpressionNode varTerm : varPositiveTerms) {
			if(result == null) {
				result = varTerm;
			}
			else {
				result = BinaryExpression.create(tb, mc, left.getSP(), result, Operator.PLUS, varTerm);
			}
		}
		
		// Добавляем отрицательные переменные слагаемые
		for(ExpressionNode varTerm : varNegativeTerms) {
			if(result == null) {
				result = new UnaryExpression(tb, mc, left.getSP(), Operator.MINUS, varTerm);
			}
			else {
				result = BinaryExpression.create(tb, mc, left.getSP(), result, Operator.MINUS, varTerm);
			}
		}

		// Добавляем константу (если она не равна нулю)
		if(totalConstant != 0.0) {
			ExpressionNode constantNode = createOptimalConstantNode(totalConstant);
			if(result == null) {
				result = constantNode;
			}
			else if(totalConstant > 0) {
				result = BinaryExpression.create(tb, mc, left.getSP(), result, Operator.PLUS, constantNode);
			}
			else {
				// Для отрицательных констант используем вычитание положительного значения
				result = BinaryExpression.create(tb, mc, left.getSP(), result, Operator.MINUS, createOptimalConstantNode(-totalConstant));
			}
		}

		return result != null ? result : new LiteralExpression(tb, mc, totalConstant);
	}
	private ExpressionNode createOptimalConstantNode(double value) {
		// Проверяем, является ли значение целым числом
		if (value == (long)value) {
			// Целое число - используем long
			return new LiteralExpression(tb, mc, (long) value);
		} else {
			// Дробное число - используем double
			return new LiteralExpression(tb, mc, value);
		}
	}
	private void collectAdditiveTerms(ExpressionNode node, List<ExpressionNode> positiveTerms, List<ExpressionNode> negativeTerms) {
		if (node instanceof BinaryExpression) {
			BinaryExpression bin = (BinaryExpression) node;
			Operator op = bin.getOperator();

			if (op == Operator.PLUS || op == Operator.MINUS) {
				// Рекурсивно собираем термы из левой части
				collectAdditiveTerms(bin.getLeft(), positiveTerms, negativeTerms);

				// Для правой части: PLUS добавляет как положительное, MINUS добавляет как отрицательное
				if (op == Operator.PLUS) {
					collectAdditiveTerms(bin.getRight(), positiveTerms, negativeTerms);
				} else {
					collectAdditiveTerms(bin.getRight(), negativeTerms, positiveTerms);
				}
				return;
			}
		}

		if (node instanceof UnaryExpression) {
			UnaryExpression unary = (UnaryExpression) node;
			if (unary.getOperator() == Operator.MINUS) {
				// Унарный минус - добавляем как отрицательное слагаемое
				collectAdditiveTerms(unary.getOperand(), negativeTerms, positiveTerms);
				return;
			}
		}

		// Базовый случай: простой терм добавляем как положительное слагаемое
		positiveTerms.add(node);
	}
	private ExpressionNode optimizeExpression(ExpressionNode node) throws CompileException {
		// Оптимизация цепочек примитивных приведений типов
		if (node instanceof CastExpression) {
			CastExpression leftCast = (CastExpression)node;
			// Если операнд - тоже приведение типа
			ExpressionNode operand = optimizeExpression(leftCast.getOperand());
			CastExpression result = new CastExpression(tb, mc, node.getSP(), leftCast.getType(), operand);
			
			if(operand instanceof CastExpression) {
				CastExpression rightCast = (CastExpression)operand;
				// Если оба типа примитивные
				if (leftCast.getType().isPrimitive() && rightCast.getType().isPrimitive()) {
					// Если внутреннее приведение - расширяющее, его можно убрать
					if (leftCast.getType().getSize() <= rightCast.getType().getSize()) {
						return optimizeExpression(new CastExpression(tb, mc, node.getSP(), leftCast.getType(), rightCast.getOperand()));
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
				else if(newType.isIntegral()) {
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
			return optimizeUnary((UnaryExpression)node);
		}

		// Оптимизация бинарных операций
		if (node instanceof BinaryExpression) {
			BinaryExpression bin = (BinaryExpression)node;
			ExpressionNode left = optimizeExpression(bin.getLeft());
			ExpressionNode right = optimizeExpression(bin.getRight());
			return optimizeOperationChain(left, bin.getOperator(), right);
		}

		return node;
	}
	
	
	// Оптимизация выражения с использованием Scope (поздний этап).
	// Осторожно с новыми инстансами выражений, теряем проинициализированные при семантическом анализе поля.
	public ExpressionNode optimizeWithScope(Scope scope, CodeGenerator cg) throws CompileException {
		if(this instanceof QualifiedPathExpression) {
			QualifiedPathExpression qpe = (QualifiedPathExpression)this;
			if(null!=qpe.getResolvedExpr()) {
				ExpressionNode result = qpe.getResolvedExpr();
				ExpressionNode optimizedExpr = result.optimizeWithScope(scope, cg);
				if(null!=optimizedExpr) {
					return optimizedExpr;
				}
				else {
					return result;
				}
			}
		}
		
		if (this instanceof CastExpression) {
			CastExpression cast = (CastExpression)this;
			ExpressionNode operand = cast.getOperand();
			ExpressionNode newOperand = cast.getOperand().optimizeWithScope(scope, cg);
			if(null != newOperand) {
				operand = newOperand;
				operand.postAnalyze(scope, cg);
			}

			VarType operandType = operand.getType();
			
			// Если внутреннее приведение - расширяющее, его можно убрать
			if (null != operandType && cast.getType().isPrimitive() && operandType.isPrimitive()) {
				if(cast.getType() == operandType || (VarType.FIXED != cast.getType() && cast.getType().getSize() >= operandType.getSize())) {
					return operand;
				}
				else if(null != newOperand && cast.getType().isFixedPoint() && operand instanceof LiteralExpression) { 
					return new LiteralExpression(tb, mc, (((LiteralExpression)operand).getNumValue()&0x7f));
				}
			}
			return null;
		}

		// Заменяем переменные на их значения из Scope (если они final и известны)
		if (this instanceof VarFieldExpression) {
			VarFieldExpression vfe = (VarFieldExpression)this;
			if(vfe.getTargetScope() instanceof ExceptionScope) {
				return new LiteralExpression(tb, mc, ((ExceptionScope)vfe.getTargetScope()).getValueIndex(vfe.getName()));
			}
			else if(vfe.getSymbol() instanceof AstHolder) {
				AstNode node = ((AstHolder)vfe.getSymbol()).getNode();
				if(node instanceof VarNode) {
					VarNode vNode = (VarNode)node;
					if(	vNode.isFinal() && VarType.CSTR != vNode.getType() &&
						(	vNode.getType().isEnum() || vNode.getInitializer() instanceof LiteralExpression ||
							vNode.getInitializer() instanceof ArrayExpression || vNode.getInitializer() instanceof VarFieldExpression) ) {
						
						return vNode.getInitializer();
					}
				}
				else if(node instanceof FieldNode) {
					FieldNode fNode = (FieldNode)node;
					if (fNode.isStatic() && fNode.isFinal() && VarType.CSTR != fNode.getType() &&
						(	fNode.getType().isEnum() || fNode.getInitializer() instanceof LiteralExpression ||
							fNode.getInitializer() instanceof ArrayExpression || fNode.getInitializer() instanceof VarFieldExpression)) {
						
						return fNode.getInitializer();
					}
				}
			}
		}
		
		if (this instanceof InstanceOfExpression) {
			InstanceOfExpression ioe = (InstanceOfExpression)this;

			// Оптимизация через флаг из postAnalyze
			if (ioe.isFulfillsContract()) {
				ExpressionNode result = new LiteralExpression(tb, mc, true);
				result.postAnalyze(scope, cg);
				return result;
			}

			// Оптимизация для final объектов
			if (ioe.getLeft() instanceof VarFieldExpression) {
				if(ioe.getLeftType().isPrimitive() && ioe.getLeftType() == ioe.getRightType()) {
					ExpressionNode result = new LiteralExpression(tb, mc, true);
					result.postAnalyze(scope, cg);
					return result;
				}

				VarFieldExpression varExpr = (VarFieldExpression)ioe.getLeft();
				Symbol varSymbol = varExpr.getSymbol();

				if(null!=varSymbol && varSymbol.isFinal() && ioe.getLeftType()!=null && VarType.EXCEPTION!=ioe.getLeftType() && ioe.getRightType()!=null) {
					VarType leftType = ioe.getLeftType();
					if(varSymbol instanceof VarSymbol) {
						AstNode node = ((VarSymbol)varSymbol).getNode();
						if(node instanceof VarNode) {
							ExpressionNode expr = ((VarNode)node).getInitializer();
							if(null != expr) leftType = expr.getType();
						}
						else if(node instanceof FieldNode) {
							ExpressionNode expr = ((FieldNode)node).getInitializer();
							if(null != expr) leftType = expr.getType();
						}
					}

					// Точное совпадение типов
					if(leftType==ioe.getRightType()) {
						ExpressionNode result = new LiteralExpression(tb, mc, true);
						result.postAnalyze(scope, cg);
						return result;
					}

					// Массивы одинаковой размерности
					if (leftType.isArray() && ioe.getRightType().isArray()) {
						// Совпадает размерность?
						if (leftType.getArrayDepth() != ioe.getRightType().getArrayDepth()) {
							ExpressionNode result = new LiteralExpression(tb, mc, false);
							result.postAnalyze(scope, cg);
							return result;
						}

						// Object[] совместим с любым массивом
						if (ioe.getRightType().isObject()) {
							ExpressionNode result = new LiteralExpression(tb, mc, true);
							result.postAnalyze(scope, cg);
							return result;
						}

						// Проверка типа элементов
						if (leftType.getClassName().equals(ioe.getRightType().getClassName())) {
							ExpressionNode result = new LiteralExpression(tb, mc, true);
							result.postAnalyze(scope, cg);
							return result;
						}
						
						// Все остальные случаи -> false
						ExpressionNode result = new LiteralExpression(tb, mc, false);
						result.postAnalyze(scope, cg);
						return result;
					}
				}
			}
			return ioe;
		}
		
		if(this instanceof PropertyExpression) {
			PropertyExpression pe = (PropertyExpression)this;
			if(Property.item==pe.getProperty() && pe.getArguments().get(0) instanceof LiteralExpression) {
				int t=45454;
				TypeReferenceExpression tre = (TypeReferenceExpression)pe.getTargetExpr();
				
				String value = ((EnumScope)tre.getScope()).getValue((int)((LiteralExpression)pe.getArguments().get(0)).getNumValue());
				EnumExpression result = new EnumExpression(tb, mc, tre, value);
				result.postAnalyze(scope, cg);
				return result;
			}
		}
		
		if (this instanceof BinaryExpression) {
			BinaryExpression bin = (BinaryExpression) this;
			ExpressionNode _left = bin.getLeft().optimizeWithScope(scope, cg);
			ExpressionNode _right = bin.getRight().optimizeWithScope(scope, cg);
			//TODO всегда возвращало значение, даже если результат не оптимизирован
//			if(null != _left || null != _right) {
				ExpressionNode result = optimizeOperationChain(	null == _left ? bin.getLeft() : _left, bin.getOperator(),
																null == _right ? bin.getRight(): _right);

				if(result instanceof ComparisonExpression) {
					ComparisonExpression ce = (ComparisonExpression)result;
					if(ce.getLeft() instanceof EnumExpression && ce.getRight() instanceof EnumExpression) {
						if(Operator.EQ==ce.getOperator() || Operator.NEQ==ce.getOperator()) {
							EnumExpression left = (EnumExpression)ce.getLeft();
							EnumExpression right = (EnumExpression)ce.getRight();
							if(left.getTargetScope()==right.getTargetScope()) {
								result = new LiteralExpression(tb, mc, left.getIndex()==right.getIndex()^Operator.EQ!=ce.getOperator());
								result.postAnalyze(scope, cg);
								return result;
							}							
						}
					}
				}


				result.postAnalyze(scope, cg);
				
				return result;
//			}
//			return null;
		}
		
		if(this instanceof UnaryExpression) {
			UnaryExpression ue = (UnaryExpression)this;
			if(Operator.NOT == ue.getOperator() && ue.getOperand() instanceof LiteralExpression) {
				return new LiteralExpression(tb, mc, !((LiteralExpression)ue.getOperand()).getBooleanValue());
			}
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

		// Замена умножения/деления на степени двойки сдвигами
		if(op==Operator.MULT || op==Operator.DIV) {
			ExpressionNode expr1 = left;
			ExpressionNode expr2 = right;
			if(expr1 instanceof LiteralExpression && op.isCommutative()) {
				expr1 = right;
				expr2 = left;
			}
			if(null!=expr1.getType() && expr1.getType().isIntegral() && expr2 instanceof LiteralExpression) {
				if(((LiteralExpression)expr2).isInteger()) {
					int shift = NumUtils.getPowerOfTwo(((LiteralExpression)expr2).getNumValue());
					if(0==shift) {
						return expr1;
					}
					if(0<shift && 64>shift) {
						return BinaryExpression.create(	tb, mc, left.getSP(), expr1, op==Operator.MULT ? Operator.SHL : Operator.SHR,
														new LiteralExpression(tb, mc, shift));
					}
				}
			}
		}

		if (Operator.MOD == op) {
			return BinaryExpression.create(tb, mc, left.getSP(), left, op, right);
		}
		
		// Собираем все термы multiplicative цепочки
		List<ExpressionNode> numerators = new ArrayList<>();   // Числители (умножения)
		List<ExpressionNode> denominators = new ArrayList<>(); // Знаменатели (деления)
		// Собираем термы из левой части
		collectMultiplicativeTerms(left, numerators, denominators);

		// Добавляем правую часть в соответствующую группу
		if (op == Operator.MULT) numerators.add(right);
		else denominators.add(right);

		// Разделяем на константы и переменные
		double totalConstant = 1.0;
		List<ExpressionNode> varNumerators = new ArrayList<>();
		List<ExpressionNode> varDenominators = new ArrayList<>();
		
		// Обрабатываем числители
		for (ExpressionNode term : numerators) {
			if (term instanceof LiteralExpression && ((LiteralExpression)term).getValue() instanceof Number) {
				Number num = (Number)((LiteralExpression)term).getValue();
				totalConstant *= num.doubleValue();
			} else {
				varNumerators.add(term);
			}
		}

		// Обрабатываем знаменатели
		for (ExpressionNode term : denominators) {
			if (term instanceof LiteralExpression && ((LiteralExpression)term).getValue() instanceof Number) {
				Number num = (Number)((LiteralExpression)term).getValue();
				double value = num.doubleValue();
				if(0.0 == value) {
					return BinaryExpression.create(tb, mc, left.getSP(), left, op, right);
				}
				else {
					totalConstant /= value;
				}
			}
			else {
				varDenominators.add(term);
			}
		}

		if (totalConstant == 0.0) {
			// Умножение на 0 - возвращаем 0
			return new LiteralExpression(tb, mc, 0);
		}
		double reciprocalValue = 1.0 / totalConstant;
		
		// Строим результирующее выражение
		ExpressionNode result = null;
		// Добавляем переменные числители
		for (ExpressionNode varTerm : varNumerators) {
			result = (result==null ? varTerm : BinaryExpression.create(tb, mc, left.getSP(), result, Operator.MULT, varTerm));
		}
		
		// Добавляем константу (умножение или деление)
		if (totalConstant != 1.0) {
			if (reciprocalValue >= 1.0 && countDecimalDigits(reciprocalValue) < countDecimalDigits(totalConstant)) {
				ExpressionNode divisorNode = createOptimalConstantNode(reciprocalValue);
				result = (result == null ? 
						BinaryExpression.create(tb, mc, left.getSP(), createOptimalConstantNode(1), Operator.DIV, divisorNode) :
						BinaryExpression.create(tb, mc, left.getSP(), result, Operator.DIV, divisorNode));
			}
			else {
				// Оставляем как есть (не точное значение)
				ExpressionNode constantNode = createOptimalConstantNode(totalConstant);
				result = (result==null ? constantNode : BinaryExpression.create(tb, mc, left.getSP(), result, Operator.MULT, constantNode));
			}
		}
		
		// Добавляем деление на переменные
		for (ExpressionNode denominator : varDenominators) {
			result = (result == null ?
					BinaryExpression.create(tb, mc, left.getSP(), createOptimalConstantNode(1), Operator.DIV, denominator) :
					BinaryExpression.create(tb, mc, left.getSP(), result, Operator.DIV, denominator));
		}

		return result != null ? result : createOptimalConstantNode(totalConstant);
	}
	private int countDecimalDigits(double value) {
		if(value == (long)value) return 0;

		String str = String.valueOf(value);
		int dotPos = str.indexOf('.');
		if(dotPos==-1) return 0;

		int end = str.length() - 1;
		while (end>dotPos && str.charAt(end)=='0') {
			end--;
		}
		return end-dotPos;
	}
	private void collectMultiplicativeTerms(ExpressionNode node, List<ExpressionNode> numerators, List<ExpressionNode> denominators) {
		if (node instanceof BinaryExpression) {
			BinaryExpression bin = (BinaryExpression) node;
			Operator op = bin.getOperator();

			if (op == Operator.MULT || op == Operator.DIV) {
				// Рекурсивно собираем термы из левой части
				collectMultiplicativeTerms(bin.getLeft(), numerators, denominators);

				// Для правой части: MULT добавляет в числители, DIV добавляет в знаменатели
				if (op == Operator.MULT) {
					collectMultiplicativeTerms(bin.getRight(), numerators, denominators);
				} else {
					collectMultiplicativeTerms(bin.getRight(), denominators, numerators);
				}
				return;
			}
		}

		// Базовый случай: простой терм добавляем в числители
		numerators.add(node);
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
		return BinaryExpression.create(tb, mc, left.getSP(), left, op, right);
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
			return BinaryExpression.create(tb, mc, left.getSP(), left, Operator.PLUS, right);
		}
		if (right instanceof LiteralExpression && ((LiteralExpression)right).getValue() instanceof String) {
			String rightStr = (String)((LiteralExpression)right).getValue();
			if (left instanceof LiteralExpression) {
				Object leftVal = ((LiteralExpression)left).getValue();
				return new LiteralExpression(tb, mc, leftVal.toString() + rightStr);
			}
		}
		return BinaryExpression.create(tb, mc, left.getSP(), left, Operator.PLUS, right);
	}	
	
	ExpressionNode optimizeUnary(UnaryExpression ue) {
		Operator op = ue.getOperator();
		ExpressionNode operand = ue.getOperand();
		
		if(operand instanceof LiteralExpression) {
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
		return new UnaryExpression(tb, mc, ue.getSP(), op, operand);
	}
	
	private ExpressionNode parseUnary() throws CompileException {
		// Обработка приведения типов (type)expr
		if(tb.match(Delimiter.LEFT_PAREN)) {
			consumeToken(tb);
			VarType type = checkPrimtiveType();
			if(type == null) type = checkClassType();
			if(null != type) type = checkArrayType(type);

			if(type != null) {
				consumeToken(tb, Delimiter.RIGHT_PAREN);
				ExpressionNode expr = parse();
				return new CastExpression(tb, mc, sp, type, expr);
			}
			else {
				tb.back();
			}
		}
		
		if (tb.match(TokenType.OPERATOR)) {
			Operator operator = ((Operator)tb.current().getValue()); //TODO check it
			
			if(operator.isUnary()) {
				consumeToken(tb);
				return new UnaryExpression(tb, mc, tb.getSP(), operator, parseUnary());
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
				return new UnaryExpression(tb, mc, tb.getSP(), realOp, parseUnary());
			}
		}
		return parsePostfix();
	}
	
	private ExpressionNode parsePostfix() throws CompileException {
		ExpressionNode expr = parsePrimary();
        
		while (true) {
			if (tb.match(TokenType.OPERATOR)) { // Обработка постфиксных операторов ++ и --
				Operator operator = (Operator)tb.current().getValue();
				if (operator == Operator.INC || operator == Operator.DEC) {
					consumeToken(tb);
					Operator realOp = (operator == Operator.INC) ? Operator.POST_INC : Operator.POST_DEC;
					expr = new UnaryExpression(tb, mc, tb.getSP(), realOp, expr);
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

			VarType type = checkPrimtiveType();
			if(null!=type) {
				List<ExpressionNode> arrDimensions = parseArrayDimensions();
				if(null!=arrDimensions) {
					for(int i=0; i<arrDimensions.size(); i++) {
						type = VarType.arrayOf(type);
					}

					ArrayInitExpression aie = null;
					if(tb.match(Delimiter.LEFT_BRACE)) {
						aie = new ArrayInitExpression(tb, mc, tb.getSP(), type);
					}
					return new NewArrayExpression(tb, mc, tb.getSP(), type, arrDimensions, aie);
				}
				else {
					throw new CompileException("Invalid 'new' expression - expected array dimensions after primitive type");
				}
			}
			else {
				List<String> ids = new ArrayList<>();
				while(true) {
					if(tb.match(TokenType.ID)) {
						ids.add(tb.consume().getStringValue());
					}
					else if(tb.match(Delimiter.LEFT_PAREN)) {
						return new NewExpression(tb, mc, sp, ids);
					}
					else if(tb.match(Delimiter.LEFT_BRACKET)) {
						return new NewArrayExpression(tb, mc, tb.getSP(), ids);
					}
					else {
						throw new CompileException("Invalid 'new' expression - expected constructor parentheses or array brackets");
					}
				}
			}
/*			VarType type = checkPrimtiveType();
			if(null != type) {
				List<ExpressionNode> arrDimensions = parseArrayDimensions(); //TODO
				for(int i=0; i<arrDimensions.size(); i++) {
					type = VarType.arrayOf(type);
				}
				
				ArrayInitExpression aie = null;
				if(tb.match(Delimiter.LEFT_BRACE)) {
					aie = new ArrayInitExpression(tb, mc, type);
				}
				return new NewArrayExpression(tb, mc, type, arrDimensions, aie);
			}
			else {
				// Парсим имя класса
				TypeReferenceExpression expr = parseTypeReference();
				return new NewExpression(tb, mc, expr, parseIndices(tb));
			}*/
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
		else if(tb.match(TokenType.ID) || tb.match(TokenType.OOP, Keyword.THIS)) {
			return parseFullQualifiedExpression(tb);
		}
		else {
			throw new CompileException("Unexpected token in expression: " + token, tb.current().getSP());
        }
    }
	
/*	private ExpressionNode parseMethodCall(ExpressionNode target) throws CompileException {
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
	}*/
	
	protected boolean isUnaryOperationValid(VarType type, Operator op) {
		switch (op) {
			case NOT: return VarType.BOOL == type;
			case BIT_NOT: return type.isIntegral();
			case PLUS:
			case MINUS: return type.isNumeric();
			default: return false;
		}
	}

	public VarType getType() {
		if(null!=type) return type;
		debugAST(this, DECLARE, true, "type is null");
		return null;
	}
	
	public String getQualifiedPath() {
		return "";
	}
	
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
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum, CGExcs excs) throws CompileException {
		throw new CompileException("Not supported here.");
	}

	@Override
	public List<AstNode> getChildren() {
		return null;
	}
}
