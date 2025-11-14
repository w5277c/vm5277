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

import java.util.Arrays;
import java.util.List;
import ru.vm5277.common.RTOSFeature;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.Operand;
import ru.vm5277.common.cg.OperandType;
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
import ru.vm5277.compiler.semantic.MethodSymbol;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.semantic.Symbol;
import static ru.vm5277.compiler.Main.debugAST;
import static ru.vm5277.common.SemanticAnalyzePhase.DECLARE;
import static ru.vm5277.common.SemanticAnalyzePhase.PRE;
import static ru.vm5277.common.SemanticAnalyzePhase.POST;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.common.StrUtils;
import ru.vm5277.common.SystemParam;
import ru.vm5277.compiler.nodes.expressions.bin.BinaryExpression;
import ru.vm5277.compiler.semantic.CIScope;
import ru.vm5277.compiler.semantic.ClassScope;
import ru.vm5277.compiler.semantic.MethodScope;

public class MethodCallExpression extends ExpressionNode {
	private			ExpressionNode			targetExpr;
	private			String					methodName;
	private	final	List<ExpressionNode>	args;
	private			VarType[]				argTypes;
	private			CIScope					cis;
	private			boolean					isThisMethodCall;
	
	public MethodCallExpression(TokenBuffer tb, MessageContainer mc, SourcePosition sp, ExpressionNode parentExpr, String methodName,
								List<ExpressionNode> args) {
		super(tb, mc, sp);

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
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		boolean result = true;
		debugAST(this, POST, true, getFullInfo());
		cgScope = cg.enterExpression(toString());

		try {
/*			if(null!=pathExpr) {
				result&=pathExpr.postAnalyze(scope, cg);

				if(!(pathExpr instanceof TypeReferenceExpression)) {
					markError("TODO Invalid method call expression:" + pathExpr);
					result = false;
				}
				ExpressionNode optimizedParentScope = pathExpr.optimizeWithScope(scope, cg);
				if(null!=optimizedParentScope) {
					pathExpr = optimizedParentScope;
					result&=pathExpr.postAnalyze(scope, cg);
				}
			}
*/

			argTypes = new VarType[args.size()];
			for(int i=0; i<args.size(); i++) {
				ExpressionNode arg = args.get(i);
				result&=arg.postAnalyze(scope, cg);
				if(result) {
					ExpressionNode optimizedExpr = arg.optimizeWithScope(scope, cg);
					if(null!=optimizedExpr) {
						args.set(i, optimizedExpr);
						arg = optimizedExpr;
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
				}
				else if(targetExpr instanceof VarFieldExpression) {
					VarType varType = ((VarFieldExpression)targetExpr).getSymbol().getType();
					if(null!=varType) {
						cis = scope.getThis().resolveCI(varType.getClassName(), false);
						if(null!=cis) {
							symbol = cis.resolveMethod(methodName, argTypes, true);
						}
					}
				}
				if(null==symbol) {
					markError(	"Method " + (null==type ? VarType.VOID : type) + " " + (null==targetExpr ? "" : targetExpr + ".") + methodName +
								"(" + StrUtils.toString(argTypes) + ") not found");
					result = false;
				}
			}
			
			if(result) {
				isThisMethodCall = (scope.getThis()==cis);
				type = symbol.getType();
				symbol.setCGScope(cgScope);
				
				((MethodSymbol)symbol).markUsed();
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
		}
		catch (CompileException e) {
			markError(e.getMessage());
			result = false;
		}

		cg.leaveExpression();
		debugAST(this, POST, false, result, getFullInfo());
		return result;
	}
	
	@Override
	public void codeOptimization(Scope scope, CodeGenerator cg) {
		CGScope oldScope = cg.setScope(cgScope);
		
		for(int i=0; i<args.size(); i++) {
			ExpressionNode arg = args.get(i);
			arg.codeOptimization(scope, cg);
			try {
				ExpressionNode optimizedExpr = arg.optimizeWithScope(scope, cg);
				if(null != optimizedExpr) {
					arg = optimizedExpr;
					args.set(i, arg);
				}
			}
			catch(CompileException ex) {
				markError(ex);
			}
		}
		
		cg.setScope(oldScope);
	}

	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum) throws CompileException {
		CGScope cgs = null == parent ? cgScope : parent;

		if(null!=targetExpr) {
			targetExpr.codeGen(cg, cgs, false);
		}

		String classNameStr = cis.getName();

		String signature = cis.getName() + "." + methodName + " " + Arrays.toString(argTypes);

		if(signature.equals("System.setParam [byte, byte]")) {
			if(	0x02!=args.size() ||
				!(args.get(0) instanceof LiteralExpression) || !(args.get(1) instanceof LiteralExpression) ||
				VarType.BYTE!=argTypes[0] || VarType.BYTE!=argTypes[1]) {
			
				throw new CompileException("System.setParam arguments must be constant byte expressions for RTOS configuration");
			}
			
			int paramId = (int)((LiteralExpression)args.get(0)).getNumValue();
			int valueId = (int)((LiteralExpression)args.get(1)).getNumValue();
			SystemParam sp = SystemParam.values()[paramId];
			switch(sp) {
				case CORE_FREQ:
				case STDOUT_PORT:
					cg.setParam(sp, valueId);
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
					else {
						cg.resetFeature(RTOSFeature.OS_FT_WELCOME);
					}
					break;
			}
		}
		else if(symbol.isNative()) {
			cg.nativeMethodInit(cgs, signature);
			for(int i=0; i<args.size(); i++) {
				ExpressionNode argExpr = args.get(i);
				cg.accResize(argTypes[i]);
				if(CodegenResult.RESULT_IN_ACCUM!=argExpr.codeGen(cg, cgs, true)) {
					throw new CompileException("Accum not used for argument:" + argExpr);
				}
				cg.nativeMethodSetArg(cgs, signature, i);
			}
			cg.nativeMethodInvoke(cgs, signature);
		}
		else {
		//	CGLabelScope rpCGScope = null;
			if(0 == argTypes.length && "getClassId".equals(symbol.getName())) {
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
				cg.constToAcc(cgs, cg.getRefSize(), type.getId(), false);
			}
			else {
				int refTypeSize = 1; // TODO Определить значение на базе количества используемых типов класса
			
				depCodeGen(cg);

				CGMethodScope mScope = (CGMethodScope)((AstHolder)symbol).getNode().getCGScope().getScope(CGMethodScope.class);

				// Не нужно беспокоиться о счетчике ссылок для аргументов, это могло бы потребоваться только для Thread.run() метода, но он не имеет
				// аргументов и не возвращает значение. Вся передача объектов в мультипоточной системе(между потоками) выполняется только через поля.

				// Всегда сохраняем heapIReg, метод может использовать его в своих целях
				cg.pushHeapReg(cgs);

				// Если это метод не нашего класса, то нужно указать адрес heap класса вызываемого метода
				if(!isThisMethodCall) {
					if(targetExpr instanceof TypeReferenceExpression) {
					}
					else if(targetExpr instanceof VarFieldExpression) {
						if(!symbol.isStatic()) {
							CGCellsScope cScope = (CGCellsScope)((VarFieldExpression)targetExpr).getSymbol().getCGScope();
							cg.setHeapReg(cgs, cScope.getCells());
						}
						else {
							//TODO cg.setHeapReg(...
						}
					}
					else {
						throw new CompileException("Unsupported target expression:" + targetExpr);
					}
				}

				putArgsToStack(cg, cgs, refTypeSize);

/*				int methodSN = -1;
				if(null != symbol.getCGScope()) {
					methodSN = ((CGClassScope)symbol.getCGScope().getParent()).getMethodSN((CGMethodScope)symbol.getCGScope());
				}
*/				

				if(cis instanceof ClassScope) {
					//TODO добавить информирование
					cg.call(cgs, mScope.getLabel());
				}
				else {
					List<AstNode> depends = cis.fillDepends(((MethodSymbol)symbol).getSignature());
					if(null != depends) {
						for(AstNode node : depends) {
							node.codeGen(cg, null, false);
						}
					}
					int methodSN = cis.getMethodSN(symbol.getName(), ((MethodSymbol)symbol).getSignature());
					cg.invokeInterfaceMethod(cgs, classNameStr, symbol.getName(), type, argTypes, VarType.fromClassName(cis.getName()), methodSN);
				}


				if(!type.isVoid()) {
					cg.accCast(null, type);
					return CodegenResult.RESULT_IN_ACCUM;
				}
			}
		}

		return null;
	}

	private Operand makeNativeOperand(CodeGenerator cg, ExpressionNode expr) throws CompileException {
		if(expr instanceof VarFieldExpression) {
			VarFieldExpression ve = (VarFieldExpression)expr;
			// Кодогенерация VarFieldExpression выполняет запись значения в аккумулятор, но нам этого не нужно, выполняем только зависимость
			ve.depCodeGen(cg);
			return new Operand(VarType.CSTR == ve.getType() ? OperandType.FLASH_RES : OperandType.LOCAL_RES, ve.getSymbol().getCGScope().getResId());
		}
		else if(expr instanceof LiteralExpression) {
			LiteralExpression le = (LiteralExpression)expr;
			if(VarType.CSTR == le.getType()) {
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
		else if(expr instanceof PropertyExpression) {
			PropertyExpression ape = (PropertyExpression)expr;
			ape.codeGen(cg, null, true);
			return new Operand(OperandType.ACCUM, null);
		}
		else if(expr instanceof EnumExpression) {
			return new Operand(OperandType.LITERAL, ((EnumExpression)expr).getIndex());
		}
		else throw new CompileException("Unexpected expression:" + expr);
	}

	private void putArgsToStack(CodeGenerator cg, CGScope cgs, int refTypeSize) throws CompileException {
		if(!args.isEmpty()) {
//			CGScope oldCGScope = cg.setScope(symbol.getCGScope());


			CGMethodScope mScope = (CGMethodScope)symbol.getCGScope(CGMethodScope.class);
			
			//	Рудимент?
			mScope.clearArgs(); //скорее всего не зайдет, нам нужно удалять только те, которые мы даобавили
			

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
						argExpr.codeGen(cg, cgs, false);
						LiteralExpression le = (LiteralExpression)argExpr;
						cg.pushConst(cgs, exprTypeSize, le.isFixed() ? le.getFixedValue() : le.getNumValue(), le.isFixed());
					}
					else if(argExpr instanceof VarFieldExpression) {
						argExpr.codeGen(cg, cgs, false);
						cg.pushCells(cgs, exprTypeSize, ((CGCellsScope)argExpr.getSymbol().getCGScope()).getCells());
					}
					else if(argExpr instanceof MethodCallExpression) {
						argExpr.codeGen(cg, cgs, true);
						cg.pushAccBE(cgs, paramVarType.getSize());
					}
					else if(argExpr instanceof BinaryExpression) {
						argExpr.codeGen(cg, cgs, true);
						cg.pushAccBE(cgs, paramVarType.getSize());
					}
					else if(argExpr instanceof EnumExpression) {
						cg.pushConst(cgs, 0x01, ((EnumExpression)argExpr).getIndex(), false);
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
	public String toString() {
		return (null==type ? VarType.VOID : type) + " " + (null==targetExpr ? "" : targetExpr + ".") + methodName + "(" + StrUtils.toString(args) + ")";
	}

	public String getFullInfo() {
		return getClass().getSimpleName() + " " + toString();
	}
}
