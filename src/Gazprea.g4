grammar Gazprea;

init:
    statement*;


statement:
    declaration
    ;

declaration:
    specifier?  ID '='  SC
    ;

specifier:
;

LT: '<';
GT: '>';
NE: '!=';
EQ: '==';

float
    :
    | UFLOAT 'e' US? ('+'|'-')? UFLOAT
    | UINTEGER 'e' US? ('+'|'-')? UINTEGER
    | UINTEGER 'e' US? ('+'|'-')? UFLOAT
    | UFLOAT 'e' US? ('+'|'-')? UINTEGER
    | UFLOAT
    ;

SC: ';';

UFLOAT
    : US? '.' UINTEGER
    | UINTEGER '.' US?
    | UINTEGER '.' UINTEGER
    ;
US: [_]*;

UINTEGER: [_]*[0-9][0-9_]*;
INTEGER: [0-9]+;
ID: [_a-zA-Z][_a-zA-Z0-9]*;

BLOCKCOMMENT: '/*' .*? '*/' -> skip;
LINECOMMENT: '//' ~[\r\n]* -> skip;
WS: [ \t\r\n]+ -> skip;