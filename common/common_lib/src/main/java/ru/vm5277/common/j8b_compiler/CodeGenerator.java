/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
18.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.common.j8b_compiler;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ru.vm5277.common.NativeBinding;
import ru.vm5277.common.Operator;
import ru.vm5277.common.RTOSFeature;
import ru.vm5277.common.SystemParam;

public abstract class CodeGenerator {
	private	int	idCntr	= 0;
	
	protected	final	String						genName;
	protected	final	Set<RTOSFeature>			RTOSFeatures	= new HashSet<>();
	protected	final	Set<String>					includes		= new HashSet<>();
	protected	final	Map<String, NativeBinding>	nbMap;
	protected	final	Map<SystemParam, Object>	params;
	protected			StringBuilder				asmSource		= new StringBuilder();
	
	public CodeGenerator(String genName, Map<String, NativeBinding> nbMap, Map<SystemParam, Object> params) {
		this.genName = genName;
		this.nbMap = nbMap;
		this.params = params;
	}
	
	public int genId() {
		return idCntr++;
	}

	public abstract int enterClass(int typeId, int[] intrerfaceIds);	//тип 
	public abstract int enterFiled(int typeId, String name);
	public abstract int enterConstructor(int[] typeIds);
	public abstract int enterMethod(int typeId, int[] typeIds);
	public abstract int enterLocal(int typeId, String name); //TODO сделать индексацию вместо имен
	public abstract int enterBlock();
	public abstract void leave();
	
	public abstract void setAcc(Operand src);
	public abstract Operand getAcc();
	public abstract void loadAcc(int srcId); //Загрузить переменную в аккумулятор
	public abstract void storeAcc(int srcId); //Записать аккумулятор в переменную
	
	public abstract void invokeMethod(int id, int typeId, Operand[] args);
	public abstract void invokeNative(String methodQName, int typeId, Operand[] parameters) throws Exception; 
	public abstract Operand emitInstanceof(Operand op, int typeId);	//todo может быть поросто boolean?
	public abstract void emitUnary(Operator op); //PLUS, MINUS, BIT_NOT, NOT, PRE_INC, PRE_DEC, POST_INC, POST_DEC
	
	//TODO набор методов для реализации команд if, switch, for, loop и .т.д
	public abstract void eNew(int typeId, Operand[] parameters, boolean canThrow);
	public abstract void eFree(Operand op);
	
	public abstract void eIf(int conditionBlockId, int thenBlockId, Integer elseBlockId);
	public abstract void eTry(int blockId, List<Case> cases, Integer defaultBlockId);
	public abstract void eWhile(int conditionBlockId, int bodyBlockId);
	public abstract void eReturn();
	public abstract void eThrow();
	
	public abstract String getVersion();

	public void postBuild() {
		StringBuilder asmHeaders = new StringBuilder();
		asmHeaders.append("# vm5277.").append(genName).append(" v").append(getVersion()).append(" at ").append(new Date().toString()).append("\n");
		asmHeaders.append("\n");
		if(params.keySet().contains(SystemParam.CORE_FREQ)) {
			asmHeaders.append(".equ core_freq = ").append(params.get(SystemParam.CORE_FREQ)).append("\n");
		}
		if(params.keySet().contains(SystemParam.STDOUT_PORT)) {
			asmHeaders.append(".equ stdout_port = ").append(params.get(SystemParam.STDOUT_PORT)).append("\n");

		}
		asmHeaders.append("\n");
		
		for(RTOSFeature feature : RTOSFeatures) {
			asmHeaders.append(".set ").append(feature).append(" = 1\n");
		}
		asmHeaders.append("\n");
		
		asmHeaders.append(".include \"devices/").append(params.get(SystemParam.MCU)).append(".def").append("\"\n");
		asmHeaders.append(".include \"core/core.asm\"\n");		
		
		for(String include : includes) {
			asmHeaders.append(".include \"").append(include).append("\"\n");
		}
		asmHeaders.append("\n");

		asmHeaders.append("main:\n");
		asmSource = asmHeaders.append(asmSource.toString()).append("\n");
	}
	
	public String getAsm() {
		return asmSource.toString();
	}
}