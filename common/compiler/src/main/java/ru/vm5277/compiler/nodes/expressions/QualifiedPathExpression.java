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
import java.util.List;
import ru.vm5277.common.lexer.Operator;
import ru.vm5277.common.Property;
import static ru.vm5277.common.SemanticAnalyzePhase.DECLARE;
import static ru.vm5277.common.SemanticAnalyzePhase.POST;
import static ru.vm5277.common.SemanticAnalyzePhase.PRE;
import ru.vm5277.common.StrUtils;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.compiler.CodegenResult;
import ru.vm5277.common.compiler.Optimization;
import ru.vm5277.common.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.Main;
import static ru.vm5277.compiler.Main.debugAST;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.BlockNode;
import ru.vm5277.compiler.nodes.FieldNode;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.nodes.commands.ReturnNode;
import ru.vm5277.compiler.nodes.expressions.bin.BinaryExpression;
import ru.vm5277.compiler.semantic.AstHolder;
import ru.vm5277.compiler.semantic.CIScope;
import ru.vm5277.compiler.semantic.EnumScope;
import ru.vm5277.compiler.semantic.MethodSymbol;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.semantic.Symbol;
import ru.vm5277.compiler.semantic.VarSymbol;

public class QualifiedPathExpression extends ExpressionNode {
	public static enum SegmentType {
		THIS,			// this
		QUALIFIED,		// obj, field, subfield
		METHOD_CALL,	// method()
		ARRAY_ACCESS,	// [index]
		
	}
	public static abstract class PathSegment {
		protected SegmentType type;
    
		public SegmentType getType() { 
			return type;
		}
	}
	public static class ThisSegment extends PathSegment {

		public ThisSegment() {
			this.type = SegmentType.THIS;
		}

		@Override
		public String toString() {
			return "this";
		}
	}
	public static class QualifiedSegment extends PathSegment {
		private final String name;

		public QualifiedSegment(String name) {
			this.type = SegmentType.QUALIFIED;
			
			this.name = name;
		}

		public String getName() {
			return name;
		}

		@Override
		public String toString() {
			return name;
		}
	}
	public static class MethodSegment extends PathSegment {
		private final List<ExpressionNode> arguments;

		public MethodSegment(List<ExpressionNode> arguments) {
			this.type = SegmentType.METHOD_CALL;
			this.arguments = arguments;
		}

		public List<ExpressionNode> getArguments() {
			return arguments;
		}
		
		@Override
		public String toString() {
			return "(" + StrUtils.toString(arguments) + ")";
		}
	}
	public static class ArraySegment extends PathSegment {
		private final List<ExpressionNode> indices;

		public ArraySegment(List<ExpressionNode> indices) {
			this.type = SegmentType.ARRAY_ACCESS;
			this.indices = indices;
		}

		public List<ExpressionNode> getIndices() {
			return indices;
		}
		
		@Override  
		public String toString() {
			StringBuilder sb = new StringBuilder();
			for (ExpressionNode index : indices) {
				sb.append("[").append(index.toString()).append("]");
			}
			return sb.toString();
		}
	}
	
	private	List<PathSegment>		segments			= new ArrayList<>();
//	private	List<ExpressionNode>	expressions			= new ArrayList<>();
	private	ExpressionNode			resolvedExpr		=null;
	private	boolean					reassigned			= false;
	private	Integer					lastAccessedSn		= null;
	private	boolean					postAnalyzed		= false;
	
	public QualifiedPathExpression(TokenBuffer tb, MessageContainer mc) throws CompileException {
		super(tb, mc);
	}
	
	public void addSegment(PathSegment segment) {
		segments.add(segment);
	}
	
	// Грубое определение для оператора new
	public boolean isArrayPath() {
		for(int i=0; i<segments.size(); i++) {
			PathSegment segment = segments.get(i);
			if(	(0==i && SegmentType.THIS!=segment.getType() && SegmentType.QUALIFIED!=segment.getType()) ||
				(0<i && i<segments.size()-1 && SegmentType.THIS==segment.getType()) ||
				(i==segments.size()-1) && SegmentType.ARRAY_ACCESS!=segment.getType()) {

				return false;
			}
		}
		return true;
	}


	@Override
	public boolean preAnalyze() {
		debugAST(this, PRE, true, getFullInfo());

		boolean result = true;

		// 1. Путь не должен быть пустым
		if (segments.isEmpty()) {
			markError("Qualified path cannot be empty");
			result = false;
		}

		// 2. Первый сегмент должен быть QualifiedSegment
		if(result) {
			if(SegmentType.QUALIFIED!=segments.get(0).getType() && SegmentType.THIS!=segments.get(0).getType()) {
				markError("First segment in qualified path must be 'this' or identifier, got: " + segments.get(0).getType());
				result = false;
			}
		}

		// 3. Проверяем базовую структуру сегментов
		for (int i=0; i<segments.size(); i++) {
			PathSegment segment = segments.get(i);
			if(0!=i && SegmentType.THIS==segment.getType()) {
				markError("Qualified path cannot have 'this' in non-initial segment"); 
				result = false;
				break;
			}
			// Две операции подряд: method()() или [][]
			if(i>0) {
				PathSegment prev = segments.get(i - 1);
				if(	(segment.getType()==SegmentType.METHOD_CALL && prev.getType()==SegmentType.METHOD_CALL) ||
					(segment.getType()==SegmentType.ARRAY_ACCESS && prev.getType()==SegmentType.ARRAY_ACCESS)) {
					
					markError("Consecutive operations are not allowed in qualified path");
					result = false;
				}
			}
		}

		// 4. Рекурсивный preAnalyze для вложенных выражений
		for(PathSegment segment : segments) {
			if(segment instanceof MethodSegment) {
				for(ExpressionNode arg : ((MethodSegment)segment).getArguments()) {
					result&=arg.preAnalyze();
				}
			}
			else if(segment instanceof ArraySegment) {
				for(ExpressionNode index : ((ArraySegment)segment).getIndices()) {
					result&=index.preAnalyze();
				}
			}
		}

		debugAST(this, PRE, false, result, getFullInfo());
		return result;
	}
	
	@Override
	public boolean declare(Scope scope) {
		boolean result = true;
		debugAST(this, DECLARE, true, getFullInfo());
		debugAST(this, DECLARE, false, result, getFullInfo());
		return result;
	}

	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		boolean result = true;
		debugAST(this, POST, true, getFullInfo());
		if(!postAnalyzed) {
			postAnalyzed = true;
			try {
				TypeReferenceExpression pathExpr = null;
				for(int i=0 ;i<segments.size(); i++) {
					PathSegment segment = segments.get(i);

					PathSegment secondSegment = null;
					if(i!=segments.size()-1) {
						secondSegment = segments.get(i+1);

						if(segment instanceof QualifiedSegment && secondSegment instanceof MethodSegment) {
							String propName = ((QualifiedSegment)segment).getName();
							Property prop = null;
							try {
								prop = Property.valueOf(propName);
							}
							catch(IllegalArgumentException ex) {
							}

							if(null!=resolvedExpr && (resolvedExpr instanceof EnumExpression || resolvedExpr.getType().isEnum())) {
								i++;
								if(null!=prop) {
									ExpressionNode expr = new PropertyExpression(tb, mc, sp, resolvedExpr, prop, ((MethodSegment)secondSegment).getArguments());
									resolvedExpr = expr;
									result&=expr.declare(scope);
									if(result) {
										result&=expr.postAnalyze(scope, cg);
										symbol = expr.getSymbol();
									}
								}
								else {
									markError("Unknown property: " + resolvedExpr.toString() + "." + propName);
								}
								continue;
							}
							else if(null!=pathExpr && pathExpr.getScope() instanceof EnumScope) {
								if(null!=prop) {
									ExpressionNode expr = new PropertyExpression(tb, mc, sp, pathExpr, prop, ((MethodSegment)secondSegment).getArguments());
									resolvedExpr = expr;
									result&=expr.declare(scope);
									if(result) {
										result&=expr.postAnalyze(scope, cg);
										symbol = expr.getSymbol();
									}
								}
								else {
									markError("Unknown property: " + resolvedExpr.toString() + "." + propName);
								}
								i++;
								continue;
							}
							else if(null!=prop && (Property.instanceId==prop || Property.typeId==prop || Property.code==prop)) {
									ExpressionNode expr = new PropertyExpression(	tb, mc, sp, null==resolvedExpr ? pathExpr : resolvedExpr,
																					prop, ((MethodSegment)secondSegment).getArguments());
									resolvedExpr = expr;
									result&=expr.declare(scope);
									if(result) {
										result&=expr.postAnalyze(scope, cg);
										symbol = expr.getSymbol();
									}
									i++;
									continue;
							}
							else {
								ExpressionNode expr = new MethodCallExpression(	tb, mc, sp,
																				null==resolvedExpr ? pathExpr : resolvedExpr,
																				((QualifiedSegment)segment).getName(),
																				((MethodSegment)secondSegment).getArguments());
								result&=expr.declare(scope);
								if(result) {
									result&=expr.postAnalyze(scope, cg);
									List<ExpressionNode> optimized = null;
									if(result && Optimization.NONE!=Main.getOptLevel()) {
										optimized = optimizeMethodCall((MethodCallExpression)expr);
									}
									if(null!=optimized) {
										symbol = null; //TODO ????
										if(0x01==optimized.size()) {
											ExpressionNode exprNode = optimized.get(0);
											result&=exprNode.declare(scope);
											if(result) {
												result&=exprNode.postAnalyze(scope, cg);
												resolvedExpr = exprNode;
												if(exprNode instanceof VarFieldExpression) {
													symbol = exprNode.getSymbol();
												}
											}
										}
										else {
											resolvedExpr = new ExpressionsContainer(tb, mc);
											for(ExpressionNode exprNode : optimized) {
												result&=exprNode.declare(scope);
												if(result) {
													result&=exprNode.postAnalyze(scope, cg);
													((ExpressionsContainer)resolvedExpr).add(exprNode);
												}
											}
										}
									}
									else {
										resolvedExpr = expr;
										symbol = expr.getSymbol();
									}
									i++;
									continue;
								}
							}
						}
					}

					
					if(segment instanceof ThisSegment) {
						pathExpr = new ThisExpression(tb, mc, scope.getThis());
						resolvedExpr = pathExpr;
						result&=pathExpr.declare(scope);
						if(result) {
							result&=pathExpr.postAnalyze(scope, cg);
						}
					}
					else if(segment instanceof QualifiedSegment) {
						QualifiedSegment qSeg = (QualifiedSegment)segment;
						Property prop = null;
						try {
							prop = Property.valueOf(qSeg.getName());
						}
						catch(IllegalArgumentException ex) {
						}

						if(null!=resolvedExpr && null!=prop) {
							ExpressionNode expr = new PropertyExpression(tb, mc, sp, resolvedExpr, prop, new ArrayList<>());
							resolvedExpr = expr;
							result&=expr.declare(scope);
							if(result) {
								result&=expr.postAnalyze(scope, cg);
								symbol = expr.getSymbol();
							}
							continue;
						}
						else if(null!=pathExpr && pathExpr.getScope() instanceof EnumScope) {
							resolvedExpr = new EnumExpression(tb, mc, pathExpr, qSeg.getName());
							result&=resolvedExpr.declare(scope);
							if(result) {
								result&=resolvedExpr.postAnalyze(scope, cg);
							}
							continue;
						}
						else {
							symbol = null==pathExpr ? scope.resolveVar(qSeg.getName()) : pathExpr.getScope().resolveVar(qSeg.getName());
							if(null!=symbol) {
								ExpressionNode expr = new VarFieldExpression(tb, mc, sp, getSN(), null==resolvedExpr ? pathExpr : resolvedExpr,	qSeg.getName());
								resolvedExpr = expr;
								result&=expr.declare(scope);
								if(result) {
									if(reassigned) {
										expr.getSymbol().setReassigned();
									}
									expr.getSymbol().setAccessed(getSN());
									result&=expr.postAnalyze(scope, cg);
									symbol = expr.getSymbol();
									continue;
								}
							}
							else {
								symbol = null==pathExpr ? scope.resolveField(qSeg.getName(), false) : pathExpr.getScope().resolveField(qSeg.getName(), true);
								if(null!=symbol) {
									ExpressionNode expr = new VarFieldExpression(tb, mc, sp, getSN(), null==resolvedExpr ?	pathExpr :
																															resolvedExpr,	qSeg.getName());
									resolvedExpr = expr;
									result&=expr.declare(scope);
									if(result) {
										result&=expr.postAnalyze(scope, cg);
										if(reassigned) {
											expr.getSymbol().setReassigned();
										}
										expr.getSymbol().setAccessed(getSN());
									}
									continue;
								}
								else {
									CIScope cis = null==pathExpr ? scope.resolveCI(qSeg.getName(), false) : pathExpr.getScope().resolveCI(qSeg.getName(), true);
									if(null!=cis) {
										pathExpr = new TypeReferenceExpression(tb, mc, pathExpr, qSeg.getName(), cis);
										result&=pathExpr.declare(scope);
										if(result) {
											result&=pathExpr.postAnalyze(scope, cg);
										}
										resolvedExpr = pathExpr;
										continue;
									}
									else {
										int t=45454;
									}
								}
							}
						}

						markError("Can't resolve " + segment.toString());
						result = false;
					}
					else if(segment instanceof ArraySegment) {
						resolvedExpr = new ArrayExpression(tb, mc, sp, resolvedExpr, ((ArraySegment)segment).getIndices());
						result&=resolvedExpr.declare(scope);
						if(result) {
							result&=resolvedExpr.postAnalyze(scope, cg);
							if(reassigned) {
								resolvedExpr.getSymbol().setReassigned();
							}
							resolvedExpr.getSymbol().setAccessed(getSN());
						}
					}
					else {
						markError("Can't resolve " + segment.toString());
						result = false;
					}
				}

				if(result && null!=symbol && symbol instanceof VarSymbol) {
					int varSeqNum = ((VarSymbol)symbol).getNode().getSN();
					if(varSeqNum>sn) {
						markError("Variable '" + symbol.getName() + "' cannot be used before its declaration");
						result = false;
					}
				}
			}
			catch(CompileException ex) {
				markError(ex);
				result = false;
			}
		}
		debugAST(this, POST, false, result, getFullInfo());
		return result;
	}
	
	@Override
	public void codeOptimization(Scope scope, CodeGenerator cg) {
		if(null!=resolvedExpr) {
			resolvedExpr.codeOptimization(scope, cg);
		}
	}
	
	public List<ExpressionNode> optimizeMethodCall(MethodCallExpression expr) {
		//TODO дополнить для методов других классов
		MethodSymbol mSymbol = (MethodSymbol)expr.getSymbol();
		if(mSymbol.isNative() || mSymbol.canThrow() || mSymbol.isInterfaceImpl()) return null;

		// Проверка на setter
		// Возвращаемый тип:void, есть параметры, в методе один элемент
		if(VarType.VOID==mSymbol.getType() && !mSymbol.getParameters().isEmpty() && 0x01==mSymbol.getNode().getChildren().size()) {
			AstNode node = mSymbol.getNode().getChildren().get(0);
			// В методе находится блок и размер его элементов не больше количества аргументов
			if(node instanceof BlockNode && node.getChildren().size() <= expr.getArguments().size()) {
				List<ExpressionNode> result = new ArrayList<>();
				for(int i=0; null!=result && i<node.getChildren().size(); i++) {
					boolean match = false;
					AstNode childNode = node.getChildren().get(i);
					// Каждый элемент блока - это бинарное выражение
					if(childNode instanceof BinaryExpression) {
						BinaryExpression be = (BinaryExpression)childNode;
						// Выражение присваивания, где оба операнда выражения обращения к переменной или полю
						if(Operator.ASSIGN == be.getOperator() && be.getLeft() instanceof VarFieldExpression && be.getRight() instanceof VarFieldExpression) {
							// Левый операнд содержит поле
							if(	(((VarFieldExpression)be.getLeft()).getSymbol() instanceof AstHolder &&
								((AstHolder)((VarFieldExpression)be.getLeft()).getSymbol()).getNode() instanceof FieldNode)) {
								// Правый операнд либо константа
								if(be.getRight() instanceof LiteralExpression) {
									result.add(be);
									match = true;
								}
								// Либо обращение к аргументу
								else if(!(((VarFieldExpression)be.getRight()).getSymbol() instanceof AstHolder)) {
									String argName = (((VarFieldExpression)be.getRight()).getSymbol()).getName();
									for(int j=0; j<mSymbol.getParameters().size(); j++) {
										Symbol paramSymbol = mSymbol.getParameters().get(j);
										if(paramSymbol.getName().equals(argName)) {
											VarFieldExpression vfe = (VarFieldExpression)expr.getPathExpr();
											result.add(BinaryExpression.create(	tb, mc, expr.getSP(),
																				new VarFieldExpression(	tb, mc, expr.getSP(), expr.getSN(), vfe,
																										((VarFieldExpression)be.getLeft()).getName(), true),
																				Operator.ASSIGN, expr.getArguments().get(j)));
											match = true;
											break;
										}
									}
								}
							}
						}
					}
					if(!match) {
						result = null;
					}
				}
				return result;
			}
		}
		//Проверка на getter
		//INFO Очень редко не оптимально по размеру(8 bit, int или выход за пределы константного смещения lds инструкций(avr)
		// Оставлю как есть, теряем 1 байт?, но есть выигрыш по времени
		if(VarType.VOID!=mSymbol.getType() && mSymbol.getParameters().isEmpty() && 0x01==mSymbol.getNode().getChildren().size()) {
			AstNode node = mSymbol.getNode().getChildren().get(0);
			if(node instanceof BlockNode && 0x01==node.getChildren().size()) {
				node = ((BlockNode)node).getChildren().get(0);
				if(node instanceof ReturnNode) {
					ExpressionNode retExpr = ((ReturnNode)node).getExpression();
					if(retExpr instanceof QualifiedPathExpression) {
						retExpr = ((QualifiedPathExpression)retExpr).getResolvedExpr();
					}
					if(retExpr instanceof VarFieldExpression && ((AstHolder)((VarFieldExpression)retExpr).getSymbol()).getNode() instanceof FieldNode) {
						FieldNode fNode = (FieldNode)((AstHolder)((VarFieldExpression)retExpr).getSymbol()).getNode();
						List<ExpressionNode> result = new ArrayList<>();
						result.add(new VarFieldExpression(tb, mc, sp, expr.getSN(), (VarFieldExpression)expr.getPathExpr(), fNode.getName(), true));
						return result;
					}
					else if(retExpr instanceof ThisExpression) {
						List<ExpressionNode> result = new ArrayList<>();
						//result.add(expr.getPathExpr());
						result.add(new CastExpression(tb, mc, sp, expr.getType(), expr.getPathExpr()));
						return result;
					}
				}
			}
		}
		return null;
	}

	//Не ясная логика, например для выражения classInst.method(...) здесь два выражения, но сохрянять результат в аккумулятор нужно только вызовы метода
	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum, CGExcs excs) throws CompileException {
		if(null!=resolvedExpr) {
			resolvedExpr.codeGen(cg, parent, true, excs);
		}
		
		return CodegenResult.RESULT_IN_ACCUM;
	}
	
	public void setReassigned() {
		reassigned = true;
	}
	public void setAccessed(int sn) {
		if(null==lastAccessedSn || lastAccessedSn<sn) {
			lastAccessedSn = sn;
		}
	}
	
	public ExpressionNode getResolvedExpr() {
		return resolvedExpr;
	}
	
	@Override
	public VarType getType() {
		if(null!=resolvedExpr && null!=resolvedExpr.getType()) {
			markWarning("COMPILER BUG: QualifiedPathExpression.getType() should never be called - use getResolvedExpr().getType() instead. Expression: " + this);
			return resolvedExpr.getType();
		}
		return null;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		for(int i=0; i<segments.size(); i++) {
			PathSegment segment = segments.get(i);
			if(0!=i && (segment instanceof ArraySegment || segment instanceof MethodSegment)) {
				sb.deleteCharAt(sb.length()-1);
			}
			sb.append(segment.toString());
			sb.append(".");
		}
		sb.deleteCharAt(sb.length()-1);
		return sb.toString();
	}	

	public String getFullInfo() {
		return getClass().getSimpleName() + " " + toString();
	}
}
