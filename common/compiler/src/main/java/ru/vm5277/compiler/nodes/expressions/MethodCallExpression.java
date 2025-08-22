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

//TODO пересмотреть getType и postAnalyze, код как минимум не оптимизирован
package ru.vm5277.compiler.nodes.expressions;

import java.util.Arrays;
import java.util.List;
import ru.vm5277.common.StrUtils;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.Operand;
import ru.vm5277.common.cg.OperandType;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.scopes.CGClassScope;
import ru.vm5277.common.cg.scopes.CGFieldScope;
import ru.vm5277.common.cg.scopes.CGMethodScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.cg.scopes.CGVarScope;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.AliasSymbol;
import ru.vm5277.compiler.semantic.ClassScope;
import ru.vm5277.compiler.semantic.InterfaceScope;
import ru.vm5277.compiler.semantic.MethodSymbol;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.semantic.Symbol;

public class MethodCallExpression extends ExpressionNode {
	private			ExpressionNode			className;
	private	final	String					methodName;
	private	final	List<ExpressionNode>	args;
	private			VarType					type;
	private			VarType[]				argTypes;
	private			VarType					classType;
	
	public MethodCallExpression(TokenBuffer tb, MessageContainer mc, ExpressionNode className, String methodName, List<ExpressionNode> arguments) {
        super(tb, mc);
        
		this.className = className;
		this.methodName = methodName;
        this.args = arguments;
    }

	public String getMethodName() {
		return methodName;
	}
	
	public ExpressionNode getClassName() {
		return className;
	}

	public List<ExpressionNode> getArguments() {
		return args;
	}
	
	@Override
	public VarType getType(Scope scope) throws CompileException {
		//поиск метода в текущем классе
		if(null == className) {
			ClassScope classScope = scope.getThis();
			if(null != classScope) {
				classType = VarType.fromClassName(classScope.getName());
			}
		}

		// Проверка существования parent (если есть)
		if (null == classType && null != className) {
			classType = className.getType(scope);
		}
		if (null == classType) throw new CompileException("Cannot resolve:" + className.toString());
		
		// Получаем типы аргументов
		argTypes = new VarType[args.size()];
		for (int i=0; i< args.size(); i++) {
			argTypes[i] = args.get(i).getType(scope);
		}

		// Если есть parent (вызов через объект или класс)
		if (null != classType) {
			// Если parent - класс и это не вызов конструктора, то вызов статического метода
			if (!(this instanceof NewExpression) && classType.isClassType()) {
				ClassScope classScope = scope.getThis().resolveClass(classType.getName());
				if (null == classScope) throw new CompileException("Class '" + classType.getName() + "' not found");

				symbol = classScope.resolveMethod(methodName, argTypes);
				if (symbol != null) return symbol.getType();
			}

			// Если parent - объект (вызов метода экземпляра)
			else {
				// Получаем класс объекта
				ClassScope classScope = scope.getThis().resolveClass(classType.getName());
				if (null == classScope) throw new CompileException("Class '" + classType.getName() + "' not found");

				if(this instanceof NewExpression) {
					symbol = classScope.resolveConstructor(methodName, argTypes);
					if (null == symbol) return null;
					return classType;
				}
				else {
					symbol = classScope.resolveMethod(methodName, argTypes);
					if (null != symbol && !symbol.isStatic()) return symbol.getType();
				}
			}

			throw new CompileException("Method '" + methodName + "(" + StrUtils.toString(argTypes) + ")' not found in " + classType);
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
			for (InterfaceScope interfaceSym : classScope.getInterfaces().values()) {
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

		throw new CompileException("Method '" + methodName + "' not found");
	}
	
	private boolean isArgumentsMatch(Scope scope, MethodSymbol method, VarType[] argTypes) throws CompileException {
		List<VarType> paramTypes = method.getParameterTypes();
		if (paramTypes.size() != argTypes.length) return false;

		for (int i = 0; i < paramTypes.size(); i++) {
			if (!isCompatibleWith(scope, argTypes[i], paramTypes.get(i))) {
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

		if(null != className) {
			if(className instanceof UnresolvedReferenceExpression) {
				UnresolvedReferenceExpression ure = (UnresolvedReferenceExpression)className;
				if("this".equals(ure.getId())) {
					className = new ThisExpression(tb, mc);
				}
				else if(null != VarType.fromClassName(ure.getId())) {
					className = new TypeReferenceExpression(tb, mc, ure.getId());
				}
				else {
					className =  new VarFieldExpression(tb, mc, ure.getId());
				}
			}

			if(!className.preAnalyze()) {
				return false;
			}
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
	public boolean declare(Scope scope) {
		if(null != className) {
			if(!className.declare(scope)) {
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		try {
			ExpressionNode optimizedParentScope = className.optimizeWithScope(scope);
			if(null != optimizedParentScope) {
				className = optimizedParentScope;
			}
		
			try {type = getType(scope);} catch(CompileException e) {markError(e); return false;}; //TODO костыль, нужен для присваивания symbol

			cgScope = cg.enterExpression();		

			// Проверка parent (если есть)
			if (null != className) {
				if (!className.postAnalyze(scope, cg)) return false;

				try {
					VarType parentType = className.getType(scope);
					if (null == parentType) {
						markError("Cannot determine type of parent expression");
						cg.leaveExpression();
						return false;
					}
				}
				catch (CompileException e) {
					markError("Parent type error: " + e.getMessage());
					cg.leaveExpression();
					return false;
				}
			}

			// Проверка аргументов
			for (int i=0; i<args.size(); i++) {
				ExpressionNode arg = args.get(i);
				if (!arg.postAnalyze(scope, cg)) return false;

				try {
					ExpressionNode optimizedExpr = arg.optimizeWithScope(scope);
					if(null != optimizedExpr) {
						args.set(i, optimizedExpr);
					}
				}
				catch(CompileException e) {
					e.printStackTrace();
				}
			}
			
			// Получаем типы аргументов
			argTypes = new VarType[args.size()];
			for (int i=0; i<args.size(); i++) {
				argTypes[i]=(args.get(i).getType(scope));
			}

			// Поиск метода в ClassScope
			String className = null;
			if(null != this.className) {
				if(this.className instanceof TypeReferenceExpression) {
					TypeReferenceExpression tre = (TypeReferenceExpression)this.className;
					className = tre.getClassName();
				}
				else if(this.className instanceof VarFieldExpression) {
					className = ((VarFieldExpression)this.className).getType(scope).getClassName();
				}
				else if(this.className instanceof ThisExpression) {
					className = scope.getThis().getName();
				}
				else {
					markError(new CompileException("Unsupported parent type: " + this.className.toString()));
					cg.leaveExpression();
					return false;
				}

				ClassScope classScope = scope.getThis().resolveClass(className);
				if(null == classScope) {
					markError(new CompileException("Class '" + className + "' not found"));
					cg.leaveExpression();
					return false;
				}
				
				if(this instanceof NewExpression) {
					symbol = classScope.resolveConstructor(methodName, argTypes);
					if (symbol == null) {
						markError("Constructor '" + methodName + "(" + StrUtils.toString(argTypes) + ")' not found");
						cg.leaveExpression();
						return false;
					}
				}
				else {
					symbol = classScope.resolveMethod(methodName, argTypes);
					if (symbol == null) {
						markError("Method '" + methodName + "(" + StrUtils.toString(argTypes) + ")' not found");
						cg.leaveExpression();
						return false;
					}
				}
				
				//Ничего не делаем с аргументами, если это нативный метод
				if(!symbol.isNative()) {
					//Создаем алиасы на переменные(из аргументов), которые содержат final переменные или переменные типа ссылки, массива и cstr
					//Для остальных аргументов создаем новые переменные, которые должны быть проинициализирвоаны в кодогенерации
					//и им должны быть присвоены значения переменных


					//TODO не верно. Незачем задавать новые переменные, достаточно поместить значения в params метода
/*					MethodScope mScope = ((MethodSymbol)symbol).getScope();
					for(int i=0; i< args.size(); i++) {
						Symbol pSymbol = ((MethodSymbol)symbol).getParameters().get(i);
						ExpressionNode expr = args.get(i);
						if(expr instanceof LiteralExpression) {
							//Это константа, создаем переменную
							mScope.addVariable(new VarSymbol(pSymbol.getName(), expr.getType(scope), pSymbol.isFinal(), false, mScope, null));
						}
						else if(expr instanceof VarFieldExpression) {
							VarFieldExpression ve = (VarFieldExpression)expr;
							VarSymbol vSymbol = (VarSymbol)ve.getSymbol();
							if(vSymbol.isFinal() || VarType.CSTR == vSymbol.getType() || VarType.CLASS == vSymbol.getType()) {
								mScope.addAlias(pSymbol.getName(), expr.getType(scope), vSymbol);
							}
							else {
								mScope.addVariable(new VarSymbol(pSymbol.getName(), expr.getType(scope), pSymbol.isFinal(), false, mScope, null));
							}
						}
						else {
							markError(new CompileException("TODO Unsupported arg: " + args.get(i)));
							cg.leaveExpression();
							return false;
						}
					}*/
				}
			}
		}
		catch (CompileException e) {
            markError(e.getMessage());
            cg.leaveExpression();
			return false;
        }
		
		cg.leaveExpression();
		return true;
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, boolean accumStore) throws Exception {
		className.codeGen(cg, false);
		
		String classNameStr = ((ClassScope)((MethodSymbol)symbol).getScope().getParent()).getName();

		if(symbol.isNative()) {
			Operand[] operands = null;
			if(!args.isEmpty()) {
				operands = new Operand[args.size()];
				// TODO костыль. Похоже нужно управлять областью видимости универсально, а не точечно
				// Необходимо, чтобы код зависимостей формировался не там, где он находится в древе AST, а там, где он необходим, что оптимально для выделения
				// регистров
				CGScope oldCGScope = cg.setScope(cgScope);//symbol.getCGScope());
				for(int i=0; i<args.size(); i++) {
					operands[i]= makeNativeOperand(cg, args.get(i));
				}
				cg.setScope(oldCGScope);
			}

			VarType[] params = null;
			if(!args.isEmpty()) {
				params = new VarType[args.size()];
				for(int i=0; i<args.size(); i++) {
					params[i] = ((MethodSymbol)symbol).getParameters().get(i).getType();
				}
			}

			cg.invokeNative(cgScope, classNameStr, symbol.getName(), (null == params ? null : Arrays.toString(params)), type, operands);
		}
		else {
			if(0 == argTypes.length && "getClassId".equals(symbol.getName())) {
				if(className instanceof VarFieldExpression) {
					CGCellsScope cScope = (CGCellsScope)((VarFieldExpression)className).getSymbol().getCGScope();
					// Только для информирования
					cg.invokeMethod(cgScope, classNameStr, symbol.getName(), type, argTypes, null);
					cg.cellsToAcc(cgScope, cScope);
				}
			}
			else if(0 == argTypes.length && "getClassTypeId".equals(symbol.getName())) {
				// Только для информирования
				cg.invokeMethod(cgScope, classNameStr, symbol.getName(), type, argTypes, null);
				cg.constToAcc(cgScope, cg.getRefSize(), classType.getId());
			}
			else {
				int refTypeSize = 1; // TODO Определить значение на базе количества используемых типов класса
				cg.pushHeapReg(cgScope);
				if(this instanceof NewExpression) {
					putArgsToStack(cg, refTypeSize);
				}
				else {
					if(className instanceof VarFieldExpression) {
						CGCellsScope cScope = (CGCellsScope)((VarFieldExpression)className).getSymbol().getCGScope();
						if(cScope instanceof CGVarScope) {
							cg.setHeapReg(cgScope, ((CGVarScope)cScope).getStackOffset(), cScope.getCells());
						}
						else {
							cg.setHeapReg(cgScope, ((CGClassScope)((CGFieldScope)cScope).getParent()).getHeapHeaderSize(), cScope.getCells());
						}
					}
					putArgsToStack(cg, refTypeSize);
				}
				depCodeGen(cg);

				CGMethodScope mScope = (CGMethodScope)symbol.getCGScope();
				cg.invokeMethod(cgScope, classNameStr, symbol.getName(), type, argTypes, mScope);
				cg.popHeapReg(cgScope);
			}
		}
		
		return null;
	}
	
	private Operand makeNativeOperand(CodeGenerator cg, ExpressionNode expr) throws Exception {
		if(expr instanceof VarFieldExpression) {
			VarFieldExpression ve = (VarFieldExpression)expr;
			// Кодогенерация VarFieldExpression выполняет запись значения в аккумулятор, но нам этого не нужно, выполняет только зависимость
			ve.depCodeGen(cg);
			return new Operand(VarType.CSTR == ve.getType(null) ? OperandType.FLASH_RES : OperandType.LOCAL_RES, ve.getSymbol().getCGScope().getResId());
		}
		else if(expr instanceof FieldAccessExpression) {
			FieldAccessExpression fae = (FieldAccessExpression)expr;
			fae.codeGen(cg);
			return new Operand(OperandType.LOCAL_RES, fae.getSymbol().getCGScope().getResId());
		}
		else if(expr instanceof LiteralExpression) {
			LiteralExpression le = (LiteralExpression)expr;
			if(VarType.CSTR == le.getType(null)) {
				CGVarScope vScope = cg.enterLocal(VarType.CSTR, -1, true, null);
				cg.leaveLocal();
				vScope.setDataSymbol(cg.defineData(vScope.getResId(), (String)le.getValue()));
				return new Operand(OperandType.FLASH_RES, vScope.getResId());
			}
			else {
				return new Operand(OperandType.LITERAL, le.getNumValue());
			}
		}
		else if(expr instanceof MethodCallExpression) {
			MethodCallExpression mce = (MethodCallExpression)expr;
			mce.codeGen(cg);
			return new Operand(OperandType.ACCUM, null);
		}
		else if(expr instanceof CastExpression) {
			CastExpression ce = (CastExpression)expr;
			ce.codeGen(cg);
			return makeNativeOperand(cg, ce.getOperand());
		}
		else if(expr instanceof BinaryExpression) {
			BinaryExpression be = (BinaryExpression)expr;
			be.codeGen(cg);
			return new Operand(OperandType.ACCUM, null);
		}
		else throw new Exception("Unexpected expression:" + expr);
	}
	
	private void putArgsToStack(CodeGenerator cg, int refTypeSize) throws Exception {
		if(!args.isEmpty()) {
			CGScope oldCGScope = cg.setScope(symbol.getCGScope());
			for(int i=0; i<args.size(); i++) {
				// Получаем параметр вызываемого метода
				Symbol paramSymbol = ((MethodSymbol)symbol).getParameters().get(i);
				VarType paramVarType = paramSymbol.getType();

				// Получаем выражение передаваемое этому параметру
				ExpressionNode argExpr = args.get(i);
				// Выполняем зависимость
				argExpr.depCodeGen(cg);
				// Получаю из scope вызываемого метода переменую с именем параметра(пустая переменная уже создана, или создан Alias)
				Symbol vSymbol = ((MethodSymbol)symbol).getScope().resolve(paramSymbol.getName());
				if(vSymbol instanceof AliasSymbol) {
					// Это алиас, который нужно что???
					throw new CompileException("Unsupported alias:" + vSymbol.toString());
				}
				else {
					int exprTypeSize = argTypes[i].getSize();
					if(-1 == exprTypeSize) exprTypeSize = cg.getRefSize();
					// Создаем новую область видимости переменной в вызываемом методе
					int varSize = paramVarType.isObject() ? refTypeSize + cg.getRefSize() : exprTypeSize;
					CGVarScope dstVScope = cg.enterLocal(vSymbol.getType(), varSize, false, vSymbol.getName());

					// Выделяем память в стеке
					dstVScope.build();
					cg.leaveLocal();
					vSymbol.setCGScope(dstVScope);

					if(argExpr instanceof LiteralExpression) {
						cg.pushConst(cgScope, exprTypeSize, ((LiteralExpression)argExpr).getNumValue());
					}
					else if(argExpr instanceof VarFieldExpression) {
						cg.pushCells(cgScope, 0, exprTypeSize, ((CGCellsScope)argExpr.getSymbol().getCGScope()).getCells());
					}
					else {
						throw new CompileException("Unsupported expression:" + argExpr);
					}
				}
			}
			cg.setScope(oldCGScope);
		}
	}
	
	@Override
	public List<AstNode> getChildren() {
		return Arrays.asList(className); //TODO проверить
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + " " + type + " " + className.toString() + "." + methodName + "(" + StrUtils.toString(argTypes) + ")";
	}
}