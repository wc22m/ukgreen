/* Programme takes file parameters
 * 1 capital stock matrix produced by CapitalTableVertExpand
 * 2 a file specifying the depreciation rate of all types of capital assets
 *
 * It then produces an  expanded depreciation matrix on standard out with the same layout as the capital stock matrix  such that
  *  each non zero stock is given an appropriate depreciation rate, all others are given unity
*

The final output matrix should have column names in the same order as the input
output table
*/
package planning;
import java.io.*;
import java.util.*;
class DepreciationTableExpand
{
    public static void main(String [] args)
    {
        if (args.length<2)
            {
                System.out.println(" Usage java planning.DepreciationTableExpand  stockmatrix.cvs deprate.cvs  ");
            }
        else
            {
                csvfilereader[] r= new csvfilereader[2];
                pcsv[] csvs=new pcsv[2];
                boolean fail=false;
                for (int i=0; i<2; i++)
                    {
                        r[i]= new csvfilereader(args[i]);
                        csvs[i]=r[i].parsecsvfile();
                        if (csvs[i]==null)
                            {
                                System.err.println("could not open or parse "+args[i]);
                                fail=true;
                            }
                    }
                if (fail)return;
                String[] stockheadersa = r[0].getcolheaders(csvs[0]);
                Vector<String> stockheaders = new Vector<String>();
                for(String v:stockheadersa)stockheaders.add(v);
                String[] stockrownames= r[0].getrowheaders(csvs[0]);
                double[][]stockdata =r[0]. getdatamatrix(csvs[0]);
                double[][] depdata = r[1]. getdatamatrix(csvs[1]);
                // print the headers
                for(String s:stockheaders)System.out.print("\""+s+"\",");
                System.out.println();
                for ( int row=1; row <depdata.length ; row++)
                    {
                        double d = depdata[row][1];
                        System.out.print("\""+stockrownames[row]+"\",");
                        for (int col=1; col <stockdata[row].length; col++)
                            {
                                if (stockdata[row][col]==0)
                                    {
                                        System.out.print(" 1");
                                    }
                                else
                                    {
                                        System.out.print(""+d);
                                    }
                                if(col<stockdata[row].length-1)System.out.print(",");
                            }
                        System.out.println();
                    }
            }
    }
}

