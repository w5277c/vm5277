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

public enum RtosFeature {
	// Разрешающие функционал обработки прерываний
	OS_FT_IRQ_INT_BLOCK,
	OS_FT_IRQ_PCINT_BLOCK,
	OS_FT_IRQ_WD_BLOCK,
	OS_FT_IRQ_TIMER_BLOCK,
	OS_FT_IRQ_TIMER_CAPTURE_BLOCK,
	OS_FT_IRQ_SPI_BLOCK,
	OS_FT_IRQ_UART_BLOCK,
	OS_FT_IRQ_ADC_BLOCK,
	OS_FT_IRQ_EEPROM_BLOCK,
	OS_FT_IRQ_ANALOG_CMP_BLOCK,
	OS_FT_IRQ_TWI_BLOCK,
	OS_FT_IRQ_SPM_BLOCK,
	OS_FT_IRQ_PTC_BLOCK,
	OS_FT_IRQ_USI_BLOCK,
	
	OS_FT_STDOUT,
	OS_FT_STDIN,
	OS_FT_BLDR_API_REUSE,
	OS_FT_SOFT_RESET,
	OS_FT_WELCOME,
	OS_FT_HEARTBEAT,
	OS_FT_DIAG,
	OS_FT_TIMER,
	OS_FT_DRAM,
	OS_FT_MULTITHREADING,
	OS_ARRAY_1D,
	OS_ARRAY_2D,
	OS_ARRAY_3D,
	OS_FT_ETRACE,
	OS_FT_PRND,
	OS_FT_ARRVIEW,
	OS_FT_STIMER,
	OS_FT_VT100,
	
	// Фичи отключающие проверки (оптимизация в релизе)
	OS_FT_GPIO_PIN_NOCHECK,
	OS_FT_ARRAY_INDEX_NOCHECK,
	OS_FT_MATH_OVF_NOCHECK;
}
