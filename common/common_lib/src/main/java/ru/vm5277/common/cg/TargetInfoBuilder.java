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

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import ru.vm5277.common.ExcsThrowPoint;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.common.VarType;

public class TargetInfoBuilder {

	
	private	List<ExcsThrowPoint>	excsThrowPoints			= new ArrayList<>();
	
	public ExcsThrowPoint addExcsThrowPoint(CodeGenerator cg, SourcePosition sp, String signature) {
		int id = excsThrowPoints.size()+1;
		ExcsThrowPoint result = new ExcsThrowPoint(id, sp, signature);
		excsThrowPoints.add(result);
		return result;
	}

	public void chooseExcsThrowPointContainer() {
		for(ExcsThrowPoint point : excsThrowPoints) {
			point.chooseContainer(128>excsThrowPoints.size());
		}
	}
	
	public int getExcsThrowPointQuantity() {
		return excsThrowPoints.size();
	}
	
	public void write(FileWriter fw) throws IOException {
		fw.write("[BUILTIN_TYPES]\n");
		fw.write("0 Object\n");
		fw.write(VarType.VOID.getId() + " " + VarType.VOID.getName() + "\n");
		fw.write(VarType.BOOL.getId() + " " + VarType.BOOL.getName() + "\n");
		fw.write(VarType.BYTE.getId() + " " + VarType.BYTE.getName() + "\n");
		fw.write(VarType.SHORT.getId() + " " + VarType.SHORT.getName() + "\n");
		fw.write(VarType.INT.getId() + " " + VarType.INT.getName() + "\n");
		fw.write(VarType.FIXED.getId() + " " + VarType.FIXED.getName() + "\n");
		fw.write(VarType.CSTR.getId() + " " + VarType.CSTR.getName() + "\n");
		fw.write(VarType.NULL.getId() + " " + VarType.NULL.getName() + "\n");
		fw.write("\n");
		
		fw.write("[EXCEPTION_TYPES]\n");
		for(int i=0; i<VarType.getExceptionTypes().size(); i++) {
			fw.write(Integer.toString(i));
			fw.write(" ");
			fw.write(VarType.getExceptionTypes().get(i));
			Integer parentId = VarType.getExceptionParent(i);
			if(null!=parentId) {
				fw.write(" : ");
				fw.write(VarType.getExceptionTypes().get(parentId));
			}
			fw.write("\n");
		}
		fw.write("\n");

		fw.write("[CLASS_TYPES]\n");
		List<VarType> list = new ArrayList<>(VarType.getClassTypes().values());
		Collections.sort(list, new Comparator<VarType>() {
			@Override
			public int compare(VarType o1, VarType o2) {
				return Integer.compare(o1.getId(), o2.getId());
			}
		});
		for(VarType type : list) {
			if(0!=type.getId()) {
				fw.write(Integer.toString(type.getId()));
				fw.write(" ");
				fw.write(type.getClassName());
				fw.write("\n");
			}
		}
		fw.write("\n");
		
		fw.write("[THROW_POINTS]\n");
		for(int i=0; i<excsThrowPoints.size(); i++) {
			ExcsThrowPoint point = excsThrowPoints.get(i);
			fw.write(point.toString());
			fw.write("\n");
		}
		fw.write("\n");
		fw.flush();
		fw.close();
	}
}
