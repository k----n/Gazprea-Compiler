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
 | declaration
 ;

declaration: type Identifier sizeData? (Assign expression)?;

returnStatement: Return expression?;

streamStatement: expression (LeftArrow | RightArrow) expression;

// Expressions
expression
 : Identifier
 | literal
 | '(' expression ')'
// | As '<' type '>' '(' expression ')'
// | generator
// | filter
 | functionCall
// | logicalOrExpression Concatenation primaryExpression
// | bitwiseOrExpression Or logicalOrExpression
// | bitwiseOrExpression Xor logicalOrExpression
// | bitwiseAndExpression And bitwiseOrExpression
// | equalityExpression Equals bitwiseAndExpression
// | equalityExpression NotEqual bitwiseAndExpression
// | comparisonExpression LessThan equalityExpression
// | comparisonExpression LessThanOrEqual equalityExpression
// | comparisonExpression GreaterThan equalityExpression
// | comparisonExpression GreaterThanOrEqual equalityExpression
// | byExpression By comparisonExpression
// | addExpression Sign byExpression
// | dotProductExpression DotProduct addExpression
// | multExpression Multiplication dotProductExpression
// | multExpression Division dotProductExpression
// | multExpression Modulus dotProductExpression
// | exponentiationExpression Exponentiation multExpression
// | Sign unaryExpression
// | Not unaryExpression
// | unaryExpression Interval unaryExpression
// | indexingExpression '[' expression ']'
 ;

// Literals
literal
 : NullLiteral
 | IdentityLiteral
// | BooleanLiteral
 | Sign? IntegerLiteral
// | RealLiteral
// | CharacterLiteral
// | vectorLiteral
// | StringLiteral
// | tupleLiteral
 ;

//vectorLiteral: '[' (expression (',' expression)*)? ']';
//
//tupleLiteral: '(' expression (',' expression)* ')';




// Keywords
Function: 'function';
Procedure: 'procedure';

Return: 'return';
Returns: 'returns';


//compilationUnit: translationUnit? EOF;
//
//translationUnit
// : statement_
// | translationUnit statement_
// ;
//
//statement_
// : statement ';'
// | notStatement
// ;
//
//statement
// : declaration
// | assignment
// | typedef
// | iterator
// | infiniteLoop
// | loop
// | conditional
// | Break
// | Continue
// | streamStatement
// | procedureCall
// ;
//
//notStatement
// : function
// | procedure
// | block
// ;
//assignment: Identifier Assign expression;
//
//typedef: Typedef type Identifier;
//
//generator: '[' Identifier In expression (',' Identifier In expression)? '|' expression ']';
//filter: '[' Identifier In expression '&' expression ']';
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
//procedureCall: Call functionCall;
functionCall: functionName '(' (expression (',' expression)*)? ')';
//
//type: typeName (typeData | sizeData)?;
//typeName
// : BuiltinType
// | Identifier
// ;
//typeData: '(' (type sizeData? Identifier? (',' type sizeData? Identifier?)*)? ')';

sizeData: '[' sizeDataValue (',' sizeDataValue)? ']';
sizeDataValue
 : Multiplication
 | IntegerLiteral
 ;

functionName
 : Identifier
 | BuiltinFunction
 ;

Assign: '=';
//Interval: '..';
//Concatenation: '||';
//DotProduct: '**';
//Equals: '==';
//NotEqual: '!=';
//LessThan: '<';
//GreaterThan: '>';
//LessThanOrEqual: '<=';
//GreaterThanOrEqual: '>=';
Multiplication: '*';
//Division: '/';
//Modulus: '%';
//Exponentiation: '^';
RightArrow: '->';
LeftArrow: '<-';

//In: 'in';
//By: 'by';
//As: 'as';
//Procedure: 'procedure';
//Function: 'function';
//Returns: 'returns';
//Return: 'return';
//Typedef: 'typedef';
//If: 'if';
//Else: 'else';
//Loop: 'loop';
//While: 'while';
//Break: 'break';
//Continue: 'continue';
//Not: 'not';
//And: 'and';
//Or: 'or';
//Xor: 'xor';
//Call: 'call';

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
//BooleanLiteral: True | False;
IntegerLiteral: (Digit '_'*)+;
//RealLiteral
// : FractionalConstant ExponentPart?
// | DigitSequence ExponentPart
// ;
//CharacterLiteral: '\'' Character '\'';
//StringLiteral: '"' CharacterSequence '"';

Sign: '+' | '-';

// Building Blocks
Identifier
 : NonDigit IdentifierCharacter*
 | '_' IdentifierCharacter+
 ;

//fragment True: 'true';
//fragment False: 'false';
//fragment FractionalConstant
// : DigitSequence? '.' DigitSequence
// | DigitSequence '.'?
// ;
//fragment DigitSequence: Digit+;
//fragment ExponentPart: [eE] Sign? DigitSequence;
//fragment Character
// : ~['\\\r\n]
// | EscapeSequence
// ;
//fragment CharacterSequence: StringCharacter+;
//fragment StringCharacter
// : ~["\\\r\n]
// | EscapeSequence
// | '\\\n'
// | '\\\r\n'
// ;
//fragment EscapeSequence: SimpleEscapeSequence;
//fragment SimpleEscapeSequence: '\\' [abnrt\\'"0];
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
