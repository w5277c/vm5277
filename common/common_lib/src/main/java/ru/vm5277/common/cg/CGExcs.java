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
import ru.vm5277.common.cg.scopes.CGLabelScope;

public class CGExcs {
	public	final	static	int	DIV_BY_ZERO		= 0x01;
	public	final	static	int	MATH_OVERFLOW	= 0x02;
	public	final	static	int	STACK_OVERFLOW	= 0x03;
	public	final	static	int	OUT_OF_MEMORY	= 0x04;
	public	final	static	int	ARRAY_INIT		= 0x05;
	public	final	static	int	INVALID_INDEX	= 0x06;

	
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
}
