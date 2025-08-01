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
import ru.vm5277.common.cg.CGCell;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.Operand;
import ru.vm5277.common.cg.OperandType;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.scopes.CGLabelScope;
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
import ru.vm5277.compiler.semantic.InterfaceSymbol;
import ru.vm5277.compiler.semantic.MethodScope;
import ru.vm5277.compiler.semantic.MethodSymbol;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.semantic.Symbol;
import ru.vm5277.compiler.semantic.VarSymbol;

public class MethodCallExpression extends ExpressionNode {
	private			ExpressionNode			parent;
	private	final	String					methodName;
	private	final	List<ExpressionNode>	args;
	private			VarType					type;
	private			VarType[]				argTypes;
	
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
	public VarType getType(Scope scope) throws CompileException {
		VarType parentType = null;
		//поиск метода в текущем классе
		if(null == parent) {
			ClassScope classScope = scope.getThis();
			if(null != classScope) {
				parentType = VarType.fromClassName(classScope.getName());
			}
		}

		// Проверка существования parent (если есть)
		if (null == parentType && null != parent) {
			parentType = parent.getType(scope);
		}
		if (null == parentType) throw new CompileException("Cannot resolve:" + parent.toString());
		
		// Получаем типы аргументов
		argTypes = new VarType[args.size()];
		for (int i=0; i< args.size(); i++) {
			argTypes[i] = args.get(i).getType(scope);
		}

		// Если есть parent (вызов через объект или класс)
		if (null != parentType) {
			// Если parent - класс (статический вызов)
			if (parentType.isClassType()) {
				ClassScope classScope = scope.getThis().resolveClass(parentType.getName());
				if (null == classScope) throw new CompileException("Class '" + parentType.getName() + "' not found");

				symbol = classScope.resolveMethod(methodName, argTypes);
				if (symbol != null) return symbol.getType();
			}

			// Если parent - объект (вызов метода экземпляра)
			else {
				// Получаем класс объекта
				ClassScope classScope = scope.getThis().resolveClass(parentType.getName());
				if (null == classScope) throw new CompileException("Class '" + parentType.getName() + "' not found");

				symbol = classScope.resolveMethod(methodName, argTypes);
				if (null != symbol && !symbol.isStatic()) return symbol.getType();
			}

			throw new CompileException("Method '" + methodName + "' not found in " + parentType);
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

		if(null != parent) {
			if(parent instanceof UnresolvedReferenceExpression) {
				UnresolvedReferenceExpression ure = (UnresolvedReferenceExpression)parent;
				if("this".equals(ure.getId())) {
					parent = new ThisExpression(tb, mc);
				}
				else if(null != VarType.fromClassName(ure.getId())) {
					parent = new TypeReferenceExpression(tb, mc, ure.getId());
				}
				else {
					parent =  new VarFieldExpression(tb, mc, ure.getId());
				}
			}

			if(!parent.preAnalyze()) {
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
		if(null != parent) {
			if(!parent.declare(scope)) {
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		try {
			ExpressionNode optimizedParentScope = parent.optimizeWithScope(scope);
			if(null != optimizedParentScope) {
				parent = optimizedParentScope;
			}
		
			try {type = getType(scope);} catch(CompileException e) {markError(e); return false;}; //TODO костыль, нужен для присваивания symbol

			cgScope = cg.enterExpression();		

			// Проверка parent (если есть)
			if (null != parent) {
				if (!parent.postAnalyze(scope, cg)) return false;

				try {
					VarType parentType = parent.getType(scope);
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
			if(null != parent) {
				if(parent instanceof TypeReferenceExpression) {
					TypeReferenceExpression tre = (TypeReferenceExpression)parent;
					className = tre.getClassName();
				}
				else if(parent instanceof VarFieldExpression) {
					className = ((VarFieldExpression)parent).getType(scope).getClassName();
				}
				else if(parent instanceof ThisExpression) {
					className = scope.getThis().getName();
				}
				else {
					markError(new CompileException("Unsupported parent type: " + parent.toString()));
					cg.leaveExpression();
					return false;
				}

				ClassScope classScope = scope.getThis().resolveClass(className);
				if(null == classScope) {
					markError(new CompileException("Class '" + className + "' not found"));
					cg.leaveExpression();
					return false;
				}
				
				symbol = classScope.resolveMethod(methodName, argTypes);
				if (symbol == null) {
					markError("Method '" + methodName + "' not found");
					cg.leaveExpression();
					return false;
				}
				
				//Ничего не делаем с аргументами, если это нативный метод
				if(!symbol.isNative()) {
					//Создаем алиасы на переменные(из аргументов), которые содержат final переменные или переменные типа ссылки, массива и cstr
					//Для остальных аргументов создаем новые переменные, которые должны быть проинициализирвоаны в кодогенерации
					//и им должны быть присвоены значения переменных
					MethodScope mScope = ((MethodSymbol)symbol).getScope();
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
					}
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
	public Object codeGen(CodeGenerator cg) throws Exception {
		String className = ((ClassScope)((MethodSymbol)symbol).getScope().getParent()).getName();

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

			cg.invokeNative(cgScope, className, symbol.getName(), (null == params ? null : Arrays.toString(params)), type, operands);
		}
		else {
			CGLabelScope rpCGScope = null;
			int refTypeSize = 1; // TODO Определить значение на базе количества используемых типов класса
			int stackOffset = 0;

			if(!args.isEmpty()) {
				CGScope oldCGScope = cg.setScope(symbol.getCGScope());
				
				// !!! Ввожу четкое разделение примитивов и ссылочных типов.
/*				// Сначала(для ссылочных параметров) нужно поместить в стек данные всех аргументов, размер которых больше refSize 
				for(int i=0; i<args.size(); i++) {
					// Получаем выражение передаваемое этому параметру
					ExpressionNode argExpr = args.get(i);
					// Выполняем зависимость
					argExpr.depCodeGen(cg);
					
					Symbol paramSymbol = ((MethodSymbol)symbol).getParameters().get(i);
					VarType paramVarType = paramSymbol.getType();
					// Проверка на ссылочный параметр

					if(paramVarType.isObject() || paramVarType.isReferenceType())	{
						int exprTypeSize = argTypes[i].getSize();
						if(-1 == exprTypeSize) exprTypeSize = cg.getRefSize();
						// Проверяем, что размер занчения больше refSize
						if(cg.getRefSize() < exprTypeSize) {
							// Необходимо поместить данные в стек
							if(argExpr instanceof LiteralExpression) {
								cg.pushConst(cgScope, exprTypeSize, ((LiteralExpression)argExpr).getNumValue());
							}
							else if(argExpr instanceof VarFieldExpression) {
								cg.pushCells(cgScope, exprTypeSize, ((CGCellsScope)argExpr.getSymbol().getCGScope()).getCells());
							}
							else {
								throw new CompileException("Unsupported expression:" + argExpr);
							}
							stackOffset += exprTypeSize;
						}
					}
				}
*/
				// Помещаем в стек адрес возврата
				rpCGScope = cg.makeLabel(cgScope.getMethodScope(), "RP", true);
				cg.pushCells(cgScope, 2, new CGCell[]{new CGCell(rpCGScope.getLName())});

				stackOffset = 0;
				// Теперь создаем необходимые переменные и формируем стек вызываемого метода
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

/*						boolean isRef = paramVarType.isObject() || paramVarType.isReferenceType();
						if(isRef)	{
							// Если тип ожидаемого параметра Object(т.е. любой тип) или reference(класс, интерфес, массив),
							// то сначала передаем ид типа
							cg.pushConst(cgScope, refTypeSize, argTypes[i].getId());
						}
*/
//						// Если размер данных типа не более размера данных ссылки, то помещаем данные
//						if(!isRef || cg.getRefSize() >= exprTypeSize) {
							if(argExpr instanceof LiteralExpression) {
								//cg.pushConst(cgScope, isRef ? cg.getRefSize() : exprTypeSize, ((LiteralExpression)argExpr).getNumValue());
								cg.pushConst(cgScope, exprTypeSize, ((LiteralExpression)argExpr).getNumValue());
							}
							else if(argExpr instanceof VarFieldExpression) {
								//cg.pushCells(cgScope, isRef ? cg.getRefSize() : exprTypeSize, ((CGCellsScope)argExpr.getSymbol().getCGScope()).getCells());
								cg.pushCells(cgScope, exprTypeSize, ((CGCellsScope)argExpr.getSymbol().getCGScope()).getCells());
							}
							else {
								throw new CompileException("Unsupported expression:" + argExpr);
							}
//						}
//						else {
//							// Размер больше. Данные ранее созранены в стек
//							cg.pushConst(cgScope, cg.getRefSize(), stackOffset);
//							stackOffset += exprTypeSize;
//						}
					}
				}
				cg.setScope(oldCGScope);
			}
			depCodeGen(cg);
			
			CGMethodScope mScope = (CGMethodScope)symbol.getCGScope();
			cg.invokeMethod(cgScope, className, symbol.getName(), type, argTypes, mScope);
			if(null != rpCGScope) cgScope.append(rpCGScope);
			if(0 != stackOffset) {
				cg.stackFree(cgScope, stackOffset);
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
				cg.defineStr(vScope, (String)le.getValue());
				return new Operand(OperandType.FLASH_RES, vScope.getResId());
			}
			else {
				return new Operand(OperandType.LITERAL, le.getNumValue());
			}
		}
		else if(expr instanceof MethodCallExpression) {
			MethodCallExpression mce = (MethodCallExpression)expr;
			mce.codeGen(cg);
			return new Operand(OperandType.RETURN, null);
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
	
	@Override
	public List<AstNode> getChildren() {
		return Arrays.asList(parent); //TODO проверить
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + " " + type + " " + parent.toString() + "." + methodName + "(" + StrUtils.toString(argTypes) + ")";
	}
}