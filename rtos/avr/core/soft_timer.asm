/*
 * Copyright 2026 konstantin@5277.ru
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

.IF OS_FT_STIMER==0x01
.IFNDEF OS_SOFT_TIMER_START_NR

/* Смещения
	HANDLER_ADDR	= 0x0000;2B-адрес обработчика (0x0000-не имеет обработчика)
	HEAP_ADDR		= 0x0002;2B-aдрес HEAP или выделенного блока памяти
	CNTR			= 0x0004;2B-счетчик порога срабатывания
	PERIOD			= 0x0006;2B-значение для полного периода
	RES_ID			= 0x0008;1B-значение заданное в коде верхнего уровня - когда в одном экземпляре несколько таймеров
*/
;-----------------------------------------------------------
OS_SOFT_TIMER_START_NR:
;-----------------------------------------------------------
;IN; X-адрес на HEAP,ACCUM_EH-SN таймера,ACCUM_EL-resId,
;ACCUM_L/H-period
;MOD: ACCUM_L/H/EL/EH
;-----------------------------------------------------------
	PUSH_Z
	PUSH_Y

	RCALL _OS_SOFT_TIMER__GET_OFFSET

	STD Z+_OS_STIMER_TABLE__OFFSET_HEAP_ADDR+0x00,XL		;Заполняем запись
	STD Z+_OS_STIMER_TABLE__OFFSET_HEAP_ADDR+0x01,XH
	STD Z+_OS_STIMER_TABLE__OFFSET_CNTR+0x00,ACCUM_L
	STD Z+_OS_STIMER_TABLE__OFFSET_CNTR+0x01,ACCUM_H
	STD Z+_OS_STIMER_TABLE__OFFSET_PERIOD+0x00,ACCUM_L
	STD Z+_OS_STIMER_TABLE__OFFSET_PERIOD+0x01,ACCUM_H
	STD Z+_OS_STIMER_TABLE__OFFSET_RES_ID,ACCUM_EL

	;TODO вынести в отдельную функцию и применить также для J8BPROC_INVOKE_METHOD_NR
	PUSH_Z
	MOVW YL,XL
	LDD ZL,Y+_OS_CLASS_HEAP_OFFSET__METADATA_ADDR+0x00		;Загружаем из HEAP адрес метаданных
	LDD ZH,Y+_OS_CLASS_HEAP_OFFSET__METADATA_ADDR+0x01
	ADIW ZL,0x01
	LPM ACCUM_EH,Z+											;Получаем количество пар(ид интерфейс + кол-во методов)

	MOV YL,ACCUM_EH											;Вычисляем адрес блока адресов методов
	LDI YH,0x00
	LSL YL
	ROL YH
	ADD YL,ZL
	ADC YH,ZH

	LDI ACCUM_H,0x00
	LDI ACCUM_L,0x00
_OS_SOFT_TIMER_START__LOOP:
	LPM ACCUM_EL,Z+											;Получаем id интерфейса
	CPI ACCUM_EL,OS_CALLBACK_IFACE_ID
	BREQ _OS_SOFT_TIMER_START__GOT_IFACE
	LPM ACCUM_EL,Z+											;Получаем количество методов в интерфейсе
	ADD ACCUM_L,ACCUM_EL
	ADC ACCUM_H,C0x00
	DEC ACCUM_EH
	BRNE _OS_SOFT_TIMER_START__LOOP
_OS_SOFT_TIMER_START__GOT_IFACE:
	LSL ACCUM_L												;Смещаемся на адрес с учетом порядкового номера метода в классе
	ROL ACCUM_H
	ADD YL,ACCUM_L
	ADC YH,ACCUM_H
	MOVW ZL,YL
	LPM ACCUM_L,Z+											;Загружаем адрес метода
	LPM ACCUM_H,Z
	POP_Z
	CLI
	STD Z+_OS_STIMER_TABLE__OFFSET_HANDLER_ADDR+0x00,ACCUM_L
	STD Z+_OS_STIMER_TABLE__OFFSET_HANDLER_ADDR+0x01,ACCUM_H
	SEI

	POP_Y
	POP_Z
	RET


;-----------------------------------------------------------
OS_SOFT_TIMER_STOP_NR:
;-----------------------------------------------------------
;IN; Y-адрес на HEAP,ACCUM_EH-SN таймера
;MOD: ACCUM_L/H/EL/EH
;-----------------------------------------------------------
	PUSH_Z

	RCALL _OS_SOFT_TIMER__GET_OFFSET
	CLI
	STD Z+_OS_STIMER_TABLE__OFFSET_HEAP_ADDR+0x00,C0x00
	STD Z+_OS_STIMER_TABLE__OFFSET_HEAP_ADDR+0x01,C0x00
	SEI

	POP_Z
	RET

;-----------------------------------------------------------
_OS_SOFT_TIMER__GET_OFFSET:
;-----------------------------------------------------------
;IN; ACCUM_EH-SN таймера
;MOD: ACCUM_L/H/EL/EH
;OUT: Z-адрес на запись в таблице таймеров
;-----------------------------------------------------------
	MOV ZL,ACCUM_EH											;Вычисляем адрес записи (*9)
	LDI ZH,0X00
	LSL ZL
	ROL ZH
	LSL ZL
	ROL ZH
	LSL ZL
	ROL ZH
	ADD ZL,ACCUM_EH
	ADC ZH,C0x00
	SUBI ZL,low(-_OS_STIMER_TABLE)
	SBCI ZH,high(-_OS_STIMER_TABLE)
	RET

.ENDIF
.ENDIF