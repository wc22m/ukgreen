/** Programme to read in the capital stock matrix produced by stock formatter
along with a second file that relabels io table columns with the categories
used in the stock matrix and a third file that gives the value of final output
values.
*
* <p>It then produces and expanded stock matrix on standard out such that
for each column in the original stock matrix for which several sub industries
exist in the io table the capital values in the original are spread among the
new multiple columns in proportion to their share in the final output of this
group of industries.<p>
The final output matrix should have column names in the same order as the final
output table
*/
package planning;
import java.io.*;
import java.util.*;
class CapitalTableExpand
{
    static int positioninheaders(String s,String [] headers)
    {
        for(int i=0; i<headers.length; i++)
            if(s.equals(headers[i]))return i;
        return -1;
    }
    static boolean verbose=false;
    public static void main(String [] args)
    {
        if (args.length<3)
            {
                System.out.println(" Usage java planning.CapitalTableExpand stockmatrix.cvs relabel.cvs finaloutput.cvs");
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
                String[] outputheaders=r[2].getcolheaders(csvs[2]);
                double[] outputvector = r[2]. getdatamatrix(csvs[2])[1];
                //  System.out.println("stock\n"+csvs[2]);
                //   for (double d :outputvector)System.out.print(" "+d);System.out.println();
                //   System.out.println("rows "+r[2]. getdatamatrix(csvs[2])[0].length);
                Hashtable<String,String> translation = new Hashtable<String,String>();
                Hashtable<String,Integer> headernum=new Hashtable<String,Integer>();
                Hashtable<String,Vector<String>> reversemap = new Hashtable<String,Vector<String>>();
                String[] transcol0 = r[1].getrowheaders(csvs[1]);
                int rows = r[1].rowcount(csvs[1]);
                //  r[1].printcsv(new PrintWriter(System.out),csvs[1]);
                for(int i=2; i<=rows; i++)
                    {
                        pcsv cell =r[1].getcell(csvs[1],i,2);
                        pcsv head =r[1].getcell(csvs[1],i,1);
                        if(cell!=null)if(head!=null)
                                {
                                    String capname= cell.tag.toString();
                                    translation.put(transcol0[i-1],capname);
                                    headernum.put(transcol0[i-1],new Integer(i-1));
                                    // System.out.println(transcol0[i-1]+"\t->\t"+cell.tag.toString() );
                                    Vector<String> list = reversemap.get(capname);
                                    if(list==null)
                                        {
                                            list = new Vector<String>();
                                            reversemap.put(capname,list);
                                        }
                                    list.add(transcol0[i-1]);
                                }
                            else System.err.println("could not find cell "+i+",1 in translation matrix ");
                    }
                int validcols=0;// number of columns for which valid headers found
                // now check that all output header names are found
                for(String s:outputheaders)
                    {
                        if(translation.get(s)==null)System.out.println("\""+s+"\",NOT IN TABLE");
                        else if(!translation.get(s).equals("NOT IN TABLE"))
                            {
                                validcols++;
                                //  System.out.println(translation.get(s));
                            }
                    }
                //     System.out.println("num valid headers ="+validcols+" out of "+outputheaders.length);
                // create a data matrix
                double [][] datamatrix = new double[stockrownames.length][validcols];
                // iterate through the final columns
                for(int i=1; i<validcols; i++)
                    {
                        String match = translation.get(outputheaders[i]);
                        if(verbose)System.out.println("outputheaders["+i+"]="+outputheaders[i]+"->"+match);
                        if(! match .equals("NOT IN TABLE"))   // this is a valid header
                            {
                                // now find all the other columns with the same match
                                Vector<String>all=reversemap.get(match);
                                if(verbose)System.out.println("other col name which match "+match);
                                if(verbose)for (String S:all)System.out.println("\t"+S);
                                // sum the total output of this class of product
                                double tot=0;
                                for(String s:all)
                                    {
                                        int k = positioninheaders(s,outputheaders);
                                        if (k<0)
                                            {
                                                System.err.println(s+" not found in output headers ");
                                                return;
                                            }
                                        if(verbose) System.out. println("pos "+k+" in final output = "+outputvector[k]);
                                        tot+=outputvector[k];
                                    }
                                double frac = outputvector[i]/tot;// the fraction of the capital stock
                                // of the given class to go under this header
                                if(verbose)System.out.println(" "+i+" "+outputheaders[i]+"/"+match+"="+frac);
                                int originalcapcol = stockheaders.indexOf(match);
                                if(originalcapcol>=0)
                                    {
                                        for(int k=0; k<datamatrix.length; k++)
                                            datamatrix[k][i]=stockdata[k][originalcapcol]*frac;
                                    }
                            }
                    }
                // print result
                String q="\"";
                String comma=",";
                for(int i=0; i<validcols; i++)
                    System.out.print(q+outputheaders[i]+q+(i<(validcols-1)?comma:""));
                System.out.println();
                for(int i=1; i<stockrownames.length; i++)
                    {
                        System.out.print(q+stockrownames[i]+q+comma);
                        for(int j=1 ; j<validcols; j++)
                            System.out.print(""+(int)datamatrix[i][j]+(j<(validcols-1)?comma:""));
                        System.out.println();
                    }
            }
    }
}
