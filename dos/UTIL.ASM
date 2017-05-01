
%define NL 13,10

%macro PUSHA 0
push ds
push es
pushf
pusha
%endmacro

%macro POPA 0
popa
popf
pop es
pop ds
%endmacro

%macro PUSH 1-* ; push all
%rep %0
push %1
%rotate 1
%endrep
%endmacro

%macro POP 1-* ; pop all
%rep %0
pop %1
%rotate 1
%endrep
%endmacro

%macro PLN 0 ; print line
call println
%endmacro

%macro PS 1+ ; print string
section .data
%%msg: db %1,'$'
section .text
push dx
mov dx, %%msg
call print
pop dx
%endmacro

%macro P8 1 ; print 8 bit hex
push ax
mov al, %1
call printal
pop ax
%endmacro

%macro P8B 1 ; print 8 bit binary
push ax
mov al, %1
call printalb
pop ax
%endmacro

%macro P16 1 ; print 16 bit hex
push ax
mov ax, %1
call printax
pop ax
%endmacro

%macro P16B 1 ; print 16 bit binary
push ax
mov ax, %1
call printaxb
pop ax
%endmacro

section .text

print: ; (dx=string)
PUSHA
mov ah, 0x09
int 0x21
POPA
ret

println:
PUSHA
mov dx, newline
mov ah, 0x09
int 0x21
POPA
ret

printal: ; print al
PUSHA
mov si, ax
shr ax, 4
mov di, ax
and si, 0xf
and di, 0xf
mov al, [hexdigits+di]
mov bl, [hexdigits+si]
mov [tmp], al
mov [tmp+1], bl
mov [tmp+2], byte '$'
mov dx, tmp
mov ah, 0x09
int 0x21
POPA
ret

printax: ; print ax
PUSHA
mov di, 3
pax1:
mov si, ax
and si, 0xf
mov bl, [hexdigits+si]
mov [tmp+di], bl
dec di
shr ax, 4
cmp di, 0
jge pax1
mov [tmp+4], byte '$'
mov dx, tmp
mov ah, 0x09
int 0x21
POPA
ret

printalb: ; print al as binary (al)
PUSHA
mov si, 0
.a:
test al, 0x80
jz .b
mov bx, '7'
sub bx, si
jmp .c
.b:
mov bl, '.'
.c:
mov [tmp+si], bl
shl al, 1
inc si
cmp si, 8
jl .a
mov [tmp+si], byte '$'
mov dx, tmp
mov ah, 9
int 21h
POPA
ret

printaxb: ; print ax as binary (ax)
PUSHA
mov si, 0
.a:
test ax, 0x8000
jz .b
mov bx, hexdigits+15
sub bx, si
mov bl, [bx]
;mov bl,'*'
jmp .c
.b:
mov bl, '.'
.c:
mov [tmp+si], bl
shl ax, 1
inc si
cmp si, 16
jl .a
mov [tmp+si], byte '$'
mov dx, tmp
mov ah, 9
int 21h
POPA
ret

printflags:
PUSHA
pushf
pop ax
PS "flags = "
P16B ax
PLN
POPA
ret

section .data

hexdigits: db "0123456789abcdef"
newline: db 13,10,'$'
tmp: times 32 db 0
