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

//TODO добавить проерку на переданный адрес метода вместо ссылки

.IFNDEF J8BPROC_CALLBACK_ADDR
;-----------------------------------------------------------
J8BPROC_CALLBACK_ADDR:
;-----------------------------------------------------------
;IN; Y-адрес на HEAP или адрес метода callback
;OUT:ACCUM_EL/EH-адрес метода callback
;-----------------------------------------------------------
// Проверяем специальный бит определяющий тип данных
.IF FLASH_SIZE<=0x8000 && SRAM_SIZE<=0x8000
	SBRS YH,0x07
	RJMP J8BPROC_CALLBACK_ADDR__CONTINUE
	MOVW ACCUM_EL,YL
	ANDI ACCUM_EH,0x7f										;Если адрес метода - выключаем бит
	LSR ACCUM_EH
	ROR ACCUM_EL
	LDI YL,0x00
	LDI YH,0x00
	RET
.ELIF FLASH_SIZE<=0x800000 && SRAM_SIZE<=0x800000
	.ERROR "Only callback bit position 15 is currently supported"
.ENDIF
J8BPROC_CALLBACK_ADDR__CONTINUE:
	PUSH_Z
	PUSH_Y
	PUSH_A16

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
J8BPROC_CALLBACK_ADDR__LOOP:
	LPM ACCUM_EL,Z+											;Получаем id интерфейса
	CPI ACCUM_EL,OS_CALLBACK_IFACE_ID
	BREQ J8BPROC_CALLBACK_ADDR__GOT_IFACE
	LPM ACCUM_EL,Z+											;Получаем количество методов в интерфейсе
	ADD ACCUM_L,ACCUM_EL
	ADC ACCUM_H,C0x00
	DEC ACCUM_EH
	BRNE J8BPROC_CALLBACK_ADDR__LOOP
	LDI ACCUM_EL,0x00
	LDI ACCUM_EH,0x00
	RJMP J8BPROC_CALLBACK_ADDR__END							;Приведет к сбросу, если по какой-то причине методав списке не будет (но компилятор не должен допускать такую ситуацию)
J8BPROC_CALLBACK_ADDR__GOT_IFACE:
	LSL ACCUM_L												;Смещаемся на адрес с учетом порядкового номера метода в классе
	ROL ACCUM_H
	ADD YL,ACCUM_L
	ADC YH,ACCUM_H
	MOVW ZL,YL
	LPM ACCUM_EL,Z+											;Загружаем адрес метода
	LPM ACCUM_EH,Z
J8BPROC_CALLBACK_ADDR__END:
	POP_A16
	POP_Y
	POP_Z
	RET
.ENDIF