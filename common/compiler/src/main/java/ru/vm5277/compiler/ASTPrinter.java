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
package ru.vm5277.compiler;

import java.io.BufferedWriter;
import java.util.List;
import java.util.Set;
import ru.vm5277.common.Operator;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.compiler.nodes.ArrayDeclarationNode;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.BlockNode;
import ru.vm5277.compiler.nodes.ClassBlockNode;
import ru.vm5277.compiler.nodes.ClassNode;
import ru.vm5277.compiler.nodes.EnumNode;
import ru.vm5277.compiler.nodes.FieldNode;
import ru.vm5277.compiler.nodes.InterfaceBodyNode;
import ru.vm5277.compiler.nodes.InterfaceNode;
import ru.vm5277.compiler.nodes.LabelNode;
import ru.vm5277.compiler.nodes.MethodNode;
import ru.vm5277.compiler.nodes.ParameterNode;
import ru.vm5277.compiler.nodes.VarNode;
import ru.vm5277.compiler.nodes.commands.BreakNode;
import ru.vm5277.compiler.nodes.commands.CommandNode.AstCase;
import ru.vm5277.compiler.nodes.commands.ContinueNode;
import ru.vm5277.compiler.nodes.commands.DoWhileNode;
import ru.vm5277.compiler.nodes.commands.ForNode;
import ru.vm5277.compiler.nodes.commands.IfNode;
import ru.vm5277.compiler.nodes.commands.ReturnNode;
import ru.vm5277.compiler.nodes.commands.SwitchNode;
import ru.vm5277.compiler.nodes.commands.ThrowNode;
import ru.vm5277.compiler.nodes.commands.TryNode;
import ru.vm5277.compiler.nodes.commands.WhileNode;
import ru.vm5277.compiler.nodes.expressions.ArrayExpression;
import ru.vm5277.compiler.nodes.expressions.ArrayInitExpression;
import ru.vm5277.compiler.nodes.expressions.BinaryExpression;
import ru.vm5277.compiler.nodes.expressions.CastExpression;
import ru.vm5277.compiler.nodes.expressions.EnumExpression;
import ru.vm5277.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.compiler.nodes.expressions.FieldAccessExpression;
import ru.vm5277.compiler.nodes.expressions.InstanceOfExpression;
import ru.vm5277.compiler.nodes.expressions.LiteralExpression;
import ru.vm5277.compiler.nodes.expressions.MethodCallExpression;
import ru.vm5277.compiler.nodes.expressions.NewArrayExpression;
import ru.vm5277.compiler.nodes.expressions.NewExpression;
import ru.vm5277.compiler.nodes.expressions.PropertyExpression;
import ru.vm5277.compiler.nodes.expressions.TernaryExpression;
import ru.vm5277.compiler.nodes.expressions.ThisExpression;
import ru.vm5277.compiler.nodes.expressions.TypeReferenceExpression;
import ru.vm5277.compiler.nodes.expressions.UnaryExpression;
import ru.vm5277.compiler.nodes.expressions.VarFieldExpression;
import ru.vm5277.compiler.tokens.Token;

public class ASTPrinter {
	private class Printer {
		private	StringBuilder	sb		= new StringBuilder();
		private	StringBuilder	spacer	= new StringBuilder();
		private	BufferedWriter	bw;
		
		public Printer(BufferedWriter bw) {
			this.bw = bw;
		}
		
		public void put(String str) {
			sb.append(str);
		}
		
		public void extend() {
			spacer.append("    ");
		}
		public void reduce() {
			spacer.delete(spacer.length()-4, spacer.length());
		}
		public void removeLast(int qnt) {
			sb.delete(sb.length()-qnt, sb.length());
		}
		public void print() {
			try {bw.write(spacer.toString());}catch(Exception ex) {}
			printLeft();
		}
		public void printLeft() {
			try {
				bw.write(sb.toString());
				bw.write("\r\n");
			}
			catch(Exception ex) {}
			sb = new StringBuilder();
		}
	}
	
	private	Printer	out;
	
	public ASTPrinter(BufferedWriter bw, ClassNode clazz) {
		out = new Printer(bw);
		printClass(clazz);
	}

	void printClass(ClassNode clazz) {
		printModifiers(clazz.getModifiers());
		out.put("class " + clazz.getName() + " ");
		ClassBlockNode cbn = clazz.getBody();
		if(null != cbn) {
			out.put("{"); out.print(); out.extend();
			for(AstNode node : cbn.getChildren()) {
				if(node instanceof InterfaceNode) {
					printInterface((InterfaceNode)node);
				}
				else if(node instanceof ClassNode) {
					printClass((ClassNode)node);
				}
				else if(node instanceof MethodNode) {
					printMethod((MethodNode)node);
				}
				else if(node instanceof FieldNode) {
					printField((FieldNode)node); out.put(";");out.print();
				}
				else if(node instanceof VarNode) {
					printVar((VarNode)node); out.put(";");out.print();
				}
				else if(node instanceof BlockNode) {
					printBody((BlockNode)node); out.print();
				}
				else if(node instanceof EnumNode) {
					EnumNode en = (EnumNode)node;
					printModifiers(en.getModifiers());
					out.put("enum ");
					out.put(en.getName());
					out.put(" {");
					if(!en.getValues().isEmpty()) {
						out.print();
						out.extend();
						for(int i=0; i<en.getValues().size(); i++) {
							
							out.put(en.getValues().get(i).toUpperCase());
							if(i!=en.getValues().size()-1) {
								out.put(",");
							}
							else {
								out.put(";");
							}
							out.print();
						}
						out.reduce();
						out.put("}");
					}
					out.print();
				}
				else {
					out.put("!unknown node:" + node); out.print();
				}
			}
			out.reduce();
			out.put("}");
		}
		out.print();
	}
	
	void printInterface(InterfaceNode iface) {
		printModifiers(iface.getModifiers());
		out.put("interface " + iface.getName() + " ");
		InterfaceBodyNode ibn = iface.getIfaceBody();
		if(null != ibn) {
			out.put("{"); out.print(); out.extend();
			for(AstNode node : ibn.getDeclarations()) {
				if(node instanceof MethodNode) {
					printMethod((MethodNode)node);
				}
				else if(node instanceof FieldNode) {
					printField((FieldNode)node); out.put(";");out.print();
				}
				else if(node instanceof BlockNode) {
					printBody((BlockNode)node); out.print();
				}
				else {
					out.put("!unknown node:" + node); out.print();
				}
			}
			out.reduce();
			out.put("}");
		}
		out.print();
	}

	void printModifiers(Set<Keyword> modifiers) {
		for(Keyword kw : modifiers) {
			out.put(kw.getName().toLowerCase() + " ");
		}
	}
	
	void printMethod(MethodNode method) {
		printModifiers(method.getModifiers());
		if(!method.isConstructor()) {
			out.put(method.getReturnType() + " ");
		}
		out.put(method.getName() + "(");
		printParameters(method.getParameters());
		out.put(")");
		if(method.canThrow()) {
			out.put(" throws ");
		}
		BlockNode body = method.getBody();
		if(null == body) {
			out.put(";");
			out.print();
		}
		else {
			out.put(" ");
			printBody(body);
			out.print();
		}
	}
	
	void printParameters(List<ParameterNode> parameters) {
		if(null != parameters && !parameters.isEmpty()) {
			for(ParameterNode parameter : parameters) {
				if(parameter.getType().isArray()) {
					out.put(parameter.getType().getElementType() + "[] ");
				}
				else if(parameter.getType().isEnum()) {
					out.put(parameter.getType().getEnumName() + " ");
				}
				else {
					out.put(parameter.getType() + " ");
				}
				out.put(parameter.getName() + ", ");
			}
			out.removeLast(2);
		}
	}
	
	void printArguments(List<ExpressionNode> arguments) {
		if(null != arguments && !arguments.isEmpty()) {
			for(ExpressionNode arg : arguments) {
				printExpr(arg); out.put(", ");
			}
			out.removeLast(2);
		}
	}

	
	void printBody(BlockNode body)  {
		out.put("{"); out.print(); out.extend();
		if(null!=body) {
			for(AstNode node : body.getChildren()) {
				if(node instanceof LabelNode) {
					out.put(((LabelNode)node).getName() + ":");
					continue;
				}
				else if(node instanceof BreakNode) {
					BreakNode bn = (BreakNode)node;
					out.put("break");
					if(null != bn.getLabel()) out.put(" " + bn.getLabel());
					out.put(";");
				}
				else if(node instanceof ContinueNode) {
					out.put("continue;");
				}
				else if(node instanceof DoWhileNode) {
					DoWhileNode dwn = (DoWhileNode)node;
					out.put("do ");
					printBody(dwn.getBody());
					out.put(" while(");
					printExpr(dwn.getCondition());
					out.put(");"); out.print();
				}
				else if(node instanceof ForNode) {
					ForNode fn = (ForNode)node;
					out.put("for(");
					if(fn.getInitialization() instanceof FieldNode) {
						printField((FieldNode)fn.getInitialization());
					}
					else if(fn.getInitialization() instanceof VarNode) {
						printVar((VarNode)fn.getInitialization());
					}
					else {
						printExpr((ExpressionNode)fn.getInitialization());
					}
					out.put("; ");
					if(null != fn.getCondition()) {
						printExpr(fn.getCondition());
					}
					out.put("; ");
					if(null != fn.getIteration()) {
						printExpr(fn.getIteration());
					}
					out.put(") ");
					printBody(fn.getBody());
					if(null!=fn.getElseBlock()) {
						out.put("else");
						printBody(fn.getElseBlock());
					}
				}
				else if(node instanceof IfNode) {
					IfNode in = (IfNode)node;

					if(in.alwaysTrue() || in.alwaysFalse()) {
						if(in.alwaysTrue()) {
							printBody(in.getThenBlock());
						}
						else if(null != in.getElseBlock()) {
							printBody(in.getElseBlock());
						}
					}
					else {
						out.put("if (");
						printExpr(in.getCondition());
						if(null != in.getVarName()) {
							out.put(" as " + in.getVarName());
						}
						out.put(") ");
						printBody(in.getThenBlock());
						if(null != in.getElseBlock()) {
							out.print();
							out.put("else ");
							printBody(in.getElseBlock());
						}
					}
				}
				else if(node instanceof ReturnNode) {
					ReturnNode rn = (ReturnNode)node;
					out.put("return");
					if(null != rn.getExpression()) {
						out.put(" ");
						printExpr(rn.getExpression());
					}
					out.put(";");
				}
				else if(node instanceof SwitchNode) {
					SwitchNode sn = (SwitchNode)node;
					out.put("switch (");
					printExpr(sn.getExpression());
					out.put(") {"); out.extend(); out.print();
					for(SwitchNode.AstCase astCase : sn.getCases()) {
						out.put("case " + astCase.getFrom());
						if(null != astCase.getTo()) {
							out.put(".." + astCase.getTo());
						}
						out.put(": ");
						out.extend(); out.print();
						printBody(astCase.getBlock());
						out.reduce(); out.print();
					}
					out.reduce(); out.put("}");
				}
				else if(node instanceof WhileNode) {
					WhileNode wn = (WhileNode)node;
					out.put("while (");
					printExpr(wn.getCondition());
					out.put(") ");
					printBody(wn.getBody());
					out.print();
				}
				else if(node instanceof ClassNode) {
					printClass((ClassNode)node);
				}
				else if(node instanceof ArrayDeclarationNode) {
					//TODO
				}
				else if(node instanceof FieldNode) {
					printField((FieldNode)node);
					out.put(";");
				}
				else if(node instanceof VarNode) {
					VarNode vNode = (VarNode)node; //TODO повотрить для остальных
					if(vNode.isFinal() && null != vNode.getSymbol() && !vNode.getSymbol().isReassigned()) {
						out.put("//CONST ");
					}
					printVar((VarNode)node);
					out.put(";");
				}
				else if(node instanceof ExpressionNode) {
					if(printExpr((ExpressionNode)node)) out.put(";");
				}
				else if(node instanceof BlockNode) {
					printBody((BlockNode)node);
					out.print();
				}
				else if(node instanceof InterfaceNode) {
					printInterface((InterfaceNode)node);
				}
				else if(node instanceof TryNode) {
					TryNode tryNode = (TryNode)node;
					out.put("try ");
					printBody(tryNode.getTryBlock());
					if(!tryNode.getCatchCases().isEmpty() || null != tryNode.getCatchDefault()) {
						out.print();
						out.put("catch (byte ");
						out.put(tryNode.getVarName());
						out.put(") ");
						if(tryNode.getCatchCases().isEmpty()) {
							printBody(tryNode.getCatchDefault());
						}
						else {
							out.put("{");
							out.print();
							out.extend();
							for(AstCase astCase : tryNode.getCatchCases()) {
								out.put("case " + astCase.getFrom());
								if(null != astCase.getTo()) {
									out.put(".." + astCase.getTo());
								}
								out.put(": ");
								printBody(astCase.getBlock());
								out.print();
							}
							if(null != tryNode.getCatchDefault()) {
								out.put("default: ");
								printBody(tryNode.getCatchDefault());
							}
							out.print();
							out.reduce();
							out.put("}");
						}
					}

				}
				else if(node instanceof ThrowNode) {
					out.put("throw ");
					printExpr(((ThrowNode)node).getExceptionExpr());
					out.put(";");
				}
				else {
					out.put("!unknown node:" + node); out.print();
				}
				out.print();
			}
		}
		out.reduce(); out.put("}");
	}
	
	void printField(FieldNode node) {
		printModifiers(node.getModifiers());
		if(null != node.getType()) {
			if(node.getType().isArray()) {
				out.put(node.getType().getElementType() + "[] ");
			}
			else if(node.getType().isEnum()) {
				out.put(node.getType().getEnumName() + " ");
			}
			else {
				out.put(node.getType() + " ");
			}
		}
		out.put(node.getName());
		if(null != node.getInitializer()) {
			out.put(" = ");
			printExpr(node.getInitializer());
		}
	}
	
	void printVar(VarNode node) {
		printModifiers(node.getModifiers());
		if(null != node.getType()) {
			if(node.getType().isArray()) {
				VarType vt = node.getType();
				String tmp = "";
				while(vt.isArray()) {
					tmp += "[]";
					vt = vt.getElementType();
				}
				out.put(vt.getName());
				out.put(tmp + " ");
			}
			else if(node.getType().isEnum()) {
				out.put(node.getType().getEnumName() + " ");
			}
			else {
				out.put(node.getType() + " ");
			}
		}
		out.put(node.getName());
		if(null != node.getInitializer()) {
			out.put(" = ");
			printExpr(node.getInitializer());
		}
	}

	boolean printExpr(ExpressionNode expr) {
		if(null==expr || expr.isDisabled()) return false;
		
		if(expr instanceof BinaryExpression) {
			BinaryExpression be = (BinaryExpression)expr;
			printOperand(be.getLeft(), be.getOperator(), true);
			out.put(be.getOperator().getSymbol());
			printOperand(be.getRight(), be.getOperator(), false);
		}
		else if (expr instanceof LiteralExpression) {
			LiteralExpression le = (LiteralExpression)expr;
			out.put(VarType.CSTR == le.getType(null) ? "\"" + Token.toStringValue(le.getValue()) + "\"" : Token.toStringValue(le.getValue()));
		}
		else if(expr instanceof MethodCallExpression) {
			MethodCallExpression mce = (MethodCallExpression)expr;
			if(null != mce.getClassName()) {
				printExpr(mce.getClassName());
				out.put(".");
			}
			out.put(mce.getMethodName() + "(");
			printArguments(mce.getArguments());
			out.put(")");
		}
		else if(expr instanceof VarFieldExpression) {
			out.put(((VarFieldExpression)expr).getValue());
		}
		else if(expr instanceof UnaryExpression) {
			UnaryExpression ue = (UnaryExpression)expr;
			Operator op = ue.getOperator();
			if(op.isPostfix()) {
				if(null != ue.getOperand()) {
					printExpr(ue.getOperand());
				}
				out.put(ue.getOperator().getSymbol());
			}
			else {
				out.put(ue.getOperator().getSymbol());
				if(null != ue.getOperand()) {
					printExpr(ue.getOperand());
				}
			}
		}
		else if(expr instanceof TernaryExpression) {
			TernaryExpression te = (TernaryExpression)expr;
			printExpr(te.getCondition());
			out.put(" ? ");
			printExpr(te.getTrueExpr());
			out.put(" : ");
			printExpr(te.getFalseExpr());
		}
		else if(expr instanceof NewExpression) {
			NewExpression ne = (NewExpression)expr;
			out.put("new ");
			out.put(ne.getMethodName());
			out.put("(");
			printArguments(ne.getArguments());
			out.put(")");
		}
		else if(expr instanceof InstanceOfExpression) {
			InstanceOfExpression ie = (InstanceOfExpression)expr;
			printExpr(ie.getLeft());
			out.put(" is ");
			printExpr(ie.getTypeExpr());
			if(null != ie.getVarName()) {
				out.put(" as " + ie.getVarName());
			}
		}
		else if(expr instanceof TypeReferenceExpression) {
			TypeReferenceExpression te = (TypeReferenceExpression)expr;
			out.put(te.getClassName());
		}
		else if(expr instanceof FieldAccessExpression) {
			FieldAccessExpression fae = (FieldAccessExpression)expr;
			out.put(fae.getClassName() + "." + fae.getFieldName());
		}
		else if(expr instanceof CastExpression) {
			CastExpression ce = (CastExpression)expr;
			out.put("(" + ce.getTargetType() + ")");
			if (!(ce.getOperand() instanceof VarFieldExpression || ce.getOperand() instanceof LiteralExpression)) {
				out.put("(");
				printExpr(ce.getOperand());
				out.put(")");
			}
			else {
				printExpr(ce.getOperand());
			}
		}
		else if(expr instanceof NewArrayExpression) {
			NewArrayExpression nae = (NewArrayExpression)expr;
			out.put("new ");
			VarType vt = null;
			try {
				vt = nae.getType(null);
				while(vt.isArray()) {
					vt = vt.getElementType();
				}
			}
			catch(Exception ex) {
			}
			out.put(vt.getName());
			for(int i=0; i<nae.getDimensions().size(); i++) {
				out.put("[");
				if(null == nae.getInitializer()) {
					printExpr(nae.getDimensions().get(i));
				}
				out.put("]");
			}
			if(null != nae.getInitializer()) {
				printExpr(nae.getInitializer());
			}
		}
		else if(expr instanceof ArrayExpression) {
			ArrayExpression ae = (ArrayExpression)expr;
			printExpr(ae.getTargetExpr());
			for(int i=0; i<ae.getDepth(); i++) {
				out.put("[");
				printExpr(ae.getIndexesExpr()[i]);
				out.put("]");
			}
		}
		else if(expr instanceof ArrayInitExpression) {
			ArrayInitExpression aie = (ArrayInitExpression)expr;
			out.put("{");
			if(!aie.getValueExprs().isEmpty()) {
				for(ExpressionNode _expr : aie.getValueExprs()) {
					printExpr(_expr);
					out.put(",");
				}
				out.removeLast(1);
			}
			out.put("}");
		}
		else if(expr instanceof ThisExpression) {
			out.put("this");
		}
		else if(expr instanceof EnumExpression) {
			EnumExpression ee = (EnumExpression)expr;
			out.put(ee.getName());
		}
		else if(expr instanceof PropertyExpression) {
			PropertyExpression pe = (PropertyExpression)expr;
			if(pe.getNameExpr() instanceof LiteralExpression) {
				out.put(((LiteralExpression)pe.getNameExpr()).getStringValue());
			}
			else {
				printExpr(pe.getNameExpr());
			}
			out.put(".");
			out.put(pe.getProperty().toString().toLowerCase());
			if(pe.getArguments().isEmpty()) {
				try {
					if(pe.getNameExpr().getType(null).isEnum()) {
						out.put("()");
					}
				}
				catch(Exception ex) {}
			}
			else {
				out.put("(");
				printArguments(pe.getArguments());
				out.put(")");
			}
		}
		else {
			out.put("!unknown expr:" + expr); out.print();
		}
		return true;
	}
	
	void printOperand(ExpressionNode operand, Operator parentOp, boolean isLeft) {
		boolean needParentheses = false;

		if (operand instanceof BinaryExpression) {
			Operator childOp = ((BinaryExpression) operand).getOperator();
			int parentPriority = Operator.PRECEDENCE.get(parentOp);
			int childPriority = Operator.PRECEDENCE.get(childOp);

			needParentheses = (childPriority < parentPriority) || (childPriority == parentPriority && !childOp.isLeftAssociative() && isLeft);
		}
		else if (operand instanceof CastExpression && parentOp != null) {
			// Всегда ставим скобки вокруг каста, если он является частью более сложного выражения
			needParentheses = true;
		}

		if (needParentheses) out.put("(");
		printExpr(operand);
		if (needParentheses) out.put(")");
	}
}
