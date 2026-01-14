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

;Протокол бутлоадера
;+----------------------------------------+
;|0x00:0x01-MAGIC=0x77                    |
;+----------------------------------------+
;|0x01:0x01-REQUEST/RESPONSE ID           |
;+----------------------------------------+
;|0x03:0x02-DATA SIZE                     |
;+----------------------------------------+
;|0x05:0x??-DATA                          |
;+----------------------------------------+
;|0x??:0x01-XOR SUM                       |
;+----------------------------------------+

;Запросы  (от хоста)
	.EQU	REQ_BLDR_INFO							= 0x00	;Запрос информации об устройстве
	.EQU	REQ_BLDR_GOTO_PROGRAM					= 0x01	;Переход на основную программу
	.EQU	REQ_BLDR_PAGE_WRITE						= 0x12	;Запись FLASH страницы
;Ответы (от бутлоадера)
	.EQU	RESP_BLDR_OK							= 0x80	;Успешное выполнение
	.EQU	RESP_BLDR_WRONG_REQUEST					= 0x88	;Ошибка в данных запроса
	.EQU	RESP_BLDR_WRONG_PAGE					= 0x89	;Не корректный номер страницы

;Тело запроса REQ_BLDR_INFO (DATA_SIZE:0x13)
;+----------------------------------------------+
;|0x05:0x01-BLDR VERSION (7,6-type,5-0-version) |			;00-стандартный бутлоадер(atmega), 01-сокращенный(tiny)
;+----------------------------------------------+
;|0x06:0x01-PLATFORM TYPE ID                    |
;+----------------------------------------------+
;|0x07:0x04-MCU SIGNATURE                       |
;+----------------------------------------------+
;|0x0b:0x08-MCU UID                             |
;+----------------------------------------------+

;Тело запроса REQ_BLDR_PAGE_WRITE(DATA_SIZE:0x02+PAGE_SIZE)
;+----------------------------------------+
;|0x05:0x02-FLASH ADDR (in bytes)         |
;+----------------------------------------+
;|0x07:0x??-PAGE DATA                     |
;+----------------------------------------+

;Остальные запросы тел не имеют

	.DEF	COK										= r3;
	.DEF	CMAGIC									= r4;

	.EQU	BLDR_VERSION							= 0b01<<0x06 + 0x00&0x3f
	.EQU	BLDR_START_WADDR						= BOOT_512W_ADDR
	.EQU	_BLDR_BUFFER_SIZE						= 0x08 + FLASH_PAGESIZE	;Максимально допустимый размер запроса
	.EQU	_BLDR_PROTOCOL_HEADER_SIZE				= 0x05
	


.ORG BOOT_256W_ADDR
BLDR_START:
	CLI
	RJMP _BLDR_START__DATASKIP
BLDR_START__DATA:
	.db	BLDR_VERSION,PLATFORM_ID							;Версия загрузчика и ид типа платформы
	.dd	DEVICE_SIGNATURE									;Сигнатура чипа
	.dw V_UID												;Уникальный идентификатор чипа (2B-vendor, 6B-device)
	.dw D_UID>>32
	.dd	D_UID&0xffffffff
_BLDR_START__DATASKIP:
.IFDEF SPH
	LDI ACCUM_L,high(SRAM_START+SRAM_SIZE-0x01)				;Инициализация стека
	STS SPH,ACCUM_L
.ENDIF
	LDI ACCUM_L,low(SRAM_START+SRAM_SIZE-0x01)
	STS SPL,ACCUM_L

	EOR C0x00,C0x00
	LDI ACCUM_L,RESP_BLDR_OK
	MOV COK,ACCUM_L
	LDI ACCUM_L,0x77
	MOV CMAGIC,ACCUM_L

	LDI ZH,0x00												;Проверка наличия прошивки программы, если 0xffff то прошивка не обнаружена
	LDI ZL,0x00
	LPM TEMP_L,Z+
	CPI TEMP_L,0xff
	BRNE _BLDR_START__PROGRAMM_DETECTED
	LPM TEMP_L,Z
	CPI TEMP_L,0xff
	BREQ _BLDR_START__LOOP									;Если прошивки нет, то переходим сразу в режим бутлоадера
_BLDR_START__PROGRAMM_DETECTED:

	CBI BLDR_UART_DDR,BLDR_UART_PINNUM						;Гарантируем режим входа на пине
															;Выполняем проверку низкого уровня на пине (должен быть подтянут к VCC через внешний резистор)
	LDI TEMP_EL,12											;Где-то 1с для 4МГц и 200мс для 20Мгц (необходимо проверить)
	LDI TEMP_H,53											;200мс нужно чтобы не пропустить код нажатой кнопки к склавиатуры хоста
	LDI TEMP_L,0
_BLDR_START__LOWLEVELDETECTOR_LOOP:
	SBIS BLDR_UART_PIN,BLDR_UART_PINNUM
	RJMP _BLDR_START__LOOP									;Переход на бутлоадер, если обнаружен низкий уровень
	SUBI TEMP_L,0x01
	SBCI TEMP_H,0x00
	SBCI TEMP_EL,0x00
	BRCC _BLDR_START__LOWLEVELDETECTOR_LOOP
_BLDR_START__GOTO_PROGRAM:
	LDI ZH,0x00
	LDI ZL,0x00
	IJMP													;Переход на программу, если низкий уровень не был обнаружен

_BLDR_START__LOOP:
	RCALL BLDR_UART_RECV_NR
	CPI ZH,0xff
	BREQ _BLDR_START__LOOP
	CPI TEMP_H,0x00
	BRNE _BLDR_START__LOOP
	CPI ZH,0x00
	BRNE PC+0x03
	CPI ZL,_BLDR_PROTOCOL_HEADER_SIZE+0x01
	BRCC _BLDR_START__LOOP
	LDI YH,high(SRAM_START)
	LDI YL,low(SRAM_START)
	LD ACCUM_L,Y
	CP ACCUM_L,CMAGIC
	BRNE _BLDR_START__LOOP
	
	SBIW ZL,0x01											;Вычитаем байт XORSUM
	
	LDI YH,high(SRAM_START)
	LDI YL,low(SRAM_START)
	LDD ACCUM_L,Y+0x01
	
	CPI ACCUM_L,REQ_BLDR_INFO
	BRNE _BLDR_START__LOOP_NOT_BLDR_INFO
;---INFO----------------------------------------------------
	CPI ZL,_BLDR_PROTOCOL_HEADER_SIZE-0x01
	BRNE PC+0x04
	LDI ACCUM_L,RESP_BLDR_WRONG_REQUEST
	RCALL _BLDR_ANSWER
	RJMP _BLDR_START__LOOP
	
	ST Y+,CMAGIC
	ST Y+,COK
	ST Y+,C0x00
	LDI ACCUM_L,0x13
	ST Y+,ACCUM_L
	LDI TEMP_L,0x13
	LDI ZH,high(BLDR_START__DATA*2)
	LDI ZL,low(BLDR_START__DATA*2)
_BLDR_START__LOOP_BLDR_INFO_LOOP:
	LPM ACCUM_L,Z+
	ST Y+,ACCUM_L
	DEC TEMP_L
	BRNE _BLDR_START__LOOP_BLDR_INFO_LOOP
	LDI XH,high(_BLDR_PROTOCOL_HEADER_SIZE-0x01+0x13)
	LDI XL,low(_BLDR_PROTOCOL_HEADER_SIZE-0x01+0x13)
	RCALL BLDR_UART_SEND_NR
	RJMP _BLDR_START__LOOP
_BLDR_START__LOOP_NOT_BLDR_INFO:
	CPI ACCUM_L,REQ_BLDR_GOTO_PROGRAM
	BRNE _BLDR_START__LOOP_NOT_BLDR_GOTO_PROGRAM
;---GOTO-PROGRAM--------------------------------------------
	CPI ZL,_BLDR_PROTOCOL_HEADER_SIZE-0x01
	BRNE PC+0x04
	LDI ACCUM_L,RESP_BLDR_WRONG_REQUEST
	RCALL _BLDR_ANSWER
	RJMP _BLDR_START__LOOP
	LDI ACCUM_L,RESP_BLDR_OK
	RCALL _BLDR_ANSWER
	CBI BLDR_UART_PORT,BLDR_UART_PINNUM						;Из измененного критична только конфигурация пина, возвращаем назад
	CBI BLDR_UART_DDR,BLDR_UART_PINNUM
	RJMP _BLDR_START__GOTO_PROGRAM							;Выполняем переход
_BLDR_START__LOOP_NOT_BLDR_GOTO_PROGRAM:
	CPI ACCUM_L,REQ_BLDR_PAGE_WRITE
	BRNE _BLDR_START__LOOP_NOT_BLDR_PAGE_WRITE
;---PAGE-WRITE----------------------------------------------
	RCALL _BLDR_CHECK_ADDR
	BREQ PC+0x02
	RJMP _BLDR_START__LOOP
	LDI YH,high(SRAM_START+_BLDR_PROTOCOL_HEADER_SIZE-0x01+0x02)
	LDI YL,low(SRAM_START+_BLDR_PROTOCOL_HEADER_SIZE-0x01+0x02)
	RCALL _BLDR_FLASHPAGE_WRITE_NR
	LDI ACCUM_L,RESP_BLDR_OK
	RCALL _BLDR_ANSWER
	RJMP _BLDR_START__LOOP
_BLDR_START__LOOP_NOT_BLDR_PAGE_WRITE:
;---INCORRECT-REQUEST---------------------------------------
	LDI ACCUM_L,RESP_BLDR_WRONG_REQUEST
	RCALL _BLDR_ANSWER
	RJMP _BLDR_START__LOOP

;-----------------------------------------------------------
_BLDR_CHECK_ADDR:
;-----------------------------------------------------------
;Проверка корректности адреса FLASH страницы
;IN;Z-адрес FLASH страницы
;OUT: Flag Z-(1=корректый)
;-----------------------------------------------------------
	RJMP _BLDR_START__LOOP
	CPI ZH,high(FLASH_SIZE-FLASH_PAGESIZE-(BOOT_256W_ADDR*2))
	BRCS _BLDR_CHECK_ADDR__CORRECT
	BRNE PC+0x3
	CPI ZL,low(FLASH_SIZE-FLASH_PAGESIZE-(BOOT_256W_ADDR*2))
	BRCS _BLDR_CHECK_ADDR__CORRECT
	LDI ACCUM_L,RESP_BLDR_WRONG_PAGE
	RCALL _BLDR_ANSWER
	CLZ
	RET
_BLDR_CHECK_ADDR__CORRECT:
	SEZ
	RET

;-----------------------------------------------------------
_BLDR_ANSWER:
;-----------------------------------------------------------
;Заполняем стандартный пакет ответа и отправляем по UART
;OUT: ACCUM_L-код ответа
;-----------------------------------------------------------
	RCALL _BLDR_MAKE_HEADER
	LDI XH,high(_BLDR_PROTOCOL_HEADER_SIZE-0x01)
	LDI XL,low(_BLDR_PROTOCOL_HEADER_SIZE-0x01)
	RJMP BLDR_UART_SEND_NR

;-----------------------------------------------------------
_BLDR_MAKE_HEADER:
;-----------------------------------------------------------
;Заполняем заголовок для ответа
;OUT: ACCUM_L-код ответа
;-----------------------------------------------------------
	ST Y+,CMAGIC
	ST Y+,ACCUM_L
	ST Y+,C0x00
	ST Y+,C0x00
	RET


;===========================================================
;========UART=FUNCTIONS=====================================
;===========================================================

;-----------------------------------------------------------
BLDR_UART_RECV_NR:											;NR-NO_RESTORE - не восстанавливает регистры X,Y,Z, ACCUM_H/L, TEMP_H
;-----------------------------------------------------------
;Слушаем UART-ожидаем данные, принимаем и обрабатываем
;OUT: Z-длина данных, TEMP_H-XORSUM, ZH=0xff-ошибка приема
;-----------------------------------------------------------
	SBI BLDR_UART_PORT,BLDR_UART_PINNUM						;Режим входа для пина UART
	CBI BLDR_UART_DDR,BLDR_UART_PINNUM

	LDI YH,high(SRAM_START)									;Устанавливаем адрес буфера
	LDI YL,low(SRAM_START)
	LDI ZH,0x00												;Длина принятых данных
	LDI ZL,0x00
	LDI TEMP_H,0x00											;Счетчик XOR суммы

BLDR_UART_RECV_NR__WAIT_BYTE:								;Ждем байт
	LDI XH,0xff												;TODO Подобрать паузу в 2 бита UART
	LDI XL,0xff
	RCALL BLDR_UART_RECV_BYTE_NR
	BRNE BLDR_UART_RECV_NR__GOT_BYTE
	CPI ZH,0x00												;Проверка на нулевую длину
	BRNE PC+0x03
	CPI ZL,0x00
	BREQ BLDR_UART_RECV_NR__WAIT_BYTE						;При длине=0 бесконечно ждем старт
	RET
	
BLDR_UART_RECV_NR__GOT_BYTE:
	CPI ZH,0xff												;Признак переполнения буфера
	BREQ BLDR_UART_RECV_NR__WAIT_BYTE						;Повторяем прием в холостую пока не завершится передача

	CPI ZH,high(_BLDR_BUFFER_SIZE)							;Проверка на достижение максимальной длины - игнорируем последующие данные
	BRCS _BLDR_UART_RECV_NR__NEXT							;Переполнения нет - переходим к блоку записи байта и переходе к следующей итерации
	BRNE PC+0x03											;Есть переполнение - переходим к следующей итерации включив признак переполнения и игнорируем запись байта
	CPI XL,low(_BLDR_BUFFER_SIZE)
	BRCS _BLDR_UART_RECV_NR__NEXT
	LDI ZH,0xff
	RJMP BLDR_UART_RECV_NR__WAIT_BYTE

_BLDR_UART_RECV_NR__NEXT:
	EOR TEMP_H,ACCUM_L										;Учитываем XORSUM
	ST Y+,ACCUM_L											;Записываем байт
	ADIW ZL,0x01											;Инкрементируем счетчик длины данных
	RJMP BLDR_UART_RECV_NR__WAIT_BYTE						;Переходим к следующей итерации

;-----------------------------------------------------------;
BLDR_UART_RECV_BYTE_NR:										;NR-NO_RESTORE - не восстанавливает регистры X, ACCUM_H
;-----------------------------------------------------------;
;Прием байта по программному UART
;IN: X-таймаут
;OUT: ACCUM_L-байт, Flag Z=1-ошибка приема
;-----------------------------------------------------------

BLDR_UART_RECV_BYTE_NR__START_LOOP:							;Ждем START
	SBIC BLDR_UART_PIN,BLDR_UART_PINNUM						;Если низкий уровень на пине, значит обнаружен START
	RJMP _BLDR_UART_RECV_BYTE_NR__GOT_START
	SBIW XL,0x01
	BRCC BLDR_UART_RECV_BYTE_NR__START_LOOP
	SEZ
	RET
	
_BLDR_UART_RECV_BYTE_NR__GOT_START:							;Получен START
	LDI XL,0x06												;Пауза ~1/3 бита ;TODO скорректировать на реальном железе
	DEC XL
	BRNE PC-0x01

	LDI XH,0x08												;Цикл в 8 бит (8n0)
_BLDR_UART_RECV_BYTE_NR__BITLOOP:
	RCALL _BLDR_UART_WAIT_NR								;Пауза в 1 бит, также сбрасывает флаг C
	SBIC BLDR_UART_PIN,BLDR_UART_PINNUM						;Передаем бит с UART в флаг C
	SEC
	ROR ACCUM_L												;Записываем флаг в аккумулятор используя битовый сдвиг
	DEC XH													;Декрементируем счетчик бит
	BRNE _BLDR_UART_RECV_BYTE_NR__BITLOOP						;Повторяем для 8 итераций

	RCALL _BLDR_UART_WAIT_NR								;Пауза в 1 бит
	CLZ
	SBIS BLDR_UART_PIN,BLDR_UART_PINNUM						;Проверяем STOP
	SEZ														;STOP не получен
	RET

;-----------------------------------------------------------;
BLDR_UART_SEND_NR:											;NR-NO_RESTORE - не восстанавливает регистры ACCUM_L\H, TEMP_L\H
;-----------------------------------------------------------;
;Передача RAM данных по UART
;IN: X-размер данных без XORSUM (не может быть 0)
;-----------------------------------------------------------
	SBI BLDR_UART_PORT,BLDR_UART_PINNUM						;Режим выхода для UART пина
	SBI BLDR_UART_DDR,BLDR_UART_PINNUM

	LDI YH,high(SRAM_START)
	LDI YL,low(SRAM_START)
	LDI TEMP_H,0x00
BLDR_UART_SEND_NR__LOOP:
	LD ACCUM_L,Y+											;Считываем байт в аккумулятор и инкрементируем индексный регистр
	EOR TEMP_H,ACCUM_L
	RCALL BLDR_UART_SEND_BYTE_NR							;Передаем байт по UART
	SBIW XL,0x01
	BRNE BLDR_UART_SEND_NR__LOOP

	MOV ACCUM_L,TEMP_H
	RCALL BLDR_UART_SEND_BYTE_NR							;Передаем байт XORSUM по UART
	RET

;-----------------------------------------------------------;
BLDR_UART_SEND_BYTE_NR:										;NR-NO_RESTORE - не восстанавливает регистры ACCUM_L\H, TEMP_L
;-----------------------------------------------------------;
;Передача байта по программному UART
;IN: ACCUM_L-байт
;-----------------------------------------------------------
	CBI BLDR_UART_PORT,BLDR_UART_PINNUM						;Выставляем START
	RCALL _BLDR_UART_WAIT_NR

	LDI TEMP_L,0x08											;Цикл передачи байта побитно
BLDR_UART_SEND_BYTE_NR__BITLOOP:
	SBRC ACCUM_L,0x00										;В зависимости от текущего бита задаем на пине высокий или низкий уровень
	SBI BLDR_UART_PORT,BLDR_UART_PINNUM
	SBRS ACCUM_L,0x00
	CBI BLDR_UART_PORT,BLDR_UART_PINNUM
	LSR ACCUM_L												;Сдвигаем биты в аккумуляторе
	
	NOP														;Строго выдерживаем паузу
	RCALL _BLDR_UART_WAIT_NR
	DEC TEMP_L												;Декрементируем счетчик бит
	BRNE BLDR_UART_SEND_BYTE_NR__BITLOOP					;Выполняем все 8 итераций

	SBI BLDR_UART_PORT,BLDR_UART_PINNUM						;Выставляем STOP
	RCALL _BLDR_UART_WAIT_NR
	RET

;-----------------------------------------------------------
_BLDR_UART_WAIT_NR:											;NR-NO_RESTORE - не восстанавливает регистр ACCUM_H
;-----------------------------------------------------------
;Пауза между битами
;-----------------------------------------------------------
	LDI ACCUM_H,0x13										;TODO скорректировать на реальном железе
	SUBI ACCUM_H,0x01
	BRNE PC-0x01
	RET


;===========================================================
;========FLASH=PROCS========================================
;===========================================================

;-----------------------------------------------------------
_BLDR_FLASHPAGE_WRITE_NR:									;NR-NO_RESTORE - не восстанавливает регистр Z,ACCUM_L,ACCUM_H,TEMP_L,r0,r1
;-----------------------------------------------------------
;Блок процедуры записи страницы FLASH
;IN: Y-адрес данных в RAM, Z-адрес страницы во FLASH
;-----------------------------------------------------------
	LDI ACCUM_L,(1<<PGERS)|(1<<SPMEN)
	RCALL _BLDR_FLASHPAGE_DO_SMP_NR

	MOVW XL,ZL
	LDI TEMP_L,FLASH_PAGESIZE/2
	LDI ACCUM_L,(1<<SPMEN)
_BLDR_FLASHPAGE_WRITE_NR__WLOOP:
	LD r0,Y+
	LD r1,Y+
	RCALL _BLDR_FLASHPAGE_DO_SMP_NR
	ADIW ZL,0x02
	DEC TEMP_L
	BRNE _BLDR_FLASHPAGE_WRITE_NR__WLOOP
	MOVW ZL,XL
	
	LDI ACCUM_L,(1<<PGWRT)|(1<<SPMEN)
	RCALL _BLDR_FLASHPAGE_DO_SMP_NR
	RET
;-----------------------------------------------------------
_BLDR_FLASHPAGE_DO_SMP_NR:									;NR-NO_RESTORE - не восстанавливает регистр ACCUM_H
;-----------------------------------------------------------
;Выполняю команду SPM
;IN: Z-адрес FLASH,r1/r0-слово, ACCUM_L-значение SPMCSR
;-----------------------------------------------------------
_BLDR_FLASHPAGE_DO_SMP__WAIT_SPM:
	LDS ACCUM_H,SPMCSR
	SBRC ACCUM_H,SPMEN
	RJMP _BLDR_FLASHPAGE_DO_SMP__WAIT_SPM

_BLDR_FLASHPAGE_DO_SMP__WAIT_EE:
	LDS ACCUM_H,EECR
	SBRC ACCUM_H,EEPE
	RJMP _BLDR_FLASHPAGE_DO_SMP__WAIT_EE

	STS SPMCSR,ACCUM_L
	SPM
	RET
