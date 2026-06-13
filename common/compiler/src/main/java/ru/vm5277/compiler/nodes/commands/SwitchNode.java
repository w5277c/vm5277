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
import ru.vm5277.common.enums.CodegenResult;
import ru.vm5277.common.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.Instance;
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
	
	public SwitchNode(Instance inst, TokenBuffer tb) {
		super(inst, tb);

		consumeToken(tb); // Потребляем "switch"
		// Парсим выражение switch
		try {
			consumeToken(tb, Delimiter.LEFT_PAREN);
		}
		catch(CompileException e) {
			markFirstError(e);
		}
		
		try {
			this.expression = new ExpressionNode(inst, tb).parse();
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
					AstCase astCase = parseCase(inst, tb);
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
					defaultBlock = tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(inst, tb, "default") : new BlockNode(inst, tb, parseStatement(inst), "default");
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
		switchScope = new BlockScope(scope, false);

		if(result) {
			// Объявление всех case-блоков
			for(AstCase astCase : cases) {
				if(result && null!=astCase.getBlock()) {
					BlockScope caseScope = new BlockScope(switchScope, false);
					result&=astCase.getBlock().declare(caseScope);
					astCase.setScope(caseScope);
				}
			}
		}

		// Объявление default-блока
		if(result && null!=defaultBlock) {
			defaultScope = new BlockScope(switchScope, false);
			result&=defaultBlock.declare(defaultScope);
		}
		
		return result;
	}

	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg, CGScope parent) {
		boolean result = true;
		boolean allCasesReturn = true;		
		cgScope = cg.enterCommand(parent, "switch");

		// Проверка типа выражения switch
		result&=expression.postAnalyze(scope, cg, cgScope);
		if(result) {
			// Резолвинг QualifiedPathExpression
			ExpressionNode resolved = resolveQualifiedPathExpr(expression);
			if(null!=resolved) {
				expression = resolved;
			}

			VarType exprType = expression.getType();
			if(!exprType.isInteger()) {
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

				astCase.postAnalyze(scope, cg, cgScope);
				
				// Анализ блока case
				result&=astCase.getBlock().postAnalyze(astCase.getScope(), cg, cgScope);
				if(!isControlFlowInterrupted(astCase.getBlock())) {
					allCasesReturn = false;
				}
			}
		}

		// Анализ default-блока (если есть)
		if(result && null!=defaultBlock) {
			result&=defaultBlock.postAnalyze(defaultScope, cg, cgScope);
		}

		if(allCasesReturn && !cases.isEmpty()) {
			markWarning("Code after switch statement may be unreachable");
		}
		
		return result;
	}

	@Override
	public void codeOptimization(Scope scope, CodeGenerator cg) {
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
	}

	@Override
	public Object codeGen(CodeGenerator cg, boolean toAccum, CGExcs excs) throws CompileException {
		if(cgDone) return null;
		cgDone = true;

		if(null==constantValue) {
			CGLabelScope switchEndLabel = new CGLabelScope(null, CGScope.genId(), LabelNames.SWITCH_END, true);
			
			// Генерируем выражение switch
			cg.accumLock(expression.getType());
			if(CodegenResult.RESULT_IN_ACCUM!=expression.codeGen(cg, true, excs)) {
				cg.accumUnlock();
				throw new CompileException("Accum not used for expr:" + expression);
			}
			cg.accumUnlock();

			// Создаем метки для всех case-блоков
			List<CGBranch> branches = new ArrayList<>();
			for(int i=0; i<cases.size(); i++) {
				branches.add(new CGBranch());
			}

			// Создаем метку для default-блока (если есть)
			CGBranch defaultBranch = null;
			if(null!=defaultBlock) {
				defaultBranch = new CGBranch(new CGLabelScope(null, CGScope.genId(), LabelNames.CASE_DEFAULT + "_", true));
			}

			// Генерируем проверки для каждого case
			for(int i=0; i<cases.size(); i++) {
				AstCase astCase = cases.get(i);
				List<Integer> values = astCase.getValues();
				
				astCase.getCGScope().append(branches.get(i).getEnd());
				CGBranch nextCondBranch = (i<branches.size()-1 ? branches.get(i+1) : defaultBranch);
				CGBranch endCondBranch = new CGBranch();
				
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
						//EQ OR, переход на branch если true
						//cg.constCond(astCase.getCGScope(), new CGCells(CGCells.Type.ACC), Operator.EQ, from, false, false, false, true, endCondBranch); 
						CGCells rightCells = new CGCells(CGCells.Type.CONST);
						rightCells.setConst(from);
						cg.cellsCpCells(astCase.getCGScope(), new CGCells(CGCells.Type.ACC), expression.getType(), rightCells, null, Operator.EQ, false, true, endCondBranch);
					}
					else {
						CGBranch endBranch = new CGBranch();
						// Диапазон значений
						//cg.constCond(astCase.getCGScope(), new CGCells(CGCells.Type.ACC), Operator.LT, from, false, false,false, true, endBranch);
						CGCells rightCells = new CGCells(CGCells.Type.CONST);
						rightCells.setConst(from);
						cg.cellsCpCells(astCase.getCGScope(), new CGCells(CGCells.Type.ACC), expression.getType(), rightCells, null, Operator.LT, false, true, endBranch);
						if(255>to) {
							//cg.constCond(astCase.getCGScope(), new CGCells(CGCells.Type.ACC), Operator.LT, to + 1, false, false, false, true, endCondBranch);
							rightCells = new CGCells(CGCells.Type.CONST);
							rightCells.setConst(to+1);
							cg.cellsCpCells(astCase.getCGScope(), new CGCells(CGCells.Type.ACC), expression.getType(), rightCells, null, Operator.LT, false, true, endCondBranch);
						}
						else {
							//cg.constCond(astCase.getCGScope(), new CGCells(CGCells.Type.ACC), Operator.LTE, to, false, false, false, true, endCondBranch);
							rightCells = new CGCells(CGCells.Type.CONST);
							rightCells.setConst(to);
							cg.cellsCpCells(astCase.getCGScope(), new CGCells(CGCells.Type.ACC), expression.getType(), rightCells, null, Operator.LTE, false, true, endCondBranch);
						}
						astCase.getCGScope().append(endBranch.getEnd());
					}
					j++;
				}
				// Переходим после проверки условия на следующий блок проверок
				cg.jump(astCase.getCGScope(), null!=nextCondBranch ? nextCondBranch.getEnd() : switchEndLabel);
				// Добавляем метку на начало тела case
				astCase.getCGScope().append(endCondBranch.getEnd());
			}

			// Генерируем код для case-блоков, отдельным блоком так как в блоках проверки настроен размер аккумулятора
			for(int i=0; i<cases.size(); i++) {
				AstCase astCase = cases.get(i);
				astCase.getBlock().codeGen(cg, false, excs);
				cg.jump(astCase.getBlock().getCGScope(), switchEndLabel);
			}

			if(null!=defaultBlock) {
				defaultBlock.getCGScope().prepend(defaultBranch.getEnd());
				defaultBlock.codeGen(cg, false, excs);
			}
//			else {
//				cgScope.append(defaultBranch.getEnd());
//			}
			cgScope.append(switchEndLabel);
		}
		else {
			BlockNode bNode = getConstantFoldedBlock();
			if(null!=bNode) {
				bNode.codeGen(cg, false, excs);
			}
		}

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
