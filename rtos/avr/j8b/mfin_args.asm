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

;Структура данных в стеке
;2Б	-регистровая пара Z										;адрес Y перед ними
;xБ	-аргументы												; - неизвестная длина 1
;2Б	-адрес возврата

.IFNDEF J8BPROC_MFIN_ARGS
;-----------------------------------------------------------
J8BPROC_MFIN_ARGS:
;-----------------------------------------------------------
;Завершает работу метода (в стеке аргументы без переменных)
;IN: ZL(ZL,ZH)-флаг(7b-используем ZH) и размер аргументов,
;-----------------------------------------------------------
	SBRS ZL,0x07											;Обнуляем ZH, если он не был передан, не использовался
	CLR ZH
	LSL ZL													;Восстанавливаем нормальное 15 битное значение(без флага)
	LSR ZH
	ROR ZL

	CLI														;Запрет прерываний из-за _SPL/_H и работы со стеком
	POP ATOM_H												;Восстанавливаем точку возврата
	POP ATOM_L

	LDS _SPL,SPL
.IFDEF SPH
	LDS _SPH,SPH
.ENDIF
	ADD _SPL,ZL
.IFDEF SPH
	ADC _SPH,ZH
.ENDIF
	STS SPL,_SPL												;Восстанавливаю стек
.IFDEF SPH
	STS SPH,_SPH
.ENDIF
	POP ZH													;Восстанавливаем Z (порядок как в аргументах)
	POP ZL
	PUSH ATOM_L												;Помещаем адрес возврата
	PUSH ATOM_H
	SEI
	RET														;Возврат
.ENDIF
