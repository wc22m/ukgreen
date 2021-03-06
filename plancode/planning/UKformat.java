package planning;
import java.io.*;
import java.util.*;
/** A  programme to  format output from the  eplc compiler, on the assumption that it is a
multi year plan using the nameing conventions of UKHarmony<p>

 * Usage java planning.UKformat products.csv techniques.csv
 *
 * <p>
    Copyright (C) 2018 William Paul Cockshott
<p>
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.
<p>
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see https://www.gnu.org/licenses/.
    *
 * */
public class UKformat
{
    static String [][] rowheads = new String[2][1];
    static String [][]colheads = new String[2][1];
    static double [][][] matrices= new double [2][1][1];
    static int tech=1,prod=0;
    static int years=0;
    /** returns true is it is the name of a technique */
    public static boolean istech(String s, String pref)
    {
        return s.startsWith(pref);
    }
    static int getyear(String s)throws Exception
    {
        int l= s.length();
        char c;
        String tail="";
        boolean ok = l>0;
        for (int i=l-1; ok; i--)
            {
                c= s.charAt(i);
                // termination conditions
                if (i<=0)  ok= false;
                if (c<'0') ok= false;
                if (c>'9') ok= false;
                if(ok) tail = c+tail;
            }
        if (tail.length()==0) throw new Exception ("no year suffix in heading "+s);
        return new Integer(tail).intValue();
    }
    /** remove the number suffix */
    static String head(String s)
    {
        int l= s.length();
        char c;
        String h="";
        for (int i=0; i<l ; i++)
            {
                c= s.charAt(i);
                // termination conditions
                if (c<='Z')
                    if (c>='A')
                        h+=c;
            }
        return h;
    }
    static void processtechs( String pref, String H)throws Exception
    {
        Hashtable<String,Integer> techs = new Hashtable<String,Integer>();
        System.out.println(","+H);
        System.out.print("year ");
        String[] headings= colheads[tech];
        int count=0;
        for(String s:headings) //find out how many years there are
            {
                if(istech(s,pref))
                    {
                        int y= getyear(s);
                        String h = head(s);
                        if(null==techs.get(h))
                            {
                                techs.put(h, new Integer(count));
                                count++;
                                System.out.print(","+h.substring(pref.length()-1,h.length()-1));
                            }
                        if (y>years)years=y;
                    }
            }
        System.out.println();
        // create matrix of values
        double[][] data = new double[years][count];
        // get all the intensities
        int i=0;
        for(int stream=1; stream<=2; stream++)
            {
                System.out.println(","+rowheads[tech][stream]);
                for(i=0; i<headings.length; i++)
                    {
                        String s=headings[i];
                        if(istech(s,pref))
                            {
                                int y= getyear(s);
                                String h = head(s);
                                if(techs.get(h)!=null)
                                    {
                                        int col = techs.get(h);
                                        data[y-1][col]=matrices[tech][stream][i];
                                    }
                            }
                    }
                for(i=1; i<=years; i++)
                    {
                        System.out.print(" "+i);
                        for(int col=0; col<count; col++)
                            System.out.format(",+%8.3g ",data[i-1][col]);
                        System.out.println();
                    }
            }
    } public static double mean(double[] m)
    {
        double sum = 0;
        for (int i = 0; i < m.length; i++)
            {
                sum += m[i];
            }
        return sum / m.length;
    }
    public static double sdev(double[] list)
    {
        double sum = 0.0;
        double mean = 0.0;
        double num=0.0;
        double numi = 0.0;
        double deno = 0.0;
        for (double i : list)
            {
                sum+=i;
            }
        mean = sum/list.length;
        for (double i : list)
            {
                numi = Math.pow(((double) i - mean), 2);
                num+=numi;
            }
        return Math.sqrt(num/list.length);
    }
    static void processprods(int row, boolean prefixec,String pref)throws Exception
    {
        Hashtable<String,Integer> prods = new Hashtable<String,Integer>();
        System.out.println(","+rowheads[prod][row]);
        System.out.print("year ,mean,sdev");
        String[] headings= colheads[prod];
        int count=0;
        for(int k=1; k<headings.length; k++)
            //find out how many years there are
            {
                String s = headings[k];
                int y= getyear(s);
                String h = head(s);
                if((prefixec && s.startsWith(pref))||(!prefixec&&!s.startsWith(pref)))
                    if(null==prods.get(h))
                        {
                            prods.put(h, new Integer(count));
                            count++;
                            System.out.print(","+h );
                        }
                if (y>years)years=y;
            }
        System.out.println();
        // create matrix of values
        double[][] data = new double[years][count];
        // get all the intensities
        int i=0;
        int stream=row;
        {
            for(i=1; i<headings.length; i++)
                {
                    String s=headings[i];
                    if((prefixec && s.startsWith(pref))||(!prefixec&&!s.startsWith(pref)))
                        {
                            int y= getyear(s);
                            String h = head(s);
                            if(prods.get(h)!=null)
                                {
                                    int col = prods.get(h);
                                    data[y-1][col]=matrices[prod][stream][i];
                                }
                        }
                }
            for(i=1; i<=years; i++)
                {
                    System.out.format(" "+i+",%.5f ,%.5f ",mean(data[i-1]),sdev(data[i-1]));
                    for(int col=0; col<count; col++)
                        System.out.format(",%.5f ",data[i-1][col]);
                    System.out.println();
                }
        }
    }
    public static void main(String [] args)throws Exception
    {
        if (args.length !=2 )
            {
                System.err.println("Usage java planning.UKformat products.csv  tech.csv");
            }
        else
            try
                {
                    csvfilereader techread,prodread ;
                    techread=new csvfilereader(args[tech]);
                    pcsv techtab = techread.parsecsvfile();
                    prodread= new csvfilereader(args[prod]);
                    pcsv prodtab = prodread.parsecsvfile();
                    if (techtab == null)
                        {
                            throw new Exception(" Error opening or parsing "+args[tech]);
                        }
                    if (prodtab == null)
                        {
                            throw new Exception(" Error opening or parsing "+args[prod]);
                        }
                    pcsv[] parsed = {prodtab,techtab };
                    for (int i=prod ; i<=tech; i++)
                        {
                            rowheads[i]=techread.getrowheaders(parsed[i]);
                            colheads[i]=techread.getcolheaders(parsed[i]);
                            matrices[i]=techread.getdatamatrix(parsed[i]);
                        }
                    processtechs("T_","Techniques");
                    processtechs("EX_","Exports");
                    processtechs("IM_","Imports");
                    System.out.println(",Products and non produced resources");
                    for(int i=1; i<=10; i++) processprods(i,false,"C_");
                    System.out.println(",Capital stocks");
                    for(int i=1; i<=10; i++) processprods(i,true,"C_");
                    processtechs("A_","Accumulation");
                }
            catch(Exception eee)
                {
                    System.err.println("Error "+eee);
                    eee.printStackTrace();
                }
    }
}
