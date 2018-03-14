parser grammar YEdVertexParser;

options {
	tokenVocab=YEdLabelLexer;
}

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

action
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
 : ~(COMMA | SEMICOLON | WHITESPACE)+
 ;

indegree
 : ~(COMMA | SEMICOLON | WHITESPACE)+
 ;

description
 : (COMMENT)+
 ;
