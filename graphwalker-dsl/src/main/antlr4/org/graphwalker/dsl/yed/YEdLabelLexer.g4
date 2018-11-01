lexer grammar YEdLabelLexer;

DOT       : '.';
SLASH     : '/';
COLON     : ':';
SEMICOLON : ';';
COMMA     : ',';
ASSIGN    : '=';
BLOCKED   : 'BLOCKED';
SHARED    : 'SHARED';
OUTDEGREE : 'OUTDEGREE';
INDEGREE  : 'INDEGREE';
INIT      : 'INIT';
SET       : 'SET' | 'Set' | 'set';
START     : [Ss][Tt][Aa][Rr][Tt];
REQTAG    : 'REQTAG';
WEIGHT    : [Ww][Ee][Ii][Gg][Hh][Tt];
DEPENDENCY    : [Dd][Ee][Pp][Ee][Nn][Dd][Ee][Nn][Cc][Yy];

JS_PLUS         : '+';
JS_MINUS        : '-';
JS_MUL          : '*';
JS_MOD          :	'%';
JS_INC          :	'++';
JS_DEC          :	'--';
JS_NOT          :	'!';
JS_PLUS_ASSIGN  : JS_PLUS ASSIGN;
JS_MINUS_ASSIGN : JS_MINUS ASSIGN;
JS_MUL_ASSIGN   : JS_MUL ASSIGN;
JS_DIV_ASSIGN   : SLASH ASSIGN;
JS_MOD_ASSIGN   : JS_MOD ASSIGN;
JS_LITERAL     	:	'"' (~["\r\n])* '"';
JS_ARRAY      	:	'[' ((Identifier (COMMA Identifier)*)|((JS_MINUS? Value) (COMMA (JS_MINUS? Value))*))? ']';
JS_FUNCTION     :	'function(' (Identifier (COMMA Identifier)*)? '){' (JS_FOR|(~[}\r\n]))* '}';
JS_FOR          :	'for(' (~[;\r\n])* ';' (~[;\r\n])* ';' (~[;\r\n])* '){' (~[}\r\n])* '}';
JS_BRACES      	:	'{' (Identifier COLON WHITESPACE* JS_MINUS? Value (COMMA WHITESPACE* (Identifier COLON WHITESPACE* JS_MINUS? Value))*)? '}';
JS_METHOD_CALL  :	Identifier '(' (~[)\r\n])* ')';

NestedBrackets
 :  '[' ( ~('[' | ']') | NestedBrackets )* ']'
 ;

Identifier
 : Letter LetterOrDigit*
 ;

Value
 : Integer | Integer? ('.' Digit+)
 ;

fragment
Integer
 : '0'
 | NonZeroDigit Digit*
 ;

fragment
Digit
 : '0'
 | NonZeroDigit
 ;

fragment
NonZeroDigit
 : [1-9]
 ;

fragment
Letter
 : [a-zA-Z$_]
 | ~[\u0000-\u00FF\uD800-\uDBFF]
   {Character.isJavaIdentifierStart(_input.LA(-1))}?
 | [\uD800-\uDBFF] [\uDC00-\uDFFF]
   {Character.isJavaIdentifierStart(Character.toCodePoint((char)_input.LA(-2), (char)_input.LA(-1)))}?
 ;

fragment
LetterOrDigit
 : [a-zA-Z0-9$_]
 | ~[\u0000-\u00FF\uD800-\uDBFF]
   {Character.isJavaIdentifierPart(_input.LA(-1))}?
 | [\uD800-\uDBFF] [\uDC00-\uDFFF]
   {Character.isJavaIdentifierPart(Character.toCodePoint((char)_input.LA(-2), (char)_input.LA(-1)))}?
 ;

LINE_COMMENT
 :   WHITESPACE '//' ~[\r\n]* -> skip
 ;

WHITESPACE
 : [ \t\r\n\u000C]+
 ;

JAVADOC_START
	: '/*' STAR* -> pushMode(IN_DESCRIPTION)
	;

COMMENT
 : '/*' ( ~[@*] | ('*' ~'/') )* '*/'
 ;

mode IN_DESCRIPTION;

MINUS            : '-';
PLUS             : '+';
ARG_SPLITTER     : ',';
ROOT_METHOD_END  : ';';
CODE_TAG         : '@code' DOCSPACE+;
BOOLEAN_VALUE    : 'true'|'false';
STRING_CAST      : '(String)'|'(string)';
NUMBER_CAST      : '(Number)'|'(number)';
BOOLEAN_CAST     : '(Boolean)'|'(boolean)';
METHOD_NAME      : LowerCaseLetter MethodLetter*;
ARGS_START       : '(';
ARGS_END         : ')';
JAVADOC_END      : WHITESPACE? STAR* '*/' -> popMode;

DESCRIPTION_COMMENT
 : (';' | '\r' '\n' | '\n' | '\r') (~'*' | '*' ~'/')*
 ;

STAR
	: '*'
	;

DOCSPACE
 : [ \t\r\n]+
 ;

STRING_LITERAL
	:	'"' StringCharacters? '"'
	;

fragment
LowerCaseLetter
	:	[a-z]
	;

fragment
MethodLetter
	:	[a-zA-Z0-9]
	;

fragment
StringCharacters
	:	StringCharacter+
	;

fragment
StringCharacter
	:	~["\\]
	|	EscapeSequence
	;

fragment
EscapeSequence
	:	'\\' [btnfr"'\\]
	|	OctalEscape
	;

fragment
OctalEscape
	:	'\\' OctalDigit
	|	'\\' OctalDigit OctalDigit
	|	'\\' ZeroToThree OctalDigit OctalDigit
	;

fragment
ZeroToThree
	:	[0-3]
	;

fragment
OctalDigit
	:	[0-7]
	;

REAL_VALUE
   : Decimal ('.' DecimalDigit+)?
   ;

fragment Decimal
  :
  '0' | '1'..'9' DecimalDigit*
  ;

fragment DecimalDigit
  :
  '0'..'9' ;
