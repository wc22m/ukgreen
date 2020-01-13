 
 java planning.testeconomy $1 $2
 echo  created problem with $1 industries to be planned for $2 years
 echo    seconds for the harmony algorithm 
   
time -f %E java -Xmx1512m planning.nyearHarmony testflow.csv testcap.csv testdep.csv testtarg.csv    >harmony.txt
 
cat harmony.txt >harmony$1$2.csv

 echo seconds to  prepare the linear programme  
 
time -f %E java planning.nyearplan testflow.csv testcap.csv testdep.csv testtarg.csv   >test.lp
 

echo seconds to execute  the linear programme
 
time -f %E lp_solve <test.lp|sort >test.txt 
 
 
# echo statistics of the linear programme specification
#echo lines, words, chars
#wc test.lp  
#echo Degree of plan fulfillment using linear programme
#tail --lines=2 test.txt 
#echo Harmony achieved by the harmony alrorithm 
# tail --lines=2 harmony.txt
echo lp solve results in file test.txt
echo harmony results in spreadsheet harmony$1$2.csv
