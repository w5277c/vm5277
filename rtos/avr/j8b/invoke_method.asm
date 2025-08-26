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

.IFNDEF J8BPROC_INVOKE_METHOD_NR
;-----------------------------------------------------------
J8BPROC_INVOKE_METHOD_NR:									;NR-NO_RESTORE - не восстанавливаю регистры
;-----------------------------------------------------------
;Переходим на код метода
;IN: Z-адрес HEAP,ACCUM_L-ид интерфейса, ACCUM_H-порядковый
;номер метода в интерфейсе
;-----------------------------------------------------------
	PUSH_Z

	ADIW ZL,0x03
	LD ACCUM_EH,Z+
	LD ZH,Z
	MOV ZL,ACCUM_EH
	ADIW ZL,0x01											;Пропускаем ид типа класса
	LPM ACCUM_EH,Z+											;Получаем количество пар(ид интерфейс + кол-во методов)

	MOV YL,ACCUM_EH											;Вычисляю адрес блока адресов методов
	LDI YH,0x00
	LSL YL
	ROL YH
	ADD YL,ZL
	ADC YH,ZH

	LDI TEMP_L,0x00											;Порядковый номер метода в классе
_J8BPROC_INVOKE_METHOD_NR__IFACEIDS_LOOP:
	LPM ACCUM_EL,Z+											;Получаю id интерфейса
	CP ACCUM_EL,ACCUM_L
	BREQ _J8BPROC_INVOKE_METHOD_NR__GOT_IFACE
	LPM ACCUM_EL,Z+											;Получаю количество методов в интерфейсе
	ADD ACCUM_H,ACCUM_EL
	DEC ACCUM_EH
	BRNE _J8BPROC_INVOKE_METHOD_NR__IFACEIDS_LOOP
_J8BPROC_INVOKE_METHOD_NR__GOT_IFACE:
	POP_Z

	LSL ACCUM_H												;Смещаюсь на адрес с учетом порядкового номера метода в классе
	ADD YL,ACCUM_H
	ADC YH,C0x00
	PUSH_Y
	RET
.ENDIF

