all:   planning/nyearplan.class plan.txt test.txt result.csv iouk 

.java.class:
	javac   $<

test.txt: planning/nyearplan.class planning/testeconomy.class testrun.sh
	bash testrun.sh 4 2

harmony.txt: planning/nyearplan.class planning/testeconomy.class testharmony.sh planning/nyearHarmony.class testharmony.sh Makefile
	bash testharmony.sh 3 3

pasharm/harmonyplan:  pasharm/harmonyplan.pas pasharm/csvfilereader.pas pasharm/technologies.pas pasharm/harmony.pas
	$(MAKE) -C pasharm
	

planning/csvfilereader.class: planning/csvfilereader.java planning/pcsv.java
	javac planning/csvfilereader.java
	
clean:
	rm planning/*.class  
	rm plan.txt
	rm model.lp

pretty: 
		astyle planning/*java
		
planning/nyearplan.class:planning/nyearplan.java planning/csvfilereader.class
		javac planning/nyearplan.java
		
plan.txt: model.lp
	lp_solve <model.lp|sort >plan.txt
	
model.lp:planning/nyearplan.class flows.csv cap.csv dep.csv labtarg.csv Makefile
	java planning.nyearplan flows.csv cap.csv dep.csv labtarg.csv   >model.lp
	
hplan.txt:  planning/nyearplan.class  planning/nyearHarmony.class    Makefile
	java planning.nyearHarmony flows.csv cap.csv dep.csv labtarg.csv   >hplan.txt
	
planning/testeconomy.class:planning/testeconomy.java
	javac planning/testeconomy.java

planning/nyearHarmony.class:planning/Harmonizer.java planning/TechnologyComplex.java planning/JointProductionTechnique.java planning/nyearHarmony.java planning/Technique.java
	javac planning/nyearHarmony.java

result.csv:testflow.csv testcap.csv testdep.csv testtarg.csv planning/nyearHarmony.class
	java planning.nyearHarmony testflow.csv testcap.csv testdep.csv testtarg.csv >result.csv
	tail --lines=2 result.csv

pasresult.csv:testflow.csv testcap.csv testdep.csv testtarg.csv pasharm/harmonyplan
	pasharm/harmonyplan testflow.csv testcap.csv testdep.csv testtarg.csv >pasresult.csv
	tail --lines=2 pasresult.csv
	
	
# section to build the info for the planning from existing
# uk stats, it uses data in ../iouk

iouk: planning/nyearplan.class  ukplan.lp ukplan.txt

ukplan.lp: planning/UKplan.class ../iouk/dep.csv ../iouk/Vexpandedcapitalstockmatrix2015.csv ../iouk/labtargs.csv ../iouk/imports.csv
	java planning.UKplan ../iouk/domesticiot.csv ../iouk/Vexpandedcapitalstockmatrix2015.csv ../iouk/dep.csv ../iouk/labtargs.csv  ../iouk/imports.csv >ukplan.lp

../iouk/dep.csv: ../iouk/deprates.csv  planning/DepreciationTableExpand.class ../iouk/Vexpandedcapitalstockmatrix2015.csv
	java planning.DepreciationTableExpand ../iouk/Vexpandedcapitalstockmatrix2015.csv ../iouk/deprates.csv > ../iouk/dep.csv
	

../iouk/Vexpandedcapitalstockmatrix2015.csv: ./planning/CapitalTableExpandVert.class ../iouk/hexpandedcapitalstockmatrix2015.csv ../iouk/asset2sources.csv ../iouk/domesticiot.csv
	java planning.CapitalTableExpandVert ../iouk/hexpandedcapitalstockmatrix2015.csv ../iouk/asset2sources.csv ../iouk/domesticiot.csv > ../iouk/Vexpandedcapitalstockmatrix2015.csv

../iouk/hexpandedcapitalstockmatrix2015.csv:	../iouk/netcapitalstockmatrix2015.csv ../iouk/trans.csv ../iouk/finaloutput.csv planning/CapitalTableExpand.class
	java planning.CapitalTableExpand ../iouk/netcapitalstockmatrix2015.csv ../iouk/trans.csv ../iouk/finaloutput.csv > ../iouk/hexpandedcapitalstockmatrix2015.csv

./planning/CapitalTableExpandVert.class:./planning/CapitalTableExpandVert.java
	javac ./planning/CapitalTableExpandVert.java

planning/CapitalTableExpand.class:planning/CapitalTableExpand.java
	javac planning/CapitalTableExpand.java

planning/DepreciationTableExpand.class: planning/DepreciationTableExpand.java
	javac planning/DepreciationTableExpand.java
		
planning/UKplan.class:planning/UKplan.java planning/csvfilereader.class
		javac planning/UKplan.java

ukplan.txt:ukplan.lp
	wc ukplan.lp
	time lp_solve <ukplan.lp >ukplanunsort.txt
	wc ukplanunsort.txt
	time sort <ukplanunsort.txt >ukplan.txt
	
