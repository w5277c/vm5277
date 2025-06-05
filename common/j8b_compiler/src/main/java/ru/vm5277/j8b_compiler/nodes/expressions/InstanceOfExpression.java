/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
13.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b_compiler.nodes.expressions;

import ru.vm5277.common.j8b_compiler.CodeGenerator;
import ru.vm5277.common.j8b_compiler.Operand;
import ru.vm5277.common.j8b_compiler.VarType;
import ru.vm5277.common.exceptions.SemanticException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.j8b_compiler.nodes.TokenBuffer;
import ru.vm5277.j8b_compiler.semantic.ClassScope;
import ru.vm5277.j8b_compiler.semantic.InterfaceSymbol;
import ru.vm5277.j8b_compiler.semantic.Scope;

public class InstanceOfExpression extends ExpressionNode {
	private	final	ExpressionNode	left;		// Проверяемое выражение
	private	final	ExpressionNode	rightExpr;	// Выражение, возвращающее тип
	private			VarType			leftType;
	private			VarType			rightType;
	private			boolean			fulfillsContract;	//Флаг реализации интерфейса
	
	public InstanceOfExpression(TokenBuffer tb, MessageContainer mc, ExpressionNode left, ExpressionNode typeExpr) {
		super(tb, mc);
		this.left = left;
		this.rightExpr = typeExpr;
	}

	@Override
	public VarType getType(Scope scope) throws SemanticException {
		// Результат instanceof всегда boolean
		return VarType.BOOL;
	}

	public ExpressionNode getLeft() {
		return left;
	}

	public ExpressionNode getTypeExpr() {
		return rightExpr;
	}

	@Override
	public boolean preAnalyze() {
		if (left == null || rightExpr == null) {
			markError("Both operands of 'is' must be non-null");
			return false;
		}
		return left.preAnalyze() && rightExpr.preAnalyze();
	}
	
	@Override
	public boolean postAnalyze(Scope scope) {
		try {
			if (!left.postAnalyze(scope)) {
				return false;
			}

			// Анализируем правую часть (тип)
			if (!rightExpr.postAnalyze(scope)) {
				return false;
			}

			// Проверяем, что typeExpr возвращает тип (класс)
			rightType = rightExpr.getType(scope);
			if (!rightType.isClassType()) {
				markError("Right-hand side of 'is' must be a class type, got: " + rightType);
				return false;
			}

			leftType = left.getType(scope);
			if (left instanceof LiteralExpression) {
				markError("Cannot check type of literals at runtime");
				return false;
			}
			
			// Проверка реализации интерфейса
			if (leftType.isClassType()) {
				if(leftType.getClassName().equals(rightType.getClassName())) {
					fulfillsContract = true;
				}
				else {
					ClassScope leftClass = scope.getThis().resolveClass(leftType.getClassName());
					InterfaceSymbol rightInterface = scope.getThis().resolveInterface(rightType.getClassName());

					
					if (null != leftClass && null != rightInterface) {
						fulfillsContract = leftClass.getInterfaces().containsKey(rightInterface.getName());
					}
				}
			}
			
			return true;
		}
		catch (SemanticException e) {
			markError(e.getMessage());
			return false;
		}
	}

	public VarType getLeftType() {
		return leftType;
	}
	
	public VarType getRightType() {
		return rightType;
	}

	public boolean isFulfillsContract() {
		return fulfillsContract;
	}
	
	@Override
	public void codeGen(CodeGenerator cg) throws Exception {
		left.codeGen(cg);
		Operand objectOp = cg.getAcc();
		rightExpr.codeGen(cg);
		Operand result = cg.emitInstanceof(objectOp, (int)cg.getAcc().getValue());
		cg.setAcc(result);
	}
	
	@Override
	public String toString() {
		return left + " is " + rightExpr;
	}
}