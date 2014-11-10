<?xml version="1.0" encoding="ISO-8859-1"?>
<!DOCTYPE helpset
	PUBLIC "-//Sun Microsystems Inc.//DTD JavaHelp HelpSet Version 1.0//EN"
		"http://java.sun.com/products/javahelp/helpset_1_0.dtd">
	<helpset version="1.0">
	

			<maps>
				<homeID>Default</homeID>
				<mapref location="LAMINA.jhm"/>
			</maps>

			<view>
				<name>TOC</name>
				<label>Table Of Contents</label>
				<type>javax.help.TOCView</type>
				<data>LAMINA.xml</data>
			</view>
			
			  <view>
			    <name>Search</name>
			    <label>Word Search</label>
			    <type>javax.help.SearchView</type>
			    <data engine="com.sun.java.help.search.DefaultSearchEngine">JavaHelpSearch</data>
			  </view>

			<presentation default="true" displayviews="true" displayviewimages="true">
					<name>Help</name>
					<title>LAMINA Help</title>
				    <size width="800" height="600" />
				    <image>masqot_icon</image>
				    <location x="0" y="0" />

			  </presentation>

	</helpset>

