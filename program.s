main:
addi x6, x5, 0
seq x5, x6, x5
li x6, a
sw x6, 0, x5
li x7, a
lw x5, x7, 0
addi x10, x5, 0
jal x1, print_int
li x10, 10
ewrite x10
ebreak

print_int:
beq x10, x0, print_int_zero
blt x10, x0, print_int_neg
addi x5, x10, 0
li x6, 0
li x7, 10
print_div_loop:
div x8, x5, x7
rem x9, x5, x7
addi x5, x8, 0
li x11, buf
add x11, x11, x6
sw x11, 0, x9
addi x6, x6, 1
bne x5, x0, print_div_loop
print_print_loop:
addi x6, x6, -1
li x11, 48
li x13, buf
add x13, x13, x6
lw x9, x13, 0
add x11, x11, x9
ewrite x11
bne x6, x0, print_print_loop
jalr x0, x1, 0
print_int_zero:
li x11, 48
ewrite x11
jalr x0, x1, 0
print_int_neg:
li x11, 45
ewrite x11
sub x5, x0, x10
addi x10, x5, 0
jal x0, print_int

a:
data 0 * 1

buf:
data 0 * 12
