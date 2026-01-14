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

.IFNDEF DEF_BOOTLADER_SECURE
.SET DEF_BOOTLADER_SECURE = 1

.WARNING "DEMO protection (XOR) - replace with a real algorithm!"

;Данная реализация приведена только в качестве примера.
;Использовать её в реальных проектах не рекомендуется из-за низкой
;стойкости защиты: ключ накладывается прямо, без перемешивания,
;что позволяет легко его подобрать.
;Также необходима проверка целостности страницы (XORSUM,CRC...)
;Неиспользуемые области данных также стоит заполнять произвольными
;числами для усложнения анализа кода.

;ВАЖНО: Данный код демонстрирует только КОНЦЕПЦИЮ реализации и не является готовым решением.
;Поскольку защита прошивки требует уникальности в подходе для разных проектов, каждый разработчик
;должен самостоятельно реализовать собственный алгоритм шифрования и проверки целостности.
;Этот пример служит лишь отправной точкой для понимания общей идеи защиты загрузчика.

;-----------------------------------------------------------
.MACRO _BLDR_SECURE
;-----------------------------------------------------------
;Процедура выполняет расшифровку зашифрованной FLASH
;страницы
;IN: Y-адрес начала блока данных FLASH страницы в RAM,
;ACCUM-регистр с номером страницы, размер страницы задан
;константой FLASH_PAGESIZE(байты)
;-----------------------------------------------------------
	LDI ZL,low(KEY1*2+0x20)									;KEY1-начальный адрес 32 байтного ключа в FLASH
	LDI ZH,high(KEY1*2+0x20)
	LDI TEMP_EL,FLASH_PAGESIZE
_BLDR__SECURE__LOOP:
	LDI TEMP_L,TEMP_EL
	ANDI TEMP_L,0b00011111
	BRNE PC+0x02
	SBIW ZL,0x20
	LD TEMP_L,Y
	LPM TEMP_H,Z+
	EOR TEMP_L,TEMP_H
	ST Y+,TEMP_L
	DEC TEMP_EL
	BRNE _BLDR__SECURE__LOOP
.ENDMACRO
.ENDIF
