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
import java.util.Objects;
import java.util.Set;
import ru.vm5277.common.ExcsThrowPoint;
import ru.vm5277.common.LabelNames;
import ru.vm5277.common.NativeBinding;
import ru.vm5277.common.Pair;
import ru.vm5277.common.enums.RtosFeature;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.scopes.CGMethodScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.cg.scopes.CGVarScope;
import ru.vm5277.common.enums.CodegenResult;
import ru.vm5277.common.VarType;
import ru.vm5277.common.exceptions.CompileException;
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
import static ru.vm5277.common.enums.SemanticAnalyzePhase.CODEGEN;
import ru.vm5277.common.lexer.SourcePosition;
import ru.vm5277.common.StrUtils;
import ru.vm5277.common.enums.RtosParam;
import ru.vm5277.common.cg.CGBranch;
import ru.vm5277.common.cg.CGCells;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.scopes.CGBlockScope;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.cg.scopes.CGMethodCallExpressionScope;
import ru.vm5277.common.cg.scopes.CGTryBlockScope;
import ru.vm5277.common.enums.J8BException;
import ru.vm5277.compiler.Instance;
import ru.vm5277.compiler.nodes.expressions.bin.BinaryExpression;
import ru.vm5277.compiler.semantic.BlockScope;
import ru.vm5277.compiler.semantic.CIScope;
import ru.vm5277.compiler.semantic.ClassScope;
import ru.vm5277.compiler.semantic.ExceptionScope;
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
	private			boolean					isWrapper			= false;
	private			CGExcs					localExcs			= new CGExcs();
	
	public MethodCallExpression(Instance inst, TokenBuffer tb, SourcePosition sp, ExpressionNode parentExpr, String methodName, List<ExpressionNode> args,
								boolean isWrapper) {
		super(inst, tb, sp);

		this.targetExpr = parentExpr;
		this.methodName = methodName;
		
		this.args = args;
		
		this.isWrapper = isWrapper;
	}

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
			if(!result) break;
		}

		debugAST(this, DECLARE, false, result, getFullInfo());
		return result;
	}

	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg, CGScope parent) {
		boolean result = true;
		debugAST(this, POST, true, getFullInfo());
		cgScope = cg.enterMethodCallExpression(parent, sp, cgScope, toString());

		headerCGScope = new CGScope(cgScope, null, CGScope.genId(), "methodCall header");
		
		try {
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
					if(cis instanceof ClassScope) {
						symbol = ((ClassScope)cis).resolveConstructor(methodName, argTypes, true, new HashSet<>());
					}
					if(null==symbol) {
						symbol = cis.resolveMethod(methodName, argTypes, false, new HashSet<>());
					}
				}
				else if(targetExpr instanceof TypeReferenceExpression) {
					cis = ((TypeReferenceExpression)targetExpr).getScope();
					symbol = cis.resolveMethod(methodName, argTypes, true, new HashSet<>());
					
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
						cis = scope.getThis().resolveCI(null, varType.getClassName(), false, new HashSet<>());
						if(null!=cis) {
							symbol = cis.resolveMethod(methodName, argTypes, true, new HashSet<>());
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
						else if(scope_ instanceof MethodScope) {
							MethodScope methodScope = (MethodScope)scope_;
							// Нашли блок кода, исключения заполнены только для catch блока, проверяем на обработку
							for(ExceptionScope catchExScope : methodScope.getExceptionScopes()) {
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
//Отказываюсь от данной реализации, выношу проверку границ массива в нативный код, таким образом снимаю требование передачи длины массива (так как функция
//вычисления длины для byte[] легковесная)
/*			// Проверяем - нативный метод требует для безопасности, чтобы после аргумента типа array следующим параметром шла его длина
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
						if(i<(argTypes.length-1) && args.get(i+1) instanceof LiteralExpression) {
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
			}*/

			if(0x02==argTypes.length && null!=cis && "System".equals(cis.getName()) && "setParam".equals(methodName)) {
				ExpressionNode paramNameExpr = args.get(0);
				ExpressionNode optimizedExpr = paramNameExpr.optimizeWithScope(scope, cg);
				if(null != optimizedExpr) {
					paramNameExpr = optimizedExpr;
				}

				if(	VarType.BYTE==argTypes[0] && VarType.BYTE==argTypes[1] &&
					paramNameExpr instanceof LiteralExpression && args.get(1) instanceof LiteralExpression) {
					
					RtosParam param = RtosParam.fromInt((int)((LiteralExpression)paramNameExpr).getNumValue());
					if(null!=param) {
						cg.getDevice().setParam(param, (int)((LiteralExpression)args.get(1)).getNumValue());
					}
					else {
						markError("Unknown RTOSParam id: " + ((LiteralExpression)paramNameExpr).getValue());
						result=false;
					}
				}
				else {
					markError("Method System.setParam(byte param, byte value) supports constants only");
					result=false;
				}
			}

			// Фичу мультипоточности включаем если обнаружен вызов Thread.start() или Timer.start(short),
			// на создание класса не обращаем внимание - он может не использоваться
			if(result && null!=symbol && symbol.isNative()) {
				if(	(0x00==argTypes.length && null!=cis && "Thread".equals(cis.getName()) && "start".equals(methodName)) ||
					(0x01==argTypes.length && null!=cis && "Timer".equals(cis.getName()) && "start".equals(methodName))) {
				
					cg.setFeature(RtosFeature.OS_FT_MULTITHREADING);
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
	
	public void exceptionsCheck() {
		if(!symbol.isNative()) {
			if(	(0x00==argTypes.length && null!=cis && cis instanceof ExceptionScope && "throw".equals(methodName)) ||
				(0x01==argTypes.length && null!=cis && cis instanceof ExceptionScope && "throw".equals(methodName) && VarType.BYTE==type)) {

				// Добавляем тим исключения генерируемый throw
				localExcs.getProduced().add(((ExceptionScope)cis).getId());
			}
		}
		
		// Для нативных методов проверяем возможность генерации исключения
		// Наличие throws означает, что нативный метод генерирует указанные исключения (также нужно указывать Unchecked исключения)
		if(symbol.isNative() && ((MethodSymbol)symbol).canThrow()) {
			for(ExceptionScope eScope : ((MethodSymbol)symbol).getScope().getExceptionScopes()) {
				localExcs.getProduced().add(eScope.getId());
			}
		}
	}

	@Override
	public Object codeGen(CodeGenerator cg, boolean toAccum, CGExcs excs) throws CompileException {
		excs.getProduced().addAll(localExcs.getProduced());
		if(cgDone || disabled) return null;
		cgDone = true;

		exceptionsCheck();
		
		CGExcs newExcs = new CGExcs();
		newExcs.getRuntimeChecks().putAll(excs.getRuntimeChecks());
		newExcs.setMethodEndLabel(excs.getMethodEndLabel());
		
		Object result = null;
		
		debugAST(this, CODEGEN, true, getFullInfo());

		VarType castType = cg._getAcc().pullCast();
		
		if(null!=targetExpr) {
			targetExpr.codeGen(cg, false, newExcs);
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
			else if(VarType.NULL==argTypes[i]) {
				argTypes[i] = methodArgTypes.get(i);
			}
		}
		
		String signature = buildSignature(cis, methodName, (MethodSymbol)symbol, argTypes);
		
		if(signature.equals("System.setParam(byte,byte)")) {
/*		Обрабатываем в postAnalyze
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
					cg.setFeature(RtosFeature.OS_FT_HEARTBEAT);
				case CORE_FREQ:
				case STDIO_PORT:
				case HALT_OK_MODE:
				case HALT_ERR_MODE:
					cg.getDevice().setParam(rtosParam, valueId);
					break;
				case MULTITHREADING:
					if(0x00!=valueId) {
						cg.setFeature(RtosFeature.OS_FT_MULTITHREADING);
					}
					break;
				case SHOW_WELCOME:
					if(0x00!=valueId) {
						cg.setFeature(RtosFeature.OS_FT_WELCOME);
					}
					break;
				case DIAG_MODE:
					if(0x00!=valueId) {
						cg.setFeature(RtosFeature.OS_FT_DIAG);
					}
					break;
			}*/
		}
		else if(cis instanceof ExceptionScope && (signature.endsWith(".throw()") || signature.endsWith(".throw(byte)"))) {
			ExceptionScope eScope = (ExceptionScope)cis;
			newExcs.getProduced().add(eScope.getId());
			if(0x01==args.size()) {
				ExpressionNode codeEn = args.get(0);
				if(VarType.BYTE!=codeEn.getType()) {
					throw new CompileException("Unsupported arg expression:'" + codeEn.toString() + "' for method: '" + signature + "'");
				}
				CGCells leftCells = new CGCells(CGCells.Type.ACC, new byte[]{1});
				CGCells rightCells = null;
				if(codeEn instanceof LiteralExpression && codeEn.getType().isInteger()) {
					rightCells = new CGCells(((LiteralExpression)codeEn).getNumValue());
				}
				else if(codeEn.getSymbol().getCGScope() instanceof CGCellsScope) {
					rightCells = ((CGCellsScope)codeEn.getSymbol().getCGScope()).getCells();
				}
				else {
					throw new CompileException("СOPMILER_BUG: Unsupported arg expression:'" + codeEn.toString() + "' for method: '" + signature + "'");
				}
				cg.cellsToCells(cgScope, leftCells, VarType.BYTE, rightCells, VarType.BYTE, castType);
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
						lbScope = ((CGBlockScope)cgScope_).getEndLabel();
						break;
					}
					else {
						for(Integer catchExceptionId : ((CGMethodScope)cgScope_.getParent()).getExceptionIds()) {
							if(eScope.isCompatible(catchExceptionId)) {
								isMethodLeave = true;
								lbScope = ((CGBlockScope)cgScope_).getEndLabel();
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
//---GET-CLASS_ID--------
		else if(signature.endsWith(".getClassId()")) {
			if(targetExpr instanceof VarFieldExpression) {
				CGCellsScope cScope = (CGCellsScope)((VarFieldExpression)targetExpr).getSymbol().getCGScope();
				cg.classIdToAcc(cgScope, cScope.getCells());
				result = CodegenResult.RESULT_IN_ACCUM;
			}
			else {
				throw new CompileException("Unsupported targetExpression: " + targetExpr.toString() + " for getClassId()");
			}
		}
//---GET-CLASS-TYPE-ID--------
		else if(signature.endsWith(".getClassTypeId()")) {
			if(targetExpr instanceof VarFieldExpression) {
				CGCellsScope cScope = (CGCellsScope)((VarFieldExpression)targetExpr).getSymbol().getCGScope();
				cg.classTypeIdToAcc(cgScope, cScope.getCells());
				result = CodegenResult.RESULT_IN_ACCUM;
			}
			else {
				throw new CompileException("Unsupported targetExpression: " + targetExpr.toString() + " for getClassTypeId()");
			}
		}
//---FINALIZE--------
		else if(signature.endsWith(".finalize()")) {
			throw new CompileException("finalize method not supported yet");
		}
//---THREAD.ISALIVE
		else if(symbol.isNative() && !cg.isSupportedFunct(RtosFeature.OS_FT_MULTITHREADING.name()) && signature.equals("Thread.isAlive()")) {
			// Если многопоточность не используется то этот метод всегда возвращает константную TRUE
			result = CodegenResult.TRUE;
		}
//---NATIVE--------
		else if(symbol.isNative()) {
			if(	signature.equals("System.out(cstr)") && args.get(0) instanceof LiteralExpression &&
				VarType.CSTR==args.get(0).getType() && ((LiteralExpression)args.get(0)).getStringValue().isEmpty()) {
				// Ничего не делаем
				// TODO вынести в AST оптимизацию
			}
			//TODO для универсальности - в методе может быть несколько аргументов
			else if(0x01==args.size() && args.get(0x00) instanceof CstrConcatExpression) {
				CstrConcatExpression ce = (CstrConcatExpression)args.get(0x00);

				// Ранее обнаружив isOverflow отказались выполнять склеивание в строку
				// так как переполнение должно генерировать исключение если находимся в блоке try-catch для MathOverflowException
				// Но на тот момент мы не могли проверить наличие блока
				// Здесь можем, и если блока нет, а LiteralExpression с переполнением есть - пытаеся оптимизировать еще раз
				Integer exceptionId = cg.getExcsChecker().getHandled(excs, J8BException.MathOverflowException.name());
				if(null==exceptionId) {
					boolean hasClearedOverflows = false;
					for(ExpressionNode expr : ce.getExprs()) {
						if(expr instanceof LiteralExpression && ((LiteralExpression)expr).isOverflow()) {
							((LiteralExpression)expr).clearOverflow();
							hasClearedOverflows = true;
						}
					}
					
					if(hasClearedOverflows) {
						ce.codeOptimization(cis, cg);
					}
				}

				for(int exprIndex=0; exprIndex<ce.getExprs().size(); exprIndex++) {
					ExpressionNode exprNode = ce.getExprs().get(exprIndex);

					
					
					
					signature = cis.getName() + "." + methodName + "("; //TODO проверить - отличается от buildSignature
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

					NativeBinding nb = cg.nativeMethodInit(exprNode.getCGScope(), signature);
					if(null==nb) {
						throw new CompileException("Not found native method binding for method: " + signature);
					}
					cg.includeFunct(nb);
					List<Byte> storedRegs = new ArrayList<>();

					if(!signature.equals("System.out(exception)")) {
						if(exprNode instanceof BinaryExpression) {
							CGBranch branch = new CGBranch();
							exprNode.getCGScope().setBranch(branch);

							exprNode.codeGen(cg, true, newExcs);

							if(branch.isUsed()) {
								//cg.constToAcc(exprNode.getCGScope(), 1, 1, false);
								cg.cellsToCells(exprNode.getCGScope(), new CGCells(CGCells.Type.ACC), VarType.BOOLEAN, new CGCells(1), VarType.BOOLEAN, null);
								CGLabelScope lbScope = new CGLabelScope(null, null, LabelNames.LOCGIC_END, true);
								cg.jump(exprNode.getCGScope(), lbScope);
								exprNode.getCGScope().append(((CGBranch)branch).getEnd());
								//cg.constToAcc(exprNode.getCGScope(), 1, 0, false);
								cg.cellsToCells(exprNode.getCGScope(), new CGCells(CGCells.Type.ACC), VarType.BOOLEAN, new CGCells(0), VarType.BOOLEAN, null);
								exprNode.getCGScope().append(lbScope);
							}
						}
						else if(exprNode instanceof UnaryExpression) {
							CGBranch branch = new CGBranch();
							exprNode.getCGScope().setBranch(branch);

							exprNode.codeGen(cg, false, newExcs);

							if(branch.isUsed()) {
								//cg.constToAcc(exprNode.getCGScope(), 1, 1, false);
								cg.cellsToCells(exprNode.getCGScope(), new CGCells(CGCells.Type.ACC), VarType.BOOLEAN, new CGCells(1), VarType.BOOLEAN, null);
								CGLabelScope lbScope = new CGLabelScope(null, null, LabelNames.LOCGIC_END, true);
								cg.jump(exprNode.getCGScope(), lbScope);
								exprNode.getCGScope().append(((CGBranch)branch).getEnd());
								//cg.constToAcc(exprNode.getCGScope(), 1, 0, false);
								cg.cellsToCells(exprNode.getCGScope(), new CGCells(CGCells.Type.ACC), VarType.BOOLEAN, new CGCells(0), VarType.BOOLEAN, null);
								exprNode.getCGScope().append(lbScope);
							}
						}
						else {
							if(CodegenResult.RESULT_IN_ACCUM!=exprNode.codeGen(cg, true, newExcs)) {
								throw new CompileException("Accum not used for argument:" + exprNode);
							}
						}

						cg.nativeMethodSetArg(exprNode.getCGScope(), signature, storedRegs, 0, exprNode.getType().isFixedPoint());
					}
					cg.nativeMethodInvoke(exprNode.getCGScope(), nb, signature, storedRegs, type, false);
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
				
				// Артефакт CGLabelScope endLbScope = new CGLabelScope(null, CGScope.genId(), LabelNames.INVOKE_END, true);
				
				NativeBinding nb = cg.nativeMethodInit((args.isEmpty() ? cgScope : args.get(0).getCGScope()), signature);
				List<Byte> storedRegs = new ArrayList<>();
				if(null==nb) {
					// Метод не найден в текущей реализации, может быть реализация в интерфейсе?
					Scope parent = ((MethodSymbol)symbol).getScope().getParent();
					if(parent instanceof InterfaceScope) {
						MethodSymbol methodSymbol = ((InterfaceScope)parent).resolveMethod(methodName, argTypes, false, new HashSet<>());
						if(null!=methodSymbol) {
							signature = buildSignature(((InterfaceScope)parent), methodName, (MethodSymbol)symbol, argTypes);
							nb = cg.nativeMethodInit((args.isEmpty() ? cgScope : args.get(0).getCGScope()), signature);
						}
					}
					if(null==nb) {
						throw new CompileException("Not found native method binding for method: " + signature);
					}
				}

				List<Byte> storedAccRegs = new ArrayList<>();
				boolean arrayBoundsChecked = isWrapper;
				boolean supportedFunct = null!=constants && null!=nb.getCgFunctName() && !nb.getCgFunctName().isEmpty() &&
										cg.isSupportedFunct(nb.getCgFunctName());
				boolean functExecuted = supportedFunct && cg.functApply(cgScope, nb.getCgFunctName(), constants);
				if(!functExecuted) {
					cg.includeFunct(nb);
					if(!signature.equals("System.out(exception)")) {
						int index=0x00;
						if(null!=nb.getMethodParams() && VarType.VOID==nb.getMethodParams()[0x00]) { // this в первом аргументе
							index=0x01;
						}

						ExpressionNode argExpr = null;
						for(int i=0; i<args.size(); i++) {
							/*if(0!=i) {
								// Необходимо проверять в какие регистры сохранился последний аргумент, если это регистры аккумулятора, то обработка следующего
								// аргумента может повредить аккумулятор - нужно сохранить в стек
								byte[] lastRegs = nb.getRegs()[(index+i)-1];
								for(byte lastReg : lastRegs) {
									if(cg.isAccumReg(lastReg)) {
										for(int rightRegsIndex = index+i; rightRegsIndex<args.size(); rightRegsIndex++) {
											byte[] rightRegs = nb.getRegs()[rightRegsIndex];
											for(byte rightReg : rightRegs) {
												if(!cg.isAccumReg(rightReg) || lastReg<rightReg) {
													args.get(i-1).getCGScope().append(cg.pushReg(lastReg));
													storedAccRegs.add(lastReg);
													break;
												}
											}
										}
									}
								}
							}*/
							//Все проще - если аргумент не последний и записался в аккумлятор то этот регистр нужно сохранить в стек
/*	Перенес в конец работы с аргументом - теряем данные если пишем в следующий - еще не обработанный аргумент
							if(i!=args.size()-1) {
								byte[] lastRegs = nb.getRegs()[(index+i)];
								for(byte lastReg : lastRegs) {
									if(cg.isAccumReg(lastReg)) {
										args.get(i+1).getCGScope().append(cg.pushReg(lastReg));
										storedAccRegs.add(lastReg);
									}
								}
							}*/

							boolean optimized = false;

							argExpr = args.get(i);
							//cg.accResize(argTypes[i]);

							if(argExpr instanceof BinaryExpression) {
								CGBranch branch = new CGBranch();
								argExpr.getCGScope().setBranch(branch);

								argExpr.codeGen(cg, true, newExcs);

								if(branch.isUsed()) {
									//cg.constToAcc(argExpr.getCGScope(), 1, 1, false);
									cg.cellsToCells(argExpr.getCGScope(), new CGCells(CGCells.Type.ACC), VarType.BOOLEAN, new CGCells(1), VarType.BOOLEAN, null);
									CGLabelScope lbScope = new CGLabelScope(null, null, LabelNames.LOCGIC_END, true);
									cg.jump(argExpr.getCGScope(), lbScope);
									argExpr.getCGScope().append(((CGBranch)branch).getEnd());
									//cg.constToAcc(argExpr.getCGScope(), 1, 0, false);
									cg.cellsToCells(argExpr.getCGScope(), new CGCells(CGCells.Type.ACC), VarType.BOOLEAN, new CGCells(0), VarType.BOOLEAN, null);
									argExpr.getCGScope().append(lbScope);
								}
							}
							else if(argExpr instanceof UnaryExpression) {
								CGBranch branch = new CGBranch();
								argExpr.getCGScope().setBranch(branch);

								argExpr.codeGen(cg, false, newExcs);

								if(branch.isUsed()) {
									//cg.constToAcc(argExpr.getCGScope(), 1, 1, false);
									cg.cellsToCells(argExpr.getCGScope(), new CGCells(CGCells.Type.ACC), VarType.BOOLEAN, new CGCells(1), VarType.BOOLEAN, null);
									CGLabelScope lbScope = new CGLabelScope(null, null, LabelNames.LOCGIC_END, true);
									cg.jump(argExpr.getCGScope(), lbScope);
									argExpr.getCGScope().append(((CGBranch)branch).getEnd());
									//cg.constToAcc(argExpr.getCGScope(), 1, 0, false);
									cg.cellsToCells(argExpr.getCGScope(), new CGCells(CGCells.Type.ACC), VarType.BOOLEAN, new CGCells(0), VarType.BOOLEAN, null);
									argExpr.getCGScope().append(lbScope);
								}
							}
							else if(argExpr instanceof LiteralExpression && ((LiteralExpression)argExpr).getType().isNumeric()) {
								LiteralExpression le = (LiteralExpression)argExpr;
								argExpr.codeGen(cg, false, newExcs);
								CGCells leftCells = new CGCells(CGCells.Type.REG, nb.getRegs()[index+i]);
								// Сохраняем регистры если они не принадлежат аккумулятору
								for(int reg : leftCells.getIds()) {
									if(!cg.isAccumReg((byte)reg)) {
										//args.get(0).getCGScope().append(cg.pushReg((byte)reg));
										storedRegs.add((byte)reg);
									}
								}
								CGCells rightCells = new CGCells(le.getType().isFixedPoint() ? le.getFixedValue() : le.getNumValue());
								cg.cellsToCells(argExpr.getCGScope(), leftCells, argTypes[i], rightCells, argExpr.getType(), null);
								optimized = true;
							}
							else if(argExpr instanceof MethodReferenceExpression) {
								argExpr.codeGen(cg, false, newExcs);
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
	// Почему именно здесь и именно так? Кодогенерация должна быть ниже
	//									if(!(argExpr instanceof MethodCallExpression)) { //TODO почему для MethodCallExpression получаем дубль? Где-то ранее подготавливаем? - Именно здесь и делаем дубль
	//										argExpr.codeGen(cg, false, excs);
	//									}

										CGCells leftCells = new CGCells(CGCells.Type.REG, nb.getRegs()[index+i]);
										// Сохраняем регистры если они не принадлежат аккумулятору
										for(int reg : leftCells.getIds()) {
											if(!cg.isAccumReg((byte)reg) && !cg.isArrReg((byte)reg)) {
												//args.get(0).getCGScope().append(cg.pushReg((byte)reg));
												storedRegs.add((byte)reg);
											}
										}
										if(null!=argExpr.getSymbol() && argExpr.getSymbol().getCGScope() instanceof CGCellsScope) {
											argExpr.codeGen(cg, false, newExcs);
											CGCells rightCells = ((CGCellsScope)argExpr.getSymbol().getCGScope()).getCells();
											if(null!=rightCells) {
												cg.cellsToCells(argExpr.getCGScope(), leftCells, null, rightCells, null, null);
												optimized = true;
											}
										}
										else if(argExpr instanceof LiteralExpression && VarType.CSTR!=argExpr.getType()) {
											LiteralExpression le = (LiteralExpression)argExpr;
											CGCells rightCells = new CGCells(le.getType().isFixedPoint() ? le.getFixedValue() : le.getNumValue());
											cg.cellsToCells(argExpr.getCGScope(), leftCells, null, rightCells, null, null);
											optimized = true;
										}
									}
									catch(Exception ex) {
										System.out.println("TODO:" + ex.getMessage());
										ex.printStackTrace();
									}
								}

								if(!optimized) {
									if(CodegenResult.RESULT_IN_ACCUM!=argExpr.codeGen(cg, true, newExcs)) {
										throw new CompileException("Accum not used for argument:" + argExpr);
									}
								}
							}

							if(!optimized) {
								//Использование метода cellsToCells упрощает задачу - он обеспечивает данные сразу в нужных регистрах.
								//TODO хорошо бы заменить всю генерацию кода по записи в регистры на базе cellsToCells
								cg.nativeMethodSetArg(argExpr.getCGScope(), signature, storedRegs, index+i, argExpr.getType().isFixedPoint());
							}
	//Отказываюсь от данной реализации, выношу проверку границ массива в нативный код, таким образом снимаю требование передачи длины массива (так как функция
	//вычисления длины для byte[] легковесная)
	/*						if(0!=i && args.get(i-1).getType().isArray()) {
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
											arrayBoundsChecked = true;
										}
									}
								}
								if(!arrayBoundsChecked) {
									cg.setFeature(RtosFeature.OS_FT_ETRACE);
									excs.getProduced().add(VarType.getExceptionId(J8BException.BoundsException.name()));
								}
								cg.nativeMethodArrArgPrepare(argExpr.getCGScope(), signature, storedRegs, index+i, arrayBoundsChecked, endLbScope);
							}*/
							if(i!=args.size()-1) {
								byte[] lastRegs = nb.getRegs()[(index+i)];
								for(byte lastReg : lastRegs) {
									if(cg.isAccumReg(lastReg)) {
										argExpr.getCGScope().append(cg.pushReg(lastReg));
										storedAccRegs.add(lastReg);
									}
								}
							}
						}
						if(null!=nb.getMethodParams() && VarType.VOID==nb.getMethodParams()[0x00]) { // targetExpr.this в первом аргументе
							if(null!=targetExpr) {
								try {
									ExpressionNode rightExpr = targetExpr;
									CGCells leftCells = new CGCells(CGCells.Type.REG, nb.getRegs()[0x00]);
									targetExpr.codeGen(cg, false, newExcs);

									// Сохраняем регистры если они не принадлежат аккумулятору
									for(int reg : leftCells.getIds()) {
										if(!cg.isAccumReg((byte)reg)) {
//											if(args.isEmpty()) {
//												cgScope.append(cg.pushReg((byte)reg));
//											}
//											else {
//												args.get(0).getCGScope().append(cg.pushReg((byte)reg));
//											}
											storedRegs.add((byte)reg);
										}
									}

									cg.cellsToCells(cgScope, leftCells, rightExpr.getType(),
													((CGCellsScope)rightExpr.getSymbol().getCGScope()).getCells(), rightExpr.getType(), castType);
								}
								catch(Exception ex) {
									//TODO генерация не учитывает что регистры аккумулятора могут быть использованы точечно, что не может сделать targetExpr.codeGen(cg, true, excs);
									ex.printStackTrace();
									throw new CompileException("TODO:" + ex.getMessage());
								}
							}
	/*						else {
								cg.thisToAcc(null==argExpr ? cgScope : argExpr.getCGScope());
							}
							if(!optimized && !"null".equals(nb.getRTOSFilePath())) {
								cg.nativeMethodSetArg(null==argExpr ? cgScope : argExpr.getCGScope(), signature, storedRegs, 0x00, false);
							}*/
						}
					}
				}
				
				if(!functExecuted) {
					for(int storedRegsIndex=storedRegs.size()-1; storedRegsIndex>=0; storedRegsIndex--) {
						if(args.isEmpty()) {
							cgScope.prepend(cg.pushReg(storedRegs.get(storedRegsIndex)));
						}
						else {
							args.get(0).getCGScope().prepend(cg.pushReg(storedRegs.get(storedRegsIndex)));
						}
					}
					
					for(int storedAccRegsIndex=storedAccRegs.size()-1; storedAccRegsIndex>=0; storedAccRegsIndex--) {
						cgScope.append(cg.popReg(storedAccRegs.get(storedAccRegsIndex)));
					}

/*//TODO нужно проверять не идут ли аккумуляторные регистры последними - если это так, то сохранять их не нужно
					for(int i=args.size()-1; i>=0; i--) {
						// Проверяем на регистры аккумулятора, которые сохранили ранее в стек, чтобы последующие выражения их не затерли
						byte[] regs = nb.getRegs()[i];
						for(int j=regs.length-1; j>=0; j--) {
							if(cg.isAccumReg(regs[j])) {
								cgScope.append(cg.popReg(regs[j]));
							}
						}
					}*/

					if(!"null".equals(nb.getRTOSFilePath())) {
						cg.nativeMethodInvoke(	cgScope, nb, signature, storedRegs, type,
												null==targetExpr && null!=nb.getMethodParams() && VarType.VOID==nb.getMethodParams()[0x00]);

						// артефакт cgScope.append(endLbScope);
						// Проверяем может ли метод генерировать исключения (обязательно наличие throws даже для unchecked иначе нет проверки
						// Также пропускаем, если удалось проверить индексы массива на уровне компиляции
						if(((MethodSymbol)symbol).canThrow() && !arrayBoundsChecked) {
							//Считаем наличие throws означает, что нативный метод генерирует указанные исключения (unchecked также необходимо указывать)
							for(ExceptionScope eScope : ((MethodSymbol)symbol).getScope().getExceptionScopes()) {
								newExcs.getProduced().add(eScope.getId());
							}
							checkThrow(cg, cgScope, newExcs, signature);
						}
					}
					else {
						cg.nativeMethodInvoke(cgScope, nb, null, storedRegs, type, false);
					}
				}
				// Все нативные методы с возвращаемым типом bool должны возвращать результат во флаге C
				if(type.isBoolean()) {
					result = CodegenResult.RESULT_IN_FLAG;
				}
				else if(!type.isVoid()) {
					result = CodegenResult.RESULT_IN_ACCUM;
				}
			}
		}
		else {
//---NOT-NATIVE--------
			//	CGLabelScope rpCGScope = null;
//---NOT-NATIVE-OTHER--------
			int refTypeSize = 1; // TODO Определить значение на базе количества используемых типов класса

			CGMethodScope mScope = null;
			CIScope parentScope = (CIScope)((MethodSymbol)symbol).getScope().getParent();
			if(parentScope instanceof ClassScope) {
				depCodeGen(cg, newExcs);

				mScope = (CGMethodScope)((AstHolder)symbol).getNode().getCGScope().getScope(CGMethodScope.class);
			}
			else if(parentScope instanceof InterfaceScope) {
				depCodeGen(cg, newExcs);

				if(null!=((AstHolder)symbol).getNode().getCGScope()) {
					mScope = (CGMethodScope)((AstHolder)symbol).getNode().getCGScope().getScope(CGMethodScope.class);
				}
			}

			// Не нужно беспокоиться о счетчике ссылок для аргументов, это могло бы потребоваться только для Thread.run() метода, но он не имеет
			// аргументов и не возвращает значение. Вся передача объектов в мультипоточной системе(между потоками) выполняется только через поля.

			// Всегда сохраняем heapIReg, метод может использовать его в своих целях
			cgScope.prepend(cg.pushHeapReg(null));

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

			putArgsToStack(cg, cgScope, refTypeSize, newExcs);

/*				int methodSN = -1;
			if(null != symbol.getCGScope()) {
				methodSN = ((CGClassScope)symbol.getCGScope().getParent()).getMethodSN((CGMethodScope)symbol.getCGScope());
			}
*/				

			if(cis instanceof ClassScope || symbol.isStatic()) {
				cg.methodInvoke(cgScope, mScope.getStartLabel(), signature, type, symbol.isStatic());
			}
			else {
				List<AstNode> depends = cis.fillDepends(((MethodSymbol)symbol).getSignature());
				if(null != depends) {
					for(AstNode node : depends) {
						node.codeGen(cg, false, newExcs);
					}
				}

//TODO!!! Если в depends только один элемент то нужно выполнять прямой вызов так как реализация только одна!

				//Не верно int methodSN = cis.getMethodSN(symbol.getName(), ((MethodSymbol)symbol).getSignature());					
				//CIS указывает на основной CIScope, в то время как symbol может быть из CIScope через extend
				int methodSN = ((MethodSymbol)symbol).getScope().getThis().getMethodSN(symbol.getName(), ((MethodSymbol)symbol).getSignature());
				if(-1==methodSN) {
					throw new CompileException("COMPILER BUG: Got invalid method SN for " + signature);
				}
				//TODO cis.getMethodSN возвращает SN с учетом не static и не native методов - но некоторые методы могут не генерироваться, если их никто не использует
				//Что нарушает порядок номеров методов.
				cg.invokeInterfaceMethod(	cgScope, classNameStr, symbol.getName(), type, argTypes,
											VarType.fromClassName(((MethodSymbol)symbol).getScope().getThis().getName()), methodSN);
			}

			// Выходим, если исключений не ожидаем и не произвели (в том числе и unchecked)
			if(!newExcs.getRuntimeChecks().isEmpty() || !newExcs.getProduced().isEmpty()) {
				checkThrow(cg, cgScope, newExcs, signature);
			}

			if(null!=type && !type.isVoid()) {
				result = CodegenResult.RESULT_IN_ACCUM;
			}
		}

		// Если метод возвращает ссылку. Необходимо определить есть ли для нее потребитель. Если нет - сразу декремент счетчика ссылок,
		// если есть - сохраняем ссылку (аккумулятор) в стек и делаем декремент ссылки из стека после отработки родительского объекта
		//TODO: Вызываемый код метода не меняет счетчики ссылок для переменных, но должен менять для полей
		if(null!=type && type.isReferenceType()) {
			if(cgScope.getParent() instanceof CGBlockScope) {
				((CGMethodCallExpressionScope)cgScope).releaseRef(cg, cgScope, type, new CGCells(CGCells.Type.ACC));
			}
			else {
				cg.pushAccBE(cgScope, cg.getRefSize(), true);
				// getBottom - когодегенация родителя еще не завершена - необхожимо гарантировать декремент ссылки именно в конце кода родителя
				((CGMethodCallExpressionScope)cgScope).releaseRef(cg, cgScope.getParent().getBottom(), type, new CGCells(CGCells.Type.STACK, new int[]{0,1}));
			}
		}
		excs.getProduced().addAll(newExcs.getProduced());
		
		releaseVars(cg);
		return result;
	}

	private void checkThrow(CodeGenerator cg, CGScope cgScope, CGExcs excs, String signature) throws CompileException {
		List<Pair<CGLabelScope, Set<Integer>>> exceptionHandlers = new ArrayList<>();
		CGLabelScope endLbScope = null;

		CGScope cgScope_ = cgScope;
		Set<Integer> produced = new HashSet<>(excs.getProduced());
		l1:				
		while(null!=cgScope_) {
			if(cgScope_ instanceof CGTryBlockScope) {
				CGTryBlockScope tryCatchBlock = (CGTryBlockScope)cgScope_;
				if(null==endLbScope && tryCatchBlock.hasFinally()) {
					endLbScope = tryCatchBlock.getEndLabel();
				}

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
				if(null==endLbScope) {
					endLbScope = ((CGBlockScope)cgScope_).getEndLabel();
				}
				break;
			}
			cgScope_ = cgScope_.getParent();
		}

		if(!exceptionHandlers.isEmpty() || !produced.isEmpty()) {
			ExcsThrowPoint excsThrowPoint = cg.getTargetInfoBuilder().addExcsThrowPoint(cg, sp, signature);
			// Возврат должен быть или на конец текущего try-catch(ожидаем finally), или в конец метода если нет вложенных блоков try-catch			
			cg.throwCheck(cgScope, exceptionHandlers, endLbScope, excsThrowPoint);
		}
	}
	
/*	private Operand makeNativeOperand(CodeGenerator cg, ExpressionNode expr, CGExcs excs) throws CompileException {
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
*/
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
					int varSize = paramVarType.isReferenceType() ? cg.getRefSize() : paramVarType.getSize();
					CGVarScope dstVScope = cg.enterLocal(symbol.getCGScope(CGMethodScope.class), null, vSymbol.getType(), varSize, false, vSymbol.getName());
					dstVScope.build(null); // Выделяем память в стеке

					vSymbol.setCGScope(dstVScope);

					if(argExpr instanceof LiteralExpression) {
						// Выполняем зависимость
						argExpr.codeGen(cg, false, excs);
						LiteralExpression le = (LiteralExpression)argExpr;
						cg.pushConst(argExpr.getCGScope(), varSize, le.getType().isFixedPoint() ? le.getFixedValue() : le.getNumValue(), le.getType().isFixedPoint());
					}
					else if(argExpr instanceof VarFieldExpression) {
						argExpr.codeGen(cg, false, excs);
						cg.pushCells(argExpr.getCGScope(), varSize, ((CGCellsScope)argExpr.getSymbol().getCGScope()).getCells());
					}
					else if(argExpr instanceof MethodReferenceExpression) {
						argExpr.codeGen(cg, false, excs);
					}
					else if(argExpr instanceof MethodCallExpression) {
						argExpr.codeGen(cg, true, excs);
						cg.pushAccBE(argExpr.getCGScope(), varSize, false);
					}
					else if(argExpr instanceof BinaryExpression) {
						CGBranch branch = new CGBranch();
						argExpr.getCGScope().setBranch(branch);

						argExpr.codeGen(cg, true, excs);

						if(branch.isUsed()) {
							//cg.constToAcc(argExpr.getCGScope(), 1, 1, false);
							cg.cellsToCells(argExpr.getCGScope(), new CGCells(CGCells.Type.ACC), VarType.BOOLEAN, new CGCells(1), VarType.BOOLEAN, null);
							CGLabelScope lbScope = new CGLabelScope(null, null, LabelNames.LOCGIC_END, true);
							cg.jump(argExpr.getCGScope(), lbScope);
							argExpr.getCGScope().append(((CGBranch)branch).getEnd());
							//cg.constToAcc(argExpr.getCGScope(), 1, 0, false);
							cg.cellsToCells(argExpr.getCGScope(), new CGCells(CGCells.Type.ACC), VarType.BOOLEAN, new CGCells(0), VarType.BOOLEAN, null);
							argExpr.getCGScope().append(lbScope);
						}
						cg.pushAccLE(argExpr.getCGScope(), varSize);
					}
					else if(argExpr instanceof EnumExpression) {
						cg.pushConst(argExpr.getCGScope(), 0x01, ((EnumExpression)argExpr).getIndex(), false);
					}
					else {
						if(CodegenResult.RESULT_IN_ACCUM!=argExpr.codeGen(cg, true, excs)) {
							throw new CompileException("Accum not used for operand:" + argExpr);
						}
						cg.pushAccLE(argExpr.getCGScope(), varSize);
					}
				}
			}
		}
	}

	public static String buildSignature(CIScope scope, String methodName, MethodSymbol symbol, VarType[] argTypes) {
		String signature = scope.getName() + "." + methodName + "(";
		for(int i=0; i<argTypes.length; i++) {
			VarType argType = argTypes[i];
			if(symbol.isNative() && argType.isArray()) {
				signature += argType.toString();
			}
			else if(symbol.isNative() && argType.isEnum()) { // Любой Enum передается в нативный метод как константа типа Byte
				signature += VarType.BYTE.getName();
			}
			else if(symbol.isNative() && argType.isClassType()) {
				signature += VarType.CLASS.getName();
			}
			else if(argType.isEnum()) {
				signature += VarType.BYTE.getName();
			}
			else {
				signature += argType.getName();
			}
			if(i!=(argTypes.length-1)) {
				signature+=",";
			}
		}
		signature+=")";
		return signature;
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

	@Override
	public String getFullInfo() {
		return getClass().getSimpleName() + " " + toString();
	}
	
	public ExpressionNode getTargetExpr() {
		return targetExpr;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof MethodCallExpression)) return false;
		MethodCallExpression expr2 = (MethodCallExpression)obj;
		if(!Objects.equals(targetExpr, expr2.getTargetExpr())) return false;
		if(!Objects.equals(methodName, expr2.getMethodName())) return false;
		return Objects.equals(args, expr2.getArguments());
	}
}
