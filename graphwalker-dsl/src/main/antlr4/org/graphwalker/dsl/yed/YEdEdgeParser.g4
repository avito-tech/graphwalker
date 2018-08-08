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
 | WHITESPACE
 ;

actions
 : SLASH (action)+
 ;

action
 : .+ SEMICOLON
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
