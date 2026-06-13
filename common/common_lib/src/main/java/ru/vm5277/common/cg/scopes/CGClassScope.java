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
import ru.vm5277.common.cg.items.CGIAsmLdLabel;
import ru.vm5277.common.enums.InstanceType;
import ru.vm5277.common.exceptions.CompileException;

public class CGClassScope extends CGScope {
	private	final	CodeGenerator				cg;
	private	final	VarType						type;
	private	final	List<ImplementInfo>			impl;
	private	final	Map<Integer, CGFieldScope>	fields				= new HashMap<>();
	private	final	Map<String, CGMethodScope>	methods				= new HashMap<>();
	private			int							fieldsOffset		= 0;
	private	final	boolean						isImported;
	private	final	InstanceType				instType;
//	private			CGLabelScope				lbFiledsInitScope;
	private			CGLabelScope				lbIIDSScope;
	private			CGIContainer				cont				= new CGIContainer();
	private			CGIContainer				statFieldsInitCont	= new CGIContainer();
	private			CGLabelScope				instInitlabel;
	private			CGIContainer				instInitCont		= new CGIContainer();
	private			CGIContainer				postfixCont			= new CGIContainer();
	
	public CGClassScope(CodeGenerator cg, CGScope parent, int id, VarType type, String name, List<ImplementInfo> impl, InstanceType instType) {
		super(parent, id, name);
		
		this.cg = cg;
		this.type = type;
		this.impl = impl;
		this.instType = instType;
		this.isImported = (InstanceType.NO_PARENT==instType);
		
		switch(instType) {
			case FIRST_THREAD:
			case THREAD:
				fieldsOffset = cg.getThreadHeaderSize();
				break;
			case TIMER:
				fieldsOffset = cg.getTimerHeaderSize();
				break;
			default:
				fieldsOffset = CodeGenerator.CLASS_HEADER_SIZE;
		}
		
//		lbFiledsInitScope = new CGLabelScope(null, null, LabelNames.FIELDS_INIT, true);
//		fieldsInitCont.append(lbFiledsInitScope);
		cg.getStaticInitContaier().append(statFieldsInitCont);
		lbIIDSScope = new CGLabelScope(null, null, LabelNames.META, true);
		
		Collections.sort(impl);
		// адрес HEAP, счетчик ссылок, адрес блока реализаций класса и интерфейсов
		
		instInitlabel = new CGLabelScope(null, CGScope.genId(), "j8b_" + getLName() + "_instinit", true);
	}

	public void addField(CGFieldScope field) {
		fields.put(field.getResId(), field);
	}
	public CGFieldScope getField(int resId) {
		return fields.get(resId);
	}
	
	public List<ImplementInfo>	getImlementInfos() {
		return impl;
	}
	
	public CGCells memAllocate(int size, boolean isStatic) {
		if(isStatic) {
			return new CGCells(CGCells.Type.STAT, size, cg.getAndAddStatPoolSize(size));
		}
		CGCells cells = new CGCells(CGCells.Type.HEAP, size, fieldsOffset);
		fieldsOffset+=size;
		return cells;
	}
	
	public void build(CodeGenerator cg, boolean isLaunchPoint, CGExcs excs) throws CompileException {
		if(VERBOSE_LO <= verbose) cont.append(new CGIText(";======== enter CLASS " + getPath('.') + "[" + instType + "] ========================"));
		prepend(cont);
		
		//TODO Похоже здесь мы знаем о всех используемых полях и можем выделить память для heap
		//Нужно перенести из CodeGenerator.buiid
		//constrInit.getCont().append(eNewInstance(cScope.getHeapOffset(), cScope.getIIDLabel(), cScope.getType(), false, excs));
		//terminate(scope, false, true);
		
		if(!isLaunchPoint) {
//			CGLabelScope runLabel = null;
			boolean haveConstr = false;
			//Не вызываем создание инстанса если конструкторов нет, иначе будут лишние включения фич типа OS_FT_DRAM
			for(CGMethodScope mScope : methods.values()) {
				if(null==mScope.getType()) {
					haveConstr = true;
				}
//				else if(mScope.getSignature().equals("run()")) {
//					runLabel = mScope.getCILabel();
//				}
			}
			if(haveConstr) {
				instInitCont.prepend(cg.eNewInstance(null, fieldsOffset, lbIIDSScope, type, instType, postfixCont, excs));
			}
		}
		
/*		for(CGMethodScope mScope : methods.values()) {
			if(null==mScope.getType()) {
				//TODO нужно вынести в общий блок инициализации инстанса, а здесь использовать его вызов
				CGScope scope = new CGScope();
				cg.eNewInstance(scope, fieldsOffset, lbIIDSScope, type, isImported, excs);
				mScope.getInitContainer().append(scope);
			}
		}*/
		
		if(VERBOSE_LO<=verbose) append(new CGIText(";======== leave CLASS " + getPath('.') + " ========================"));
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
	public CGLabelScope getFieldsInitLabel() {
		return instInitlabel;
	}
	
	public InstanceType getInstType() {
		return instType;
	}
	
	@Override
	public String toString() {
		return "class " + name;
	}

	public void addMethod(CGMethodScope mScope) {
		methods.put(mScope.getSignature(), mScope);
	}
	
	public CGIContainer getFieldsInitCont() {
		return instInitCont;
	}
	public CGIContainer getStatFieldsInitCont() {
		return statFieldsInitCont;
	}
	
	@Override
	public String getSource() throws CompileException {
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
		
		boolean isThread = false;
		if(!impl.isEmpty()) {
			implSB.append(",");
			StringBuilder methodsAddrSB = new StringBuilder("\n\t.dw ");
			int totalImplemented=0;
			for(ImplementInfo pair : impl) {
				isThread |= pair.getType().getClassName().equals(VarType.CLASSNAME_THREAD);
				isThread |= pair.getType().getClassName().equals(VarType.CLASSNAME_TIMER);
				int implementedCntr = 0;
				for(String mehodId : pair.getSignatures()) {
					CGMethodScope mScope = methods.get(mehodId);
					// Метод может не использоваться в кодогенерации
					if(null!=mScope && mScope.isUsed()) { //TODO похоже isUsed = рудимент
						methodsAddrSB.append(mScope.getLabel().getName()).append(",");
						implementedCntr++;
					}
					else {
						// Не записываем методы реализованные в интерфейсах (они либо вызываются нативно, либо статичны и известны на этапе компиляции)
						//methodsAddrSB.append(0).append(",");
					}
				}
				totalImplemented+=implementedCntr;
				implSB.append(pair.getType().getId()).append(",").append(implementedCntr).append(",");
			}
			implSB.deleteCharAt(implSB.length()-1);
			if(0!=totalImplemented) {
				methodsAddrSB.deleteCharAt(methodsAddrSB.length()-1);
				implSB.append(methodsAddrSB);
			}
		}
		cont.append(new CGIText(implSB.toString()));
		
		if(!instInitCont.getItems().isEmpty()) {
			// Если не статических полей больше 1 - формируем общий блок кода
//			if(0x01<instInitCont.getItems().size()) {
				// В общем блоке также инициализация HEAP (ранее был в каждом коснтрукторе)

				int constCntr = 0;
				for(CGMethodScope mScope : methods.values()) {
					if(null!=postfixCont && !postfixCont.isEMpty() && isThread && null!=mScope && mScope.getSignature().equals("run()")) {
						//TODO костыль. Не менять порядок, см Generator.eNewInstance
						//Не получилось получить доступ к методу run в build, его адрес нужен для создания нового инстанса Thread
						((CGIAsmLdLabel)postfixCont.getItems().get(0x00)).setPostfix("low(" + mScope.getLabel().getLName() + ")");
						((CGIAsmLdLabel)postfixCont.getItems().get(0x02)).setPostfix("high(" + mScope.getLabel().getLName() + ")");
					}

					if(null==mScope.getType()) {
						constCntr++;
					}
				}

				if(0x01==constCntr) {
					for(CGMethodScope mScope : methods.values()) {
						if(null==mScope.getType()) {
							mScope.getFieldInitCallCont().append(instInitCont);
						}
					}
				}
				else if(0x01<constCntr) {
					instInitCont.prepend(instInitlabel);

					instInitCont.append(cg.eReturn(null, VarType.VOID, null));

					for(CGMethodScope mScope : methods.values()) {
						if(null==mScope.getType()) {
							mScope.getFieldInitCallCont().append(cg.call(null, instInitlabel));
						}
					}

					cont.append(instInitCont);
				}
/*			}
			else { // Если поле одно, то общий блок кода не оптимален, просто включаю поле в конструкторы
				for(CGMethodScope mScope : methods.values()) {
					if(null==mScope.getType()) {
						mScope.getFieldInitCallCont().clear();
						mScope.getFieldInitCallCont().append(instInitCont);
					}
				}
			}*/
		}
	
		return super.getSource();
	}
}
