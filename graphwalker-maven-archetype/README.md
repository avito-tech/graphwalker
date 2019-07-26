GraphWalker Maven Archetype
================

A GraphWalker maven archetype. To help create boilerplate GraphWalker maven projects.

To create a sample project:

~~~sh
%> mvn archetype:generate -B -DarchetypeGroupId=org.graphwalker -DarchetypeArtifactId=graphwalker-maven-archetype -DarchetypeVersion=4.0.0-SNAPSHOT -DgroupId=com.company -DartifactId=myProject
~~~

Then cd into the project:
~~~sh
%> cd myProject
~~~
Build and run the test:
~~~sh
%> mvn :graphwalker:test
~~~

