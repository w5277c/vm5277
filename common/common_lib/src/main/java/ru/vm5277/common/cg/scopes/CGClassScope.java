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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ru.vm5277.common.ImplementInfo;
import ru.vm5277.common.LabelNames;
import ru.vm5277.common.cg.CGCells;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.items.CGIContainer;
import ru.vm5277.common.cg.items.CGIText;
import ru.vm5277.common.VarType;
import ru.vm5277.common.exceptions.CompileException;

public class CGClassScope extends CGScope {
	private	final	CodeGenerator				cg;
	private	final	VarType						type;
	private	final	List<ImplementInfo>			impl;
	private	final	Map<Integer, CGFieldScope>	fields			= new HashMap<>();
	private	final	Map<String, CGMethodScope>	methods			= new HashMap<>();
	private			int							fieldsOffset	= 0;
	private	final	boolean						isImported;
	private			CGLabelScope				lbFiledsInitScope;
	private			CGLabelScope				lbIIDSScope;
	private			CGIContainer				cont			= new CGIContainer();
	private			CGIContainer				fieldsInitCont	= new CGIContainer();
	
	public CGClassScope(CodeGenerator cg, CGScope parent, int id, VarType type, String name, List<ImplementInfo> impl, boolean isRoot) {
		super(parent, id, name);
		
		this.cg = cg;
		this.type = type;
		this.impl = impl;
		this.isImported = isRoot;
		
		fieldsOffset = CodeGenerator.CLASS_HEADER_SIZE;
		
		lbFiledsInitScope = new CGLabelScope(null, null, LabelNames.FIELDS_INIT, true);
		fieldsInitCont.append(lbFiledsInitScope);
		lbIIDSScope = new CGLabelScope(null, null, LabelNames.META, true);
		
		Collections.sort(impl);
		// адрес HEAP, счетчик ссылок, адрес блока реализаций класса и интерфейсов
	}

	public void addField(CGFieldScope field) {
		fields.put(field.getResId(), field);
	}
	public CGFieldScope getField(int resId) {
		return fields.get(resId);
	}
	
	public CGCells memAllocate(int size, boolean isStatic) {
		if(isStatic) {
			return new CGCells(CGCells.Type.STAT, size, cg.getAndAddStatPoolSize(size));
		}
		CGCells cells = new CGCells(CGCells.Type.HEAP, size, fieldsOffset);
		fieldsOffset+=size;
		return cells;
	}
	
	public void build(CodeGenerator cg, CGExcs excs) throws CompileException {
		if(VERBOSE_LO <= verbose) cont.append(new CGIText(";======== enter CLASS " + getPath('.') + " ========================"));
		prepend(cont);
		
		//TODO Похоже здесь мы знаем о всех используемых полях и можем выделить память для heap
		//Нужно перенести из CodeGenerator.buiid
		//constrInit.getCont().append(eNewInstance(cScope.getHeapOffset(), cScope.getIIDLabel(), cScope.getType(), false, excs));
		//terminate(scope, false, true);
		
		for(CGMethodScope mScope : methods.values()) {
			if(null==mScope.getType()) {
				CGScope scope = new CGScope();
				cg.eNewInstance(scope, fieldsOffset, lbIIDSScope, type, isImported, excs);
				mScope.getInitContainer().append(scope);
			}
		}
		
		if(VERBOSE_LO <= verbose) append(new CGIText(";======== leave CLASS " + getPath('.') + " ========================"));
	}
	
	public int getHeapOffset() {
		return fieldsOffset;
	}
	
	public boolean isImported() {
		return isImported;
	}
	
	@Override
	public String getLName() {
		return "C" + name;
	}
	
	public VarType getType() {
		return type;
	}
	
	public CGLabelScope getIIDLabel() {
		return lbIIDSScope;
	}
	public CGLabelScope getFieldInitLabel() {
		return lbFiledsInitScope;
	}
	
	@Override
	public String toString() {
		return "class " + name + " '" + getPath('.') + resId;
	}

	public void addMethod(CGMethodScope mScope) {
		methods.put(mScope.getSignature(), mScope);
	}
	
	public CGIContainer getFieldsInitCont() {
		return fieldsInitCont;
	}
	
	@Override
	public String getSource() {
		// Поздний этап сборки, так как использование методов класса известно только на этапе кодогенерации
		
		// Формируем FLASH блок с ид типов класса и реализованных интерфейсов
		// - ид номер типа класса(1 байт)
		// - количество интерфейсов реализованных интерфейсов(1 байт)
		// Формируем запись из двух байт для каждого интерфейса
		// - ид номер типа интерфейса(1 байт)
		// - количество методов в интерфейсе
		// Формируем адреса методов
		// - адрес метода(2 байта)

		// Обязательные условия
		// - все интерфейсы упорядочены(например по VarType ID) и не зависят от порядка и наличия в implements
		// - методы отсутствующие в интерфейсах идут после остальных методов
		// - методы повторяющиеся в интерфейсах участвуют в подсчете количества методов, а также дублируются в таблице адресов методов

		StringBuilder implSB = new StringBuilder(lbIIDSScope.getName());
		implSB.append(":\n\t.db ").append(type.getId()).append(",").append(impl.size());
			
		if(!impl.isEmpty()) {
			implSB.append(",");
			StringBuilder methodsAddrSB = new StringBuilder("\n\t.dw ");
			for(ImplementInfo pair : impl) {
				implSB.append(pair.getType().getId()).append(",").append(pair.getSignatures().size()).append(",");

				for(String mehodId : pair.getSignatures()) {
					CGMethodScope mScope = methods.get(mehodId);
					// Метод может не использоваться в кодогенерации
					if(null!=mScope && mScope.isUsed()) { //TODO похоже isUsed = рудимент
						methodsAddrSB.append(mScope.getLabel().getName()).append(",");
					}
					else {
						methodsAddrSB.append(0).append(",");
					}
				}
			}
			implSB.deleteCharAt(implSB.length()-1);
			methodsAddrSB.deleteCharAt(methodsAddrSB.length()-1);
			implSB.append(methodsAddrSB);
		}
		cont.append(new CGIText(implSB.toString()));
		
		if(0x01<fieldsInitCont.getItems().size()) {
			try {
				fieldsInitCont.append(cg.eReturn(null, 0, 0, null));
			}
			catch(Exception ex) {}
			cont.append(fieldsInitCont);
		}
		else {
			for(CGMethodScope mScope : methods.values()) {
				if(null == mScope.getType() && null != mScope.getFieldInitCallCont()) {
					mScope.getFieldInitCallCont().disable();
				}
			}
		}
	
		return super.getSource();
	}
}
