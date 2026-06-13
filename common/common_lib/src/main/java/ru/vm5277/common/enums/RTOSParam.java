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
package ru.vm5277.common.enums;

import java.util.HashMap;
import java.util.Map;


// При изменение учитывать runtime/rtos/RTOSParam.j8b!

public enum RTOSParam { 
	// Частота в КГц процессора, содержит переданное значение из параметра запуска компилятора - int
	CORE_FREQ(0, true, "CPU core frequency in KHz (e.g., 8000, 16000). Set automatically from -F compiler argument"),
	
	// Тип микроконтроллера, содержит переданное значение из параметра запуска компилятора - string
	MCU(1, true, "Target MCU model (e.g., atmega328p, attiny85). Set automatically from compiler argument"),

	// Подключает код программного вызова бутлодера - bool
	SOFT_RESET(2, false, "Include software reset routine to jump to bootloader from application code (0=off, 1=on)"),
	
	// Переиспользование функций бутлоадера - bool
	BLDR_API_REUSE(3, false, "Reuse bootloader API functions in application to save flash space (0=off, 1=on)"),
	
	// Задаем порт для ввода/вывода (может быть указан как один пин или как два пина, пример: PC4 или PD0/PD1 - string
	STDIO_PORT(4, false, "STDIO port pin(s): single pin for half-duplex (e.g., PC4) or TX/RX pair (e.g., PD0/PD1)"),
	
	// Задаем RTOS GPIO порт для индикации активности - string
	ACTLED_PORT(5, false, "Activity LED port pin. Blinks to indicate RTOS heartbeat (e.g., PB5)"),
	
	// Сообщаем RTOS о необходимости вывода приветствия при старте - bool
	SHOW_WELCOME(6, false, "Print system banner on boot (0=off, 1=on)"),
	
	// Сообщаем RTOS о режиме многопоточности
	MULTITHREADING(7, true, "Enable multi-threading (0=single-thread, 1=multi-thread)"),
	
	// Выдерживание паузы перед выполнением основной программы
	INIT_DELAY(8, false, "Initial delay before main() in milliseconds"),
	
	// Задаем поведение при завершении программы
	HALT_OK_MODE(9, false, "Behavior on normal program completion (can require ACTLED_PORT)"),
	
	// Задаем поведение при необработанном исключении
	HALT_ERR_MODE(10, false, "Behavior on unhandled exception (can require ACTLED_PORT)"),
	
	// Размер точки трассировки (7, 15 бит)
	ETRACE_POINT_BITSIZE(11, true, "Exception trace point bit size: 7 or 15 bits"),
	
	DIAG_MODE(12, false, "RTOS diagnostic mode");
	
	private static final Map<Integer, RTOSParam> ids = new HashMap<>();
	static {
		for (RTOSParam type : RTOSParam.values()) {
			ids.put(type.getId(), type);
		}
	}

	private	int	id;
	private	boolean	hidden;
	private	String	description;

	private RTOSParam(int id, boolean hiden, String description) {
		this.id = id;
		this.hidden = hiden;
		this.description = description;
	}

	public int getId() {
		return id;
	}
	
	public boolean isHidden() {
		return hidden;
	}

	public static RTOSParam fromInt(int id) {
		return ids.get(id);
	}
	
	public static RTOSParam fromName(String name) {
		try {
			return valueOf(name.toUpperCase());
		}
		catch(Exception ex) {
			return null;
		}
	}
	
	@Override
	public String toString() {
		return name().toUpperCase() + " - " + description;
	}

}
