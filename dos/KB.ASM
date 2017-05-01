; kbc tests
; irq 0 is timer (delivered on int 1c)
; irq 1/int 09 is keyboard

bits 16
org 100h
jmp start

%include "util.asm"

%define PICCMD 0x20
%define PICDATA 0x21
%define KBDATA 0x60
%define KBCMD 0x64
%define EOI 0x20
%define READCONFIG 0x20
%define WRITECONFIG 0x60

section .text
start:

call installtimer
call installkb
call kbstop

call printstatus
call readconfig

;call echo
;call echo2

call selftest
mov al, 0x45
call writeconfig

call kbstart


; test disableaux, enableaux
; disablekey, enablekey
; iftestaux
; writekeyout
; writeauxout
; writeauxin
; test inhibit int on pic (io port 21h)


PLN
PS "exit", NL
call printstatus
call removekb
call removetimer
mov ah, 0
int 21h






echo2: ; async echo
PS "async echo = "
call clearkbdata
mov ax, 100
call sleep
call printkbdata
ret

kbstop: ; stop kb interrupts on PIC
in al, PICDATA
or al, 2
out PICDATA, al
ret

kbstart: ; start kb interrupts on PIC
in al, PICDATA
and al, 0xfd
out PICDATA, al
ret



sleep: ; (ax=ticks)
mov bx, [tick]
add bx, ax
.a:
mov cx, [tick]
cmp cx, bx
jl .a
ret

clearkbdata: ; requires kbstop
mov [kbcount], word 0
ret

printkbdata: ; requires kbstop
mov ax, [kbcount]
PS "kbcount = "
P16 ax
PS " kbdata = "
mov bx, 0
.a:
cmp bx, ax 
jge .x
P8 [kbdata+bx]
PS " "
inc bx
jmp .a
.x:
PLN
ret

echo: ; requires kbstop
mov bx, [tick]
PS "sync echo = "
.a:
in al, KBCMD
test al, 1 ; can read?
jz .b
in al, KBDATA
P8 al
PS " "
.b:
mov cx, [tick]
sub cx, bx
cmp cx, 100 ; timeout
jge .x
jmp .a
.x:
PLN
call kbstart
ret


keyouttest:
mov [kbcount], word 0
PS "kbcmd d2 (keyout, should read ff) = "
mov al, 0xd2
call kbcmd
jnz .a
PS "cmd error"
jmp .x
.a:
mov al, 0xff
call kbwrite
jnz .b
PS "write error"
jmp .x
.b:
call waitint
jnz .c
PS "int timeout"
jmp .x
.c:
call kbread
jnz .x
PS "read error"
.x:
PLN
ret


selftest: ; reqires kbstop
PS "kbcmd aa (selftest) = "
mov al, 0xaa
call kbcmd
jnz .b
PS "cmd error "
jmp .x
.b:
call kbread
jnz .c
PS "read error "
jmp .x
.c:
P8 al
.x:
PLN
call printstatus
call readconfig
call kbstart
ret



printstatus: ; () -> (al=status)
in al, KBCMD
PS "kbstatus = "
P8 al
PS " = "
P8B al
PLN
ret

writeconfig: ; requires kbstop (al=config) -> (nz=success)
PUSH ax, bx
mov bl, al
PS "sync write config = "
mov al, WRITECONFIG
call kbcmd
jnz .b
PS "cmd error"
jmp .x
.b:
mov al, bl
call kbwrite
jnz .c
PS "data error"
jmp .x
.c:
P8 al
PS " = "
P8B al
.x:
PLN
POP ax,bx
ret

readconfig: ; read config without handler -> (nz=success, al=config)
PS "sync read config = "
mov al, READCONFIG
call kbcmd
jnz .b
PS "cmd error"
jmp .x
.b:
call kbread
jnz .c
PS "read error"
jmp .x
.c:
P8 al
PS " = "
P8B al
.x:
PLN
ret



;;;;; kb functions ;;;;;




kbcmd: ; write keyboard command (al=cmd) -> (nz=success)
call waitin
jz .x
out KBCMD, al
.x:
ret

kbread: ; read keyboard data () -> (nz=success,al=data)
call waitout
jnz .a
;PS "KBR0 "
mov al, 0xff
jmp .x
.a:
in al, KBCMD
;PS "KBR1CMD="
;P8B al
;PS " "
in al, KBDATA
.x:
ret

kbwrite: ; write keyboard data (al=data) -> (nz=success)
call waitin
jz .x
out KBDATA, al
.x:
ret

waitint: ; wait for kb int () -> (nz=success)
PUSH ax,bx,cx,dx
mov bx, [tick]
.a:
mov cx, [kbcount]
test cx, cx
jnz .b
mov dx, [tick]
sub dx, bx
cmp dx, 20
jl .a
mov bx, 0 ; timeout
jmp .x
.b:
mov bx, 1 ; success
.x:
mov [kbcount], word 0
test bx,bx
POP dx,cx,bx,ax
ret

waitin: ; wait for input buffer clear () -> (nz=success)
PUSH ax,bx,cx,dx
mov cx, [tick]
.a:
in al, KBCMD
test al, 2
jz .b
mov dx, [tick]
sub dx, cx
cmp dx, 20
jl .a
mov bx, 0 ; fail
jmp .x
.b: ; success
mov bx, 1
.x:
test bx,bx
POP dx,cx,bx,ax
ret


waitout: ; wait for output buffer full () -> (nz=success)
PUSH ax,bx,cx,dx
mov cx, [tick]
.a:
in al, KBCMD
test al, 1
jnz .b
mov dx, [tick]
sub dx, cx
cmp dx, 20
jl .a
;PS "WO0 "
mov bx, 0 ; fail
jmp .x
.b:
;PS "WO1="
;P8B al
;PS " "
mov bx, 1 ; success
.x:
test bx,bx
POP dx,cx,bx,ax
ret




kbhandler: ; read keyboard byte, add to buffer, send eoi
PUSHA
mov ax, cs
mov ds, ax
mov bx, [kbcount]
in al, KBCMD
test al, 1 ; is output full?
jz .x
in al, KBDATA
cmp bx, 16 ; is buffer full?
jge .x
mov [kbdata+bx], al
.x:
inc bx
mov [kbcount], bx
mov al, EOI
out PICCMD, al
POPA
iret


timerhandler:
;PUSHA
inc word [cs:tick]
;POPA
iret


ticktest: ; test the timer handler
mov dx, [tick]
PS "tick1="
P16 dx
.t:
mov cx, [tick]
cmp cx, dx
je .t
PS " tick2="
P16 cx
PLN
ret

installtimer:
mov bx, 0x1c
mov cx, cs
mov dx, timerhandler
call sethandler
mov [timer], si
mov [timer+2], di
ret

removetimer:
mov bx, 0x1c
mov cx, [timer]
mov dx, [timer+2]
call sethandler
ret

installkb:
mov bx, 0x09
mov cx, cs
mov dx, kbhandler
call sethandler
mov [keyboard], si
mov [keyboard+2], di
ret

removekb:
mov bx, 0x09
mov cx, [keyboard]
mov dx, [keyboard+2]
call sethandler
ret


sethandler: ; install new handler (bx=int, cx:dx=seg:p) -> (si:di=old)
push ax
cli
mov ax, 0
mov es, ax
shl bx, 2
mov si, [es:bx+2]
mov di, [es:bx]
mov [es:bx+2], cx
mov [es:bx], dx
sti
pop ax
PS "vector "
P16 bx
PS " was "
P16 si
PS ":"
P16 di
PS " now "
P16 cx
PS ":"
P16 dx
PLN
ret






section .data
tick: dw 0
timer: times 2 dw 0
keyboard: times 2 dw 0
kbcount: dw 0
kbdata: times 16 db 0
timermsg: db '.$'
