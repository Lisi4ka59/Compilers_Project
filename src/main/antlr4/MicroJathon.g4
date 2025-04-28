grammar MicroJathon;

program: statement* EOF;

statement
    : variable '=' expr ';'
    | 'print' '(' expr ')' ';'
    | 'if' '(' expr ')' block ('else' block)?
    | 'while' '(' expr ')' block
    | block
    ;

block: '{' statement* '}';

expr

    : expr 'or' expr                  # OrExpr
    | expr 'and' expr                 # AndExpr
    | 'not' expr                      # NotExpr
    | expr op=('*'|'/') expr         # MulDivExpr
    | expr op=('+'|'-') expr         # AddSubExpr
    | expr op=('=='|'!='|'<'|'>'|'<='|'>=') expr # CompareExpr
    | 'round' '(' expr ')'           # RoundExpr
    | '(' expr ')'                   # ParenExpr
    | INT                            # IntExpr
    | FLOAT                          # FloatExpr
    | STRING                         # StringExpr
    | variable                       # VarExpr
    ;

variable: ID;

// Lexer rules
ID      : [a-zA-Z_] [a-zA-Z_0-9]*;
INT     : [0-9]+;
FLOAT   : [0-9]+'.'[0-9]+;
STRING  : '"' (~["\\])* '"';
WS      : [ \t\r\n]+ -> skip;
COMMENT : '//' ~[\r\n]* -> skip;