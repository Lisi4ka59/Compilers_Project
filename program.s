.data
newline: .asciiz "\n"
a: .word 0
b: .word 0
str4: .asciiz "НОД для чисел 1071 и 462:"

.text
.globl main
main:
li t0, 1071
sw t0, a
li t0, 462
sw t0, b
L0:
lw t0, a
addi sp, sp, -4
sw t0, 0(sp)
lw t0, b
lw t1, 0(sp)
addi sp, sp, 4
sub t0, t1, t0
snez t0, t0
beqz t0, L1
lw t0, a
addi sp, sp, -4
sw t0, 0(sp)
lw t0, b
lw t1, 0(sp)
addi sp, sp, 4
sgt t0, t1, t0
beqz t0, L2
lw t0, a
addi sp, sp, -4
sw t0, 0(sp)
lw t0, b
lw t1, 0(sp)
addi sp, sp, 4
sub t0, t1, t0
sw t0, a
j L3
L2:
lw t0, b
addi sp, sp, -4
sw t0, 0(sp)
lw t0, a
lw t1, 0(sp)
addi sp, sp, 4
sub t0, t1, t0
sw t0, b
L3:
j L0
L1:
la a0, str4
li a7, 4
ecall
la a0, newline
li a7, 4
ecall
lw t0, a
mv a0, t0
li a7, 1
ecall
la a0, newline
li a7, 4
ecall
li a7, 93
ecall
