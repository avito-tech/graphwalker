parser grammar YEdVertexParser;

options {
	tokenVocab=YEdLabelLexer;
}

import DescriptionParser;

parse
 locals [java.util.Set<String> fields = new java.util.HashSet<String>();]
 : start
 | field* EOF
 ;

field
 : {!$parse::fields.contains("names")}? names {$parse::fields.add("names");}
 | {!$parse::fields.contains("shared")}? shared {$parse::fields.add("shared");}
 | {!$parse::fields.contains("outdegrees")}? outdegrees {$parse::fields.add("outdegrees");}
 | {!$parse::fields.contains("indegrees")}? indegrees {$parse::fields.add("indegrees");}
 | {!$parse::fields.contains("blocked")}? blocked {$parse::fields.add("blocked");}
 | {!$parse::fields.contains("actions")}? actions {$parse::fields.add("actions");}
 | {!$parse::fields.contains("sets")}? sets {$parse::fields.add("sets");}
 | {!$parse::fields.contains("reqtags")}? reqtags {$parse::fields.add("reqtags");}
 | {!$parse::fields.contains("description")}? description {$parse::fields.add("description");}
 | WHITESPACE
 ;

start
 : WHITESPACE* (START) WHITESPACE*
 ;

shared
 : SHARED WHITESPACE* COLON WHITESPACE* Identifier
 ;

names
 : name (SEMICOLON name)*
 ;

name
 : Identifier (DOT Identifier)*
 ;

blocked
 : BLOCKED
 ;

actions
 : INIT WHITESPACE* COLON WHITESPACE* (action)+
 ;

sets
 : SET WHITESPACE* COLON WHITESPACE* (set)+
 ;

action
 : .+ SEMICOLON
 ;

set
 : .+ SEMICOLON
 ;

reqtags
 : REQTAG WHITESPACE* (COLON | ASSIGN) WHITESPACE* reqtagList
 ;

outdegrees
 : OUTDEGREE WHITESPACE* (COLON | ASSIGN) WHITESPACE* outdegreeList SEMICOLON
 ;

indegrees
 : INDEGREE WHITESPACE* (COLON | ASSIGN) WHITESPACE* indegreeList SEMICOLON
 ;

reqtagList
 : (reqtag WHITESPACE* COMMA WHITESPACE*)* reqtag
 ;

outdegreeList
 : (outdegree WHITESPACE* COMMA WHITESPACE*)* outdegree
 ;

indegreeList
 : (indegree WHITESPACE* COMMA WHITESPACE*)* indegree
 ;

reqtag
 : ~(COMMA)+
 ;

outdegree
 : element
 ;

indegree
 : element WHITESPACE* description? WHITESPACE* guard? WHITESPACE* weight?
 | element WHITESPACE* guard? WHITESPACE* description? WHITESPACE* weight?
 | description WHITESPACE* element? WHITESPACE* guard? WHITESPACE* weight?
 | description WHITESPACE* guard? WHITESPACE* element? WHITESPACE* weight?
 | guard WHITESPACE* description? WHITESPACE* element? WHITESPACE* weight?
 | guard WHITESPACE* element? WHITESPACE* description? WHITESPACE* weight?
 | element WHITESPACE* description? WHITESPACE* weight? WHITESPACE* guard?
 | element WHITESPACE* guard? WHITESPACE* weight? WHITESPACE* description?
 | description WHITESPACE* element? WHITESPACE* weight? WHITESPACE* guard?
 | description WHITESPACE* guard? WHITESPACE* weight? WHITESPACE* element?
 | guard WHITESPACE* description? WHITESPACE* weight? WHITESPACE* element?
 | guard WHITESPACE* element? WHITESPACE* weight? WHITESPACE* description?
 | element WHITESPACE* weight? WHITESPACE* description? WHITESPACE* guard?
 | element WHITESPACE* weight? WHITESPACE* guard? WHITESPACE* description?
 | description WHITESPACE* weight? WHITESPACE* element? WHITESPACE* guard?
 | description WHITESPACE* weight? WHITESPACE* guard? WHITESPACE* element?
 | guard WHITESPACE* weight? WHITESPACE* description? WHITESPACE* element?
 | guard WHITESPACE* weight? WHITESPACE* element? WHITESPACE* description?
 ;

element
 : ~(COMMA | SEMICOLON | WHITESPACE)+
 ;

guard
 : NestedBrackets
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
 : CODE_TAG (voidExpression|booleanMethod)
 ;
