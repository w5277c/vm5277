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
package ru.vm5277.common.compiler;

import ru.vm5277.common.cg.scopes.CGBlockScope;

public class Case {
	private	final	long			from;
	private	final	Long			to;
	private	final	CGBlockScope	blockScope;
	
	public Case(long from, Long to, CGBlockScope blockScope) {
		this.from = from;
		this.to = to;
		this.blockScope = blockScope;
	}
	
	public long getFrom() {
		return from;
	}
	
	public Long getTo() {
		return to;
	}
	
	public CGBlockScope getBlockScope() {
		return blockScope;
	}
	
	@Override
	public String toString() {
		return (null == to ? from : from + "-" + to) + ":" + blockScope;
	}
}
