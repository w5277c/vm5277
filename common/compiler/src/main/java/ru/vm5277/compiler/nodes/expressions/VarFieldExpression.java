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

import java.util.Objects;
import ru.vm5277.common.lexer.Operator;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.CGBranch;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.compiler.CodegenResult;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import static ru.vm5277.compiler.Main.debugAST;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.semantic.VarSymbol;
import static ru.vm5277.common.SemanticAnalyzePhase.DECLARE;
import static ru.vm5277.common.SemanticAnalyzePhase.PRE;
import static ru.vm5277.common.SemanticAnalyzePhase.POST;
import ru.vm5277.common.lexer.SourcePosition;
import ru.vm5277.common.cg.CGCells;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.scopes.CGFieldScope;
import ru.vm5277.compiler.semantic.AstHolder;
import ru.vm5277.compiler.semantic.CIScope;
import ru.vm5277.compiler.semantic.FieldSymbol;
import ru.vm5277.compiler.semantic.MethodScope;

public class VarFieldExpression extends ExpressionNode {
    private	final	ExpressionNode	targetExpr;
	private final	String			name;
	private			CIScope			targetScope;
	private			boolean			isInliningMode;
	
    public VarFieldExpression(TokenBuffer tb, MessageContainer mc, SourcePosition sp, Integer sn, ExpressionNode pathExpr, String name) {
		this(tb, mc, sp, sn, pathExpr, name, false);
	}
	public VarFieldExpression(	TokenBuffer tb, MessageContainer mc, SourcePosition sp, Integer sn, ExpressionNode pathExpr, String name,
								boolean isInliningMode) {
        super(tb, mc, sp);
         
		if(null!=sn) {
			this.sn=sn;
		}
		this.targetExpr = pathExpr;
		this.name = name;
		this.isInliningMode = isInliningMode;
    }
    
	public ExpressionNode getTargetExpr() {
		return targetExpr;
	}
	
	public CIScope getTargetScope() {
		return targetScope;
	}
	
	public boolean isSameVarFiled(VarFieldExpression vfe) {
		if(!Objects.equals(name, vfe.getName())) {
			return false;
		}
		if(symbol.getClass()!=vfe.getSymbol().getClass()) {
			return  false;
		}
		return Objects.equals(targetScope, vfe.getTargetScope());
	}

	public String getName() {
		return name;
	}
	
	@Override
	public boolean preAnalyze() {
		boolean result = true;
		debugAST(this, PRE, true, getFullInfo());
		
		debugAST(this, PRE, false, result, getFullInfo());
		return result;
	}
	
	@Override
	public boolean declare(Scope scope) {
		boolean result = true;
		debugAST(this, DECLARE, true, getFullInfo());
		
		try {
			if(null==targetExpr) {
				symbol = scope.resolveVar(name);
				if(null==symbol) {
					symbol = scope.resolveField(name, false);
				}
				targetScope = scope.getThis();
				if(null!=symbol) type = symbol.getType();
			}
			else {
				result&=targetExpr.declare(scope);
			}
		}
		catch(CompileException ex) {
			markError(ex);
			result = false;
		}

		debugAST(this, DECLARE, false, result, getFullInfo() + (declarationPendingNodes.containsKey(this) ? " [DP]" : ""));
		return result;
	}

	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		if(null!=postResult) return postResult;
		postResult = true;

		debugAST(this, POST, true, getFullInfo() + " type:" + type);
		cgScope = cg.enterExpression(toString());

		if(null==symbol && null!=targetExpr) {
			try {
				if(targetExpr instanceof ThisExpression) {
					targetScope = scope.getThis();
				}
				else if(targetExpr instanceof VarFieldExpression) {
					VarFieldExpression vfe = (VarFieldExpression)targetExpr;
					CIScope parentTargetScope = vfe.getTargetScope();
					targetScope = parentTargetScope.resolveCI(vfe.getSymbol().getType().getClassName(), true);
				}
				else {
					targetScope = ((TypeReferenceExpression)targetExpr).getScope();
				}

				//TODO реализовать в Scope'ах проверки доступов к классам и полям импортируемых файлов
				
				if(null!=targetScope) {
					symbol = targetScope.resolveField(name, true);
					if(null!=symbol) {
						type = symbol.getType();
					}
				}
			}
			catch(CompileException ex) {
				markError(ex);
				postResult = false;
			}
		}
		
		if(postResult && null!=symbol && symbol instanceof VarSymbol) {
			if(getSN()<((AstHolder)symbol).getNode().getSN()) {
				markError("Var '" + symbol.getName() + "' cannot be used before declaration");
				postResult = false;
			}
		}
		
		if(postResult && null!=symbol && symbol instanceof FieldSymbol) {
			FieldSymbol fSymbol = (FieldSymbol)symbol;
			MethodScope mScope = scope.getMethod();
			if(!isInliningMode && mScope.getSymbol().isStatic() && !fSymbol.isStatic()) {
				markError("Non-static field '" + fSymbol.getName() + "' cannot be referenced from a static context");
				postResult = false;
			}

			// Проверка приватного доступа
			if(fSymbol.isPrivate() && scope.getThis() == targetScope.getThis()) {
				
			}
		}

		cg.leaveExpression();
		debugAST(this, POST, false, postResult, getFullInfo());
		return postResult;
	}
	
	@Override
	public void codeOptimization(Scope scope, CodeGenerator cg) {
		CGScope oldScope = cg.setScope(cgScope);
		
		if(!symbol.isReassigned()) {
			symbol.setFinal(true);
		}
		
		cg.setScope(oldScope);
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum, CGExcs excs) throws CompileException {
		if(disabled) return null;
		CGScope cgs = null == parent ? cgScope : parent;
		
		// Актуализируем symbol
		//getSymbol();
		
		depCodeGen(cg, excs);
/*			if(symbol instanceof AliasSymbol) {
				Symbol vfSymbol = scope.resolveVFSymbol(value);
				while(vfSymbol instanceof AliasSymbol) {
					symbol = ((AliasSymbol)vfSymbol).getSymbol();
					vfSymbol = scope.resolveVFSymbol(symbol.getName());
					if(null != vfSymbol && null != vfSymbol.getCGScope()) {
						depCodeGen(cg);
		if(toAccum) {
							CGVarScope vScope = (CGVarScope)vfSymbol.getCGScope();
							cg.cellsToAcc(cgs, vScope);
							//Назначаем алиас ссылающийся на реальную переменную
						}
						symbol = vfSymbol;
						break;
					}
				}
			}
			else {*/
//				CGCellsScope cScope = (CGCellsScope)symbol.getCGScope(CGCellsScope.class);
//				if(null!=cScope) {
					
					if(null!=targetExpr) {
						targetExpr.codeGen(cg, parent, false, excs);
					}
					if(null!=targetExpr && targetExpr instanceof VarFieldExpression) { //Не смотрим на isInliningMode
						cg.cellsToArrReg(cgs, ((CGCellsScope)((VarFieldExpression)targetExpr).getSymbol().getCGScope()).getCells());
					}

					if(null!=targetExpr && isInliningMode) {
						// Режим AST инлайнинга
						CGFieldScope cgfs = (CGFieldScope)symbol.getCGScope();
						if(!cgfs.isStatic()) {
							CGCells cgcs = new CGCells(CGCells.Type.HEAP_ALT, cgfs.getCells().getIds());
							cgfs.setCells(cgcs);
						}
					}
					if(toAccum) {
						cg.cellsToAcc(cgs, (CGCellsScope)symbol.getCGScope());
					}
//				}
//				else {
//					throw new CompileException("Unsupported scope: " + symbol.getCGScope());
//				}
//		}

		return (toAccum ? CodegenResult.RESULT_IN_ACCUM : null);
	}
	
	public void codeGen(CodeGenerator cg, CGScope parent, boolean isInvert, boolean opOr, CGBranch brScope, CGExcs excs) throws CompileException {
		depCodeGen(cg, excs);
		
		CGScope cgs = null == parent ? cgScope : parent;
		
		if(null!=brScope) {
			CGCellsScope cScope = (CGCellsScope)symbol.getCGScope();
			cg.constCond(cgs, cScope.getCells(), Operator.NEQ, 0, symbol.getType().isFixedPoint(), false, isInvert, opOr, brScope); // NEQ, так как проверяем на 0
		}
	}

	public void postCodeGen(CodeGenerator cg, CGScope parent) throws CompileException {
		//TODO рудимент
		if(!disabled) {
			CGScope cgs = null == parent ? cgScope : parent;

			if(null!=targetExpr && targetExpr instanceof VarFieldExpression) { //Не смотрим на isInliningMode
				if(!isInliningMode) {
					cg.popHeapReg(cgs);
				}
			}
		}
	}
	
	@Override
	public String getQualifiedPath() {
		return (null!=targetExpr ? targetExpr.getQualifiedPath() + ".": "" ) + name;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	public String getFullInfo() {
		return getClass().getSimpleName() + ": " + name;
	}
}