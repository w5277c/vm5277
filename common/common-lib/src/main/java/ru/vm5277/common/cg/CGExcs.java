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

package ru.vm5277.common.cg;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import ru.vm5277.common.lexer.SourcePosition;
import ru.vm5277.common.VarType;
import ru.vm5277.common.cg.scopes.CGLabelScope;

public class CGExcs {
	private	SourcePosition				sp;
	private	Map<Integer,CGLabelScope>	runtimeChecks	= new HashMap<>();	// Обрабатываемые (наличие try-catch) исключения для runtime проверок
	private	Set<Integer>				produced		= new HashSet<>();	// Сгенерированные исключения throw или runtime проверками
	private	CGLabelScope				methodEndLabel;
	
	public CGExcs() {
	}
	
	public Map<Integer,CGLabelScope> getRuntimeChecks() {
		return runtimeChecks;
	}
	
	public Set<Integer> getProduced() {
		return produced;
	}
	
	public CGLabelScope getMeathodEndLabel() {
		return methodEndLabel;
	}
	public void setMethodEndLabel(CGLabelScope lbScope) {
		methodEndLabel = lbScope;
	}
	
	public Integer getThrowable(int exId) {
		Integer id = exId;
		while(null!=id) {
			if(runtimeChecks.containsKey(id)) {
				return id;
			}
			id=VarType.getExceptionParent(id);
		}
		return null;
	}
	
	public void setSourcePosition(SourcePosition sp) {
		this.sp = sp;
	}
	public SourcePosition getSourcePosition() {
		return sp;
	}
	
}
