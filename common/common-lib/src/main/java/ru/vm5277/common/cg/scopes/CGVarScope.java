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
package ru.vm5277.common.cg.scopes;

import java.util.HashSet;
import java.util.Set;
import ru.vm5277.common.cg.CGCells;
import ru.vm5277.common.cg.items.CGIText;
import ru.vm5277.common.VarType;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.RegPair;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.lexer.SourcePosition;

public class CGVarScope extends CGCellsScope {
	private			CGCells			cells;
	private			CGScope			lastUsage;
	private			Set<RegPair>	usedRegsShot = new HashSet<>();
	
	public CGVarScope(CGScope parent, SourcePosition sp, int resId, VarType type, int size, boolean isConstant, String name) {
		super(parent, sp, resId, type, size, name);

		this.isConstant = isConstant;
	}
	
	public void makeUsedRegsShot() {
		//TODO перед коммитом убедиться в коректности решения и удалить черновые записи
		// Cлепок занятых регистров мк
		// Когда кодогенератор дойдет но выделения ресурсов(регистров мк) он сможет увидеть какие регистры заняты.
		// Дело в том, что порядок построения кода формируется на базе зависимости(On-Demand) - в момент обращения к элементам(класс, метод, переменная, поле и т.п.)
		// Но позиция этого блока кода формируется деревом AST.
		
		// Пример:
		// 1. byte a=123; // пока игнорирую - нет зависимости
		// 2. System.out(a); // зависимость, выделяю r20 и формирую код для иницализации относительно дерева AST
		// 3. byte b=234; // пока игнорирую - нет зависимости
		// 4. System.out(a); // зависимость, инициализация уже выполнена
		//  вижу, что далее 'a' не используется - освобождаю r20
		// 5. System.out(b); // зависимоть, выделяю освобоженный r20 и формирую код для иницализации относительно дерева AST
		// ИТОГ: в сформированном коде с порядком как в дереве AST в строке 3 используется r20
		
		// РЕШЕНИЕ: при кодогенерации переменной (выделении ресурса) исключать из доступных регистров регистры помеченные в слепке как занятые.

		
		// Чушь. On-Demand может только скзать что нужно выделить ресурсы, но в какой момент времени она не говорит.
		// Я также не могу менять порядок AST - иначе поломается последовательность выполнения
		// Сейчас я выделяю ресурсы при генерации блока, а не при первом нахождении блока в AST
		// ЧТо если получать все узлы AST между текущей позицией и узлом запрашивающим ресурс и по этим узлам выстраивать занятые регистры?
		// Я могу в VarSymbol записать sp начала и конца жизни, также могу сформировать список всех VarScope в определенном диапазоне и вытащить все их
		// используемые регистры через CGScope и этого достаточно для листа занятых регистров
		// ВОт насчет конца жизни не уверен - блок кода может быть статически удален но в AST он будет
		// А не достаточно просто начала жизни?
		
		// Да, это похожая реализация на то что я уже сделал, но разница лишь в том, что регистры я беру не из пула занятых в методе лили блоке а из спика
		// регистраций переменных которые находятся между инициализацией новой переменной и кодом перового использования
		// А что насчет вложенных блоков? - Вообще не волнует, я беру список из BlockScope и также его предков. 
		
		if(parent instanceof CGBlockScope) {
			usedRegsShot = ((CGBlockScope)parent).getUsedRegs();
		}
	}

	public void build(Set<CGCells> usedRegsCells) throws CompileException {
		if(!isConstant) {
			if(parent instanceof CGMethodScope) { // Используется при вызове метода(передача параметров)
				cells = ((CGMethodScope)parent).argAllocate(this, size);
				((CGMethodScope)parent).addArg(this);
			}
			else if(parent instanceof CGBlockScope) { // Инициализация переменной в блоке
				if(null!=usedRegsCells) {
					for(CGCells cells : usedRegsCells) {
						for(int regId : cells.getIds()) {
							usedRegsShot.add(new RegPair((byte)regId, false));
						}
					}
				}
				
				cells = ((CGBlockScope)parent).memAllocate(this, usedRegsShot, size);
//				((CGBlockScope)parent).addVar(this); // Пееренесено в VArNode.postAnalyze
			}
			else {
				throw new CompileException("TODO unexpected CGScope:" + parent + " for local init");
			}
			if(VERBOSE_LO <= verbose) append(new CGIText(";allocated " + cells.toString() + " for " + toString()));
		}
	}
	
	public void checkAndRelease(CodeGenerator cg, CGScope cgScope) throws CompileException {
		if(cgScope==lastUsage) {
			if(lastUsage instanceof CGCommandScope && ((CGCommandScope)lastUsage).getName().equals("return")) {
				if(VERBOSE_LO <= verbose) lastUsage.append(new CGIText(";skip release for '" + name + "' (return node), cells:" + cells.toString()));
			}
			else {
				if(type.isClassType() && !type.isEnum()) {
					if(VERBOSE_LO <= verbose) lastUsage.append(new CGIText(";release class '" + name + "', cells:" + cells.toString()));
					cg.updateClassRefCount(lastUsage, cells, false);
				}
				else if(type.isArray()) {
					if(VERBOSE_LO <= verbose) lastUsage.append(new CGIText(";release array '" + name + "', cells:" + cells.toString()));
					cg.updateArrRefCount(lastUsage, cells, false);
				}
				else {
					if(null!=cells && CGCells.Type.REG==cells.getType()) {
						if(VERBOSE_LO <= verbose) lastUsage.append(new CGIText(	";release var '" + name + "', cells:" + cells.toString() +
																				" (" + ((CGBlockScope)parent).getPath('.') + ")"));
						for(int regId : cells.getIds()) {
							((CGBlockScope)parent).releaseReg(this, (byte)regId);
						}
					}
				}
			}
			lastUsage = null;
		}
	}

	
	@Override
	public CGCells getCells() {
		return cells;
	}
	public void setCells(CGCells cells) {
		this.cells = cells;
	}

	public void setLastUsage(CGScope scope) {
		lastUsage = scope;
	}
	public CGScope getLastUsage() {
		return lastUsage;
	}

	public Set<RegPair> getUsedRegsShot() {
		return usedRegsShot;
	}
	
	@Override
	public String toString() {
		return (parent instanceof CGMethodScope ? "arg " : "var ") + type + " '" + getPath('.') + "'" + (null!=lastUsage ? ", last usage:" + lastUsage : "");
	}
}
