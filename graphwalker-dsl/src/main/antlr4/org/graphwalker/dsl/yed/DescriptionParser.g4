parser grammar DescriptionParser;

options {
	tokenVocab=YEdLabelLexer;
}

voidMethod
 : methodName ARGS_START argList? ARGS_END
 ;

methodName
 : IDENTIFIER_NAME
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
 : (stringExpression|numberExpression|datasetVariable|datasetStringVariable|booleanExpression)
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

datasetVariable
 : DATASET_PARAMETER
 ;

datasetStringVariable
 : DATASET_STRING_PARAMETER
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

parameterName
 : IDENTIFIER_NAME
 ;

labelArgList
 : labelArgument (ARG_SPLITTER DOCSPACE* labelArgument)*
 ;

labelArgument
 : parameterName DESCRIPTION_COLON DOCSPACE* stringVariable
 | parameterName DESCRIPTION_COLON DOCSPACE* numberVariable
 | parameterName DESCRIPTION_COLON DOCSPACE* booleanVariable
 ;
