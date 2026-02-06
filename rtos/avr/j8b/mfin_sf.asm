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
	SBRS ZL,0x07											;Обнуляем ZH, если он не был передан, не использовался
	CLR ZH
	LSL ZL													;Восстанавливаем нормальное 15 битное значение(без флага)
	LSR ZH
	ROR ZL
	ADIW ZL,0x04											;+Y(2B)+ADDR(2B)
	ADD ZL,YL												;Нахожу адрес стека до вызова и записи аргументов
	ADC ZH,YH												

	POP YH													;Восстанавливаем Y
	POP YL
	CLI														;Запрет прерываний из-за _SPL/_H и работы со стеком
	POP _SPH												;Восстанавливаем адрес возврата
	POP _SPL
	STS SPL,ZL												;Восстанавливаю стек
	STS SPH,ZH
	POP ZH													;Восстанавливаю Z
	POP ZL
	PUSH _SPL												;Помещаю адрес возврата в стек
	PUSH _SPH
	SEI														;Разрешаю прерывания (верхний уровень не может блокировать прерывания, они всегда резрешены)
	RET
.ENDIF
