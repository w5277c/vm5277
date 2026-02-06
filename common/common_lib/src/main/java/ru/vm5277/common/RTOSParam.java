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
package ru.vm5277.common;

// При изменение учитывать runtime/rtos/RTOSParam.j8b!

public enum RTOSParam { 
	// Частота в МГц процессора, содержит переданное значение из параметра запуска компилятора
	CORE_FREQ,
	
	// Тип микроконтроллера, содержит переданное значение из параметра запуска компилятора
	MCU,

	// Режим в котором должен присутствовать на чипе бутлоадер vm5277. 
	// Автоматически включаются фичи: STDIN, STDOUT.
	// Некоторые функции RTOS будут вызываться из бутлоадера.
	// Может быть передан как параметр запуска компилятора
	DEVEL_MODE,
	
	// Задаем порт для ввода/вывода (может юыть указан как один пин или как два пина, пример: PC4 или PD0/PD1
	STDIO_PORT,
	
	// Указываем RTOS порт для индикации активности
	ACTLED_PORT,
	
	// Сообщаем RTOS о необходимости вывода приветствия при старте
	SHOW_WELCOME,
	
	// Сообщаем RTOS о режиме многопоточности
	MULTITHREADING,
	
	// Выдерживание паузы перед выполнением основной программы
	INIT_DELAY,
	
	// Алгоритм поведения при завершении программы
	HALT_OK_MODE,
	
	// Алгоритм поведения при необработанном исключении
	HALT_ERR_MODE,
	
	// Размер точки трассировки (7, 15 бит)
	ETRACE_POINT_BITSIZE;
}
