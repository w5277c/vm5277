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

package ru.vm5277.compiler.nodes.commands;

import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.BlockNode;
import ru.vm5277.compiler.nodes.TokenBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.cg.scopes.CGTryBlockScope;
import ru.vm5277.common.VarType;
import ru.vm5277.common.cg.scopes.CGBlockScope;
import ru.vm5277.common.enums.J8BException;
import ru.vm5277.common.enums.OptimizationType;
import ru.vm5277.common.lexer.Delimiter;
import ru.vm5277.common.lexer.J8BKeyword;
import ru.vm5277.common.lexer.TokenType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.compiler.Instance;
import ru.vm5277.compiler.nodes.CatchBlock;
import ru.vm5277.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.compiler.nodes.expressions.TypeReferenceExpression;
import ru.vm5277.compiler.semantic.BlockScope;
import ru.vm5277.compiler.semantic.ExceptionScope;
import ru.vm5277.compiler.semantic.Scope;

public class TryNode extends CommandNode {
	private	BlockNode				tryBlock;
	private	List<CatchBlock>		catchBlocks	= new ArrayList<>();
	private	BlockNode				finallyBlock;
	
	public TryNode(Instance inst, TokenBuffer tb) {
		super(inst, tb);
		
        consumeToken(tb); // Потребляем "try"
		
		// Блок try
		if(tb.match(Delimiter.LEFT_BRACE)) {
			try {
				tryBlock = new BlockNode(inst, tb, false, true, "try");

				// Парсим параметр catch (byte errCode)
				while(tb.match(J8BKeyword.CATCH)) {
					consumeToken(tb); // Потребляем "catch"
					consumeToken(tb, Delimiter.LEFT_PAREN);

					if(tb.match(TokenType.IDENTIFIER)) {
						List<ExpressionNode> args = new ArrayList<>();
						args.add(parseFullQualifiedExpression(tb, false));
						while(tb.match(Delimiter.COMMA)) {
							tb.consume();
							args.add(parseFullQualifiedExpression(tb, false));
						}

						if(tb.match(TokenType.IDENTIFIER)) {
							String varName = consumeToken(tb).getStringValue();
							consumeToken(tb, Delimiter.RIGHT_PAREN);
							
							if(tb.match(Delimiter.LEFT_BRACE)) {
								catchBlocks.add(new CatchBlock(inst, tb, args, varName));
							}
						}
						else {
							tb.skip(Delimiter.LEFT_BRACE);
						}
					}
					else {
						tb.skip(Delimiter.LEFT_BRACE);
					}
				}
				if(tb.match(J8BKeyword.FINALLY)) {
					consumeToken(tb); // Потребляем "finally"
					consumeToken(tb, Delimiter.LEFT_BRACE);
					finallyBlock = new BlockNode(inst, tb, true, false, "finally");
					tryBlock.setHasFinally();
				}
			}
			catch(CompileException ex) {
				markError(ex);
			}
		}
		else {
			markError("Expected '{' after 'try'");
		}
	}

	public List<CatchBlock> getCatchBlocks() {
		return catchBlocks;
	}

	public BlockNode getTryBlock() {
		return tryBlock;
	}

	@Override
	public boolean preAnalyze() {
		boolean result = true;

		if(null==tryBlock) {
			markError("Try block cannot be null");
			result = false;
		}

		if(catchBlocks.isEmpty() && null==finallyBlock) {
			markError("Missing catch/finally block");
			result = false;
		}

		if(result) {
			result&=tryBlock.preAnalyze();
		}
		
		if(result) {
			for(CatchBlock cBlock : catchBlocks) {
				result&=cBlock.preAnalyze();
			}
		}
		
		if(result&& null!=finallyBlock) {
			result&=finallyBlock.preAnalyze();
		}
		
		return result;
	}
	
	@Override
	public boolean declare(Scope scope) {
		boolean result = true;
		
		result&=tryBlock.declare(scope);

		if(result) {
			for(CatchBlock cBlock : catchBlocks) {
				result&=cBlock.declare(scope);
			}
		}
		
		if(result&& null!=finallyBlock) {
			result&=finallyBlock.declare(scope);
		}
		
		return result;
	}
	
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg, CGScope parent) {
		boolean result = true;
		cgScope = cg.enterCommand(parent, sp, "try");
		
		CGScope tryScope = new CGBlockScope(cg, parent, sp, CGScope.genId(), "try");

		// Сначала выполняет обработку catch блоков, так как минимум нам нужно заполнить в BlockScope handlingExcsScopes
		if(result && !catchBlocks.isEmpty()) {
			CGScope catchScope = new CGBlockScope(cg, parent, sp, CGScope.genId(), "catch");
			for(CatchBlock cBlock : catchBlocks) {
				result&=cBlock.postAnalyze(cBlock.getScope(), cg, catchScope, tryBlock);
			}
		}

		if(result) {
			result&=tryBlock.postAnalyze(tryBlock.getScope(), cg, tryScope);
		}

		if(result && null!=finallyBlock) {
			CGScope finallyScope = new CGBlockScope(cg, parent, sp, CGScope.genId(), "finally");
			result&=finallyBlock.postAnalyze(finallyBlock.getScope(), cg, finallyScope);
		}
		
		if(result) {
			BlockScope tryBlockScope = tryBlock.getScope();
			int runtimeExceptionId = VarType.getExceptionId(J8BException.RuntimeException.name());
			for(CatchBlock cBlock : catchBlocks) {
				Set<Integer> ids = new HashSet<>();
				
				for(ExpressionNode expr : cBlock.getArgs()) {
					ExceptionScope eScope = (ExceptionScope)((TypeReferenceExpression)expr).getScope();
					ids.add(VarType.getExceptionId(eScope.getName()));
				}
				((CGTryBlockScope)tryBlock.getCGScope()).addCatchExceptions(ids);
			}
		}
		
		return result;
	}
	
	@Override
	public void codeOptimization(Scope scope, CodeGenerator cg) {
		tryBlock.codeOptimization(scope, cg);
		
		for(CatchBlock cBlock : catchBlocks) {
			cBlock.codeOptimization(cBlock.getScope(), cg);
		}
		
		if(null!=finallyBlock) {
			finallyBlock.codeOptimization(scope, cg);
		}
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, boolean toAccum, CGExcs excs) throws CompileException {
		if(cgDone || disabled) return null;
		cgDone = true;

		//CGScope cgs = null == parent ? cgScope : parent;

		CGExcs newExcs = new CGExcs();
		CGTryBlockScope tbs = ((CGTryBlockScope)tryBlock.getCGScope());
		for(CGLabelScope lbScope : tbs.getCatchLabels()) {
			for(int exceptionId : tbs.getExceptionIds(lbScope)) {
				newExcs.getRuntimeChecks().put(exceptionId, lbScope);
			}
		}
		newExcs.getRuntimeChecks().putAll(excs.getRuntimeChecks());

		newExcs.setMethodEndLabel(tbs.getEndLabel());
		tryBlock.codeGen(cg, false, newExcs);

		for(CatchBlock cBlock : catchBlocks) {
			if(OptimizationType.NONE==inst.getOptimizationType()) {
				cBlock.codeGen(cg, cgScope, false, excs, true);
				l1:
				for(ExceptionScope eScope : cBlock.getExceptionScopes()) {
					if(newExcs.getProduced().contains(eScope.getId())) {
						newExcs.getProduced().remove(eScope.getId());
						break l1;
					}
					for(Integer exId : VarType.getExceptionDescendant(eScope.getId())) {
						if(newExcs.getProduced().contains(exId)) {
							newExcs.getProduced().remove(eScope.getId());
							break l1;
						}
					}
				}
			}
			else {
				boolean isUsed = false;
				if(!newExcs.getProduced().isEmpty()) {
					l1:
					for(ExceptionScope eScope : cBlock.getExceptionScopes()) {
						if(newExcs.getProduced().contains(eScope.getId())) {
							newExcs.getProduced().remove(eScope.getId());
							isUsed = true;
							break l1;
						}
						for(Integer exId : VarType.getExceptionDescendant(eScope.getId())) {
							if(newExcs.getProduced().contains(exId)) {
								newExcs.getProduced().remove(eScope.getId());
								isUsed = true;
								break l1;
							}
						}
					}
				}
				cBlock.codeGen(cg, cgScope, false, excs, isUsed);
			}
		}
		
		if(null!=finallyBlock) {
			finallyBlock.codeGen(cg, false, newExcs);
		}
		
		excs.getProduced().addAll(newExcs.getProduced());
		
		releaseVars(cg);
		return null;
	}
	
	@Override
	public List<AstNode> getChildren() {
		List<AstNode> result = new ArrayList<>();
		result.add(tryBlock);
		result.addAll(catchBlocks);
		if(null!=finallyBlock) result.add(finallyBlock);
		return result;
	}
}
