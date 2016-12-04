grammar Gazprea;

compilationUnit: topLevelCode+ EOF;

topLevelCode
 : declaration ';'
 | codeBlock
 ;

codeBlock
 : function
 | procedure
 ;

// Functions + Procedures
function: Function Identifier argumentList returnType (functionBlock | ';');
procedure: Procedure Identifier argumentList returnType? (functionBlock | ';');

argumentList: '(' (argument (',' argument)*)? ')';
argument: type Identifier;
returnType: Returns type;

functionBlock
 : block
 | '=' expression ';'
 ;

// Blocks
block: '{' translationalUnit* '}';

tupleTypeDetails: '(' (tupleTypeAtom ',')+ tupleTypeAtom ')';
tupleTypeAtom: type Identifier? sizeData?;

// Types
type
 : TypeSpecifier? typeName tupleTypeDetails? TypeType?
 | TypeSpecifier typeName? TypeType?
 ;

typeName
 : BuiltinType
 | Identifier
 ;

// Non-Top Level Code
translationalUnit
 : statement_
 | block
 ;

// Statements
statement_
 : statement ';'
 | notstatement
 ;

statement
 : returnStatement
 | breakStatement
 | continueStatement
 | streamStatement
 | assignment
 | declaration
 | procedureCall
 ;

notstatement
 : conditional
 | infiniteLoop
 | predicatedLoop
 | iteratorLoop
 ;

declaration: type Identifier sizeData? (Assign expression)?
            | typedef;

returnStatement: Return expression?;

breakStatement: Break;

continueStatement: Continue;

streamStatement: expression arrow=(LeftArrow | RightArrow) expression;

conditional: If expression translationalUnit (Else translationalUnit)?;


// loops
infiniteLoop: Loop translationalUnit;

predicatedLoop: prePredicatedLoop | postPredicatedLoop;
prePredicatedLoop: Loop While expression translationalUnit;
postPredicatedLoop: Loop translationalUnit While expression ';';

iteratorLoop: Loop iteratorLoopVariables translationalUnit;
iteratorLoopVariables: iteratorLoopVariable (',' iteratorLoopVariable)*;
iteratorLoopVariable: Identifier In expression;

// Expressions
expression
 : expression Dot Dot expression // interval
 | expression Dot RealLiteral // interval
 | RealLiteral RealLiteral // interval
 | RealLiteral Dot expression // interval
 | expression RealLiteral // tuple access
 | expression Dot Identifier // tuple access
 | literal
 | Identifier
 | As '<' (type | tupleTypeDetails) '>' ('(' expression ')' | expression)
 | generator
 | filter
 | functionCall
 | expression '[' expression (',' expression)* ']'
 | expression op=Interval expression // TODO: this may never be accessed STILL MAKE FUNCTION FOR IT ANYWAYS
 | <assoc=right> op=(Sign|Not) expression
 | <assoc=right> expression op=Exponentiation expression
 | expression op=DotProduct expression // TODO: NOT SURE ABOUT THIS PRECEDENCE
 | expression op=(Asteriks|Division|Modulus) expression
 | expression op=Sign expression
 | expression op=By expression
 | expression op=(LessThan|LessThanOrEqual|GreaterThan|GreaterThanOrEqual) expression
 | expression op=(Equals|NotEqual) expression
 | expression op=And expression
 | expression op=(Or|Xor) expression
 | <assoc=right> expression op=Concatenation expression
 | '(' expression ')'
 ;

// Literals
literal
 : NullLiteral
 | IdentityLiteral
 | BooleanLiteral
 | RealLiteral
 | Sign? IntegerLiteral
 | CharacterLiteral
 | vectorLiteral
 | StringLiteral
 | tupleLiteral
 ;

// important to note empty matrices will look like vectors
matrixLiteral: '[' vectorLiteral (',' vectorLiteral)* ']';
vectorLiteral: '[' (expression (',' expression)*)? ']';
tupleLiteral: '(' expression (',' expression)+ ')';


// Keywords
Function: 'function';
Procedure: 'procedure';

Return: 'return';
Returns: 'returns';

assignment
    : Identifier RealLiteral Assign expression
    | Identifier Dot Identifier Assign expression
    | Identifier (',' Identifier)* Assign expression
    ;

typedef: Typedef type Identifier;

generator: '[' Identifier In expression (',' Identifier In expression)? '|' expression ']';
filter: '[' Identifier In expression '&' expression ']';

//
//iterator: Loop Identifier In expression block;
//infiniteLoop: Loop block;
//loop: Loop While expression block;
//conditional: If '(' expression ')' block (Else (block | conditional))? ;
//block
// : '{' translationUnit '}'
// | statement ';'
// ;
//
//function: Function Identifier typeData Returns type ((block | Assign expression ';') | ';');
//procedure: Procedure Identifier typeData (block | ';');
procedureCall: Call functionCall;
functionCall: functionName '(' (expression (',' expression)*)? ')';
//
//type: typeName (typeData | sizeData)?;
//typeName
// : BuiltinType
// | Identifier
// ;
//typeData: '(' (type sizeData? Identifier? (',' type sizeData? Identifier?)*)? ')';

sizeData: '[' (expression | Asteriks) ']'
        | '[' (Asteriks | expression) ',' (Asteriks | expression) ']' // matrix size data
        ;

functionName
 : Identifier
 | BuiltinFunction
 ;

Assign: '=';
Interval: '..';
Dot: '.';
Concatenation: '||';
DotProduct: '**';
Equals: '==';
NotEqual: '!=';
LessThan: '<';
GreaterThan: '>';
LessThanOrEqual: '<=';
GreaterThanOrEqual: '>=';
Asteriks: '*';
Division: '/';
Modulus: '%';
Exponentiation: '^';
RightArrow: '->';
LeftArrow: '<-';

In: 'in';
By: 'by';
As: 'as';
Typedef: 'typedef';
If: 'if';
Else: 'else';
Loop: 'loop';
While: 'while';
Break: 'break';
Continue: 'continue';
Not: 'not';
And: 'and';
Or: 'or';
Xor: 'xor';

Call: 'call';

TypeSpecifier
 : 'var'
 | 'const'
 ;

TypeType
: 'vector'
| 'interval'
| 'matrix';

BuiltinType
 : 'boolean'
 | 'integer'
 | 'real'
 | 'character'
 | 'interval'
 | 'vector'
 | 'string'
 | 'matrix'
 | 'tuple'
 ;

BuiltinFunction
 : 'std_input'
 | 'std_output'
 | 'length'
 ;

// Literals
NullLiteral: 'null';
IdentityLiteral: 'identity';
BooleanLiteral: True | False;
IntegerLiteral: (Digit '_'*)+;
RealLiteral
 : FractionalConstant ExponentPart?
 | DigitSequence ExponentPart
 ;
CharacterLiteral: '\'' Character '\'';
StringLiteral: '"' CharacterSequence '"';

Sign: '+' | '-';

// Building Blocks
Identifier
 : NonDigit IdentifierCharacter*
 | '_' IdentifierCharacter+
 ;

fragment True: 'true';
fragment False: 'false';
fragment FractionalConstant
 : DigitSequence? Dot '_'* DigitSequence
 | DigitSequence Dot?
 ;
fragment DigitSequence: (Digit '_'*)+;
fragment ExponentPart: [eE] '_'* Sign? '_'* DigitSequence;
fragment Character
 : ~['\\\r\n]
 | EscapeSequence
 ;
fragment CharacterSequence: StringCharacter+;
fragment StringCharacter
 : ~["\\\r\n]
 | EscapeSequence
 | '\\\n'
 | '\\\r\n'
 ;
fragment EscapeSequence: SimpleEscapeSequence;
fragment SimpleEscapeSequence: '\\' [abnrt\\'"0];
fragment NonDigit: [a-zA-Z];
fragment Digit: [0-9];
fragment IdentifierCharacter
 : Digit
 | NonDigit
 | '_'
 ;

// Ignore
Whitespace: [ \t]+ -> skip;
NewLine: ('\r' '\n'? | '\n') -> skip;
LineComment: '//' (~'\n')* -> skip;
BlockComment: '/*' .*? '*/' -> skip;