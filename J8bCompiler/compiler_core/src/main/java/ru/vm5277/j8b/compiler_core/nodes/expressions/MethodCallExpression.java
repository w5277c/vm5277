/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
----------------------------------------------------------------------------------------------------------------------------------------------------------------
;TODO пересмотреть getType и postAnalyze, код как минимум не оптимизирован
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler_core.nodes.expressions;

import java.util.ArrayList;
import java.util.List;
import ru.vm5277.j8b.compiler.common.CodeGenerator;
import ru.vm5277.j8b.compiler.common.Operand;
import ru.vm5277.j8b.compiler.common.enums.VarType;
import ru.vm5277.j8b.compiler.common.exceptions.SemanticException;
import ru.vm5277.j8b.compiler_core.messages.MessageContainer;
import ru.vm5277.j8b.compiler_core.nodes.TokenBuffer;
import ru.vm5277.j8b.compiler_core.semantic.ClassScope;
import ru.vm5277.j8b.compiler_core.semantic.InterfaceSymbol;
import ru.vm5277.j8b.compiler_core.semantic.MethodSymbol;
import ru.vm5277.j8b.compiler_core.semantic.Scope;

public class MethodCallExpression extends ExpressionNode {
	private	final	ExpressionNode			parent;
	private	final	String					methodName;
	private	final	List<ExpressionNode>	args;
	private			MethodSymbol			symbol;
	
    public MethodCallExpression(TokenBuffer tb, MessageContainer mc, ExpressionNode parent, String methodName, List<ExpressionNode> arguments) {
        super(tb, mc);
        
		this.parent = parent;
		this.methodName = methodName;
        this.args = arguments;
    }

	public String getMethodName() {
		return methodName;
	}
	
	public ExpressionNode getParent() {
		return parent;
	}

	public List<ExpressionNode> getArguments() {
		return args;
	}
	
	@Override
	public VarType getType(Scope scope) throws SemanticException {
		VarType parentType = null;
		//поиск метода в текущем классе
		if(null == parent) {
			ClassScope classScope = Scope.getThis(scope);
			if(null != classScope) {
				parentType = VarType.fromClassName(classScope.getName());
			}
		}

		// Проверка существования parent (если есть)
		if (null == parentType && null != parent) {
			try {
				parentType = parent.getType(scope);
				if (null == parentType) throw new SemanticException("Cannot resolve parent expression");
			}
			catch (SemanticException e) {
				throw new SemanticException("Invalid parent in method call: " + e.getMessage());
			}
		}
		
		// Получаем типы аргументов
		List<VarType> argTypes = new ArrayList<>();
		for (ExpressionNode arg : args) {
			argTypes.add(arg.getType(scope));
		}

		// Если есть parent (вызов через объект или класс)
		if (null != parentType) {
			// Если parent - класс (статический вызов)
			if (parentType.isClassType()) {
				ClassScope classScope = scope.resolveClass(parentType.getName());
				if (null == classScope) throw new SemanticException("Class '" + parentType.getName() + "' not found");

				symbol = classScope.resolveMethod(methodName, argTypes);
				if (symbol != null) return symbol.getType();
			}

			// Если parent - объект (вызов метода экземпляра)
			else {
				// Получаем класс объекта
				ClassScope classScope = scope.resolveClass(parentType.getName());
				if (null == classScope) throw new SemanticException("Class '" + parentType.getName() + "' not found");

				symbol = classScope.resolveMethod(methodName, argTypes);
				if (null != symbol && !symbol.isStatic()) return symbol.getType();
			}

			throw new SemanticException("Method '" + methodName + "' not found in " + parentType);
		}

		// Вызов метода текущего класса (без parent)
		if (scope instanceof ClassScope) {
			symbol = ((ClassScope)scope).resolveMethod(methodName, argTypes);
			if (null != symbol) return symbol.getType();
		}

		// TODO Проверка статических импортов
/*		if (scope instanceof ClassScope) {
			MethodSymbol method = ((ClassScope)scope).resolveStaticImport(methodName, argTypes);
			if (null != method) return method.getType();
		}
*/
		// Проверка интерфейсов (если scope - класс)
		if (scope instanceof ClassScope) {
			ClassScope classScope = (ClassScope)scope;
			for (InterfaceSymbol interfaceSym : classScope.getInterfaces().values()) {
				List<MethodSymbol> methods = interfaceSym.getMethods(methodName);
				if (null != methods) {
					for (MethodSymbol interfaceMethod : methods) {
						if (isArgumentsMatch(scope, interfaceMethod, argTypes)) {
							symbol = interfaceMethod;
							return interfaceMethod.getType();
						}
					}
				}
			}
		}

		throw new SemanticException("Method '" + methodName + "' not found");
	}
	
	public MethodSymbol getMethod() {
		return symbol;
	}
	
	private boolean isArgumentsMatch(Scope scope, MethodSymbol method, List<VarType> argTypes) {
		List<VarType> paramTypes = method.getParameterTypes();
		if (paramTypes.size() != argTypes.size()) return false;

		for (int i = 0; i < paramTypes.size(); i++) {
			if (!isCompatibleWith(scope, argTypes.get(i), paramTypes.get(i))) {
				return false;
			}
		}
		return true;
	}
	@Override
	public boolean preAnalyze() {
		if (methodName == null || methodName.isEmpty()) {
			markError("Method name cannot be empty");
			return false;
		}

		if (args == null) {
			markError("Arguments list cannot be null");
			return false;
		}

		for (ExpressionNode arg : args) {
			if (arg == null) {
				markError("Argument cannot be null");
				return false;
			}
			if (!arg.preAnalyze()) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public boolean postAnalyze(Scope scope) {

		try {getType(scope);} catch(SemanticException e) {markError(e);}; //TODO костыль, нужен для присваивания symbol
		// Проверка parent (если есть)
		if (null != parent) {
			if (!parent.postAnalyze(scope)) return false;

			try {
				VarType parentType = parent.getType(scope);
				if (null == parentType) {
					markError("Cannot determine type of parent expression");
					return false;
				}
			}
			catch (SemanticException e) {
				markError("Parent type error: " + e.getMessage());
				return false;
			}
		}

		try {		
			// Проверка аргументов
			for (ExpressionNode arg : args) {
				if (!arg.postAnalyze(scope)) return false;
			}

			// Получаем типы аргументов
			List<VarType> argTypes = new ArrayList<>();
			for (ExpressionNode arg : args) {
				argTypes.add(arg.getType(scope));
			}

			// Поиск метода в ClassScope
			if (scope instanceof ClassScope) {
				ClassScope classScope = (ClassScope)scope;
				symbol = classScope.resolveMethod(methodName, argTypes);
				if (symbol == null) {
					markError("Method '" + methodName + "' not found");
					return false;
				}
			}
		}
		catch (SemanticException e) {
            markError(e.getMessage());
            return false;
        }
		return true;
	}
	
	@Override
	public void codeGen(CodeGenerator cg) {
		Operand[] operands = null;
		if(!args.isEmpty()) {
			
			operands = new Operand[args.size()];
			for(int i=0; i<args.size(); i++) {
				args.get(i).codeGen(cg);
				operands[i] = cg.getAcc();
			}
		}
		cg.invokeMethod(symbol.getRuntimeId(), operands);
	}
}