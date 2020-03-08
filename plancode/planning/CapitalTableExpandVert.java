/* Programme takes file parameters
 * 1 capital stock matrix produced by CapitalTableExpand
 * 2 source index file that relabels capital asset types to industries that make them
 * 3 file that contains the io table
 * It then produces an  expanded stock matrix on standard with the same layout as the iotable  such that
  * for each row in the original stock matrix for which several source industries
  * exist in the source index file capital values in the original matrix (file 1) are spread among the
  * new multiple rows in proportion to the flows shown in the corresponding columns in the iotable.
  * The underlying assumption for this approach is that the flows shown in the io table are replacement for
  * depreciation and will be proportional to the corresponding capital stocks
*

The final output matrix should have column names in the same order as the input
output table
*/
package planning;
import java.io.*;
import java.util.*;
class CapitalTableExpandVert
{
    public static void main(String [] args)
    {
        if (args.length<3)
            {
                System.out.println(" Usage java planning.CapitalTableExpandVert stockmatrix.cvs sourceindes.cvs iotable.cvs");
            }
        else
            {
                csvfilereader[] r= new csvfilereader[3];
                pcsv[] csvs=new pcsv[3];
                boolean fail=false;
                for (int i=0; i<3; i++)
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
                String[] ioheaders=r[2].getcolheaders(csvs[2]);
                double[][] iomatrix= r[2]. getdatamatrix(csvs[2]) ;
                String[]iorownames =r[2].getrowheaders(csvs[2]);
                Vector<String> iorows = new Vector<String>();
                for(String v:iorownames)iorows.add(v);
                String[]transrownames =r[1].getrowheaders(csvs[1]);
                // create a data matrix
                double [][] datamatrix = new double[iorownames.length][ioheaders.length];
                for (int i=1; i<transrownames.length-1; i++)
                    {
                        //   System.out.println(""+"stockrow["+(i )+"]\t "+ stockrownames[i   ]);
                        //   System.out.println(""+"transrow["+(i )+"]\t "+ transrownames[i   ]);
                        if(!transrownames[i].equals(stockrownames[i]))
                            {
                                System.err.println("\nError \t"+"trans name and stock name dont match row "+i);
                                return;
                            }
                        pcsv start=r[1].getline(csvs[1],i +1 );
                        if(start==null)
                            {
                                System.err.println("\nError \t"+"start==null");
                                return;
                            }
                        String[] sources= r[1].getcolheaders(start);
                        int nonnulls=0;
                        for(int j=1; j<sources.length; j++)if(!sources[j].equals(""))nonnulls=j;
                        int[] iotrownums=new int[nonnulls];
                        for (int j=1; j<sources.length; j++) try
                                {
                                    if(!sources[j].equals(""))
                                        {
                                            iotrownums[j-1]=iorows.indexOf(sources[j]);
                                            if(iotrownums[j-1]<0)
                                                {
                                                    System.err.println("\nError \t"+sources[j]+ " was not a row header in the iotable ");
                                                    return;
                                                }
                                        }
                                }
                            catch(Exception e1)
                                {
                                    System.err.println(""+e1);
                                    e1.printStackTrace();
                                    return;
                                }
                        for (int j=1; j<nonnulls; j++)
                            {
                                //  System.out.print("\t"+sources[j]+ " "+iotrownums[j-1]);
                            }
                        //System.out.println();
                        // now traverse the columns of the iotable
                        for(int col=1 ; col<stockheadersa.length; col++)
                            {
                                double total=0;
                                for(int src=0; src<nonnulls; src++)
                                    total+=iomatrix[iotrownums[src]][col];
                                //System.out.println("total "+total);
                                for(int src=0; src<nonnulls; src++)
                                    datamatrix[iotrownums[src]][col]+=stockdata[i][col]*iomatrix[iotrownums[src]][col]/total;
                            }
                    }
                printResult( ioheaders,iorownames,  datamatrix);
            }
    }
    // print result
    static void printResult(String[] ioheaders,String[]iorownames, double[][]datamatrix)
    {
        String q="\"";
        String comma=",";
        for(int i=0; i<ioheaders.length; i++)
            System.out.print(q+ioheaders[i]+q+(i<(ioheaders.length-1)?comma:""));
        System.out.println();
        for(int i=1; i<iorownames.length; i++)
            {
                System.out.print(q+iorownames[i]+q+comma);
                for(int j=1 ; j<ioheaders.length; j++)
                    System.out.print(""+(int)datamatrix[i][j]+(j<(ioheaders.length-1)?comma:""));
                System.out.println();
            }
    }
}
