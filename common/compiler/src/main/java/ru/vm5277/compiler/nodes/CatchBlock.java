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

package ru.vm5277.compiler.nodes;

import java.util.List;
import java.util.Set;
import ru.vm5277.common.LabelNames;
import ru.vm5277.common.cg.CGCells;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGBlockScope;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.cg.scopes.CGTryBlockScope;
import ru.vm5277.common.cg.scopes.CGVarScope;
import ru.vm5277.common.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.compiler.nodes.expressions.TypeReferenceExpression;
import ru.vm5277.compiler.semantic.ExceptionScope;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.semantic.VarSymbol;

public class CatchBlock extends BlockNode {
	private	List<ExpressionNode>	args;
	private	String					varName;
	private	BlockNode				blockNode;
	private	BlockNode				tryBlockNode;
	private	ExceptionScope			eScope;
	
	public CatchBlock(TokenBuffer tb, MessageContainer mc, List<ExpressionNode> args, String varName) throws CompileException {
		super(tb, mc);
		
		this.args = args;
		this.varName = varName;
		
	}

	public List<ExpressionNode> getArgs() {
		return args;
	}

	public String getVarName() {
		return varName;
	}

	@Override
	public boolean preAnalyze() {
		boolean result = super.preAnalyze();
		
		if(result) {
			if(args.isEmpty()) {
				markError("Expected exception name or comma-separated names");
				result = false;
			}

			try {
				validateName(varName);
			}
			catch(CompileException ex) {
				markError(ex);
				result = false;
			}
			
			for(ExpressionNode expr : args) {
				result&=expr.preAnalyze();
			}
		}
	
		return result;
	}

	@Override
	public boolean declare(Scope scope) {
		boolean result = super.declare(scope);
		
		if(result) {
			for(ExpressionNode expr : args) {
				result&=expr.declare(scope);
			}
		}
		
		if(result) {
			symbol = new VarSymbol(varName, VarType.EXCEPTION, true, false, blockScope, this);
			try {
				blockScope.addVariable(symbol);
			}
			catch(CompileException e) {
				markError(e);
			}
		}
		
		return result;
	}
	
	public boolean postAnalyze(Scope scope, CodeGenerator cg, BlockNode tryBlockNode) {
		boolean result = super.postAnalyze(scope, cg);

		try {
			this.tryBlockNode = tryBlockNode;
			
			if(result) {
				for(int i=0; i<args.size(); i++) {
					ExpressionNode expr = args.get(i);

					result&=expr.postAnalyze(scope, cg);
					if(result) {
						ExpressionNode optimizedExpr = expr.optimizeWithScope(scope, cg);
						if(null!=optimizedExpr) {
							expr = optimizedExpr;
							args.set(i, optimizedExpr);
						}
					}

					if(!(expr instanceof TypeReferenceExpression && VarType.EXCEPTION==expr.getType()))  {
						markError("Catch parameter must be an exception type, got:" + expr);
						result = false;
						break;
					}
					else {
						eScope = (ExceptionScope)((TypeReferenceExpression)expr).getScope();
						tryBlockNode.getScope().addExceptionScope(eScope);
					}
				}
			}
		}
		catch(CompileException ex) {
			markError(ex);
			result = false;
		}

		return result;
	}

	public ExceptionScope getExceptionScope() {
		return eScope;
	}
	
	@Override
	public void codeOptimization(Scope scope, CodeGenerator cg) {
		super.codeOptimization(scope, cg);
		
		CGScope oldScope = cg.setScope(cgScope);

		try {
			for(int i=0; i<args.size(); i++) {
				ExpressionNode optimizedExpr = args.get(i).optimizeWithScope(scope, cg);
				if(null != optimizedExpr) {
					args.set(i, optimizedExpr);
				}
			}
		}
		catch(CompileException ex) {
			markError(ex);
		}
		
		cg.setScope(oldScope);
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum, CGExcs excs) throws CompileException {
		if(cgDone || disabled) return null;
		cgDone = true;
		
		CGScope cgs = null == parent ? cgScope : parent;
		
		CGLabelScope lbCatchEndScope = new CGLabelScope(null, CodeGenerator.genId(), LabelNames.CATCH_SKIP, true);
		cg.jump(cgs, lbCatchEndScope);
		for(int i=0; i<args.size(); i++) {
			ExpressionNode expr = args.get(i);
			String path = ((TypeReferenceExpression)expr).getQualifiedPath();
			cgs.append(((CGTryBlockScope)tryBlockNode.getCGScope()).getCatchLabel(VarType.getExceptionId(path)));
		}
		
		
		CGVarScope vScope = new CGVarScope(cgs, CodeGenerator.genId(), VarType.SHORT, 0x02, true, varName);
		vScope.setCells(new CGCells(CGCells.Type.ACC));
		((VarSymbol)symbol).setCGScope(vScope);
		
		for(AstNode node : children) {
			//Не генерирую безусловно переменные, они будут сгенерированы только при обращении
			if(!(node instanceof VarNode)) {
				node.codeGen(cg, cgs, false, excs);
			}
		}
		
		cgs.append(lbCatchEndScope);
		
		((CGBlockScope)cgScope).build(cg, false, excs);
		((CGBlockScope)cgScope).restoreRegsPool();
		
		
		
		return null;
	}
}
