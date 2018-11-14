parser grammar YEdEdgeParser;

options {
	tokenVocab=YEdLabelLexer;
}

import DescriptionParser;

parse
 locals [java.util.Set<String> fields = new java.util.HashSet<String>();]
 : field* EOF
 ;

field
 : {!$parse::fields.contains("names")}? names {$parse::fields.add("names");}
 | {!$parse::fields.contains("guard")}? guard {$parse::fields.add("guard");}
 | {!$parse::fields.contains("actions")}? actions {$parse::fields.add("actions");}
 | {!$parse::fields.contains("blocked")}? blocked {$parse::fields.add("blocked");}
 | {!$parse::fields.contains("reqtags")}? reqtags {$parse::fields.add("reqtags");}
 | {!$parse::fields.contains("weight")}? weight {$parse::fields.add("weight");}
 | {!$parse::fields.contains("dependency")}? dependency {$parse::fields.add("dependency");}
 | {!$parse::fields.contains("description")}? description {$parse::fields.add("description");}
 | {!$parse::fields.contains("dataset")}? dataset {$parse::fields.add("dataset");}
 | {!$parse::fields.contains("htmlStart")}? htmlStart
 | {!$parse::fields.contains("htmlEnd")}? htmlEnd
 | {!$parse::fields.contains("htmlBr")}? htmlBr
 | WHITESPACE
 ;

actions
 : SLASH action ((SEMICOLON | COMMA) action)* SEMICOLON
 ;

action
 : actionPart+
 ;

actionPart
 : WHITESPACE* Identifier (DOT Identifier)* WHITESPACE* actionOperator? WHITESPACE* ((JS_NOT? Identifier) | (JS_MINUS? Value) | JS_LITERAL | JS_FUNCTION | JS_ARRAY | JS_METHOD_CALL | JS_BRACES)?
 ;

actionOperator
 : JS_PLUS | JS_MINUS | JS_MUL | SLASH | JS_MOD | JS_INC | JS_DEC
 | ASSIGN | JS_PLUS_ASSIGN | JS_MINUS_ASSIGN | JS_MUL_ASSIGN | JS_DIV_ASSIGN | JS_MOD_ASSIGN
 ;

reqtags
 : REQTAG WHITESPACE* (COLON | ASSIGN) WHITESPACE* reqtagList
 ;

reqtagList
 : (reqtag WHITESPACE* COMMA WHITESPACE*)* reqtag
 ;

reqtag
 : ~(COMMA)+
 ;

guard
 : NestedBrackets
 ;

blocked
 : BLOCKED
 ;

names
 : name (SEMICOLON name)*
 ;

name
 : Identifier (DOT Identifier)*
 ;

dependency
 : DEPENDENCY WHITESPACE* ASSIGN WHITESPACE* Value
 ;

weight
 : WEIGHT WHITESPACE* ASSIGN WHITESPACE* Value
 ;

description
 : comment
 | JAVADOC_START DOCSPACE* code DOCSPACE* JAVADOC_END
 | JAVADOC_START DOCSPACE* code DOCSPACE* DESCRIPTION_COMMENT JAVADOC_END
 ;

comment
 : COMMENT
 ;

code
 : CODE_TAG voidExpression
 ;

htmlStart
 : HTML_TAG_START WHITESPACE*
 ;

htmlEnd
 : HTML_TAG_END
 ;

htmlBr
 : HTML_BR
 ;

dataset
 : htmlTable
 ;

htmlTable
 : HTML_TABLE_START WHITESPACE* tableHeader WHITESPACE* (tableRow WHITESPACE*)+ HTML_TABLE_END
 ;

tableHeader
 : HTML_TR_START WHITESPACE* (tableHeaderCell WHITESPACE*)+ HTML_TR_END
 ;

tableRow
 : HTML_TR_START WHITESPACE* (tableBodyCell WHITESPACE*)+ HTML_TR_END
 ;

tableBodyCell
 : HTML_TD_START (numericValue|stringQuote|stringValue|booleanValue) HTML_TD_END
 ;

tableHeaderCell
 : HTML_TH_START Identifier HTML_TH_END
 ;

numericValue
 : (JS_MINUS|JS_PLUS)? Value
 ;

booleanValue
 : BOOLEAN_VALUE
 ;

stringValue
 : Identifier
 ;

stringQuote
 : JS_LITERAL
 ;
