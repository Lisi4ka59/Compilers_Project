main:
li x10, 1059
ewrite x10
li x10, 1089
ewrite x10
li x10, 1083
ewrite x10
li x10, 1086
ewrite x10
li x10, 1074
ewrite x10
li x10, 1080
ewrite x10
li x10, 1077
ewrite x10
li x10, 32
ewrite x10
li x10, 1073
ewrite x10
li x10, 1077
ewrite x10
li x10, 1079
ewrite x10
li x10, 32
ewrite x10
li x10, 101
ewrite x10
li x10, 108
ewrite x10
li x10, 115
ewrite x10
li x10, 101
ewrite x10
li x10, 10
ewrite x10
li x5, 5
li x6, a
sw x6, 0, x5
li x5, 10
li x6, b
sw x6, 0, x5
li x7, a
lw x5, x7, 0
addi x6, x5, 0
li x7, b
lw x5, x7, 0
slt x5, x6, x5
beq x5, x0, L0
li x5, 1
addi x10, x5, 0
jal x1, print_int
li x10, 10
ewrite x10
jal x0, L1
L0:
L1:
li x10, 1059
ewrite x10
li x10, 1089
ewrite x10
li x10, 1083
ewrite x10
li x10, 1086
ewrite x10
li x10, 1074
ewrite x10
li x10, 1080
ewrite x10
li x10, 1077
ewrite x10
li x10, 32
ewrite x10
li x10, 1089
ewrite x10
li x10, 32
ewrite x10
li x10, 101
ewrite x10
li x10, 108
ewrite x10
li x10, 115
ewrite x10
li x10, 101
ewrite x10
li x10, 10
ewrite x10
li x5, 15
li x6, c
sw x6, 0, x5
li x7, c
lw x5, x7, 0
addi x6, x5, 0
li x5, 10
slt x5, x6, x5
beq x5, x0, L2
li x5, 0
addi x10, x5, 0
jal x1, print_int
li x10, 10
ewrite x10
jal x0, L3
L2:
li x5, 2
addi x10, x5, 0
jal x1, print_int
li x10, 10
ewrite x10
L3:
li x10, 1062
ewrite x10
li x10, 1080
ewrite x10
li x10, 1082
ewrite x10
li x10, 1083
ewrite x10
li x10, 32
ewrite x10
li x10, 119
ewrite x10
li x10, 104
ewrite x10
li x10, 105
ewrite x10
li x10, 108
ewrite x10
li x10, 101
ewrite x10
li x10, 10
ewrite x10
li x5, 0
li x6, i
sw x6, 0, x5
L4:
li x7, i
lw x5, x7, 0
addi x6, x5, 0
li x5, 3
slt x5, x6, x5
beq x5, x0, L5
li x7, i
lw x5, x7, 0
addi x10, x5, 0
jal x1, print_int
li x10, 10
ewrite x10
li x7, i
lw x5, x7, 0
addi x6, x5, 0
li x5, 1
add x5, x6, x5
li x6, i
sw x6, 0, x5
jal x0, L4
L5:
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
b:
data 0 * 1
c:
data 0 * 1
i:
data 0 * 1

buf:
data 0 * 12
