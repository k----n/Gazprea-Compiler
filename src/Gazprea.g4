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
    : subfloat 'e' ('_')*? ('+'|'-')? subfloat
    | INTEGER 'e' ('_')*? ('+'|'-')? INTEGER
    | INTEGER 'e' ('_')*? ('+'|'-')? subfloat
    | subfloat 'e' ('_')*? ('+'|'-')? INTEGER
    | subfloat
    ;

subfloat
    : ('_')*? '.' UINTEGER
    | UINTEGER '.' US?
    | UINTEGER '.' UINTEGER
    ;

INTEGER: [0-9]+;
ID: [_a-zA-Z][_a-zA-Z0-9]*;

SC: ';';

BLOCKCOMMENT: '/*' .*? '*/' -> skip;
LINECOMMENT: '//' ~[\r\n]* -> skip;
WS: [ \t\r\n]+ -> skip;