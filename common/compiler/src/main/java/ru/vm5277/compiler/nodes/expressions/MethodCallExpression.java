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
import ru.vm5277.common.cg.items.CGIContainer;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.scopes.CGMethodScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.cg.scopes.CGVarScope;
import ru.vm5277.common.compiler.CodegenResult;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.AliasSymbol;
import ru.vm5277.compiler.semantic.AstHolder;
import ru.vm5277.compiler.semantic.ClassScope;
import ru.vm5277.compiler.semantic.InterfaceScope;
import ru.vm5277.compiler.semantic.MethodSymbol;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.semantic.Symbol;

public class MethodCallExpression extends ExpressionNode {
	private			ExpressionNode			classNameExpr;
	private	final	String					methodName;
	private	final	List<ExpressionNode>	args;
	private			VarType					type;
	private			VarType[]				argTypes;
	private			VarType					classType;
	
	public MethodCallExpression(TokenBuffer tb, MessageContainer mc, ExpressionNode className, String methodName, List<ExpressionNode> arguments) {
        super(tb, mc);
        
		this.classNameExpr = className;
		this.methodName = methodName;
        this.args = arguments;
    }

	public String getMethodName() {
		return methodName;
	}
	
	public ExpressionNode getClassName() {
		return classNameExpr;
	}

	public List<ExpressionNode> getArguments() {
		return args;
	}
	
	@Override
	public VarType getType(Scope scope) throws CompileException {
		if(null!=type) return type;
		//поиск метода в текущем классе
		if(null == classNameExpr) {
			ClassScope classScope = scope.getThis();
			if(null != classScope) {
				classType = VarType.fromClassName(classScope.getName());
			}
		}

		// Проверка существования parent (если есть)
		if (null == classType && null != classNameExpr) {
			classType = classNameExpr.getType(scope);
		}
		if (null == classType) throw new CompileException("Cannot resolve:" + classNameExpr.toString());
		
		// Получаем типы аргументов
		argTypes = new VarType[args.size()];
		for (int i=0; i< args.size(); i++) {
			argTypes[i] = args.get(i).getType(scope);
		}

		InterfaceScope iScope = scope.getThis().resolveScope(classType.getName());
		if (null != iScope) {
			// Если parent - класс и это не вызов конструктора, то вызов статического метода
			if (!(this instanceof NewExpression) && classType.isClassType()) {
				if (null == iScope) throw new CompileException("Class '" + classType.getName() + "' not found");

				symbol = iScope.resolveMethod(methodName, argTypes);
				if (symbol != null) return symbol.getType();
			}

			// Если parent - объект (вызов метода экземпляра)
			else {
				// Получаем класс объекта
				if (null == iScope) throw new CompileException("Class '" + classType.getName() + "' not found");

				if(this instanceof NewExpression && iScope instanceof ClassScope) {
					symbol = ((ClassScope)iScope).resolveConstructor(methodName, argTypes);
					if (null == symbol) return null;
					return classType;
				}
				else {
					symbol = iScope.resolveMethod(methodName, argTypes);
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

		if(null != classNameExpr) {
			if(classNameExpr instanceof UnresolvedReferenceExpression) {
				UnresolvedReferenceExpression ure = (UnresolvedReferenceExpression)classNameExpr;
				if("this".equals(ure.getId())) {
					classNameExpr = new ThisExpression(tb, mc);
				}
				else if(null != VarType.fromClassName(ure.getId())) {
					classNameExpr = new TypeReferenceExpression(tb, mc, ure.getId());
				}
				else {
					classNameExpr =  new VarFieldExpression(tb, mc, ure.getId());
				}
			}

			if(!classNameExpr.preAnalyze()) {
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
		boolean result = true;
		
		if(null != classNameExpr) {
			if(!classNameExpr.declare(scope)) {
				return false;
			}
		}
		
		for (int i=0; i<args.size(); i++) {
			ExpressionNode arg = args.get(i);
			result &= arg.declare(scope);
		}

		return result;
	}
	
	//TODO добавить проверку на границы массива(если его размеры и индексы заданы константами). Функционал есть в makeNativeOperand
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		boolean result = true;
		cgScope = cg.enterExpression(toString());
		
		try {
			ExpressionNode optimizedParentScope = classNameExpr.optimizeWithScope(scope, cg);
			if(null != optimizedParentScope) {
				classNameExpr = optimizedParentScope;
			}
		
			try {
				type = getType(scope);
			}
			catch(CompileException e) {
				markError(e);
				result = false;
			} //TODO костыль, нужен для присваивания symbol


			// Проверка parent (если есть)
			if(null != classNameExpr) {
				if (!classNameExpr.postAnalyze(scope, cg)) {
					result = false;
				}
				else {
					try {
						VarType parentType = classNameExpr.getType(scope);
						if(null == parentType) {
							markError("Cannot determine type of parent expression");
							result = false;
						}
					}
					catch (CompileException e) {
						markError("Parent type error: " + e.getMessage());
						result = false;
					}
				}
			}

			// Проверка аргументов
			for(int i=0; i<args.size(); i++) {
				ExpressionNode arg = args.get(i);
				if(!arg.postAnalyze(scope, cg)) {
					result = false;
				}
				else {
					try {
						ExpressionNode optimizedExpr = arg.optimizeWithScope(scope, cg);
						if(null != optimizedExpr) {
							args.set(i, optimizedExpr);
						}
					}
					catch(CompileException e) {
						e.printStackTrace();
					}
				}
			}
			
			// Получаем типы аргументов
			argTypes = new VarType[args.size()];
			for(int i=0; i<args.size(); i++) {
				argTypes[i]=(args.get(i).getType(scope));
			}

			// Поиск метода в ClassScope
			String className = null;
			if(null != this.classNameExpr) {
				if(this.classNameExpr instanceof TypeReferenceExpression) {
					TypeReferenceExpression tre = (TypeReferenceExpression)this.classNameExpr;
					className = tre.getClassName();
				}
				else if(this.classNameExpr instanceof VarFieldExpression) {
					className = ((VarFieldExpression)this.classNameExpr).getType(scope).getClassName();
				}
				else if(this.classNameExpr instanceof ThisExpression) {
					className = scope.getThis().getName();
				}
				else {
					markError(new CompileException("Unsupported parent type: " + this.classNameExpr.toString()));
					result = false;
				}

				InterfaceScope iScope = scope.getThis().resolveScope(className);
				if(null == iScope) {
					markError(new CompileException("Class '" + className + "' not found"));
					result = false;
				}
				
				if(result) {
					if(this instanceof NewExpression) {
						if(iScope instanceof ClassScope) {
							symbol = ((ClassScope)iScope).resolveConstructor(methodName, argTypes);
							if(symbol == null) {
								markError("Constructor '" + methodName + "(" + StrUtils.toString(argTypes) + ")' not found");
								result = false;
							}
						}
						else {
							markError(new CompileException("Class '" + className + "' not found"));
							result = false;
						}
					}
					else {
						symbol = iScope.resolveMethod(methodName, argTypes);
						if(symbol == null) {
							markError("Method '" + methodName + "(" + StrUtils.toString(argTypes) + ")' not found");
							result = false;
						}
					}
				}

				symbol.setCGScope(cgScope);
				
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
            result = false;
        }
		
		cg.leaveExpression();
		return result;
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum) throws Exception {
		CGScope cgs = null == parent ? cgScope : parent;
		
		classNameExpr.codeGen(cg, null, false);
		
		InterfaceScope iScope = (InterfaceScope)((MethodSymbol)symbol).getScope().getParent();
		String classNameStr = iScope.getName();
		
		if(symbol.isNative()) {
			Operand[] operands = null;
			if(!args.isEmpty()) {
				operands = new Operand[args.size()];
				// TODO костыль. Похоже нужно управлять областью видимости универсально, а не точечно
				// Необходимо, чтобы код зависимостей формировался не там, где он находится в древе AST, а там, где он необходим, что оптимально для выделения
				// регистров
//				CGScope oldCGScope = cg.setScope(cgScope);//symbol.getCGScope());
				for(int i=0; i<args.size(); i++) {
					operands[i]= makeNativeOperand(cg, args.get(i));
				}
//				cg.setScope(oldCGScope);
			}

			VarType[] params = null;
			if(!args.isEmpty()) {
				params = new VarType[args.size()];
				for(int i=0; i<args.size(); i++) {
					params[i] = ((MethodSymbol)symbol).getParameters().get(i).getType();
				}
			}

			cg.invokeNative(cgs, classNameStr, symbol.getName(), (null == params ? null : Arrays.toString(params)), type, operands);
		}
		else {
		//	CGLabelScope rpCGScope = null;
			if(0 == argTypes.length && "getClassId".equals(symbol.getName())) {
				if(classNameExpr instanceof VarFieldExpression) {
					CGCellsScope cScope = (CGCellsScope)((VarFieldExpression)classNameExpr).getSymbol().getCGScope();
					// Только для информирования
					cg.invokeClassMethod(cgs, classNameStr, symbol.getName(), type, argTypes, null, false);
					cg.cellsToAcc(cgs, cScope);
				}
			}
			else if(0 == argTypes.length && "getClassTypeId".equals(symbol.getName())) {
				// Только для информирования
				cg.invokeClassMethod(cgs, classNameStr, symbol.getName(), type, argTypes, null, false);
				cg.constToAcc(cgs, cg.getRefSize(), classType.getId(), false);
			}
			else {
				int refTypeSize = 1; // TODO Определить значение на базе количества используемых типов класса
//				cg.pushHeapReg(cgScope);

				if(this instanceof NewExpression) {
					putArgsToStack(cg, cgs, refTypeSize);
				}
				else {
					if(classNameExpr instanceof VarFieldExpression) {
						CGCellsScope cScope = (CGCellsScope)((VarFieldExpression)classNameExpr).getSymbol().getCGScope();
						cg.setHeapReg(cgs, cScope.getCells());
					}
					putArgsToStack(cg, cgs, refTypeSize);
				}
				depCodeGen(cg);

/*				int methodSN = -1;
				if(null != symbol.getCGScope()) {
					methodSN = ((CGClassScope)symbol.getCGScope().getParent()).getMethodSN((CGMethodScope)symbol.getCGScope());
				}
*/				
				//TODO было раньше, явно не корректно, но нужно проверить для всех вариаций
//				CGMethodScope mScope = (CGMethodScope)symbol.getCGScope(CGMethodScope.class);
				CGMethodScope mScope = (CGMethodScope)((AstHolder)symbol).getNode().getCGScope().getScope(CGMethodScope.class);
				if(null != mScope) {
					cg.invokeClassMethod(cgs, classNameStr, symbol.getName(), type, argTypes, mScope.getLabel(), classNameExpr instanceof ThisExpression);
				}
				else {
					List<AstNode> depends = iScope.fillDepends(((MethodSymbol)symbol).getSignature());
					if(null != depends) {
						for(AstNode node : depends) {
							node.codeGen(cg, null, false);
						}
					}
					int methodSN = iScope.getMethodSN(symbol.getName(), ((MethodSymbol)symbol).getSignature());
					cg.invokeInterfaceMethod(cgs, classNameStr, symbol.getName(), type, argTypes, VarType.fromClassName(iScope.getName()), methodSN);
				}
//				cg.popHeapReg(cgScope);
			}
			
			if(!type.isVoid()) {
				cg.accCast(null, type);
				return CodegenResult.RESULT_IN_ACCUM;
			}
		}

		return null;
	}
	
	private Operand makeNativeOperand(CodeGenerator cg, ExpressionNode expr) throws Exception {
		if(expr instanceof VarFieldExpression) {
			VarFieldExpression ve = (VarFieldExpression)expr;
			// Кодогенерация VarFieldExpression выполняет запись значения в аккумулятор, но нам этого не нужно, выполняем только зависимость
			ve.depCodeGen(cg);
			return new Operand(VarType.CSTR == ve.getType(null) ? OperandType.FLASH_RES : OperandType.LOCAL_RES, ve.getSymbol().getCGScope().getResId());
		}
		else if(expr instanceof FieldAccessExpression) {
			FieldAccessExpression fae = (FieldAccessExpression)expr;
			fae.codeGen(cg, null, false);
			return new Operand(OperandType.LOCAL_RES, fae.getSymbol().getCGScope().getResId());
		}
		else if(expr instanceof LiteralExpression) {
			LiteralExpression le = (LiteralExpression)expr;
			if(VarType.CSTR == le.getType(null)) {
				CGVarScope vScope = cg.enterLocal(VarType.CSTR, -1, true, null);
				cg.leaveLocal();
				vScope.setDataSymbol(cg.defineData(vScope.getResId(), -1, (String)le.getValue()));
				return new Operand(OperandType.FLASH_RES, vScope.getResId());
			}
			else if(le.isFixed()) {
				return new Operand(OperandType.LITERAL_FIXED, le.getFixedValue());
			}
			else {
				return new Operand(OperandType.LITERAL, le.getNumValue());
			}
		}
		else if(expr instanceof MethodCallExpression) {
			MethodCallExpression mce = (MethodCallExpression)expr;
			mce.codeGen(cg, null, false);
			return new Operand(OperandType.ACCUM, null);
		}
		else if(expr instanceof CastExpression) {
			CastExpression ce = (CastExpression)expr;
			ce.codeGen(cg, null, false);
			return makeNativeOperand(cg, ce.getOperand());
		}
		else if(expr instanceof BinaryExpression) {
			BinaryExpression be = (BinaryExpression)expr;
			be.codeGen(cg, null, false);
			return new Operand(OperandType.ACCUM, null);
		}
		else if(expr instanceof UnaryExpression) {
			UnaryExpression ue = (UnaryExpression)expr;
			ue.codeGen(cg, null, true);
			return new Operand(OperandType.ACCUM, null);
		}
		else if(expr instanceof ArrayExpression) {
			ArrayExpression ae = (ArrayExpression)expr;
			ae.codeGen(cg, null, false);
			
			return new Operand(OperandType.ARRAY, null);
		}
		else if(expr instanceof ArrayPropertyExpression) {
			ArrayPropertyExpression ape = (ArrayPropertyExpression)expr;
			ape.codeGen(cg, null, true);
			return new Operand(OperandType.ACCUM, null);
		}

		else throw new Exception("Unexpected expression:" + expr);
	}
	
	private void putArgsToStack(CodeGenerator cg, CGScope cgs, int refTypeSize) throws Exception {
		if(!args.isEmpty()) {
//			CGScope oldCGScope = cg.setScope(symbol.getCGScope());
			
			CGMethodScope mScope = (CGMethodScope)symbol.getCGScope(CGMethodScope.class);
			mScope.clearArgs(); //скорее всего не зайдет, нам нужно удалять только те, которые мы даобавили
			
			CGIContainer cont = new CGIContainer();
			for(int i=0; i<args.size(); i++) {
			//for(int i=args.size()-1; i>=0; i--) {
				// Получаем параметр вызываемого метода
				Symbol paramSymbol = ((MethodSymbol)symbol).getParameters().get(i);
				VarType paramVarType = paramSymbol.getType();

				// Получаем выражение передаваемое этому параметру
				ExpressionNode argExpr = args.get(i);
				// Получаю из scope вызываемого метода переменую с именем параметра(пустая переменная уже создана, или создан Alias)
				Symbol vSymbol = ((MethodSymbol)symbol).getScope().resolveSymbol(paramSymbol.getName());
				if(vSymbol instanceof AliasSymbol) {
					// Это алиас, который нужно что???
					throw new CompileException("Unsupported alias:" + vSymbol.toString());
				}
				else {
					int exprTypeSize = argTypes[i].getSize();
					if(-1 == exprTypeSize) exprTypeSize = cg.getRefSize();
					// Создаем новую область видимости переменной в вызываемом методе
					int varSize = paramVarType.isObject() ? refTypeSize + cg.getRefSize() : exprTypeSize;
					
					CGScope oldCGScope = cg.setScope(symbol.getCGScope(CGMethodScope.class));
					CGVarScope dstVScope = cg.enterLocal(vSymbol.getType(), varSize, false, vSymbol.getName());
					dstVScope.build(); // Выделяем память в стеке
					cg.leaveLocal();
					cg.setScope(oldCGScope);
					
					vSymbol.setCGScope(dstVScope);

					if(argExpr instanceof LiteralExpression) {
						// Выполняем зависимость
						argExpr.depCodeGen(cg);
						LiteralExpression le = (LiteralExpression)argExpr;
						cont.append(cg.pushConst(null, exprTypeSize, le.isFixed() ? le.getFixedValue() : le.getNumValue(), le.isFixed()));
					}
					else if(argExpr instanceof VarFieldExpression) {
						// Выполняем зависимость
						argExpr.depCodeGen(cg);
						cont.append(cg.pushCells(null, exprTypeSize, ((CGCellsScope)argExpr.getSymbol().getCGScope()).getCells()));
					}
					else if(argExpr instanceof MethodCallExpression) {
						// Выполняем зависимость
						argExpr.codeGen(cg, null, true);
						cont.append(cg.pushAccBE(null, paramVarType.getSize()));
					}
					else if (argExpr instanceof BinaryExpression) {
						argExpr.codeGen(cg, null, true);
						cont.append(cg.pushAccBE(null, paramVarType.getSize()));
					}
					else {
						throw new CompileException("Unsupported expression:" + argExpr);
					}
				}
			}
			cgs.append(cont);
		}
	}
	
	@Override
	public List<AstNode> getChildren() {
		return Arrays.asList(classNameExpr); //TODO проверить
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + " " + type + " " + classNameExpr.toString() + "." + methodName + "(" + StrUtils.toString(argTypes) + ")";
	}
}