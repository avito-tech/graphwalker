parser grammar DescriptionParser;

options {
	tokenVocab=YEdLabelLexer;
}

voidMethod
 : methodName ARGS_START argList? ARGS_END
 ;

methodName
 : METHOD_NAME
 ;

stringMethod
 : STRING_CAST DOCSPACE* methodName ARGS_START argList? ARGS_END
 ;

numberMethod
 : NUMBER_CAST DOCSPACE* methodName ARGS_START argList? ARGS_END
 ;

booleanMethod
 : BOOLEAN_CAST DOCSPACE* methodName ARGS_START argList? ARGS_END
 ;

argList
 : argument (ARG_SPLITTER DOCSPACE* argument)*
 ;

argument
 : (stringExpression|numberExpression|booleanExpression)
 ;

voidExpression
 : voidMethod
 ;

stringExpression
 : stringVariable|stringMethod
 ;

numberExpression
 : numberVariable|numberMethod
 ;

booleanExpression
 : booleanVariable|booleanMethod
 ;

stringVariable
 : STRING_LITERAL
 ;

numberVariable
 : (MINUS)? REAL_VALUE
 ;

booleanVariable
 : BOOLEAN_VALUE
 ;
