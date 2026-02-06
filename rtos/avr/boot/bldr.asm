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
;|0x02:0x02-DATA SIZE                     |
;+----------------------------------------+
;|0x04:0x??-DATA                          |
;+----------------------------------------+
;|0x??:0x01-XOR SUM                       |
;+----------------------------------------+

;Запросы  (от хоста)
	.EQU	REQ_BLDR_INFO							= 0x00	;Запрос информации об устройстве (в основной программе в режиме отладки будет приводить к запуску бутлоадера)
	.EQU	REQ_BLDR_REBOOT							= 0x01	;Рестарт через WATCHDOG
	.EQU	REQ_BLDR_PAGE_ERASE						= 0x10	;Очистка FLASH страницы
	.EQU	REQ_BLDR_PAGE_VERIFY					= 0x11	;Проверка FLASH страницы
	.EQU	REQ_BLDR_PAGE_WRITE						= 0x12	;Запись FLASH страницы
	.EQU	REQ_BLDR_PAGE_EVERIFY					= 0x20	;Проверка FLASH страницы (зашифрованные данные - на будущее)
	.EQU	REQ_BLDR_PAGE_EWRITE					= 0x21	;Запись FLASH страницы (зашифрованные данные - на будущее)
	.EQU	REQ_BLDR_FUSE_READ						= 0x30	;Чтение значений фьюзов
	.EQU	REQ_BLDR_FUSE_WRITE						= 0x31	;Запись значений фьюзов
;Ответы (от бутлоадера)
	.EQU	RESP_BLDR_OK							= 0x80	;Успешное выполнение
	.EQU	RESP_BLDR_IDENTICAL						= 0x81	;Страницы идентичны (для REQ_BLDR_PAGE_VERIFY и REQ_BLDR_PAGE_WRITE)
	.EQU	RESP_BLDR_NOT_EQUAL						= 0x82	;Страницы не идентичны (для REQ_BLDR_PAGE_VERIFY)
	.EQU	RESP_BLDR_WRONG_REQUEST					= 0x88	;Ошибка в данных запроса
	.EQU	RESP_BLDR_WRONG_PAGE					= 0x89	;Некорректный номер страницы
	.EQU	RESP_BLDR_WRONG_PAGESIZE				= 0x8a	;Некорректный размер страницы
	.EQU	RESP_BLDR_DENIED						= 0x8b	;Отклонено

;Тело запроса REQ_BLDR_INFO (DATA_SIZE:0x0f)
;+----------------------------------------------+
;|0x05:0x01-BLDR VERSION (7,6-type,5-0-version) |			;00-стандартный бутлоадер(atmega), 01-сокращенный(tiny)
;+----------------------------------------------+
;|0x06:0x01-PLATFORM TYPE ID                    |
;+----------------------------------------------+
;|0x07:0x04-MCU SIGNATURE                       |
;+----------------------------------------------+
;|0x0b:0x08-MCU UID                             |
;+----------------------------------------------+
;|0x13:0x01-FIRMWARE VERSION (0xff-empty)       |
;+----------------------------------------------+

;Тело запроса REQ_BLDR_PAGE_VERIFY и REQ_BLDR_PAGE_WRITE(DATA_SIZE:0x02+PAGE_SIZE)
;+----------------------------------------+
;|0x05:0x02-FLASH ADDR (in bytes)         |
;+----------------------------------------+
;|0x07:0x??-PAGE DATA                     |
;+----------------------------------------+

;Тело запроса REQ_BLDR_PAGE_ERASE (DATA_SIZE:0x02)
;+----------------------------------------+
;|0x05:0x02-FLASH ADDR (in bytes)         |
;+----------------------------------------+


;Тело запроса REQ_BLDR_FUSE_WRITE и ответа на запрос REQ_BLDR_FUSE_READ (DATA_SIZE:0x06)
;+----------------------------------------+
;|0x05:0x06-FUSE DATA                     | - LlHhEe (каждому байту добавлено инвертированное значение)
;+----------------------------------------+

;Остальные запросы тел не имеют

	.DEF	COK										= r3;
	.DEF	CMAGIC									= r4;

	.EQU	BITDELAY_CONST							= 0x13
	.EQU	BLDR_VERSION							= 0b01000000 | 0x00000000 ;7,6 - тип, 5-0 - версия
	.EQU	BLDR_START_WADDR						= BOOT_512W_ADDR
	.EQU	_BLDR_BUFFER_SIZE						= 0x08 + FLASH_PAGESIZE	;Максимально допустимый размер запроса
	.EQU	_BLDR_PROTOCOL_HEADER_SIZE				= 0x05	;Размер заголовка с учетом XOR SUM
	

.ORG BLDR_START_WADDR
BLDR_START:
	CLI
	RJMP _BLDR_START__DATASKIP
BLDR_VECTORS_TABLE:
	RJMP BLDR_UART_RECV_BYTE__START_SKIPPED_NR				;BOOT_512W_ADDR+0x02
	RJMP BLDR_UART_SEND_BYTE								;BOOT_512W_ADDR+0x03
	RJMP BLDR_WATCHDOG_STOP									;BOOT_512W_ADDR+0x04
BLDR_START__DATA:											;BOOT_512W_ADDR+0x05
	.db	BLDR_VERSION,PLATFORM_ID							;Версия загрузчика и ид типа платформы
	.db	low(DEVICE_SIGNATURE>>0x18),low(DEVICE_SIGNATURE>>0x10)	;Сигнатура чипа
	.db	low(DEVICE_SIGNATURE>>0x08),low(DEVICE_SIGNATURE)
	.db (V_UID>>0x08)&0xff,(V_UID)&0xff						;Уникальный идентификатор чипа (2B-vendor, 6B-device)
	.db (D_UID>>0x28)&0xff,(D_UID>>0x20)&0xff,(D_UID>>0x18)&0xff,(D_UID>>0x10)&0xff,(D_UID>>0x08)&0xff,(D_UID)&0xff
.IFDEF KEY1	
	.dd KEY1
	.dd KEY2
	.dd KEY3
	.dd KEY4
	.dd KEY5
	.dd KEY6
	.dd KEY7
	.dd KEY8
.ENDIF
_BLDR_START__DATASKIP:
.IFDEF SPH
	LDI ACCUM_L,high(SRAM_START+SRAM_SIZE-0x01)				;Инициализация стека
	STS SPH,ACCUM_L
.ENDIF
	LDI ACCUM_L,low(SRAM_START+SRAM_SIZE-0x01)
	STS SPL,ACCUM_L

	RCALL BLDR_WATCHDOG_STOP

	EOR C0x00,C0x00
	LDI ACCUM_L,RESP_BLDR_OK
	MOV COK,ACCUM_L
	LDI ACCUM_L,0x77
	MOV CMAGIC,ACCUM_L

	EOR FLAGS,FLAGS

;PB2
SBI DDRB,5
SBI PORTB,5

SBI PINB,5
SBI PINB,5
SBI PINB,5
SBI PINB,5

.IFDEF STDIO_PORT_REGID
	CBI STDIO_DDR_REGID,STDIO_PINNUM						;Гарантируем режим входа на пине
	CBI STDIO_PORT_REGID,STDIO_PINNUM						;Внутренняя подтяжка
.ELSE
	CBI STDIN_DDR_REGID,STDIN_PINNUM						;Гарантируем режим входа на пине
	SBI STDIN_PORT_REGID,STDIN_PINNUM						;Внутренняя подтяжка
	SBI STDOUT_DDR_REGID,STDOUT_PINNUM						;Гарантируем режим выхода на пине
	SBI STDOUT_PORT_REGID,STDOUT_PINNUM						;Высокий уровень
.ENDIF

	LDI ZH,0x00												;Проверка наличия прошивки программы, если 0xffff то прошивка не обнаружена
	LDI ZL,0x00
	LPM TEMP_L,Z+
	CPI TEMP_L,0xff
	BRNE _BLDR_START__PROGRAMM_DETECTED
	LPM TEMP_L,Z
	CPI TEMP_L,0xff
	BREQ _BLDR_START__LOOP									;Если прошивки нет, то переходим сразу в режим бутлоадера
_BLDR_START__PROGRAMM_DETECTED:



	LDI TEMP_H,BITDELAY_CONST
	RCALL _BLDR_UART_BIT_DELAY

	WDR														;Запускаем WATCHDOG с ожиданием в 1с
	LDS ACCUM_L,WDTCR
	ORI ACCUM_L,(1<<WDCE)|(1<<WDE)
	STS WDTCR,ACCUM_L
	LDI ACCUM_L,(0<<WDE)|(1<<WDIE)|(0<<WDP3)|(1<<WDP2)|(1<<WDP1)|(0<<WDP0)
	STS WDTCR,ACCUM_L

_BLDR_START__WATCHDOG_LOOP:
.IFDEF STDIO_PORT_REGID
	SBIC STDIO_PIN_REGID,STDIO_PINNUM
.ELSE
	SBIC STDIN_PIN_REGID,STDIN_PINNUM
.ENDIF
	RJMP PC+0x03
	RCALL BLDR_WATCHDOG_STOP
	RJMP _BLDR_START__MAIN_LOOP
	LDS ACCUM_L,WDTCR
	SBRS ACCUM_L,WDIF
	RJMP _BLDR_START__WATCHDOG_LOOP
	RCALL _BLDR_REBOOT

_BLDR_START__MAIN_LOOP:
_BLDR_START__LOOP:
SBI PINB,5
SBI PINB,5
SBI PINB,5
SBI PINB,5
SBI PINB,5
SBI PINB,5

	RCALL BLDR_UART_RECV_NR
	CPI XH,0xff
	BREQ _BLDR_START__LOOP
	CPI TEMP_EH,0x00
	BRNE _BLDR_START__LOOP
	CPI XH,0x00
	BRNE PC+0x03
	CPI XL,_BLDR_PROTOCOL_HEADER_SIZE
	BRCS _BLDR_START__LOOP
	LDI YH,high(SRAM_START)
	LDI YL,low(SRAM_START)
	LD ACCUM_L,Y
	CP ACCUM_L,CMAGIC
	BRNE _BLDR_START__LOOP
	
	;RCALL _BLDR_UART_FRAME_DELAY
	SBIW XL,0x01											;Вычитаем байт XORSUM
	
	LDI YH,high(SRAM_START)
	LDI YL,low(SRAM_START)
	LDD ACCUM_L,Y+0x01
	
	CPI XH,0x00
	BREQ PC+0x02
	RJMP _BLDR_START__LOOP_NOT_BLDR_PAGE_ERASE
	
	CPI ACCUM_L,REQ_BLDR_INFO
	BRNE _BLDR_START__LOOP_NOT_BLDR_INFO
;---INFO----------------------------------------------------
	CPI XL,_BLDR_PROTOCOL_HEADER_SIZE-0x01
	BREQ PC+0x04
	LDI ACCUM_L,RESP_BLDR_WRONG_REQUEST
	RCALL _BLDR_ANSWER
	RJMP _BLDR_START__LOOP
	
	ST Y+,CMAGIC
	ST Y+,COK
	ST Y+,C0x00
	LDI ACCUM_L,0x0f
	ST Y+,ACCUM_L
	LDI TEMP_L,0x0e
	LDI ZH,high(BLDR_START__DATA*2)
	LDI ZL,low(BLDR_START__DATA*2)
_BLDR_START__LOOP_BLDR_INFO_LOOP:
	LPM ACCUM_L,Z+
	ST Y+,ACCUM_L
	DEC TEMP_L
	BRNE _BLDR_START__LOOP_BLDR_INFO_LOOP
	LDI ZH,high(BLDR_START_WADDR*2-0x01)					;Последний байт прошивки может содержать версию прошивки основной программы
	LDI ZL,low(BLDR_START_WADDR*2-0x01)
	LPM ACCUM_L,Z
	ST Y,ACCUM_L
	LDI XH,high(_BLDR_PROTOCOL_HEADER_SIZE-0x01+0x0f)
	LDI XL,low(_BLDR_PROTOCOL_HEADER_SIZE-0x01+0x0f)
	RCALL BLDR_UART_SEND_NR
	RJMP _BLDR_START__LOOP
_BLDR_START__LOOP_NOT_BLDR_INFO:
	CPI ACCUM_L,REQ_BLDR_REBOOT
	BRNE _BLDR_START__LOOP_NOT_BLDR_REBOOT
;---REBOOT--------------------------------------------------
	CPI XL,_BLDR_PROTOCOL_HEADER_SIZE-0x01
	BREQ PC+0x04
	LDI ACCUM_L,RESP_BLDR_WRONG_REQUEST
	RCALL _BLDR_ANSWER
	RJMP _BLDR_START__LOOP
	LDI ACCUM_L,RESP_BLDR_OK
	RCALL _BLDR_ANSWER
	RJMP _BLDR_REBOOT
_BLDR_START__LOOP_NOT_BLDR_REBOOT:
	CPI ACCUM_L,REQ_BLDR_FUSE_READ
	BRNE _BLDR_START__LOOP_NOT_BLDR_FUSE_READ
;---FUSE-READ-----------------------------------------------
	CPI XL,_BLDR_PROTOCOL_HEADER_SIZE-0x01
	BREQ PC+0x04
	LDI ACCUM_L,RESP_BLDR_WRONG_REQUEST
	RCALL _BLDR_ANSWER
	RJMP _BLDR_START__LOOP
	ST Y+,CMAGIC
	ST Y+,COK
	ST Y+,C0x00
	LDI ACCUM_L,0x06
	ST Y+,ACCUM_L
	LDI ZL,FUSE_LOW_ADDR*2
	RCALL _BLDR_READ_FUSE_TO_SRAM__NR
	LDI ZL,FUSE_HIGH_ADDR*2
	RCALL _BLDR_READ_FUSE_TO_SRAM__NR
	LDI ZL,FUSE_EXT_ADDR*2
	RCALL _BLDR_READ_FUSE_TO_SRAM__NR
	LDI XH,high(_BLDR_PROTOCOL_HEADER_SIZE-0x01+0x06)
	LDI XL,low(_BLDR_PROTOCOL_HEADER_SIZE-0x01+0x06)
	RCALL BLDR_UART_SEND_NR
	RJMP _BLDR_START__LOOP
_BLDR_START__LOOP_NOT_BLDR_FUSE_READ:
	CPI ACCUM_L,REQ_BLDR_PAGE_ERASE
	BRNE _BLDR_START__LOOP_NOT_BLDR_PAGE_ERASE
;---PAGE-ERASE----------------------------------------------
	CPI XL,_BLDR_PROTOCOL_HEADER_SIZE+0x02-0x01
	BREQ PC+0x04
	LDI ACCUM_L,RESP_BLDR_WRONG_REQUEST
	RCALL _BLDR_ANSWER
	RJMP _BLDR_START__LOOP
	LDD ZH,Y+0x04
	LDD ZL,Y+0x05
	RCALL _BLDR_CHECK_ADDR
	BREQ PC+0x02
	RJMP _BLDR_START__LOOP
	RCALL _BLDR_FLASHPAGE_ERASE_NR
	LDI ACCUM_L,RESP_BLDR_OK
	RCALL _BLDR_ANSWER
	RJMP _BLDR_START__LOOP
_BLDR_START__LOOP_NOT_BLDR_PAGE_ERASE:
	CPI ACCUM_L,REQ_BLDR_FUSE_WRITE
	BRNE _BLDR_START__LOOP_NOT_BLDR_FUSE_WRITE
;---FUSE-WRITE----------------------------------------------
	CPI XL,_BLDR_PROTOCOL_HEADER_SIZE+0x06-0x01
	BREQ PC+0x04
	LDI ACCUM_L,RESP_BLDR_WRONG_REQUEST
	RCALL _BLDR_ANSWER
	RJMP _BLDR_START__LOOP
	LDD TEMP_L,Y+0x02
	CPI TEMP_L,0x00
	BRNE _BLDR_START__LOOP_BLDR_FUSE_WRITE_INVALID_DATA
	LDD TEMP_L,Y+0x03
	CPI TEMP_L,0x06
	BRNE _BLDR_START__LOOP_BLDR_FUSE_WRITE_INVALID_DATA
	LDD ACCUM_L,Y+0x04
	LDD ACCUM_H,Y+0x06
	LDD ACCUM_EL,Y+0x08
	LDD TEMP_L,Y+0x05
	COM TEMP_L
	CP ACCUM_L,TEMP_L
	BRNE _BLDR_START__LOOP_BLDR_FUSE_WRITE_INVALID_DATA
	LDD TEMP_L,Y+0x07
	COM TEMP_L
	CP ACCUM_H,TEMP_L
	BRNE _BLDR_START__LOOP_BLDR_FUSE_WRITE_INVALID_DATA
	LDD TEMP_L,Y+0x09
	COM TEMP_L
	CP ACCUM_EL,TEMP_L
	BREQ PC+0x04
_BLDR_START__LOOP_BLDR_FUSE_WRITE_INVALID_DATA:
	LDI ACCUM_L,RESP_BLDR_WRONG_REQUEST
	RCALL _BLDR_ANSWER
	RJMP _BLDR_START__LOOP
	MOV r1,C0x00
	MOV r0,ACCUM_L											;Значение Low Fuse
	LDI ACCUM_L,(1<<BLBSET)|(1<<SPMEN)
	LDI ZH,0x00
	LDI ZL,FUSE_LOW_ADDR*2
	RCALL _BLDR_FLASHPAGE_DO_SMP_NR
	MOV r0,ACCUM_H											;Значение High Fuse
	LDI ZL,FUSE_HIGH_ADDR*2
	RCALL _BLDR_FLASHPAGE_DO_SMP_NR
	MOV r0,ACCUM_EL											;Значение Ext Fuse
	LDI ZL,FUSE_EXT_ADDR*2
	RCALL _BLDR_FLASHPAGE_DO_SMP_NR
	LDI ACCUM_L,RESP_BLDR_OK
	RCALL _BLDR_ANSWER
	RJMP _BLDR_START__LOOP
_BLDR_START__LOOP_NOT_BLDR_FUSE_WRITE:
	CPI XH,high(_BLDR_PROTOCOL_HEADER_SIZE-0x01+0x02+FLASH_PAGESIZE)
	BRNE PC+0x03
	CPI XL,low(_BLDR_PROTOCOL_HEADER_SIZE-0x01+0x02+FLASH_PAGESIZE)
	BREQ PC+0x04
	LDI ACCUM_L,RESP_BLDR_WRONG_PAGESIZE
	RCALL _BLDR_ANSWER
	RJMP _BLDR_START__LOOP
	
	CPI ACCUM_L,REQ_BLDR_PAGE_VERIFY
	BRNE _BLDR_START__LOOP_NOT_BLDR_PAGE_VERIFY
;---PAGE-VERIFY---------------------------------------------
	LDD ZH,Y+0x04
	LDD ZL,Y+0x05
	RCALL _BLDR_CHECK_ADDR
	BREQ PC+0x02
	RJMP _BLDR_START__LOOP
	RCALL _BLDR_VERIFY_FLASHPAGE
	LDI ACCUM_L,RESP_BLDR_OK
	BREQ PC+0x02
	LDI ACCUM_L,RESP_BLDR_NOT_EQUAL
	RCALL _BLDR_ANSWER
	RJMP _BLDR_START__LOOP

_BLDR_START__LOOP_NOT_BLDR_PAGE_VERIFY:
	CPI ACCUM_L,REQ_BLDR_PAGE_WRITE
	BRNE _BLDR_START__LOOP_NOT_BLDR_PAGE_WRITE
;---PAGE-WRITE----------------------------------------------
	LDD ZH,Y+0x04
	LDD ZL,Y+0x05
	RCALL _BLDR_CHECK_ADDR
	BREQ PC+0x02
	RJMP _BLDR_START__LOOP
	MOVW TEMP_L,ZL
	RCALL _BLDR_VERIFY_FLASHPAGE
	BRNE PC+0x04
	LDI ACCUM_L,RESP_BLDR_IDENTICAL
	RCALL _BLDR_ANSWER
	RJMP _BLDR_START__LOOP
	MOVW ZL,TEMP_L
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
BLDR_WATCHDOG_STOP:
;-----------------------------------------------------------
	WDR
	PUSH ACCUM_L
	LDS ACCUM_L,MCUSR
	ANDI ACCUM_L,low(~(1<<WDRF))
	STS MCUSR,ACCUM_L
	LDS ACCUM_L,(1<<WDCE)|(1<<WDE)
	STS WDTCR,ACCUM_L
	LDI ACCUM_L,(0<<WDE)|(1<<WDIF)
	STS WDTCR,ACCUM_L
	POP ACCUM_L
	RET
;-----------------------------------------------------------
_BLDR_VERIFY_FLASHPAGE:
;-----------------------------------------------------------
;Проверка идентичности FLASH страницы
;IN;Z-адрес FLASH страницы
;OUT: Flag Z-(0=идентичны)
;-----------------------------------------------------------
	LDI XH,high(FLASH_PAGESIZE)
	LDI XL,low(FLASH_PAGESIZE)
	LDI YH,high(SRAM_START+_BLDR_PROTOCOL_HEADER_SIZE-0x01+0x02)
	LDI YL,low(SRAM_START+_BLDR_PROTOCOL_HEADER_SIZE-0x01+0x02)
_BLDR_VERIFY_FLASHPAGE__LOOP:
	LPM ACCUM_L,Z+
	LD ACCUM_H,Y+
	CP ACCUM_L,ACCUM_H
	BREQ PC+0x02
	RET
	SBIW XL,0x01
	BRNE _BLDR_VERIFY_FLASHPAGE__LOOP
	SEZ
	RET

;-----------------------------------------------------------
_BLDR_CHECK_ADDR:											;Не восстанавливает X,Y,ACCUM_L,TEMP_H/L
;-----------------------------------------------------------
;Проверка корректности адреса FLASH страницы
;IN;Z-адрес FLASH страницы
;OUT: Flag Z-(1=корректый)
;-----------------------------------------------------------
	CPI ZH,high(FLASH_SIZE-FLASH_PAGESIZE-1024+1)
	BRCS _BLDR_CHECK_ADDR__CORRECT
	BRNE PC+0x3
	CPI ZL,low(FLASH_SIZE-FLASH_PAGESIZE-1024*2+1)
	BRCS _BLDR_CHECK_ADDR__CORRECT
	LDI ACCUM_L,RESP_BLDR_WRONG_PAGE
	RCALL _BLDR_ANSWER
	CLZ
	RET
_BLDR_CHECK_ADDR__CORRECT:
	SEZ
	RET

;-----------------------------------------------------------
_BLDR_ANSWER:												;Не восстанавливает X,Y,ACCUM_L,TEMP_H/L
;-----------------------------------------------------------
;Заполняем стандартный пакет ответа и отправляем по UART
;OUT: ACCUM_L-код ответа
;-----------------------------------------------------------
	LDI YH,high(SRAM_START)
	LDI YL,low(SRAM_START)
	RCALL _BLDR_MAKE_HEADER
	LDI XH,high(_BLDR_PROTOCOL_HEADER_SIZE-0x01)
	LDI XL,low(_BLDR_PROTOCOL_HEADER_SIZE-0x01)
	RJMP BLDR_UART_SEND_NR

;-----------------------------------------------------------
_BLDR_MAKE_HEADER:											;Не восстанавливает Y
;-----------------------------------------------------------
;Заполняем заголовок для ответа
;IN: ACCUM_L-код ответа
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
BLDR_UART_RECV_NR:											;Не восстанавливает регистры X,Y,Z, ACCUM_L, TEMP_H
;-----------------------------------------------------------
;Слушаем UART-ожидаем данные, принимаем и обрабатываем
;OUT: X-длина данных, TEMP_EH-XORSUM, XH=0xff-ошибка приема
;-----------------------------------------------------------
	LDI YH,high(SRAM_START)									;Устанавливаем адрес буфера
	LDI YL,low(SRAM_START)
	LDI XH,0x00												;Длина принятых данных
	LDI XL,0x00
	LDI TEMP_EH,0x00										;Счетчик XOR суммы

BLDR_UART_RECV_NR__WAIT_BYTE:								;Ждем байт
	LDI ZH,0												;Пауза в 2 бита UART
	LDI ZL,24												;~10мкс на 16МГц
	RCALL BLDR_UART_RECV_BYTE_NR
	BRNE BLDR_UART_RECV_NR__GOT_BYTE
	CPI XH,0x00												;Проверка на нулевую длину
	BRNE PC+0x03
	CPI XL,0x00
	BREQ BLDR_UART_RECV_NR__WAIT_BYTE						;При длине=0 бесконечно ждем старт
	RET
	
BLDR_UART_RECV_NR__GOT_BYTE:
	CPI XH,0xff												;Признак переполнения буфера
	BREQ BLDR_UART_RECV_NR__WAIT_BYTE						;Повторяем прием в холостую пока не завершится передача

	CPI XH,high(_BLDR_BUFFER_SIZE)							;Проверка на достижение максимальной длины - игнорируем последующие данные
	BRCS _BLDR_UART_RECV_NR__NEXT							;Переполнения нет - переходим к блоку записи байта и переходе к следующей итерации
	BRNE PC+0x03											;Есть переполнение - переходим к следующей итерации включив признак переполнения и игнорируем запись байта
	CPI XL,low(_BLDR_BUFFER_SIZE)
	BRCS _BLDR_UART_RECV_NR__NEXT
	LDI XH,0xff
	RJMP BLDR_UART_RECV_NR__WAIT_BYTE

_BLDR_UART_RECV_NR__NEXT:
	EOR TEMP_EH,ACCUM_L										;Учитываем XORSUM
	ST Y+,ACCUM_L											;Записываем байт
	ADIW XL,0x01											;Инкрементируем счетчик длины данных
	RJMP BLDR_UART_RECV_NR__WAIT_BYTE						;Переходим к следующей итерации

;-----------------------------------------------------------;
BLDR_UART_RECV_BYTE_NR:										;Не восстанавливает регистры Z, TEMP_L
;-----------------------------------------------------------;
;Прием байта по программному UART
;IN: Z-таймаут
;OUT: ACCUM_L-байт, Flag Z=1-ошибка приема
;-----------------------------------------------------------

BLDR_UART_RECV_BYTE_NR__START_LOOP:							;Ждем START
.IFDEF STDIO_PORT_REGID
	SBIS STDIO_PIN_REGID,STDIO_PINNUM						;Если низкий уровень на пине, значит обнаружен START
.ELSE
	SBIS STDIN_PIN_REGID,STDIN_PINNUM
.ENDIF
	RJMP _BLDR_UART_RECV_BYTE_NR__GOT_START
	SBIW ZL,0x01
	BRCC BLDR_UART_RECV_BYTE_NR__START_LOOP
	SEZ
	RET
	
_BLDR_UART_RECV_BYTE_NR__GOT_START:							;Получен START
	LDI TEMP_H,BITDELAY_CONST/3+BITDELAY_CONST+1			;Пауза ~1/3 + 1 бита
	RCALL _BLDR_UART_BIT_DELAY
	
BLDR_UART_RECV_BYTE__START_SKIPPED_NR:
	LDI TEMP_L,0x08											;Цикл в 8 бит (8n0)
_BLDR_UART_RECV_BYTE_NR__BITLOOP:
.IFDEF STDIO_PORT_REGID
	SBIC STDIO_PIN_REGID,STDIO_PINNUM						;Передаем бит с UART в флаг C
.ELSE
	SBIC STDIN_PIN_REGID,STDIN_PINNUM
.ENDIF
	SEC
	ROR ACCUM_L												;Записываем флаг в аккумулятор используя битовый сдвиг
	LDI TEMP_H,BITDELAY_CONST
	RCALL _BLDR_UART_BIT_DELAY								;Пауза в 1 бит, также сбрасывает флаг C
	DEC TEMP_L												;Декрементируем счетчик бит
	BRNE _BLDR_UART_RECV_BYTE_NR__BITLOOP					;Повторяем для 8 итераций

	LDI TEMP_H,1
	RCALL _BLDR_UART_BIT_DELAY
	CLZ
.IFDEF STDIO_PORT_REGID
	SBIS STDIO_PIN_REGID,STDIO_PINNUM						;Проверяем STOP
.ELSE
	SBIS STDIN_PIN_REGID,STDIN_PINNUM
.ENDIF
	SEZ														;STOP не получен
	RET

;-----------------------------------------------------------;
BLDR_UART_SEND_NR:											;Не восстанавливает регистры Y,X,ACCUM_L, TEMP_L\H
;-----------------------------------------------------------;
;Передача RAM данных по UART
;IN: X-размер данных без XORSUM (не может быть 0)
;-----------------------------------------------------------
.IFDEF STDIO_PORT_REGID
	SBI STDIO_PORT_REGID,STDIO_PINNUM						;Режим выхода для UART пина
	SBI STDIO_DDR_REGID,STDIO_PINNUM
.ENDIF
	LDI TEMP_H,BITDELAY_CONST
	RCALL _BLDR_UART_BIT_DELAY

	LDI YH,high(SRAM_START)
	LDI YL,low(SRAM_START)
	LDI TEMP_EH,0x00
BLDR_UART_SEND_NR__LOOP:
	LD ACCUM_L,Y+											;Считываем байт в аккумулятор и инкрементируем индексный регистр
	EOR TEMP_EH,ACCUM_L
	RCALL BLDR_UART_SEND_BYTE								;Передаем байт по UART
	SBIW XL,0x01
	BRNE BLDR_UART_SEND_NR__LOOP

	MOV ACCUM_L,TEMP_EH
	RCALL BLDR_UART_SEND_BYTE								;Передаем байт XORSUM по UART

.IFDEF STDIO_PORT_REGID
	CBI STDIO_DDR_REGID,STDIO_PINNUM
	CBI STDIO_PORT_REGID,STDIO_PINNUM						;Режим входа для пина UART без внутренней подтяжки
.ELSE
	NOP
	NOP
.ENDIF
	LDI TEMP_H,BITDELAY_CONST
	RCALL _BLDR_UART_BIT_DELAY
	RET

;-----------------------------------------------------------;
BLDR_UART_SEND_BYTE:
;-----------------------------------------------------------;
;Передача байта по программному UART
;IN: ACCUM_L-байт
;-----------------------------------------------------------
	PUSH_T16
	PUSH ACCUM_L
	
.IFDEF STDIO_PORT_REGID
	CBI STDIO_PORT_REGID,STDIO_PINNUM						;Выставляем START
.ELSE
	CBI STDOUT_PORT_REGID,STDOUT_PINNUM
.ENDIF
	LDI TEMP_H,BITDELAY_CONST
	RCALL _BLDR_UART_BIT_DELAY

	LDI TEMP_L,0x08											;Цикл передачи байта побитно
BLDR_UART_SEND_BYTE_NR__BITLOOP:
	SBRC ACCUM_L,0x00										;В зависимости от текущего бита задаем на пине высокий или низкий уровень
.IFDEF STDIO_PORT_REGID
	SBI STDIO_PORT_REGID,STDIO_PINNUM
.ELSE
	SBI STDOUT_PORT_REGID,STDOUT_PINNUM
.ENDIF
	SBRS ACCUM_L,0x00
.IFDEF STDIO_PORT_REGID
	CBI STDIO_PORT_REGID,STDIO_PINNUM
.ELSE
	CBI STDOUT_PORT_REGID,STDOUT_PINNUM
.ENDIF
	LSR ACCUM_L												;Сдвигаем биты в аккумуляторе
	
	LDI TEMP_H,BITDELAY_CONST-1
	RCALL _BLDR_UART_BIT_DELAY
	DEC TEMP_L												;Декрементируем счетчик бит
	BRNE BLDR_UART_SEND_BYTE_NR__BITLOOP					;Выполняем все 8 итераций

.IFDEF STDIO_PORT_REGID
	SBI STDIO_PORT_REGID,STDIO_PINNUM						;Выставляем STOP
.ELSE
	SBI STDOUT_PORT_REGID,STDOUT_PINNUM
.ENDIF
	LDI TEMP_H,BITDELAY_CONST-1
	RCALL _BLDR_UART_BIT_DELAY
	
	POP ACCUM_L
	POP_T16
	RET

;-----------------------------------------------------------
_BLDR_UART_FRAME_DELAY:
;-----------------------------------------------------------
;Пауза между запросом и ответом
;-----------------------------------------------------------
	PUSH_T16
	LDI TEMP_H,0x14
	LDI TEMP_L,0x00
_BLDR_UART_FRAME_DELAY__LOOP:
	RCALL _BLDR_UART_BIT_DELAY
	DEC TEMP_L
	BRNE _BLDR_UART_FRAME_DELAY__LOOP
	POP_T16
	RET

;-----------------------------------------------------------
_BLDR_UART_BIT_DELAY:
;-----------------------------------------------------------
;Пауза между битами
;IN: TEMP_H - значение паузы
;OUT: сбрасывает флаг C
;-----------------------------------------------------------
	SUBI TEMP_H,0x01
	BRNE PC-0x01
	RET

;-----------------------------------------------------------
_BLDR_REBOOT:
;-----------------------------------------------------------
;Перезагружка МК
;-----------------------------------------------------------
	RCALL BLDR_WATCHDOG_STOP
	RCALL _BLDR_UART_FRAME_DELAY
.IFDEF STDIO_PORT_REGID
	CBI STDIO_DDR_REGID,STDIO_PINNUM
	SBI STDIO_PORT_REGID,STDIO_PINNUM
.ELSE
	CBI STDOUT_DDR_REGID,STDOUT_PINNUM
	SBI STDOUT_PORT_REGID,STDOUT_PINNUM
.ENDIF
	LDI_Z 0x00
	IJMP

;===========================================================
;========FLASH=PROCS========================================
;===========================================================

;-----------------------------------------------------------
_BLDR_FLASHPAGE_WRITE_NR:									;NR-NO_RESTORE - не восстанавливает регистр Z,ACCUM_L,ACCUM_EH,TEMP_L,r0,r1
;-----------------------------------------------------------
;Блок процедуры записи страницы FLASH
;IN: Y-адрес данных в RAM, Z-адрес страницы во FLASH
;-----------------------------------------------------------
	RCALL _BLDR_FLASHPAGE_ERASE_NR

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
	LDI ACCUM_L,(1<<RWWSRE)|(1<<SPMEN)
	RCALL _BLDR_FLASHPAGE_DO_SMP_NR
	RET

;-----------------------------------------------------------
_BLDR_FLASHPAGE_ERASE_NR:									;NR-NO_RESTORE - не восстанавливает регистр ACCUM_L
;-----------------------------------------------------------
;Блок процедуры стирания страницы FLASH
;IN: Z-адрес страницы во FLASH
;-----------------------------------------------------------
	LDI ACCUM_L,(1<<PGERS)|(1<<SPMEN)
	RCALL _BLDR_FLASHPAGE_DO_SMP_NR
	LDI ACCUM_L,(1<<RWWSRE)|(1<<SPMEN)
	RCALL _BLDR_FLASHPAGE_DO_SMP_NR
	RET

;-----------------------------------------------------------
_BLDR_READ_FUSE_TO_SRAM__NR:								;NR-NO_RESTORE - не восстанавливает регистры ACCUM_L,YH,YL
;-----------------------------------------------------------
;Чтение байта Fuse битов
;IN:ACCUM_L-значение SPMCSR, Z-src адрес,Y-dst адрес 
;-----------------------------------------------------------
	LDI ACCUM_L,(1<<BLBSET)|(1<<SPMEN)
	LDI ZH,0x00
	STS SPMCSR,ACCUM_L
	LPM ACCUM_L, Z
	ST Y+, ACCUM_L
	COM ACCUM_L
	ST Y+, ACCUM_L
	RET

;-----------------------------------------------------------
_BLDR_FLASHPAGE_DO_SMP_NR:									;NR-NO_RESTORE - не восстанавливает регистр ACCUM_EH
;-----------------------------------------------------------
;Выполняю команду SPM
;IN: Z-адрес FLASH,r1/r0-слово, ACCUM_L-значение SPMCSR
;-----------------------------------------------------------
_BLDR_FLASHPAGE_DO_SMP__WAIT_SPM:
	LDS ACCUM_EH,SPMCSR
	SBRC ACCUM_EH,SPMEN
	RJMP _BLDR_FLASHPAGE_DO_SMP__WAIT_SPM

_BLDR_FLASHPAGE_DO_SMP__WAIT_EE:
	LDS ACCUM_EH,EECR
	SBRC ACCUM_EH,EEPE
	RJMP _BLDR_FLASHPAGE_DO_SMP__WAIT_EE

	STS SPMCSR,ACCUM_L
	SPM
	RET
