#!/bin/sh

#define the command/path to use to call 'java'
export JAVA_PATH="java"

#check that java is actually installed
$JAVA_PATH -version >/dev/null 2>&1

if [ $? != "0" ]; then
	echo "Unable to start: Java ($JAVA_PATH) is not installed!"
else
	#Testing if JAI is installed
	$JAVA_PATH JAITest;
	
	#If JAI is found, start LAMINA. If not, display error message
	if [ $? != "0" ]; then
		$JAVA_PATH JAINotFound
	else
		$JAVA_PATH -Xms100m -Xmx400m -Xss48m -jar Lamina.jar
	fi
fi
