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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ru.vm5277.common.ExcsThrowPoint;
import ru.vm5277.common.LabelNames;
import ru.vm5277.common.NativeBinding;
import ru.vm5277.common.Pair;
import ru.vm5277.common.enums.RTOSFeature;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.Operand;
import ru.vm5277.common.cg.OperandType;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.scopes.CGMethodScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.cg.scopes.CGVarScope;
import ru.vm5277.common.enums.CodegenResult;
import ru.vm5277.common.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.AliasSymbol;
import ru.vm5277.compiler.semantic.AstHolder;
import ru.vm5277.compiler.semantic.MethodSymbol;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.semantic.Symbol;
import static ru.vm5277.compiler.Main.debugAST;
import static ru.vm5277.common.enums.SemanticAnalyzePhase.DECLARE;
import static ru.vm5277.common.enums.SemanticAnalyzePhase.PRE;
import static ru.vm5277.common.enums.SemanticAnalyzePhase.POST;
import ru.vm5277.common.lexer.SourcePosition;
import ru.vm5277.common.StrUtils;
import ru.vm5277.common.enums.RTOSParam;
import ru.vm5277.common.cg.CGBranch;
import ru.vm5277.common.cg.CGCells;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.scopes.CGBlockScope;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.cg.scopes.CGTryBlockScope;
import ru.vm5277.common.enums.J8BException;
import ru.vm5277.common.lexer.Operator;
import ru.vm5277.compiler.Instance;
import ru.vm5277.compiler.nodes.expressions.bin.BinaryExpression;
import ru.vm5277.compiler.semantic.BlockScope;
import ru.vm5277.compiler.semantic.CIScope;
import ru.vm5277.compiler.semantic.ClassScope;
import ru.vm5277.compiler.semantic.ExceptionScope;
import ru.vm5277.compiler.semantic.InitNodeHolder;
import ru.vm5277.compiler.semantic.InterfaceScope;
import ru.vm5277.compiler.semantic.MethodScope;

public class MethodCallExpression extends ExpressionNode {
	private			ExpressionNode			targetExpr;
	private			String					methodName;
	private	final	List<ExpressionNode>	args;
	private			VarType[]				argTypes;
	private			CIScope					cis;
	private			boolean					isThisMethodCall;
	private			CGScope					headerCGScope;
	
	public MethodCallExpression(Instance inst, TokenBuffer tb, SourcePosition sp, ExpressionNode parentExpr, String methodName, List<ExpressionNode> args) {
		super(inst, tb, sp);

		this.targetExpr = parentExpr;
		this.methodName = methodName;

		this.args = args;
	}

	public String getMethodName() {
		return methodName;
	}

	public ExpressionNode getPathExpr() {
		return targetExpr;
	}

	public List<ExpressionNode> getArguments() {
		return args;
	}

	@Override
	public boolean preAnalyze() {
		boolean result = true;
		debugAST(this, PRE, true, getFullInfo());

		if(null==args) {
			markError("Arguments list cannot be null");
			result = false;
		}
		else {
//			if(null!=pathExpr) {
//				result&=pathExpr.preAnalyze();
//			}

			for(int i=0; i<args.size(); i++) {
				ExpressionNode arg = args.get(i);
				if(null==arg) {
					markError("Argument cannot be null");
					result=false;
				}
				result&=arg.preAnalyze();
			}
		}

		debugAST(this, PRE, false, result, getFullInfo());
		return result;
	}

	@Override
	public boolean declare(Scope scope) {
		boolean result = true;
		debugAST(this, DECLARE, true, getFullInfo());

//		if(null!=methodScope) {
//			if(!methodScope.declare(scope)) {
//				return false;
//			}
//		}

		for(int i=0; i<args.size(); i++) {
			ExpressionNode arg = args.get(i);
			result&=arg.declare(scope);
		}

		debugAST(this, DECLARE, false, result, getFullInfo());
		return result;
	}

	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg, CGScope parent) {
		boolean result = true;
		debugAST(this, POST, true, getFullInfo());
		cgScope = cg.enterExpression(parent, cgScope, toString());

		headerCGScope = new CGScope(cgScope, CGScope.genId(), "methodCall header");
		
		try {
			//TODO блок был закоменчен
			if(null!=targetExpr) {
				result&=targetExpr.postAnalyze(scope, cg, cgScope);
				if(result) {
					// Резолвинг QualifiedPathExpression
					ExpressionNode resolved = resolveQualifiedPathExpr(targetExpr);
					if(null!=resolved) {
						targetExpr = resolved;
					}
				}
			}

			argTypes = new VarType[args.size()];
			for(int i=0; i<args.size(); i++) {
				ExpressionNode arg = args.get(i);
				result&=arg.postAnalyze(scope, cg, cgScope);
				if(result) {
					// Резолвинг QualifiedPathExpression
					ExpressionNode resolved = resolveQualifiedPathExpr(arg);
					if(null!=resolved) {
						arg = resolved;
						args.set(i, resolved);
					}
					argTypes[i] = arg.getType();
				}
			}
			
			if(result) {
				if(null==targetExpr) {
					cis = scope.getThis();
					symbol = cis.resolveMethod(methodName, argTypes, false);
				}
				else if(targetExpr instanceof TypeReferenceExpression) {
					cis = ((TypeReferenceExpression)targetExpr).getScope();
					symbol = cis.resolveMethod(methodName, argTypes, true);
					
					// Проверяем наличие throws в методе или блоков try-catch для ExceptionScope throw (кроме unchecked exceptions)
					if(cis instanceof ExceptionScope) {
						ExceptionScope eScope = (ExceptionScope)cis;
						VarType.setUsedException(eScope.getId());
						
						if(!eScope.isUnchecked()) {
							Scope scope_ = scope;
							while(null!=scope_) {
								if(scope_ instanceof BlockScope) {
									BlockScope tryBlockScope = (BlockScope)scope_;
									if(eScope.isCompatible(tryBlockScope.getHandlingExcsScopes())) {
										break;
									}
								}
								else if(scope_ instanceof MethodScope) {
									if(eScope.isCompatible(((MethodScope)scope_).getExceptionScopes())) {
										break;
									}
								}
								scope_ = scope_.getParent();
							}

							if(null==scope_) {
								markError("Unhandled exception '" + eScope.getName() + "'");
								result = false;
							}
						}
					}
				}
				else if(targetExpr instanceof VarFieldExpression) {
					VarType varType = ((VarFieldExpression)targetExpr).getSymbol().getType();
					if(null!=varType) {
						cis = scope.getThis().resolveCI(null, varType.getClassName(), false);
						if(null!=cis) {
							symbol = cis.resolveMethod(methodName, argTypes, true);
						}
					}
				}
				if(null==symbol) {
					markError(	"Method " + (null==type ? VarType.VOID : type) + " " + getQualifiedPath() +
								"(" + StrUtils.toString(argTypes) + ") not found");
					result = false;
				}
			}
			
			if(result) {
				isThisMethodCall = (scope.getThis()==cis);
				type = symbol.getType();
				symbol.setCGScope(cgScope);

				((MethodSymbol)symbol).markUsed();
				
/*
				Set<ExceptionScope> eScopes = ((MethodScope)((MethodSymbol)symbol).getScope()).getExceptionScopes();
				if(!eScopes.isEmpty()) {
					for(ExceptionScope eScope : eScopes) {
						Scope _scope = scope;
						while(null!=_scope) {
							if(_scope instanceof BlockScope) {
								if(((BlockScope)_scope).containsException(eScope)) {
									break;
								}
							}
							else if(_scope instanceof MethodScope) {
								if(((MethodScope)_scope).containsException(eScope)) {
									break;
								}
							}
							_scope = _scope.getParent();
						}

						if(null==_scope) {
						}
					}
				}*/
			}
			
			if(result) {
				if(isThisMethodCall) {
					MethodScope mScope = scope.getMethod();
					if(!symbol.isStatic() && null!=mScope && mScope.getSymbol().isStatic()) {
						//TODO не корректный номер строки в сообщении
						markError("Non-static method '" + symbol.getName() + "' cannot be referenced from a static context");
						result = false;
					}
				}
			}
			
			if(result && ((MethodSymbol)symbol).canThrow()) {
				int runtimeExceptionId = VarType.getExceptionId(J8BException.RuntimeException.name());
				l1:
				// Перебираем все исключения перечисленные в throws вызываемого метода
				for(ExceptionScope eScope : ((MethodSymbol)symbol).getScope().getExceptionScopes()) {
					// Проверяем unchecked исключения (для них не обязательны обработчики)
					Integer esi = eScope.getId();
					while(null!=esi) {
						if(runtimeExceptionId==esi) continue l1;
						esi = VarType.getExceptionParent(esi);
					}
					
					Scope scope_ = scope;
					while(null!=scope_) {
						if(scope_ instanceof BlockScope) {
							BlockScope tryBlockScope = (BlockScope)scope_;
							// Нашли блок кода, исключения заполнены только для catch блока, проверяем на обработку
							for(ExceptionScope catchExScope : tryBlockScope.getHandlingExcsScopes()) {
								// Точное совпадение
								if(catchExScope.getId()==eScope.getId()) {
									continue l1;
								}
								// Проверяем потомков исключения в catch блоке на совпадение
								for(int exId : VarType.getExceptionDescendant(catchExScope.getId())) {
									if(exId==eScope.getId()) {
										continue l1;
									}
								}
							}
						}
						scope_ = scope_.getParent();
					}
					markError("Unhandled exception type: " + eScope.getName());
				}
			}
			
			// Поддерживаем только byte[] массив для нативных методов
			// Проверяем - нативный метод требует для безопасности, чтобы после аргумента типа array следующим параметром шла его длина
			if(result && symbol.isNative()) {
				for(int i=0; i<argTypes.length; i++) {
					if(argTypes[i].isArray()) {
						if(VarType.BYTE!=args.get(i).getType().getElementType() || 0x01!=args.get(i).getType().getArrayDepth()) {
							markError("Native method: only byte[] arrays are supported");
							result = false;
						}
						if(i>=(argTypes.length-1) || (VarType.SHORT!=argTypes[i+1] && VarType.BYTE!=argTypes[i+1])) {
							markError("Native method: array must be followed by length");
							result = false;
						}
						
						// Пытаемся определить размер массива и переданный размер данных в аргументе за массивом.
						// Это возможно если размер данных - константа, а массив объявлен как final и сразу проинициализирован в объявлении переменной/поля.
						//TODO После можно попробовать доработать - получать ноду инициализующую массив также из конструкторов
						//NOTE codeOptimization в случае оптимизации вызывает postAnalyze повторно (через rePostAnalyze)
						if(args.get(i+1) instanceof LiteralExpression) {
							ExpressionNode expr = args.get(i);
							if(	null!=expr.getSymbol() && expr.getSymbol() instanceof AstHolder && expr.getSymbol().isFinal() && 
								((AstHolder)expr.getSymbol()).getNode() instanceof InitNodeHolder) {
								
								InitNodeHolder inh = (InitNodeHolder)((AstHolder)expr.getSymbol()).getNode();
								if(null!=inh && null!=inh.getInitNode() && inh.getInitNode() instanceof NewArrayExpression) {
									NewArrayExpression nae = (NewArrayExpression)inh.getInitNode();
									int realArraySize = nae.getConstDimensions()[0];
									LiteralExpression le = (LiteralExpression)args.get(i+1);
									if(le.getNumValue()>realArraySize) {
										markError("Native method: length " + le + " exceeds byte[" + realArraySize + "] bounds");
									}
								}
							}
						}
					}
				}
			}
		}
		catch (CompileException e) {
			markError(e.getMessage());
			result = false;
		}

		debugAST(this, POST, false, result, getFullInfo());
		return result;
	}
	
	@Override
	public void codeOptimization(Scope scope, CodeGenerator cg) {
		boolean optimized = false;
		
		if(null!=targetExpr) {
			targetExpr.codeOptimization(scope, cg);
			try {
				ExpressionNode optimizedExpr = targetExpr.optimizeWithScope(scope, cg);
				if(null != optimizedExpr) {
					targetExpr = optimizedExpr;
					optimized = true;
				}
			}
			catch(CompileException ex) {
				markError(ex);
			}
		}
		
		for(int i=0; i<args.size(); i++) {
			ExpressionNode arg = args.get(i);
			arg.codeOptimization(scope, cg);
			try {
				ExpressionNode optimizedExpr = arg.optimizeWithScope(scope, cg);
				if(null != optimizedExpr) {
					arg = optimizedExpr;
					args.set(i, arg);
					argTypes[i] = arg.getType();
					optimized = true;
				}
			}
			catch(CompileException ex) {
				markError(ex);
			}
		}
		
		if(optimized) {
			rePostAnalyze(scope, cg, cgScope);
		}
	}

	@Override
	public Object codeGen(CodeGenerator cg, boolean toAccum, CGExcs excs) throws CompileException {

		if(null!=targetExpr) {
			targetExpr.codeGen(cg, false, excs);
		}

		String classNameStr = cis.getName();

		// Тип аргумента в вызове и в методе могут отличаться например methos(short s) также подходит для аргуметна типа byte
		// Модифицирую типы аргументов
		List<VarType> methodArgTypes = ((MethodSymbol)symbol).getParameterTypes();
		for(int i=0; i<argTypes.length; i++) {
			//TODO костыль, Проверяем на CSTR: в codeOptimization аргумент может быть оптимизирован вполть до смены типа.
			//А значит найденный метод может быть не верным и в условии ниже может пройти откат типа на старый.
			//Сейчас смена типа возможна в выражениях System.out(i + ' '); где i - является константой.
			//Похоже это единственный случай, поэтому временно вводим проверку на CSTR тип.
			if(VarType.CSTR!=argTypes[i] && argTypes[i].getSize()<methodArgTypes.get(i).getSize()) {
				argTypes[i] = methodArgTypes.get(i);
			}
		}
		
		String signature = cis.getName() + "." + methodName + "(";
		for(int i=0; i<argTypes.length; i++) {
			VarType argType = argTypes[i];
			//кодогенератор должен пропустить заголовок массива и проверить, что следующим аргументом идет длина массива типа short,
			// которую он также должен проверить на границу
			if(symbol.isNative() && argType.isArray()) {
			 // Для RTOS любой массив - ссылка 
				signature += VarType.CLASS.getName();
			}
			else {
				signature += argType.getName();
			}
			if(i!=(argTypes.length-1)) {
				signature+=",";
			}
		}
		signature+=")";
		
		if(signature.equals("System.setParam(byte,byte)")) {
			if(	0x02!=args.size() ||
				!(args.get(0) instanceof LiteralExpression) || !(args.get(1) instanceof LiteralExpression) ||
				VarType.BYTE!=argTypes[0] || VarType.BYTE!=argTypes[1]) {
			
				throw new CompileException("System.setParam arguments must be constant byte expressions for RTOS configuration");
			}
			
			int paramId = (int)((LiteralExpression)args.get(0)).getNumValue();
			int valueId = (int)((LiteralExpression)args.get(1)).getNumValue();
			RTOSParam rtosParam = RTOSParam.values()[paramId];
			switch(rtosParam) {
				case ACTLED_PORT:
					cg.setFeature(RTOSFeature.OS_FT_HEARTBEAT);
				case CORE_FREQ:
				case STDIO_PORT:
				case HALT_OK_MODE:
				case HALT_ERR_MODE:
					cg.getDevice().setParam(rtosParam, valueId);
					break;
				case MULTITHREADING:
					if(0x00!=valueId) {
						cg.setFeature(RTOSFeature.OS_FT_MULTITHREADING);
					}
					break;
				case SHOW_WELCOME:
					if(0x00!=valueId) {
						cg.setFeature(RTOSFeature.OS_FT_WELCOME);
					}
					break;
				case DIAG_MODE:
					if(0x00!=valueId) {
						cg.setFeature(RTOSFeature.OS_FT_DIAG);
					}
					break;
			}
		}
		else if(cis instanceof ExceptionScope && (signature.endsWith(".throw()") || signature.endsWith(".throw(byte)"))) {
			ExceptionScope eScope = (ExceptionScope)cis;
			excs.getProduced().add(eScope.getId());
			if(0x01==args.size()) {
				cg.exCodeToAccH(cgScope, ((LiteralExpression)args.get(0)).getNumValue());
			}

			CGLabelScope lbScope = null;
			boolean	isMethodLeave = false;
			CGScope cgScope_ = cgScope;
			l1:			
			// Ищем подходящий обработчик исключения: блок catch или выход из метода
			while(null!=cgScope_) {
				if(cgScope_ instanceof CGTryBlockScope) {
					// Нашли блок try-catch
					for(Integer catchExceptionId : ((CGTryBlockScope)cgScope_).getExceptionIds()) {
						if(eScope.isCompatible(catchExceptionId)) {
							lbScope = ((CGTryBlockScope)cgScope_).getCatchLabel(catchExceptionId);
							break l1;
						}
					}
				}
				if(null!=cgScope_.getParent() && cgScope_.getParent() instanceof CGMethodScope) {
					// Нашли превый блок в методе
					if(eScope.isUnchecked()) {
						isMethodLeave = true;
						lbScope = ((CGBlockScope)cgScope_).getELabel();
						break;
					}
					else {
						for(Integer catchExceptionId : ((CGMethodScope)cgScope_.getParent()).getExceptionIds()) {
							if(eScope.isCompatible(catchExceptionId)) {
								isMethodLeave = true;
								lbScope = ((CGBlockScope)cgScope_).getELabel();
								break l1;
							}
						}
					}
				}
				cgScope_ = cgScope_.getParent();
			}
			
			if(null==lbScope) {
				throw new CompileException("COMPILER ERROR: Unable to locate exception handling label for " + signature);
			}

			ExcsThrowPoint excsThrowPoint = cg.getTargetInfoBuilder().addExcsThrowPoint(cg, sp, signature);
			cg.eThrow(cgScope, eScope.getId(), isMethodLeave, lbScope, 0x01==args.size(), excsThrowPoint);
		}
		else if(symbol.isNative()) {
			// Фичу мультипоточности включаем если обнаружен вызов Thread.start()
			if(signature.equals("Thread.start()")) {
				cg.setFeature(RTOSFeature.OS_FT_MULTITHREADING);
			}
			if(signature.equals("Timer.start(short)")) {
				cg.setFeature(RTOSFeature.OS_FT_MULTITHREADING);
			}
			
			if(	signature.equals("System.out(cstr)") && args.get(0) instanceof LiteralExpression &&
				VarType.CSTR==args.get(0).getType() && ((LiteralExpression)args.get(0)).getStringValue().isEmpty()) {
				// Ничего не делаем
				// TODO вынести в AST оптимизацию
			}
			//TODO для универсальности - в методе может быть несколько аргументов
			else if(0x01==args.size() && args.get(0x00) instanceof CstrConcatExpression) {
				CstrConcatExpression ce = (CstrConcatExpression)args.get(0x00);
				for(int exprIndex=0; exprIndex<ce.getExprs().size(); exprIndex++) {
					ExpressionNode exprNode = ce.getExprs().get(exprIndex);

					signature = cis.getName() + "." + methodName + "(";
					for(int i=0; i<argTypes.length; i++) {
						VarType argType = exprNode.getType();
						if(argType.isEnum()) {
							argType = VarType.BYTE;
						}
						signature += argType.getName();
						if(i!=(argTypes.length-1)) {
							signature+=",";
						}
					}
					signature+=")";

					if(	signature.equals("System.out(cstr)") && exprNode instanceof LiteralExpression &&
						VarType.CSTR==exprNode.getType() && ((LiteralExpression)exprNode).getStringValue().isEmpty()) {
						
						continue;
					}

					
					if(null==cg.nativeMethodInit(exprNode.getCGScope(), signature, false)) {
						throw new CompileException("Not found native method binding for method: " + signature);
					}
					List<Byte> storedRegs = new ArrayList<>();
					cg.accumLock(exprNode.getType());
					if(!signature.equals("System.out(exception)")) {
						if(exprNode instanceof BinaryExpression) {
							CGBranch branch = new CGBranch();
							exprNode.getCGScope().setBranch(branch);

							exprNode.codeGen(cg, true, excs);

							if(branch.isUsed()) {
								cg.constToAcc(exprNode.getCGScope(), 1, 1, false);
								CGLabelScope lbScope = new CGLabelScope(null, null, LabelNames.LOCGIC_END, true);
								cg.jump(exprNode.getCGScope(), lbScope);
								exprNode.getCGScope().append(((CGBranch)branch).getEnd());
								cg.constToAcc(exprNode.getCGScope(), 1, 0, false);
								exprNode.getCGScope().append(lbScope);
							}
						}
						else if(exprNode instanceof UnaryExpression) {
							CGBranch branch = new CGBranch();
							exprNode.getCGScope().setBranch(branch);

							exprNode.codeGen(cg, false, excs);

							if(branch.isUsed()) {
								cg.constToAcc(exprNode.getCGScope(), 1, 1, false);
								CGLabelScope lbScope = new CGLabelScope(null, null, LabelNames.LOCGIC_END, true);
								cg.jump(exprNode.getCGScope(), lbScope);
								exprNode.getCGScope().append(((CGBranch)branch).getEnd());
								cg.constToAcc(exprNode.getCGScope(), 1, 0, false);
								exprNode.getCGScope().append(lbScope);
							}
						}
						else {
							if(CodegenResult.RESULT_IN_ACCUM!=exprNode.codeGen(cg, true, excs)) {
								throw new CompileException("Accum not used for argument:" + exprNode);
							}
						}
						cg.accumUnlock();

						cg.nativeMethodSetArg(exprNode.getCGScope(), signature, storedRegs, 0, exprNode.getType().isFixedPoint());
					}
					cg.nativeMethodInvoke(exprNode.getCGScope(), signature, storedRegs, type);
				}
			}
			else {
				long[] constants = new long[args.size()];
				for(int i=0; i<args.size(); i++) {
					ExpressionNode argExpr = args.get(i);
					if(argExpr instanceof LiteralExpression) {
						LiteralExpression le = (LiteralExpression) argExpr;
						if(le.getType().isNumeric()) {
							constants[i] = le.getNumValue();
						}
						else {
							constants = null;
							break;
						}
					}
					else {
						constants = null;
						break;
					}
				}
				
				CGLabelScope endLbScope = new CGLabelScope(null, CGScope.genId(), LabelNames.INVOKE_END, true);
				
				NativeBinding nb = cg.nativeMethodInit((args.isEmpty() ? cgScope : args.get(0).getCGScope()), signature, null!=constants);
				List<Byte> storedRegs = new ArrayList<>();
				if(null==nb) {
					// Метод не найден в текущей реализации, может быть реализация в интерфейсе?
					Scope parent = ((MethodSymbol)symbol).getScope().getParent();
					if(parent instanceof InterfaceScope) {
						MethodSymbol methodSymbol = ((InterfaceScope)parent).resolveMethod(methodName, argTypes, false);
						if(null!=methodSymbol) {
							signature = ((InterfaceScope)parent).getName() + "." + methodName + "(";
							for(int i=0; i<argTypes.length; i++) {
								VarType argType = argTypes[i];
								signature += argType.getName();
								if(i!=(argTypes.length-1)) {
									signature+=",";
								}
							}
						}
						signature+=")";
						nb = cg.nativeMethodInit((args.isEmpty() ? cgScope : args.get(0).getCGScope()), signature, null!=constants);
					}
					if(null==nb) {
						throw new CompileException("Not found native method binding for method: " + signature);
					}
				}

				boolean checkedByCompiler = false; // Пока просто флаг конкретно для ArrayBoundsException
				boolean supportedFunct = null!=constants && null!=nb.getCgFunctName() && !nb.getCgFunctName().isEmpty() &&
										cg.isSupportedFunct(nb.getCgFunctName());
				if(!supportedFunct && !signature.equals("System.out(exception)")) {
					int index=0x00;
					if(null!=nb.getMethodParams() && VarType.VOID==nb.getMethodParams()[0x00]) { // this в первом аргументе
						index=0x01;
					}

					ExpressionNode argExpr = null;
					for(int i=0; i<args.size(); i++) {
						
						if(0!=i) {
							// Необходимо проверять в какие регистры сохранился последний аргумент, если это регистры аккумулятора, то обработка следующего
							// аргумента может повредить аккумулятор - нужно сохранить в стек
							for(byte reg : nb.getRegs()[i-1]) {
								if(cg.isAccumReg(reg)) {
									args.get(i-1).getCGScope().append(cg.pushReg(reg));
								}
							}
						}
						
						boolean optimized = false;
						
						argExpr = args.get(i);
						//cg.accResize(argTypes[i]);

						cg.accumLock(argTypes[i].isArray() ? VarType.SHORT : argTypes[i]);
						if(argExpr instanceof BinaryExpression) {
							CGBranch branch = new CGBranch();
							argExpr.getCGScope().setBranch(branch);

							argExpr.codeGen(cg, true, excs);
							
							if(branch.isUsed()) {
								cg.constToAcc(argExpr.getCGScope(), 1, 1, false);
								CGLabelScope lbScope = new CGLabelScope(null, null, LabelNames.LOCGIC_END, true);
								cg.jump(argExpr.getCGScope(), lbScope);
								argExpr.getCGScope().append(((CGBranch)branch).getEnd());
								cg.constToAcc(argExpr.getCGScope(), 1, 0, false);
								argExpr.getCGScope().append(lbScope);
							}
						}
						else if(argExpr instanceof UnaryExpression) {
							CGBranch branch = new CGBranch();
							argExpr.getCGScope().setBranch(branch);

							argExpr.codeGen(cg, false, excs);

							if(branch.isUsed()) {
								cg.constToAcc(argExpr.getCGScope(), 1, 1, false);
								CGLabelScope lbScope = new CGLabelScope(null, null, LabelNames.LOCGIC_END, true);
								cg.jump(argExpr.getCGScope(), lbScope);
								argExpr.getCGScope().append(((CGBranch)branch).getEnd());
								cg.constToAcc(argExpr.getCGScope(), 1, 0, false);
								argExpr.getCGScope().append(lbScope);
							}
						}
						else if(argExpr instanceof LiteralExpression && ((LiteralExpression)argExpr).getType().isNumeric()) {
							LiteralExpression le = (LiteralExpression)argExpr;
							targetExpr.codeGen(cg, false, excs);
							argExpr.codeGen(cg, false, excs);
							CGCells leftCells = new CGCells(CGCells.Type.REG, nb.getRegs()[index+i]);
							// Сохраняем регистры если они не принадлежат аккумулятору
							for(int reg : leftCells.getIds()) {
								if(!cg.isAccumReg((byte)reg)) {
									cg.pushReg((byte)reg);
									storedRegs.add((byte)reg);
								}
							}
							CGCells rightCells = new CGCells(CGCells.Type.CONST);
							rightCells.setConst(le.getType().isFixedPoint() ? le.getFixedValue() : le.getNumValue());

							cg.cellsToCells(argExpr.getCGScope(), leftCells, argTypes[i], rightCells, argExpr.getType());
							optimized = true;
						}
						else {
							// На этом этапе выражения типа
							// lds r16,_os_stat_pool+0
							// lds r17,_os_stat_pool+1
							// movw r26,r16
							// оптимизировать не получится, результат нужен в аккумуляторе так как front не знает в какие регистры нужно писать и
							// нужно ли сохранять эти регистры.
							// Зато такую оптимизацию легко сделать после сборки
							// update: знает регистры, они лежат в nb:
							if(!(argExpr instanceof PropertyExpression)) { //Пока не умеем загружать значения Property прямо в регистры не используя аккумулятор
								try {
									ExpressionNode rightExpr = targetExpr;
									targetExpr.codeGen(cg, false, excs);
									if(!(argExpr instanceof MethodCallExpression)) { //TODO почему для MethodCallExpression получаем дубль? Где-то ранее подготавливаем?
										argExpr.codeGen(cg, false, excs);
									}
									CGCells leftCells = new CGCells(CGCells.Type.REG, nb.getRegs()[index+i]);
									// Сохраняем регистры если они не принадлежат аккумулятору
									for(int reg : leftCells.getIds()) {
										if(!cg.isAccumReg((byte)reg) && !cg.isArrReg((byte)reg)) {
											argExpr.getCGScope().append(cg.pushReg((byte)reg));
											storedRegs.add((byte)reg);
										}
									}
									if(null!=argExpr.getSymbol() && argExpr.getSymbol().getCGScope() instanceof CGCellsScope) {
										CGCells rightCells = ((CGCellsScope)argExpr.getSymbol().getCGScope()).getCells();
										if(null!=rightCells) {
											cg.cellsToCells(argExpr.getCGScope(), leftCells, argExpr.getType(), rightCells, argExpr.getType());
											optimized = true;
										}
									}
								}
								catch(Exception ex) {
									System.out.println("TODO:" + ex.getMessage());
								}
							}

							if(!optimized) {
								if(CodegenResult.RESULT_IN_ACCUM!=argExpr.codeGen(cg, true, excs)) {
									throw new CompileException("Accum not used for argument:" + argExpr);
								}
							}
						}
						cg.accumUnlock();
						
						if(!optimized) {
							//Использование метода cellsToCells упрощает задачу - он обеспечивает данные сразу в нужных регистрах.
							//TODO хорошо бы заменить всю генерацию кода по записи в регистры на базе cellsToCells
							cg.nativeMethodSetArg(argExpr.getCGScope(), signature, storedRegs, index+i, argExpr.getType().isFixedPoint());
						}
						if(0!=i && args.get(i-1).getType().isArray()) {
							if(args.get(i) instanceof LiteralExpression) {
								ExpressionNode expr = args.get(i-1);
								if(	null!=expr.getSymbol() && expr.getSymbol() instanceof AstHolder && expr.getSymbol().isFinal() && 
									((AstHolder)expr.getSymbol()).getNode() instanceof InitNodeHolder) {

									InitNodeHolder inh = (InitNodeHolder)((AstHolder)expr.getSymbol()).getNode();
									if(null!=inh && null!=inh.getInitNode() && inh.getInitNode() instanceof NewArrayExpression) {
										NewArrayExpression nae = (NewArrayExpression)inh.getInitNode();
										int realArraySize = nae.getConstDimensions()[0];
										LiteralExpression le = (LiteralExpression)args.get(i);
										if(le.getNumValue()>realArraySize) {
											throw new CompileException("COMPILER BUG: array index out of array bounds - must be checked in semattic");
										}
										checkedByCompiler = true;
									}
								}
							}
							if(!checkedByCompiler) {
								cg.setFeature(RTOSFeature.OS_FT_ETRACE);
								excs.getProduced().add(VarType.getExceptionId(J8BException.ArrayBoundsException.name()));
							}
							cg.nativeMethodArrArgPrepare(argExpr.getCGScope(), signature, storedRegs, index+i, checkedByCompiler, endLbScope);
						}
					}
					if(null!=nb.getMethodParams() && VarType.VOID==nb.getMethodParams()[0x00]) { // this в первом аргументе
						boolean optimized = false;
						if(null!=targetExpr) {
							try {
								ExpressionNode rightExpr = targetExpr;
								CGCells leftCells = new CGCells(CGCells.Type.REG, nb.getRegs()[0x00]);
								targetExpr.codeGen(cg, false, excs);
								
								cg.cellsToCells(cgScope,
												leftCells, rightExpr.getType(),
												((CGCellsScope)rightExpr.getSymbol().getCGScope()).getCells(), rightExpr.getType());
								optimized = true;
							}
							catch(Exception ex) {
								//TODO генерация не учитывает что регистры аккумулятора могут быть использованы точечно, что не может сделать targetExpr.codeGen(cg, true, excs);
								throw new CompileException("TODO:" + ex.getMessage());
							}
						}
						else {
							cg.thisToAcc(null==argExpr ? cgScope : argExpr.getCGScope());
						}
						if(!optimized && !"null".equals(nb.getRTOSFilePath())) {
							cg.nativeMethodSetArg(null==argExpr ? cgScope : argExpr.getCGScope(), signature, storedRegs, 0x00, false);
						}
					}
				}
				
				if(supportedFunct) {
					cg.functApply(cgScope, nb.getCgFunctName(), constants);
				}
				else {
					for(int i=args.size()-2; i>=0; i--) {
						// Проверяем на регистры аккумулятора, которые сохранили ранее в стек, чтобы последующие выражения их не затерли
						byte[] regs = nb.getRegs()[i];
						for(int j=regs.length-1; j>=0; j--) {
							if(cg.isAccumReg(regs[j])) {
								cgScope.append(cg.popReg(regs[j]));
							}
						}
					}

					if(!"null".equals(nb.getRTOSFilePath())) {
						cg.nativeMethodInvoke(cgScope, signature, storedRegs, type);

						cgScope.append(endLbScope);
						// Проверяем может ли метод генерировать исключения (обязательно наличие throws даже для unchecked иначе нет проверки
						// Также пропускаем, если удалось проверить индексы массива на уровне компиляции
						if(((MethodSymbol)symbol).canThrow() && !checkedByCompiler) {
							if((!excs.getRuntimeChecks().isEmpty() || !excs.getProduced().isEmpty())) {
								checkThrow(cg, cgScope, excs, signature);
							}
						}
					}
					else {
						cg.nativeMethodInvoke(cgScope, null, storedRegs, type);
					}
					// Все нативные методы с возвращаемым типом bool должны возвращать резуьтат во флаге C
					if(type.isBoolean()) {
						return CodegenResult.RESULT_IN_FLAG;
					}
					if(!type.isVoid()) {
						return CodegenResult.RESULT_IN_ACCUM;
					}
				}
			}
			
			// Точно нативным методам нужно бросать исключения?
			//checkThrow(cg, cgScope, cgs);
		}
		else {
		//	CGLabelScope rpCGScope = null;
			if(0==argTypes.length && "getClassId".equals(symbol.getName())) {
//TODO				if(methodScope instanceof VarFieldExpression) {
//					CGCellsScope cScope = (CGCellsScope)((VarFieldExpression)methodScope).getSymbol().getCGScope();
//					// Только для информирования
//					cg.invokeClassMethod(cgs, classNameStr, symbol.getName(), type, argTypes, null, false);
//					cg.cellsToAcc(cgs, cScope);
//				}
			}
			else if(0 == argTypes.length && "getClassTypeId".equals(symbol.getName())) {
				// Только для информирования
//				cg.invokeClassMethod(cgs, classNameStr, symbol.getName(), type, argTypes, null, false);
				//TODO добавить информирование				
				cg.constToAcc(cgScope, cg.getRefSize(), type.getId(), false);
			}
			else {
				int refTypeSize = 1; // TODO Определить значение на базе количества используемых типов класса
			
				CGMethodScope mScope = null;
				CIScope parentScope = (CIScope)((MethodSymbol)symbol).getScope().getParent();
				if(parentScope instanceof ClassScope) {
					depCodeGen(cg, excs);
					
					mScope = (CGMethodScope)((AstHolder)symbol).getNode().getCGScope().getScope(CGMethodScope.class);
				}

				// Не нужно беспокоиться о счетчике ссылок для аргументов, это могло бы потребоваться только для Thread.run() метода, но он не имеет
				// аргументов и не возвращает значение. Вся передача объектов в мультипоточной системе(между потоками) выполняется только через поля.

				// Всегда сохраняем heapIReg, метод может использовать его в своих целях
				cg.pushHeapReg(cgScope);

				// Если это метод не нашего класса, то нужно указать адрес heap класса вызываемого метода
				if(!isThisMethodCall) {
					if(targetExpr instanceof TypeReferenceExpression) {
					}
					else if(targetExpr instanceof VarFieldExpression) {
						if(!symbol.isStatic()) {
							CGCellsScope cScope = (CGCellsScope)((VarFieldExpression)targetExpr).getSymbol().getCGScope();
							cg.setHeapReg(cgScope, cScope.getCells());
						}
						else {
							//TODO cg.setHeapReg(...
						}
					}
					else {
						throw new CompileException("Unsupported target expression:" + targetExpr);
					}
				}

				putArgsToStack(cg, cgScope, refTypeSize, excs);

/*				int methodSN = -1;
				if(null != symbol.getCGScope()) {
					methodSN = ((CGClassScope)symbol.getCGScope().getParent()).getMethodSN((CGMethodScope)symbol.getCGScope());
				}
*/				

				if(cis instanceof ClassScope) {
					cg.methodInvoke(cgScope, mScope.getLabel(), signature, type);
				}
				else {
//					if(parentScope instanceof ClassScope) {
						List<AstNode> depends = cis.fillDepends(((MethodSymbol)symbol).getSignature());
						if(null != depends) {
							for(AstNode node : depends) {
								node.codeGen(cg, false, excs);
							}
						}
//					}
					int methodSN = cis.getMethodSN(symbol.getName(), ((MethodSymbol)symbol).getSignature());
					//TODO cis.getMethodSN возвращает SN с учетом не static и не native методов - но некоторые методы могут не генерироваться, если их никто не использует
					//Что нарушает порядок номеров методов.
					cg.invokeInterfaceMethod(cgScope, classNameStr, symbol.getName(), type, argTypes, VarType.fromClassName(cis.getName()), methodSN);
				}

				// Выходим, если исключений не ожидаем и не произвели (в том числе и unchecked)
				if(!excs.getRuntimeChecks().isEmpty() || !excs.getProduced().isEmpty()) {
					checkThrow(cg, cgScope, excs, signature);
				}
				
				if(!type.isVoid()) {
					return CodegenResult.RESULT_IN_ACCUM;
				}
			}
		}

		return null;
	}

	private void checkThrow(CodeGenerator cg, CGScope cgScope, CGExcs excs, String signature) throws CompileException {
		List<Pair<CGLabelScope, Set<Integer>>> exceptionHandlers = new ArrayList<>();
		CGLabelScope endMethodLbScope = null;

		CGScope cgScope_ = cgScope;
		Set<Integer> produced = new HashSet<>(excs.getProduced());
		l1:				
		while(null!=cgScope_) {
			if(cgScope_ instanceof CGTryBlockScope) {
				CGTryBlockScope tryCatchBlock = (CGTryBlockScope)cgScope_;
				for(CGLabelScope catchLabel : tryCatchBlock.getCatchLabels()) {
					// Проверка на базовый Exception - под него попадает любое исключение
					if(tryCatchBlock.containsException(catchLabel, 0x00)) {
						exceptionHandlers.add(new Pair<CGLabelScope, Set<Integer>>(tryCatchBlock.getCatchLabel(0x00), null));
						// Все обрабатываемые исключения здесь обрабатаны
						//TODO должен делать catch блок: excs.getRuntimeChecks().clear();
						//TODO должен делать catch блок: excs.getProduced().clear();
						produced.clear();
						break l1;
					}

					// Список в котором будут ид исключения, обрабатывемые текущим catch блоком
					HashSet<Integer> exceptionIds = new HashSet<>();
					// Перебираем произведенные исключения
					for(int producedExceptionId : new HashSet<>(produced)) {
						// Анализируем в цикле текущее исключение и все от которых оно наследуется
						boolean gotIt = false;
						Integer exceptionId = producedExceptionId;
						while(null!=exceptionId) {
							// Если исключение обрабатывается в данном catch, то добавляем его в список и удаляем из ожидаемых
							if(tryCatchBlock.containsException(catchLabel, exceptionId)) {
								//TODO должен делать catch блок: excs.getRuntimeChecks().remove(exceptionId);
								exceptionIds.add(exceptionId);
								gotIt = true;
								// Достаточно первого найденного совместимого типа в иерархии
								break;
							}
							// Получаем родителя
							exceptionId = VarType.getExceptionParent(exceptionId);
						}
						// Удаляем исключение из списка произведенных (указываем что оно обработано)
						if(gotIt) {
							//TODO должен делать catch блок: excs.getProduced().remove(producedExceptionId);
							produced.remove(producedExceptionId);
						}
					}

					// Если список обрабатываемых исключений не пустой, то формируем обработчик
					if(!exceptionIds.isEmpty()) {
						exceptionHandlers.add(new Pair<CGLabelScope, Set<Integer>>(catchLabel, exceptionIds));
					}
				}
			}
			// Находим выход из метода
			if(null!=cgScope_.getParent() && cgScope_.getParent() instanceof CGMethodScope) {
				endMethodLbScope = ((CGBlockScope)cgScope_).getELabel();
				break;
			}
			cgScope_ = cgScope_.getParent();
		}

		ExcsThrowPoint excsThrowPoint = cg.getTargetInfoBuilder().addExcsThrowPoint(cg, sp, signature);
		cg.throwCheck(cgScope, exceptionHandlers, endMethodLbScope, excsThrowPoint);
	}
	
	private Operand makeNativeOperand(CodeGenerator cg, ExpressionNode expr, CGExcs excs) throws CompileException {
		if(expr instanceof VarFieldExpression) {
			VarFieldExpression ve = (VarFieldExpression)expr;
			// Кодогенерация VarFieldExpression выполняет запись значения в аккумулятор, но нам этого не нужно, выполняем только зависимость
			ve.depCodeGen(cg, excs);
			return new Operand(VarType.CSTR == ve.getType() ? OperandType.FLASH_RES : OperandType.LOCAL_RES, ve.getSymbol().getCGScope().getResId());
		}
		else if(expr instanceof LiteralExpression) {
			LiteralExpression le = (LiteralExpression)expr;
			if(VarType.CSTR==le.getType()) {
				CGVarScope vScope = cg.enterLocal(cgScope, VarType.CSTR, -1, true, null);
				vScope.setDataSymbol(cg.defineData(vScope.getResId(), -1, le.getStringValue()));
				return new Operand(OperandType.FLASH_RES, vScope.getResId());
			}
			else if(le.getType().isFixedPoint()) {
				return new Operand(OperandType.LITERAL_FIXED, le.getFixedValue());
			}
			else {
				return new Operand(OperandType.LITERAL, le.getNumValue());
			}
		}
		else if(expr instanceof MethodCallExpression) {
			MethodCallExpression mce = (MethodCallExpression)expr;
			mce.codeGen(cg, false, excs);
			return new Operand(OperandType.ACCUM, null);
		}
		else if(expr instanceof CastExpression) {
			CastExpression ce = (CastExpression)expr;
			ce.codeGen(cg, false, excs);
			return makeNativeOperand(cg, ce.getOperand(), excs);
		}
		else if(expr instanceof BinaryExpression) {
			BinaryExpression be = (BinaryExpression)expr;
			be.codeGen(cg, false, excs);
			return new Operand(OperandType.ACCUM, null);
		}
		else if(expr instanceof UnaryExpression) {
			UnaryExpression ue = (UnaryExpression)expr;
			ue.codeGen(cg, true, excs);
			return new Operand(OperandType.ACCUM, null);
		}
		else if(expr instanceof ArrayExpression) {
			ArrayExpression ae = (ArrayExpression)expr;
			ae.codeGen(cg, false, excs);

			return new Operand(OperandType.ARRAY, null);
		}
		else if(expr instanceof PropertyExpression) {
			PropertyExpression ape = (PropertyExpression)expr;
			ape.codeGen(cg, true, excs);
			return new Operand(OperandType.ACCUM, null);
		}
		else if(expr instanceof EnumExpression) {
			return new Operand(OperandType.LITERAL, ((EnumExpression)expr).getIndex());
		}
		else throw new CompileException("Unexpected expression:" + expr);
	}

	private void putArgsToStack(CodeGenerator cg, CGScope cgs, int refTypeSize, CGExcs excs) throws CompileException {
		if(!args.isEmpty()) {
//			CGScope oldCGScope = cg.setScope(symbol.getCGScope());


			CGMethodScope mScope = (CGMethodScope)symbol.getCGScope(CGMethodScope.class);
			
			//	Рудимент?
			if(null!=mScope) {
				mScope.clearArgs(); //скорее всего не зайдет, нам нужно удалять только те, которые мы даобавили
			}
			

			//CGIContainer cont = new CGIContainer();
			for(int i=0; i<args.size(); i++) {
			//for(int i=args.size()-1; i>=0; i--) {
				// Получаем параметр вызываемого метода
				Symbol paramSymbol = ((MethodSymbol)symbol).getParameters().get(i);
				VarType paramVarType = paramSymbol.getType();

				// Получаем выражение передаваемое этому параметру
				ExpressionNode argExpr = args.get(i);
				// Получаю из scope вызываемого метода переменую с именем параметра(пустая переменная уже создана, или создан Alias)
				Symbol vSymbol = ((MethodSymbol)symbol).getScope().resolveVar(paramSymbol.getName());
				if(vSymbol instanceof AliasSymbol) {
					// Это алиас, который нужно что???
					throw new CompileException("Unsupported alias:" + vSymbol.toString());
				}
				else {
					// Создаем новую область видимости переменной в вызываемом методе
					//TODO для чего было refTypeSize + cg.getRefSize()?
					//int varSize = paramVarType.isObject() || paramVarType.isArray() ? refTypeSize : paramVarType.getSize();
					//refTypeSize - это размер размера типа, а нам похоже нужен размер типа (для объекта и массива в том числе)
					int varSize = paramVarType.isObject() || paramVarType.isArray() ? cg.getRefSize() : paramVarType.getSize();
					CGVarScope dstVScope = cg.enterLocal(symbol.getCGScope(CGMethodScope.class), vSymbol.getType(), varSize, false, vSymbol.getName());
					dstVScope.build(); // Выделяем память в стеке

					vSymbol.setCGScope(dstVScope);

					if(argExpr instanceof LiteralExpression) {
						// Выполняем зависимость
						argExpr.codeGen(cg, false, excs);
						LiteralExpression le = (LiteralExpression)argExpr;
						cg.pushConst(cgs, varSize, le.getType().isFixedPoint() ? le.getFixedValue() : le.getNumValue(), le.getType().isFixedPoint());
					}
					else if(argExpr instanceof VarFieldExpression) {
						argExpr.codeGen(cg, false, excs);
						cg.pushCells(cgs, varSize, ((CGCellsScope)argExpr.getSymbol().getCGScope()).getCells());
					}
					else if(argExpr instanceof MethodCallExpression) {
						argExpr.codeGen(cg, true, excs);
						cg.pushAccBE(cgs, paramVarType.getSize());
					}
					else if(argExpr instanceof BinaryExpression) {
						CGBranch branch = new CGBranch();
						cgs.setBranch(branch);

						argExpr.codeGen(cg, true, excs);

						if(branch.isUsed()) {
							cg.constToAcc(cgs, 1, 1, false);
							CGLabelScope lbScope = new CGLabelScope(null, null, LabelNames.LOCGIC_END, true);
							cg.jump(cgs, lbScope);
							cgs.append(((CGBranch)branch).getEnd());
							cg.constToAcc(cgs, 1, 0, false);
							cgs.append(lbScope);
						}
						cg.pushAccLE(cgs, paramVarType.getSize());
					}
					else if(argExpr instanceof EnumExpression) {
						cg.pushConst(cgs, 0x01, ((EnumExpression)argExpr).getIndex(), false);
					}
					else if(argExpr instanceof CastExpression) {
						argExpr.codeGen(cg, true, excs);
						cg.pushAccLE(cgs, paramVarType.getSize());
					}
					else {
						throw new CompileException("Unsupported expression:" + argExpr);
					}
				}
			}
		}
	}

	@Override
	public List<AstNode> getChildren() {
		return null;
	}

	@Override
	public String getQualifiedPath() {
		return (null!=targetExpr ? targetExpr.getQualifiedPath() + ".": "" ) + methodName;
	}

	@Override
	public String toString() {
		return (null==type ? VarType.VOID : type) + " " + (null==targetExpr ? "" : targetExpr + ".") + methodName + "(" + StrUtils.toString(args) + ")";
	}

	public String getFullInfo() {
		return getClass().getSimpleName() + " " + toString();
	}
}
