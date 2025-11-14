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

import java.util.List;
import static ru.vm5277.common.SemanticAnalyzePhase.DECLARE;
import static ru.vm5277.common.SemanticAnalyzePhase.POST;
import static ru.vm5277.common.SemanticAnalyzePhase.PRE;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.common.StrUtils;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.scopes.CGMethodScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.cg.scopes.CGVarScope;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import static ru.vm5277.compiler.Main.debugAST;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.nodes.expressions.bin.BinaryExpression;
import ru.vm5277.compiler.semantic.AstHolder;
import ru.vm5277.compiler.semantic.CIScope;
import ru.vm5277.compiler.semantic.ClassScope;
import ru.vm5277.compiler.semantic.InterfaceScope;
import ru.vm5277.compiler.semantic.MethodSymbol;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.semantic.Symbol;

public class NewExpression extends ExpressionNode {
	private	List<String>			path;
	private	List<ExpressionNode>	args;
	private	VarType[]				argTypes;
	private	CIScope					cis;
	private	String					methodName;
	
	public NewExpression(TokenBuffer tb, MessageContainer mc, SourcePosition sp, List<String> path) throws CompileException {
		super(tb, mc, sp);
		
		this.path = path;
		this.args = parseArguments(tb);
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
			if(1<path.size()) {
				cis = scope.getThis();
				for(int i=0; i<path.size()-1; i++) {
					cis = cis.resolveCI(path.get(i), i!=0);
					if(null==cis || cis instanceof InterfaceScope) {
						markError("Unresolved class scope '" + path.get(i) + "' in '" + toString() + "'");
						result = false;
					}
				}
			}
			methodName = path.get(path.size()-1);
			
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
					}
					argTypes[i] = arg.getType();
				}
			}
			
			if(result) {
				if(null==cis) {
					symbol = ((ClassScope)scope.getThis()).resolveConstructor(methodName, argTypes, false);
				}
				else {
					symbol = ((ClassScope)cis).resolveConstructor(methodName, argTypes, true);
				}
				if(null==symbol) {
					markError("Method '" + methodName + "' not found");
					result = false;
				}
			}
			
			if(result) {
				type = VarType.fromClassName(symbol.getName());
				symbol.setCGScope(cgScope);
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

		CIScope cis = (CIScope)((MethodSymbol)symbol).getScope().getParent();
		String classNameStr = cis.getName();

		int refTypeSize = 1; // TODO Определить значение на базе количества используемых типов класса
		// Всегда сохраняем heapIReg, метод может использовать его в своих целях
		cg.pushHeapReg(cgs);

		putArgsToStack(cg, cgs, refTypeSize);
		depCodeGen(cg);

		CGMethodScope mScope = (CGMethodScope)((AstHolder)symbol).getNode().getCGScope().getScope(CGMethodScope.class);
		cg.call(cgs, mScope.getLabel());

		return null;
	}

	private void putArgsToStack(CodeGenerator cg, CGScope cgs, int refTypeSize) throws CompileException {
		if(!args.isEmpty()) {

			CGMethodScope mScope = (CGMethodScope)symbol.getCGScope(CGMethodScope.class);
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

	@Override
	public List<AstNode> getChildren() {
		return null;
	}

	@Override
	public String toString() {
		return "new " + StrUtils.toString(path) + "(" + StrUtils.toString(argTypes) + ")";
	}
	
	public String getFullInfo() {
		return getClass().getSimpleName() + " " + toString();
	}
}
