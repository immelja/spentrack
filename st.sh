#!/bin/bash  
mvn exec:java -Dexec.mainClass="com.immelja.App" -Dexec.classpathScope=runtime  
git add .  
git commit -m "wip"
git push origin master