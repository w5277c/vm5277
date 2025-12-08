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

package ru.vm5277.compiler.semantic;

import java.util.HashMap;
import java.util.Map;
import ru.vm5277.common.exceptions.CompileException;

public class GlobalScope extends Scope implements ImportableScope {
	protected	final	Map<String, CIScope>		imported		= new HashMap<>();
	
	public GlobalScope() {
		super(null);
	}
	
	@Override
	public void addCI(CIScope cis, boolean isInternal) throws CompileException {
		if(null!=imported.get(cis.getName())) throw new CompileException(cis.getName() + "' conflicts with import");

		if(isInternal) {
			addInternal(cis);
		}
		else {
			imported.put(cis.getName(), cis);
		}
	}

	@Override
	public CIScope resolveCI(String name, boolean isQualifiedAccess) {
		return resolveCI(null, name, isQualifiedAccess);
	}
	@Override
	public CIScope resolveCI(Scope caller, String name, boolean isQualifiedAccess) {
		if(isQualifiedAccess) {
			return internal.get(name);
		}

		CIScope cis = internal.get(name);
		if(null!=cis) return cis;

		//TODO здесь необходимо проверять доступ по модификаторам и caller
		cis = imported.get(name);
		if(null!=cis) return cis;

		return null;
	}

	@Override
	public Symbol resolveField(Scope caller, String name, boolean isQualifiedAccess) throws CompileException {
		return null;
	}
	@Override
	public Symbol resolveVar(String name) throws CompileException {
		return null;
	}

	@Override
	public void addImport(String importPath, String alias) throws CompileException {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public void addStaticImport(String importPath, String alias) throws CompileException {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public boolean checkStaticImportExists(String path) {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}
}
