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

// Types
type
 : TypeSpecifier? typeName TypeType?
 | TypeSpecifier typeName? TypeType?
 ;

typeName
 : BuiltinType
 | Identifier
 ;

// Non-Top Level Code
translationalUnit
 : statement ';'
 | block
 ;

// Statements
statement
 : returnStatement
 | streamStatement
 | assignment
 | declaration
 | procedureCall
 ;

declaration: type Identifier sizeData? (Assign expression)?;

returnStatement: Return expression?;

streamStatement: expression arrow=(LeftArrow | RightArrow) expression;

// Expressions
expression
 : Identifier
 | literal
 | '(' expression ')'
 | As '<' type '>' '(' expression ')'
 | generator
 | filter
 | functionCall
 | <assoc=right> expression op=Concatenation expression
 | expression op=(Or|Xor) expression
 | expression op=And expression
 | expression op=(Equals|NotEqual) expression
 | expression op=(LessThan|LessThanOrEqual|GreaterThan|GreaterThanOrEqual) expression
 | expression op=By expression
 | expression op=Sign expression
 | expression op=DotProduct expression
 | expression op=(Multiplication|Division|Modulus) expression
 | <assoc=right> expression op=Exponentiation expression
 | <assoc=right> op=(Sign|Not) expression
 | expression op=Interval expression
 | expression '[' expression ']'
 ;

// Literals
literal
 : NullLiteral
 | IdentityLiteral
 | BooleanLiteral
 | Sign? IntegerLiteral
 | RealLiteral
 | CharacterLiteral
 | vectorLiteral
 | StringLiteral
 | tupleLiteral
 ;

vectorLiteral: '[' (expression (',' expression)*)? ']';
//
tupleLiteral: '(' expression (',' expression)* ')';


// Keywords
Function: 'function';
Procedure: 'procedure';

Return: 'return';
Returns: 'returns';

assignment: Identifier Assign expression;

typedef: Typedef type Identifier;
//
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

sizeData: '[' IntegerLiteral (',' IntegerLiteral)? ']';

functionName
 : Identifier
 | BuiltinFunction
 ;

Assign: '=';
Interval: '..';
Concatenation: '||';
DotProduct: '**';
Equals: '==';
NotEqual: '!=';
LessThan: '<';
GreaterThan: '>';
LessThanOrEqual: '<=';
GreaterThanOrEqual: '>=';
Multiplication: '*';
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

TypeType: 'vector';

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
 : DigitSequence? '.' DigitSequence
 | DigitSequence '.'?
 ;
fragment DigitSequence: Digit+;
fragment ExponentPart: [eE] Sign? DigitSequence;
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
