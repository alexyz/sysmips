abs.s f0, f1
add $1, $2, zero
add.d f4, f6, f8
addi t0, t1, 1234
start:
addiu $4, $5, 0x10
addu r20, r21, r22
and s0, s1, s2
andi ra, ra, -1
b x1
nop
x1: 
bal start
bc1f start
bc1t start
beq $15, $16, start
beql r2, r3, start
bgez r5, start


end:
break 1
