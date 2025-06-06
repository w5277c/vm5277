;-----------------------------------------------------------------------------------------------------------------------
;Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
;-----------------------------------------------------------------------------------------------------------------------
;30.05.2025	konstantin@5277.ru		Взят с проекта core5277
;-----------------------------------------------------------------------------------------------------------------------

	.DEVICE ATmega328P

	.EQU	BOOT_512W_ADDR							= 0x3e00;Начальный адрес килобайтного бутлоадера в словах
	.EQU 	FLASH_PAGESIZE							= 0x40*0x02	;128Б-размер страницы FLASH
	.EQU	FLASH_SIZE								= 0x8000;32K
	.EQU	EEPROM_PAGESIZE							= 0x04	;4Б-размер страницы EEPROM
	.EQU	EEPROM_SIZE								= 0x0400;1K
	.EQU	PORTS_QUANTITY							= 0x04	;Кол-во реальных портов (---,PORTB,PORTC,PORTD)

	.include "common.asm"

;---CONSTANTS-----------------------------------------------
	.EQU	_MCALL_SIZE								= 0x02	;Размер MCALL
	.EQU	_C5_IR_QNT								= 0x1a	;Количество перырваний(без RESET)
	.EQU	_C5_PCINT_PORTS_QNT						= 0x03	;Количество портов поддерживающих внешние прерывания(PCINT)
	.EQU	_C5_RAM_BORDER_SIZE						= 0x20	;Минимальное допустимое расстояние между вершинами FREE_RAM и STACK
	.SET	_C5_STACK_SIZE							= 0x60	;Стек ядра
	.EQU	RAMSTART								= 0x0100
	.EQU	RAMEND									= 0x08ff
	.SET	INPUT_BUFFER_SIZE						= 0x80	;Размер буфера ввода (не менее 2)

	;---IR---
	.EQU	C5_IR_INT0								= 0x01
	.EQU	C5_IR_INT1								= 0x02
	.EQU	C5_IR_PCINT0							= 0x03
	.EQU	C5_IR_PCINT1							= 0x04
	.EQU	C5_IR_PCINT2							= 0x05
	.EQU	C5_IR_WDT								= 0x06
	.EQU	C5_IR_TIMER2_COMPA						= 0x07
	.EQU	C5_IR_TIMER2_COMPB						= 0x08
	.EQU	C5_IR_TIMER2_OVF						= 0x09
	.EQU	C5_IR_TIMER1_CAPT						= 0x0a
	.EQU	C5_IR_TIMER1_COMPA						= 0x0b
	.EQU	C5_IR_TIMER1_COMPB						= 0x0c
	.EQU	C5_IR_TIMER1_OVF						= 0x0d
	.EQU	C5_IR_RESERVED_CORE_TIMER1				= 0x0e
	.EQU	C5_IR_RESERVED_CORE_TIMER2				= 0x0f
	.EQU	C5_IR_TIMER0_OVF						= 0x10
	.EQU	C5_IR_SPI								= 0x11
	.EQU	C5_IR_UART_RX							= 0x12
	.EQU	C5_IR_UART_UDRE							= 0x13
	.EQU	C5_IR_UART_TX							= 0x14
	.EQU	C5_IR_ADC								= 0x15
	.EQU	C5_IR_EE_READY							= 0x16
	.EQU	C5_IR_ANALOG_COMP						= 0x17
	.EQU	C5_IR_TWI								= 0x18
	.EQU	C5_IR_SPM_READY							= 0x19

	;SBI/CBI/SBIS/SBIC port defines
	.EQU	PORTA									= 0xff
	.EQU	DDRA									= 0xff
	.EQU	PINA									= 0xff
	.EQU	PORTB									= 0x05
	.EQU	DDRB									= 0x04
	.EQU	PINB									= 0x03
	.EQU	PORTC									= 0x08
	.EQU	DDRC									= 0x07
	.EQU	PINC									= 0x06
	.EQU	PORTD									= 0x0b
	.EQU	DDRD									= 0x0a
	.EQU	PIND									= 0x09
	.EQU	PORTE									= 0xff
	.EQU	DDRE									= 0xff
	.EQU	PINE									= 0xff
	.EQU	PORTF									= 0xff
	.EQU	DDRF									= 0xff
	.EQU	PINF									= 0xff
	.EQU	PORTG									= 0xff
	.EQU	DDRG									= 0xff
	.EQU	PING									= 0xff

	.EQU	SDA										= PC4
	.EQU	SCL										= PC5
	.EQU	RXD										= PD0
	.EQU	TXD										= PD1
	.EQU	RXD0									= RXD
	.EQU	TXD0									= TXD
	.EQU	SCK										= PB5
	.EQU	MISO									= PB4
	.EQU	MOSI									= PB3
	.EQU	SS										= PB2
	.EQU	OC2A									= PB3

	;---IO-REGISTERS---------------------------------------
	.EQU	SREG									= 0x5f
	.EQU	SPH										= 0x5e
	.EQU	SPL										= 0x5d
	.EQU	MCUCSR									= 0x54
	.EQU	MCUSR									= MCUCSR
	.EQU		WDRF								= 0x03
	.EQU		BORF								= 0x02
	.EQU		EXTRF								= 0x01
	.EQU		PORF								= 0x00
	.EQU	PRR										= 0x64
	.EQU		PRTWI								= 0x07
	.EQU		PRTIM2								= 0x06
	.EQU		PRTIM0								= 0x05
	.EQU		PRTIM1								= 0x03
	.EQU		PRSPI								= 0x02
	.EQU		PRUSART0							= 0x01
	.EQU		PRADC								= 0x00
	;---TWI---
	.EQU	TWCR									= 0xbc
	.EQU		TWINT								= 0x07
	.EQU		TWEA								= 0x06
	.EQU		TWSTA								= 0x05
	.EQU		TWSTO								= 0x04
	.EQU		TWWC								= 0x03
	.EQU		TWEN								= 0x02
	.EQU		TWIE								= 0x00
	.EQU	TWDR									= 0xbb
	.EQU	TWAR									= 0xba
	.EQU		TWGCE								= 0x00
	.EQU	TWSR									= 0xb9
	.EQU		TWPS1								= 0x01
	.EQU		TWPS0								= 0x00
	.EQU	TWBR									= 0xb8
	;---TIMER0---
	.EQU	TIMSK0									= 0x6e
	.EQU		OCIE0B								= 0x02
	.EQU		OCIE0A								= 0x01
	.EQU		TOIE0								= 0x00
	.EQU	TCCR0A									= 0x44
	.EQU		COM0A1								= 0x07
	.EQU		COM0A0								= 0x06
	.EQU		COM0B1								= 0x05
	.EQU		COM0B0								= 0x04
	.EQU		WGM01								= 0x01
	.EQU		WGM00								= 0x00
	.EQU	TCCR0B									= 0x45
	.EQU		FOC0A								= 0x07
	.EQU		FOC0B								= 0x06
	.EQU		WGM02								= 0x03
	.EQU		CS02								= 0x02
	.EQU		CS01								= 0x01
	.EQU		CS00								= 0x00
	.EQU	TCNT0									= 0x46
	.EQU	OCR0A									= 0x47
	.EQU	OCR0B									= 0x48
	.EQU	TIFR0									= 0x35
	.EQU		OCF0B								= 0x02
	.EQU		OCF0A								= 0x01
	.EQU		TOV0								= 0x00
	;---TIMER1---
	.EQU	TIMSK1									= 0x6f
	.EQU		ICIE1								= 0x05
	.EQU		OCIE1B								= 0x02
	.EQU		OCIE1A								= 0x01
	.EQU		TOIE1								= 0x00
	.EQU	TCCR1A									= 0x80
	.EQU		COM1A1								= 0x07
	.EQU		COM1A0								= 0x06
	.EQU		COM1B1								= 0x05
	.EQU		COM1B0								= 0x04
	.EQU		WGM11								= 0x01
	.EQU		WGM10								= 0x00
	.EQU	TCCR1B									= 0x81
	.EQU		ICNC1								= 0x07
	.EQU		ICES1								= 0x06
	.EQU		WGM13								= 0x04
	.EQU		WGM12								= 0x03
	.EQU		CS12								= 0x02
	.EQU		CS11								= 0x01
	.EQU		CS10								= 0x00
	.EQU	TCCR1C									= 0x82
	.EQU		FOC1A								= 0x07
	.EQU		FOC1B								= 0x06
	.EQU	TCNT1H									= 0x85
	.EQU	TCNT1L									= 0x84
	.EQU	OCR1AH									= 0x89
	.EQU	OCR1AL									= 0x88
	.EQU	OCR1BH									= 0x8b
	.EQU	OCR1BL									= 0x8a
	.EQU	TIFR1									= 0x36
	.EQU		ICF1								= 0x05
	.EQU		OCF1B								= 0x02
	.EQU		OCF1A								= 0x01
	.EQU		TOV1								= 0x00
	;---TIMER2---
	.EQU	TIMSK2									= 0x70
	.EQU		OCIE2B								= 0x02
	.EQU		OCIE2A								= 0x01
	.EQU		TOIE2								= 0x00
	.EQU	TCCR2A									= 0xb0
	.EQU		COM2A1								= 0x07
	.EQU		COM2A0								= 0x06
	.EQU		COM2B1								= 0x05
	.EQU		COM2B0								= 0x04
	.EQU		WGM21								= 0x01
	.EQU		WGM20								= 0x00
	.EQU	TCCR2B									= 0xb1
	.EQU		FOC2A								= 0x07
	.EQU		FOC2B								= 0x06
	.EQU		WGM22								= 0x03
	.EQU		CS22								= 0x02
	.EQU		CS21								= 0x01
	.EQU		CS20								= 0x00
	.EQU	TCNT2									= 0xb2
	.EQU	OCR2A									= 0xb3
	.EQU	OCR2B									= 0xb4
	.EQU	TIFR2									= 0x37
	.EQU		OCF2B								= 0x02
	.EQU		OCF2A								= 0x01
	.EQU		TOV2								= 0x00
	;---UART---
	.EQU	UDR0									= 0xc6
	.EQU	UCSR0A									= 0xc0
	.EQU		RXC0								= 0x07
	.EQU		TXC0								= 0x06
	.EQU		UDRE0								= 0x05
	.EQU		FE0									= 0x04
	.EQU		DOR0								= 0x03
	.EQU		UPE0								= 0x02
	.EQU		U2X0								= 0x01
	.EQU		MPCM0								= 0x00
	.EQU	UCSR0B									= 0xc1
	.EQU		RXCIE0								= 0x07
	.EQU		TXCIE0								= 0x06
	.EQU		UDRIE0								= 0x05
	.EQU		RXEN0								= 0x04
	.EQU		TXEN0								= 0x03
	.EQU		UCSZ02								= 0x02
	.EQU		RXB80								= 0x01
	.EQU		TXB80								= 0x00
	.EQU	UCSR0C									= 0xc2
	.EQU		UMSEL01								= 0x07
	.EQU		UMSEL00								= 0x06
	.EQU		UPM01								= 0x05
	.EQU		UPM00								= 0x04
	.EQU		USBS0								= 0x03
	.EQU		UCSZ01								= 0x02
	.EQU		UDCRD0								= 0x02
	.EQU		UCSZ00								= 0x01
	.EQU		UCPHA0								= 0x01
	.EQU		UCPOL0								= 0x00
	.EQU		URSEL0								= 0x08	;Для совместимости
	.EQU	UBRR0H									= 0xc5
	.EQU	UBRR0L									= 0xc4
	;---PCINT---
	.EQU	PCICR									= 0x68
	.EQU		PCIE2								= 0x02
	.EQU		PCIE1								= 0x01
	.EQU		PCIE0								= 0x00
	.EQU	PCIFR									= 0x3b
	.EQU		PCIF2								= 0x02
	.EQU		PCIF1								= 0x02
	.EQU		PCIF0								= 0x00
	.EQU	PCMSK0									= 0x6b
	.EQU		PCINT7								= 0x07
	.EQU		PCINT6								= 0x06
	.EQU		PCINT5								= 0x05
	.EQU		PCINT4								= 0x04
	.EQU		PCINT3								= 0x03
	.EQU		PCINT2								= 0x02
	.EQU		PCINT1								= 0x01
	.EQU		PCINT0								= 0x00
	.EQU	PCMSK1									= 0x6c
	.EQU		PCINT14								= 0x06
	.EQU		PCINT13								= 0x05
	.EQU		PCINT12								= 0x04
	.EQU		PCINT11								= 0x03
	.EQU		PCINT10								= 0x02
	.EQU		PCINT9								= 0x01
	.EQU		PCINT8								= 0x00
	.EQU	PCMSK2									= 0x6d
	.EQU		PCINT23								= 0x07
	.EQU		PCINT22								= 0x06
	.EQU		PCINT21								= 0x05
	.EQU		PCINT20								= 0x04
	.EQU		PCINT19								= 0x03
	.EQU		PCINT18								= 0x02
	.EQU		PCINT17								= 0x01
	.EQU		PCINT16								= 0x00
	;---INT---
	.EQU	EICRA									= 0x69
	.EQU		ISC11								= 0x03
	.EQU		ISC10								= 0x02
	.EQU		ISC01								= 0x01
	.EQU		ISC00								= 0x00
	.EQU	EIMSK									= 0x3d
	.EQU		INT0								= 0x00
	.EQU		INT1								= 0x01
	.EQU	EIFR									= 0x3c
	.EQU		INTF0								= 0x00
	.EQU		INTF1								= 0x01

	;---EEPROM---
	.EQU	EEARH									= 0x42
	.EQU	EEARL									= 0x41
	.EQU	EEDR									= 0x40
	.EQU	EECR									= 0x3F
	.EQU		EEPM1								= 0x05
	.EQU		EEPM0								= 0x04
	.EQU		EERIE								= 0x03
	.EQU		EEMPE								= 0x02
	.EQU		EEMWE								= EEMPE
	.EQU		EEPE								= 0x01
	.EQU		EEWE								= EEPE
	.EQU		EERE								= 0x00
	;---ADC---
	.EQU	ADMUX									= 0x7c
	.EQU		REFS1								= 0x07
	.EQU		REFS0								= 0x06
	.EQU		ADLAR								= 0x05
	.EQU		MUX3								= 0x03
	.EQU		MUX2								= 0x02
	.EQU		MUX1								= 0x01
	.EQU		MUX0								= 0x00
	.EQU	ADCSRA									= 0x7a
	.EQU		ADEN								= 0x07
	.EQU		ADSC								= 0x06
	.EQU		ADATE								= 0x05
	.EQU		ADIF								= 0x04
	.EQU		ADIE								= 0x03
	.EQU		ADPS2								= 0x02
	.EQU		ADPS1								= 0x01
	.EQU		ADPS0								= 0x00
	.EQU	ADCSRB									= 0x7b
	.EQU		ACME								= 0x06
	.EQU		ADTS2								= 0x02
	.EQU		ADTS1								= 0x01
	.EQU		ADTS0								= 0x00
	.EQU	ADCL									= 0x78
	.EQU	ADCH									= 0x79
	.EQU	ADC0									= 0x00
	.EQU	ADC1									= 0x01
	.EQU	ADC2									= 0x02
	.EQU	ADC3									= 0x03
	.EQU	ADC4									= 0x04
	.EQU	ADC5									= 0x05
	.EQU	ADC6									= 0x06
	.EQU	ADC7									= 0x07
	.EQU	ADCT									= 0x08
	;--SPI---
	.EQU	SPCR									= 0x4c
	.EQU		SPIE								= 0x07
	.EQU		SPE									= 0x06
	.EQU		DORD								= 0x05
	.EQU		MSTR								= 0x04
	.EQU		CPOL								= 0x03
	.EQU		CPHA								= 0x02
	.EQU		SPR1								= 0x01
	.EQU		SPR0								= 0x00
	.EQU	SPSR									= 0x4d
	.EQU		SPIF								= 0x07
	.EQU		WCOL								= 0x06
	.EQU		SPI2X								= 0x00
	.EQU	SPDR									= 0x4e
	;--SPM---
	.EQU	SPMCSR									= 0x57
	.EQU	SPMCR									= SPMCSR
	.EQU		SPMIE								= 0x07
	.EQU		RWWSB								= 0x06
	.EQU		SIGRD								= 0x05
	.EQU		RWWSRE								= 0x04
	.EQU		BLBSET								= 0x03
	.EQU		PGWRT								= 0x02
	.EQU		PGERS								= 0x01
	.EQU		SPMEN								= 0x00
	;---WATCHDOG---
	.EQU	WDTCR									= 0x60
	.EQU		WDIF								= 0x07
	.EQU		WDIE								= 0x06
	.EQU		WDP3								= 0x05
	.EQU		WDCE								= 0x04
	.EQU		WDE									= 0x03
	.EQU		WDP2								= 0x02
	.EQU		WDP1								= 0x01
	.EQU		WDP0								= 0x00

	;---ADC-VOLTAGE-REFERENCE---
	.EQU	ADC_VREF_AREF							= (0<<REFS1)|(0<<REFS0)
	.EQU	ADC_VREF_AVCC							= (0<<REFS1)|(1<<REFS0)
	.EQU	ADC_VREF_1_1_CAP						= (1<<REFS1)|(1<<REFS0)
	;---ADC-PRESCALLER---
	.EQU	ADC_PRESC_X1							= 0x00
	.EQU	ADC_PRESC_X2							= 0x01
	.EQU	ADC_PRESC_X4							= 0x02
	.EQU	ADC_PRESC_X8							= 0x03
	.EQU	ADC_PRESC_X16							= 0x04
	.EQU	ADC_PRESC_X32							= 0x05
	.EQU	ADC_PRESC_X64							= 0x06
	.EQU	ADC_PRESC_X128							= 0x07
;---TIMER-C-COMPARE-OUTPUT-MODES---
	.EQU	TIMER_C_COM_DISCONNECTED				= 0x00<<0x06
	.EQU	TIMER_C_COM_TOGGLE						= 0x01<<0x06
	.EQU	TIMER_C_COM_CLEAR						= 0x02<<0x06
	.EQU	TIMER_C_COM_SET							= 0x03<<0x06

	.SET	INT0_PORT								= PD2
	.SET	INT1_PORT								= PD3

.ORG 0x0000
	JMP	_INIT												; Reset Handler
	CALL	_C5_IR											; External Interrupt0 Handler
	CALL	_C5_IR											; External Interrupt1 Handler
	CALL	_C5_IR											; Pin Change Interrupt Request 0
	CALL	_C5_IR											; Pin Change Interrupt Request 1
	CALL	_C5_IR											; Pin Change Interrupt Request 2
	CALL	_C5_IR											; Watchdog Time-out Interrupt
	CALL	_C5_IR											; Timer/Counter2 Compare Match A
	CALL	_C5_IR											; Timer/Counter2 Compare Match B
	CALL	_C5_IR											; Timer/Counter2 Overflow
	CALL	_C5_IR											; Timer/Counter1 Capture Event
	CALL	_C5_IR											; Timer/Counter1 Compare Match A
	CALL	_C5_IR											; Timer/Counter1 Compare Match B
	CALL	_C5_IR											; Timer/Counter1 Overflow
	JMP	_C5_TIMER_A_IR										; Timer/Counter0 Compare Match A
	JMP	_C5_TIMER_B_IR										; Timer/Counter0 Compare Match B
	CALL	_C5_IR											; Timer/Counter0 Overflow
	CALL	_C5_IR											; SPI Serial Transfer Complete
	CALL	_C5_IR											; USART Rx Complete
	CALL	_C5_IR											; USART, Data Register Empty
	CALL	_C5_IR											; USART, Tx Complete
	CALL	_C5_IR											; ADC Conversion Complete
	CALL	_C5_IR											; EEPROM Ready
	CALL	_C5_IR											; Analog Comparator
	CALL	_C5_IR											; 2-wire Serial Interface
	CALL	_C5_IR											; Store Program Memory Ready

PORTS_TABLE:												;Таблица из адресов IO PINx регистров (-,B,C,D)
	.db	0x00,0x23,0x26,0x29
PCINT_TABLE:
	.db	0x00,PCMSK0,PCMSK1,PCMSK2
INT_TABLE:
	.db	(1<<INT0),(1<<INT1)
_INIT:
	CLI
	;Инициализация стека
	LDI TEMP,high(RAMEND)
	STS SPH,TEMP
	LDI TEMP,low(RAMEND)
	STS SPL,TEMP
	EOR C0x00,C0x00
	LDI TEMP,0xff
	MOV C0xff,TEMP
	JMP MAIN

APPLICATION_BLOCK:

.MACRO MCALL
	CALL @0
.ENDMACRO
.MACRO MJMP
	JMP @0
.ENDMACRO

.MACRO _C5_TIMERA_INIT
	_C5_POWER_ON PRTIM0										;Подпитываю TIMER0

	LDI TEMP,(0<<WGM01)|(0<<WGM00)
	STS TCCR0A,TEMP
	LDI TEMP,(0<<CS02)|(1<<CS01)|(0<<CS00)					;/8 - for 16MHz
	STS TCCR0B,TEMP
	STS TCNT0,C0x00
	LDI TEMP,TIMERS_SPEED-0x01								;0x64 - 100(16MHz)
	STS OCR0A,TEMP
	LDI TEMP,(1<<OCF0A)
	STS TIFR0,TEMP
.ENDMACRO

.MACRO _CORRE5277_TIMERA_START
	LDS TEMP,TIMSK0
	ORI TEMP,(1<<OCIE0A)
	STS TIMSK0,TEMP
.ENDMACRO

.MACRO _CORRE5277_TIMERA_CORRECTOR
	PUSH TEMP
	LDS TEMP,TCNT0
	ADD TEMP,@0
	STS OCR0A,TEMP
	LDI TEMP,(1<<OCF0A)
	STS TIFR0,TEMP
	POP TEMP
.ENDMACRO

.MACRO _C5_TIMERB
	LDS TEMP_L,TCNT0
	ADD TEMP_L,TEMP
	STS OCR0B,TEMP_L
	LDS TEMP_H,TIFR0
	ORI TEMP_H,(1<<OCF0B)
	STS TIFR0,TEMP_H
	;Установка флага
	LDI TEMP_H,(1<<_CFL_TIMER_B_USE)
	OR _C5_COREFLAGS,TEMP_H
	;Запуск таймера
	LDS TEMP_H,TIMSK0
	ORI TEMP_H,(1<<OCIE0B)
	STS TIMSK0,TEMP_H
	SBRC _C5_COREFLAGS,_CFL_TIMER_B_USE
	RJMP PC-0x01
	;Останов таймера
	LDS TEMP_H,TIMSK0
	ANDI TEMP_H,~(1<<OCIE0B)
	STS TIMSK0,TEMP_H
.ENDMACRO

.MACRO _C5_POWER_ON
	PUSH TEMP
	LDS TEMP,PRR
	ANDI TEMP,low(~(1<<@0))
	STS PRR,TEMP
	POP TEMP
.ENDMACRO
.MACRO _C5_POWER_OFF
	PUSH TEMP
	LDS TEMP,PRR
	ORI TEMP,low(1<<@0)
	STS PRR,TEMP
	POP TEMP
.ENDMACRO
