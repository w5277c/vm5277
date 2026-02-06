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

import ru.vm5277.compiler.nodes.BlockNode;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.common.lexer.Delimiter;
import ru.vm5277.common.lexer.J8BKeyword;
import java.util.ArrayList;
import java.util.List;
import ru.vm5277.common.LabelNames;
import ru.vm5277.common.lexer.Operator;
import ru.vm5277.common.cg.CGBranch;
import ru.vm5277.common.cg.CGCells;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.compiler.CodegenResult;
import ru.vm5277.common.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.expressions.LiteralExpression;
import ru.vm5277.compiler.semantic.BlockScope;
import ru.vm5277.compiler.semantic.Scope;

public class SwitchNode extends CommandNode {
	private			ExpressionNode	expression;
	private	final	List<AstCase>	cases			= new ArrayList<>();
	private			BlockNode		defaultBlock	= null;
	private			BlockScope		switchScope;
	private			BlockScope		defaultScope;
	private			Integer			constantValue;
	
	public SwitchNode(TokenBuffer tb, MessageContainer mc) {
		super(tb, mc);

		consumeToken(tb); // Потребляем "switch"
		// Парсим выражение switch
		try {
			consumeToken(tb, Delimiter.LEFT_PAREN);
		}
		catch(CompileException e) {
			markFirstError(e);
		}
		
		try {
			this.expression = new ExpressionNode(tb, mc).parse();
		}
		catch(CompileException e) {
			markFirstError(e);
		}
		
		try {
			consumeToken(tb, Delimiter.RIGHT_PAREN);
		}
		catch(CompileException e) {
			markFirstError(e);
		}
		
		try {
			consumeToken(tb, Delimiter.LEFT_BRACE);
		}
		catch(CompileException e) {
			markFirstError(e);
		}
		
		// Парсим case-блоки
		while(!tb.match(Delimiter.RIGHT_BRACE)) {
			if(tb.match(J8BKeyword.CASE)) {
				try {
					AstCase astCase = parseCase(tb, mc);
					if(null!=astCase) {
						cases.add(astCase);
					}
				}
				catch(CompileException ex) {
					markError(ex);
					tb.skip(Delimiter.COLON);
				}
			}
			else if(tb.match(J8BKeyword.DEFAULT)) {
				consumeToken(tb); // Потребляем "default"
				try {
					consumeToken(tb, Delimiter.COLON);
				}
				catch(CompileException e) {
					markFirstError(e);
				}
				
				try {
					defaultBlock = tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb, mc, "switch.default") :
																	new BlockNode(tb, mc, parseStatement(), "switch.default");
				}
				catch(CompileException e) {
					markFirstError(e);
				}
			}
			else {
				markFirstError(parserError("Expected 'case' or 'default' in switch statement"));
				break;
			}
		}
		try {
			consumeToken(tb, Delimiter.RIGHT_BRACE);
		}
		catch(CompileException e) {
			markFirstError(e);
		}
	}

	public ExpressionNode getExpression() {
		return expression;
	}

	public List<AstCase> getCases() {
		return cases;
	}

	public BlockNode getDefaultBlock() {
		return defaultBlock;
	}

	@Override
	public boolean preAnalyze() {
		boolean result = true;
		// Проверка выражения switch
		if(null!=expression) {
			result&=expression.preAnalyze();
		}
		else {
			markError("Switch expression cannot be null");
			result = false;
		}

		if(result) {
			// Проверка всех case-блоков
			for(AstCase astCase : cases) {
				if(result && null!=astCase.getBlock()) {
					result&=astCase.getBlock().preAnalyze();
				}
			}
		}

		// Проверка default-блока
		if(result && null!=defaultBlock) {
			result&=defaultBlock.preAnalyze();
		}
		
		return result;
	}

	@Override
	public boolean declare(Scope scope) {
		boolean result = true;
		
		// Объявление выражения switch
		result&=expression.declare(scope);

		// Создаем новую область видимости для switch
		switchScope = new BlockScope(scope);

		if(result) {
			// Объявление всех case-блоков
			for(AstCase astCase : cases) {
				if(result && null!=astCase.getBlock()) {
					BlockScope caseScope = new BlockScope(switchScope);
					result&=astCase.getBlock().declare(caseScope);
					astCase.setScope(caseScope);
				}
			}
		}

		// Объявление default-блока
		if(result && null!=defaultBlock) {
			defaultScope = new BlockScope(switchScope);
			result&=defaultBlock.declare(defaultScope);
		}
		
		return result;
	}

	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		boolean result = true;
		boolean allCasesReturn = true;		
		cgScope = cg.enterCommand();

		// Проверка типа выражения switch
		result&=expression.postAnalyze(scope, cg);
		if(result) {
			try {
				ExpressionNode optimizedExpr = expression.optimizeWithScope(scope, cg);
				if(null!=optimizedExpr) {
					expression = optimizedExpr;
					result&=expression.postAnalyze(scope, cg);
				}
			}
			catch (CompileException e) {
				markFirstError(e);
				result = false;
			}
		}		

		if(result) {
			VarType exprType = expression.getType();
			if(!exprType.isIntegral()) {
				markError("Switch expression must be integral type, got: " + exprType);
				result = false;
			}
		}

		if(result) {
			// Проверка case-значений на уникальность
			List<Integer> caseValues = new ArrayList<>();
			for(AstCase astCase : cases) {
				if(result) {
					// Проверка на дубликаты
					for(Integer num : astCase.getValues()) {
						if(caseValues.contains(num)) {
							markError("Duplicate case value in range: " + num);
							result = false;
						}
						else {
							caseValues.add(num);
						}
					}
				}

				// Анализ блока case
				result&=astCase.getBlock().postAnalyze(astCase.getScope(), cg);
				if(!isControlFlowInterrupted(astCase.getBlock())) {
					allCasesReturn = false;
				}
			}
		}

		// Анализ default-блока (если есть)
		if(result && null!=defaultBlock) {
			result&=defaultBlock.postAnalyze(defaultScope, cg);
		}

		if(allCasesReturn && !cases.isEmpty()) {
			markWarning("Code after switch statement may be unreachable");
		}
		
		cg.leaveCommand();
		return result;
	}

	@Override
	public void codeOptimization(Scope scope, CodeGenerator cg) {
		CGScope oldScope = cg.setScope(cgScope);
		
		expression.codeOptimization(scope, cg);
		try {
			ExpressionNode optimizedExpr = expression.optimizeWithScope(scope, cg);
			if(null != optimizedExpr) {
				expression = optimizedExpr;
			}
		}
		catch(CompileException ex) {
			markError(ex);
		}

		for(AstCase astCase : cases) {
			astCase.getBlock().codeOptimization(scope, cg);
		}
		
		if(null!=defaultBlock) {
			defaultBlock.codeOptimization(scope, cg);
		}
		
		if(expression instanceof LiteralExpression) {
			constantValue = (int)((LiteralExpression)expression).getNumValue();
		}

		cg.setScope(oldScope);
	}

	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum, CGExcs excs) throws CompileException {
		if(cgDone) return null;
		cgDone = true;

		CGScope cgs = null == parent ? cgScope : parent;
		CGLabelScope endLabel = new CGLabelScope(null, CGScope.genId(), LabelNames.CASE_END, true);
		
		if(null==constantValue) {
			// Генерируем выражение switch
			cg.getAccum().set(-1==expression.getType().getSize() ? cg.getRefSize() : expression.getType().getSize(), false);
			if(CodegenResult.RESULT_IN_ACCUM!=expression.codeGen(cg, cgs, true, excs)) {
				throw new CompileException("Accum not used for expr:" + expression);
			}

			// Создаем метки для всех case-блоков
			List<CGBranch> branches = new ArrayList<>();
			for(AstCase astCase : cases) {
				branches.add(new CGBranch());
			}

			// Создаем метку для default-блока (если есть)
			CGLabelScope defaultLabel = null;
			if(null!=defaultBlock) {
				defaultLabel = new CGLabelScope(null, CGScope.genId(), LabelNames.CASE_DEFAULT + "_", true);
			}

			// Генерируем проверки для каждого case
			for(int i=0; i<cases.size(); i++) {
				AstCase astCase = cases.get(i);
				CGBranch branch = branches.get(i);
				List<Integer> values = astCase.getValues();
				
				int j = 0;
				while(j<values.size()) {
					int from = values.get(j);
					int to = from;

					// Ищем непрерывную последовательность
					while(j+1<values.size() && values.get(j+1)==values.get(j)+1) {
						j++;
						to = values.get(j);
					}

					if(from==to) {
						// Одиночное значение
						cg.constCond(cgs, new CGCells(CGCells.Type.ACC), Operator.EQ, from, false, false, false, true, branch); 
					} else {
						CGBranch endBranch = new CGBranch();
						// Диапазон значений
						cg.constCond(cgs, new CGCells(CGCells.Type.ACC), Operator.LT, from, false, false,false, true, endBranch);
						if(255 > to) {
							cg.constCond(cgs, new CGCells(CGCells.Type.ACC), Operator.LT, to + 1, false, false, false, true, branch);
						} else {
							cg.constCond(cgs, new CGCells(CGCells.Type.ACC), Operator.LTE, to, false, false, false, true, branch);
						}
						cgs.append(endBranch.getEnd());						
					}

					j++;
				}
			}

			cg.jump(cgs, null!=defaultLabel ? defaultLabel : endLabel);

			// Генерируем код для case-блоков
			for(int i=0; i<cases.size(); i++) {
				AstCase astCase = cases.get(i);
				CGBranch branch = branches.get(i);

				cgs.append(branch.getEnd());
				astCase.getBlock().codeGen(cg, cgs, false, excs);

				cg.jump(cgs, endLabel);
			}

			if(null!=defaultLabel) {
				cgs.append(defaultLabel);
				defaultBlock.codeGen(cg, cgs, false, excs);

			}
		}
		else {
			BlockNode bNode = getConstantFoldedBlock();
			if(null!=bNode) {
				bNode.codeGen(cg, cgs, false, excs);
			}
		}
		cgs.append(endLabel);

		return null;
	}
	
	public Integer getConstantValue() {
		return constantValue;
	}
	
	public BlockNode getConstantFoldedBlock() {
		if(null==constantValue) return null;
		
		BlockNode bNode = null;
		for(int i=0; i<cases.size(); i++) {
			AstCase astCase = cases.get(i);
			if(astCase.getValues().contains(constantValue.intValue())) {
				return astCase.getBlock();
			}
		}
		if(null==bNode && null!=defaultBlock) {
			bNode = defaultBlock;
		}
		return bNode;
	}
	
	@Override
	public List<AstNode> getChildren() {
		List<AstNode> result = new ArrayList<>(cases);
		if(null != defaultBlock) result.add(defaultBlock);
		return result;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("switch (");
		sb.append(expression).append(") {\n");

		for (AstCase astCase : cases) {
			sb.append("case ");
			for(Integer num : astCase.getValues()) {
				sb.append(num).append(",");
			}
			sb.deleteCharAt(sb.length()-1);
			sb.append(": ").append(astCase.getBlock()).append("\n");
		}

		if (null != defaultBlock) {
			sb.append("default: ").append(defaultBlock).append("\n");
		}

		sb.append("}");
		return sb.toString();
	}
}
