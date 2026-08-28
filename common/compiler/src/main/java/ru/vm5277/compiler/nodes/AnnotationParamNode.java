/*
 * Copyright 2026 konstantin@5277.ru
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ru.vm5277.common.lexer.TokenType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.lexer.Delimiter;
import ru.vm5277.common.lexer.Operator;
import ru.vm5277.compiler.Instance;

public class AnnotationParamNode extends AstNode {
	private			String				annotationName;
	private			Map<String, Object>	pairs			= new HashMap<>();
    private	final	List<Object>		values			= new ArrayList<>();
	
	public AnnotationParamNode(Instance inst, TokenBuffer tb, String annotationName) throws CompileException {
		super(inst, tb);
		
		this.annotationName = annotationName;
		
		while(true) {
			if(tb.match(TokenType.NUMBER)) {
				values.add(tb.consume().getStringValue());
			}
			else if(tb.match(TokenType.IDENTIFIER) || tb.match(TokenType.STRING)) {
				if(tb.match(Operator.ASSIGN)) {
					String paramName = tb.consume().getStringValue();
					tb.consume();
					if(tb.match(TokenType.NUMBER) || tb.match(TokenType.IDENTIFIER)) {
						Object obj = tb.consume().getStringValue();
						pairs.put(paramName, obj);
						values.add(obj);
					}
					else if(tb.match(Delimiter.LEFT_BRACE)) {
						Object obj = readArray(tb);
						pairs.put(paramName, obj);
						values.add(obj);
					}
				}
				else {
					values.add(tb.consume().getStringValue());
				}
			}
			else if(tb.match(Delimiter.LEFT_BRACE)) {
				values.add(readArray(tb));
				
			}
			if(tb.match(Delimiter.COMMA)) {
				tb.consume();
			}
			else {
				break;
			}
		}
	}

	private List<Byte> readArray(TokenBuffer tb) throws CompileException {
		List<Byte> arrData = new ArrayList<>();
		do{
			tb.consume();
			
			if(tb.match(TokenType.NUMBER) || tb.match(TokenType.IDENTIFIER)) {
				arrData.add(((Number)tb.consume().getValue()).byteValue());
			}
			else if(tb.match(TokenType.CHARACTER)) {
				arrData.add((byte)((Character)tb.consume().getValue()).charValue());
			}
			else {
				markError("Unexpected token type: " + tb.current().getType() + " in @" + annotationName);
				tb.skip(Delimiter.RIGHT_BRACE);
				break;
			}
		}
		while(tb.match(Delimiter.COMMA));
		consumeToken(tb, Delimiter.RIGHT_BRACE);
		return arrData;
	}
	

	public Integer getNumValue(int index) {
		if(index>=values.size()) return null;
		
		try {
			Object obj = values.get(index);
			if(obj instanceof Number) {
				return ((Number)obj).intValue();
			}
			else {
				return Integer.parseInt((String)obj);
			}
		}
		catch(Exception ex) {
			return null;
		}
	}

	public Integer getNumValue(String paramName) {
		Object obj = pairs.get(paramName);
		if(null==obj) return null;
		
		try {
			if(obj instanceof Number) {
				return ((Number)obj).intValue();
			}
			else {
				return Integer.parseInt((String)obj);
			}
		}
		catch(Exception ex) {
			return null;
		}
	}

	public String getTextValue(int index) {
		if(index>=values.size()) return null;
		
		try {
			return (String)values.get(index);
		}
		catch(Exception ex) {
			return null;
		}
	}

	public List<Object> getValues() {
		return values;
	}
	
	public byte[] getBytes(int index) {
		if(index>=values.size()) return null;
		
		Object data = values.get(index);
		if(null!=data && data instanceof List) {
			List<Byte> bytes = (List<Byte>)data;
			byte[] result = new byte[bytes.size()];
			for(int i=0; i<result.length; i++) {
				result[i] = bytes.get(i);
			}
			return result;
		}
		return null;
	}
	
	public String getAnnotationName() {
		return annotationName;
	}
	
	@Override
	public List<AstNode> getChildren() {
		return new ArrayList<>();
	}
}
