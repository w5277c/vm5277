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
import ru.vm5277.compiler.Delimiter;
import ru.vm5277.compiler.Keyword;
import ru.vm5277.compiler.TokenType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.CatchBlock;
import ru.vm5277.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.compiler.nodes.expressions.TypeReferenceExpression;
import ru.vm5277.compiler.semantic.ExceptionScope;
import ru.vm5277.compiler.semantic.Scope;

public class TryNode extends CommandNode {
	private	BlockNode				tryBlock;
	public	List<CatchBlock>		catchBlocks	= new ArrayList<>();
	
	public TryNode(TokenBuffer tb, MessageContainer mc) {
		super(tb, mc);
		
        consumeToken(tb); // Потребляем "try"
		
		// Блок try
		if(tb.match(Delimiter.LEFT_BRACE)) {
			try {
				tryBlock = new BlockNode(tb, mc, false, true);

				// Парсим параметр catch (byte errCode)
				while(tb.match(Keyword.CATCH)) {
					consumeToken(tb); // Потребляем "catch"
					consumeToken(tb, Delimiter.LEFT_PAREN);

					if(tb.match(TokenType.ID)) {
						List<ExpressionNode> args = new ArrayList<>();
						args.add(parseFullQualifiedExpression(tb));
						while(tb.match(Delimiter.COMMA)) {
							tb.consume();
							args.add(parseFullQualifiedExpression(tb));
						}

						if(tb.match(TokenType.ID)) {
							String varName = consumeToken(tb).getStringValue();
							consumeToken(tb, Delimiter.RIGHT_PAREN);
							
							if(tb.match(Delimiter.LEFT_BRACE)) {
								catchBlocks.add(new CatchBlock(tb, mc, args, varName));
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

		if(catchBlocks.isEmpty()) {
			markError("Missing catch block");
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
		
		return result;
	}
	
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		boolean result = true;
		cgScope = cg.enterCommand();
		

		if(result) {
			for(CatchBlock cBlock : catchBlocks) {
				result&=cBlock.postAnalyze(cBlock.getScope(), cg, tryBlock);
			}
		}

		if(result) {
			result&=tryBlock.postAnalyze(tryBlock.getScope(), cg);
		}

		if(result) {
			for(CatchBlock cBlock : catchBlocks) {
				Set<Integer> ids = new HashSet<>();
				
				for(ExpressionNode expr : cBlock.getArgs()) {
					ExceptionScope eScope = (ExceptionScope)((TypeReferenceExpression)expr).getScope();
					ids.add(VarType.getExceptionId(eScope.getName()));
				}
				((CGTryBlockScope)tryBlock.getCGScope()).addCatchExceptions(ids);
			}
		}

		
		cg.leaveCommand();
		return result;
	}
	
	@Override
	public void codeOptimization(Scope scope, CodeGenerator cg) {
		CGScope oldScope = cg.setScope(cgScope);
		
		tryBlock.codeOptimization(scope, cg);
		
		for(CatchBlock cBlock : catchBlocks) {
			cBlock.codeOptimization(cBlock.getScope(), cg);
		}

		cg.setScope(oldScope);
	}

	
	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum, CGExcs excs) throws CompileException {
		if(cgDone) return null;
		cgDone = true;

		CGScope cgs = null == parent ? cgScope : parent;

		CGExcs newExcs = new CGExcs();
		CGTryBlockScope tbs = ((CGTryBlockScope)tryBlock.getCGScope());
		for(CGLabelScope lbScope : tbs.getCatchLabels()) {
			for(int exceptionId : tbs.getExceptionIds(lbScope)) {
				newExcs.getRuntimeChecks().put(exceptionId, lbScope);
			}
		}
		newExcs.getRuntimeChecks().putAll(excs.getRuntimeChecks());

		tryBlock.codeGen(cg, cgs, false, newExcs);
		
		for(CatchBlock cBlock : catchBlocks) {
			cBlock.codeGen(cg, cgs, false, excs);
		}
		
		excs.getProduced().addAll(newExcs.getProduced());
		
		return null;
	}
	
	@Override
	public List<AstNode> getChildren() {
		List<AstNode> result = new ArrayList<>();
		result.add(tryBlock);
		result.addAll(catchBlocks);
		return result;
	}
}
