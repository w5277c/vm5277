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

;Структура данных в стеке
;2Б	-регистровая пара Z										;адрес Y перед ними
;xБ	-аргументы
;2Б	-адрес возврата
;2Б	-регистровая пара Y
;xБ	-локальные переменные=стек фрейм


.IFNDEF J8BPROC_MFIN_SF
;-----------------------------------------------------------
J8BPROC_MFIN_SF:
;-----------------------------------------------------------
;Завершает работу метода
;IN: ZL(ZL,ZH)-флаг(7b-используем ZH) и размер аргументов,
;Y-адрес стека(после регистровой пары Z)
;-----------------------------------------------------------
	CLI														;Восстанавливаю адрес стека
	STS SPL,YL
	STS SPH,YH
	SEI
	SBRC ZL,0x07											;Обнуляем ZH, если он не был передан, не использовался
	CLR ZH
	LSL ZL													;Восстанавливаем нормальное 15 битное значение(без флага)
	LSR ZH
	ROL ZL
	SUB YL,ZL												;Перемещаемся на адрес возврата
	SBC YH,ZH
	POP ZH													;Восстанавливаем Z
	POP ZL
	LDD J8B_ATOM,Y+0x00										;Восстанавливаю точку выхода и размещаю ее в стеке
	PUSH J8B_ATOM
	LDD J8B_ATOM,Y+0x01
	PUSH J8B_ATOM
	LDD J8B_ATOM,Y+0x02										
	LDD YL,Y+0x03
	MOV YH,J8B_ATOM
	RET
.ENDIF
