package planning;
import java.io.*;
import java.util.*;
/** A programme to construct 5 year or n year socialist plans for the UK
 * from UK statistics
 *
 * It produces an output file of the plan in lp-solve format on standard out<p>
 * Usage java planning.UKplan flowmatrix.csv capitalmatrix.csv depreciationmatrix.csv laboursupplyandtargets.csv imports.csv
 *
 * <p>
    Copyright (C) 2018 William Paul Cockshott

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see https://www.gnu.org/licenses/.
 * */
public class UKplan
{
    static final int flow=0,cap=1,dep=2,targ=3, imports=4;
    static String [][] rowheads = new String[5][1];
    static String [][]colheads = new String[5][1];
    static final short consistancyrange = 129;// only check the row and col headers up to this
    static final short lastimportrow =105; // disregard imports below this

    static double [][][] matrices= new double [5][1][1];
    static double []outputs;
    static double[] labour;
    static int maxprod;

    static int   consistent(String []shorter,String[]longer,int range)   /* return -1 if the lists are consistent */
    {
        int shortone=shorter.length;
        if(longer.length<shorter.length)shortone=longer.length;
        if (shortone > range) shortone= range;
        for(int i= 0 ; i<shortone; i++)
            {
                if(shorter[i]==null) return i;
                if(longer[i]==null) return i;
                if (!tidy(shorter[i]).equals(tidy(longer[i])))return i;
            }
        return -1;
    }
    static int   consistent(String []shorter,String[]longer )
    {
        return consistent(shorter,longer,consistancyrange);
    }
    public static void main(String [] args)throws Exception
    {
        if (args.length !=5 )
            {
                System.err.println("Usage java planning.UKplan flowmatrix.csv capitalmatrix.csv depreciationmatrix.csv laboursupplyandtargets.csv  imports.csv");
            }
        else
            {
                csvfilereader flowread,capread,depread,labtargread, importread;
                flowread=new csvfilereader(args[flow]);
                pcsv flowtab = flowread.parsecsvfile();
                capread= new csvfilereader(args[cap]);
                pcsv captab = capread.parsecsvfile();
                depread = new csvfilereader(args[dep]);
                pcsv deptab = depread.parsecsvfile();
                labtargread = new csvfilereader(args[targ]);
                pcsv labetctab=labtargread .parsecsvfile();
                importread = new csvfilereader(args[imports]);
                pcsv importctab=importread .parsecsvfile();
                if (flowtab == null)
                    {
                        throw new Exception(" Error opening or parsing "+args[flow]);
                    }
                if (captab == null)
                    {
                        throw new Exception(" Error opening or parsing "+args[cap]);
                    }
                if (deptab == null)
                    {
                        throw new Exception(" Error opening or parsing "+args[dep]);
                    }
                if (labetctab == null)
                    {
                        throw new Exception(" Error opening or parsing "+args[targ]);
                    }
                if (importctab == null)
                    {
                        throw new Exception(" Error opening or parsing "+args[imports]);
                    }
                pcsv[] parsed = {flowtab,captab,deptab,labetctab,importctab};
                for (int i=flow ; i<=imports; i++)
                    {
                        rowheads[i]=flowread.getrowheaders(parsed[i]);
                        colheads[i]=flowread.getcolheaders(parsed[i]);
                        matrices[i]=flowread.getdatamatrix(parsed[i]);
                        int consistency=consistent(colheads[flow],colheads[i]);
                        if(consistency>=0) if(i!= targ)throw new Exception(" flow table col header inconsistent with header of table "+args[i]
                                        +"\n"+  colheads[flow][consistency]+" !="+colheads[i][consistency]+" at position "+consistency);
                        if(i!= targ)
                            {
                                consistency=consistent(colheads[i],rowheads[i],(i==imports?lastimportrow:consistancyrange));
                                if(consistency>0) throw new Exception("   col header inconsistent with row header for table  "+i
                                                                          +"\n'"+  colheads[i][consistency]+"' !='"+rowheads[i][consistency]+"' at position "+consistency
                                                                          +"\ncolheads="+Arrays.toString(colheads[i])
                                                                          +"\nrowheads="+Arrays.toString(rowheads[i]));
                            }
                    }
                outputs = matrices[flow][outputrowinheaders() ];
                //    System.out.println("labour row is "+labourRow());
                labour = matrices[flow][labourRow()];
                // System.out.println("outputs "+Arrays.toString(outputs));
                // System.out.println("labour "+Arrays.toString(labour));
                // System.out.println("flow matrix "+Arrays.deepToString(matrices[flow]));
                // System.out.println("row headers "+Arrays.deepToString(rowheads));
                int years = countyears(rowheads[targ]);
                //   System.out.println("years to run "+years);
                maxprod=129;// this is specific to the UK domestic table that I am using
                int year;
                System.out.println(maximiser(years));
                for (year=1; year<=years; year++)
                    {
                        // set a target fiven by leontief demand for year
                        System.out.println(targeqn(year));
                        System.out.println(labourtotal(year));
                        // now print out labour supply constraint
                        int targlab = findinheaders("LABOUR",colheads[targ]);
                        int targdeficit =findinheaders("TRADEDEFICIT",colheads[targ]);
                        System.out.println(namelabourfor(year)+"\t<=\t" +matrices[targ][year][targlab]+";");
                        System.out.println("/* trade deficit constraint */\n"+matrices[targ][year][targdeficit]+">="+nameimport(year)+"-"+nameexport(year)+";");
                        for(int product=1; product<=maxprod; product++)
                            {
                                for(int stock =1; stock<=maxprod; stock++)
                                    {
                                        String eq=outputequationfor(product,stock,year);
                                        if(eq !="")System.out.println("\n"+eq);
                                        eq = flowconstraintfor(product,stock,year);
                                        if(eq !="")System.out.println("\n"+eq);
                                        if(matrices[dep][stock][product]>0)
                                            System.out.println("\n"+namedep(product,stock,year)+" =\t"+matrices[dep][stock][product]+" "+namecap(product,stock,year)+";");
                                        if (year>1)
                                            {
                                                System.out.println("\n"+accumulationconstraint(product,stock,year));
                                            }
                                        else     // set initial capital stocks
                                            {
                                                System.out.println("\n"+namecap(product,stock,year)+"\t<=\t"+ matrices[cap][stock][product]+";");
                                            }
                                    }
                                System.out.println(labourconstraintfor(product,year));
                                System.out.println("\n/* accumulation ("+product+","+year+")*/\n"+accumulationtotal(product,year));
                                System.out.println("\n/* imports ("+product+","+year+")*/\n"+importtotal(product,year));
                                System.out.println(productiveconsumption(product,year));
                                System.out.println(nameconsumption(product,year)+"\t<=\t"+ nameoutput(product,year) +
                                                   " \n\t- \t"+nameaccumulation(product,year)+
                                                   "\n\t-\t"+nameproductiveconsumption(product,year)+
                                                   "\n\t+\t"+nameimport(product,year)+
                                                   "\n\t-\t"+nameexports(product,year)+";");
                            }
                        /* now sum the total imports for the year */
                        System.out.println(" \n"+importtotal( year));
                        System.out.println("\n"+exporttotal(year));
                    }
            }
    }
    static String maximiser(int years)
    {
        String s="  max:\t"+nametarget(1);
        for (int i =2; i<=years; i++) s+= (" +\t"+nametarget(i));
        return s+";";
    }
    static double gettargnorm(int year)
    {
        double total=0;
        for(int i=1; i<=maxprod; i++) total = total +(matrices[targ][year][i]*matrices[targ][year][i]);
        return Math.sqrt(total);
    }
    static int findinheaders(String key,String[] headers)
    {
        for(int i=0; i<headers.length; i++)if(headers[i].equals(key))return i;
        return -1;
    }
    static String targeqn(int year) throws Exception
    {
// in the current case we just aim to maximise sum of private and governement consumption
        String s= "";
        int privatecons = findinheaders("HOUSEHOLDS", colheads[flow]);
        if(privatecons<0)throw new Exception("could not find indes of HOUSEHOLDS");
        int publiccons = findinheaders("PUBLICSECTOR",colheads[flow]);
        if(publiccons<0)throw new Exception("could not find indes of PUBLICSECTOR");
        for (int i=1; i<=maxprod; i++) s= s +
                                              nametarget(year)+" <=\t"+( 1/(matrices[flow][i][privatecons]+matrices[flow][i][publiccons]) )+ " "+nameconsumption(i,year)+";\n";
        return s ;
    }
    static String productiveconsumption(int product, int year)
    {
        String s=nameproductiveconsumption(product,year)+"\t>=\t"+nameflow(1,product,year);
        for (int i=2; i<=maxprod; i++)
            if(matrices[flow][i ][product]>0)s= s +"\n\t +\t"  +nameflow(i,product,year);
        return s+";";
    }
    static String accumulationtotal(int product, int year)
    {
        String s=nameaccumulation(product,year)+"\t>=\t"+nameaccumulation(1,product,year);
        for (int i=2; i<=maxprod; i++)
            if(matrices[cap][i ][product]>0)s= s +" \n\t+\t"  +nameaccumulation(i,product,year);
        return s+";";
    }
    static String importtotal(int product, int year)
    {
        String s=nameimport(product,year)+"\t>=\t"+nameimport(1,product,year);
        for (int i=2; i<=lastimportrow; i++)
            if(matrices[imports][i ][product]>0)s= s +" \n\t+\t"  +nameimport(i,product,year);
        return s+";";
    }
    static String importtotal(  int year)
    {
        String s=nameimport( year)+"\t>=\t"+nameimport(1, year);
        for (int i=2; i<=lastimportrow; i++)s= s +" \n\t+\t"  +nameimport(i, year);
        return s+";";
    }
    static String exporttotal(  int year)
    {
        String s=nameexport( year)+"\t>=\t"+nameexports(1, year);
        for (int i=2; i<=maxprod; i++)s= s +" \n\t+\t"  +nameexports(i, year);
        return s+";";
    }
    static String labourtotal(  int year)
    {
        String s=namelabourfor( year)+"\t>=\t"+namelabourfor(1, year);
        for (int i=2; i<=maxprod; i++)s= s +"\n +\t"  +namelabourfor(i, year);
        return s+";";
    }
    static int outputrowinheaders()throws Exception
    {
        int i;
        for(i=0; i<rowheads[flow].length; i++)
            if (rowheads[flow][i].equals("output".toUpperCase()))return i;
        throw new Exception("No output row in flow matrix");
    }
    static int labourRow()throws Exception
    {
        int i;
        for(i=0; i<rowheads[flow].length; i++)
            if (rowheads[flow][i].equals("labour".toUpperCase()))return i  ;
        throw new Exception("No labour row in flow matrix");
    }
    static String flowconstraintfor(int product, int input, int year)
    {
        String s=nameoutput(product,year)+"\t<=\n\t";
        if(matrices[flow][input][product]!=0.0)
            {
                if(input<lastimportrow)
                    {
                        double ratio = (outputs[product]/(matrices[flow][input][product]+matrices[imports][input][product]));
                        s=s+ ratio +
                          " "+nameflow(product,input,year)+"\n\t+"+ratio +" "+nameimport(product,input,year)+";";
                    }
                else
                    {
                        s=s+ (outputs[product]/(matrices[flow][input][product] ))+
                          " "+nameflow(product,input,year)+ ";";
                    }
            }
        else
            {
                s="";
            }
        return s;
    }
    static String outputequationfor(int product,int stock, int year)
    {
        String s=nameoutput(product,year)+"\t<=\n\t";
        if(matrices[cap][stock][product]!=0.0)
            {
                s=s+ (outputs[product]/matrices[cap][stock][product])+" "+namecap(product,stock,year)+";";
            }
        else
            {
                s="";
            }
        return s;
    }
    static String labourconstraintfor(int product,  int year)throws Exception
    {
        String s=nameoutput(product,year)+"\t<=\n\t\t";
        s=s+(outputs[product]/labour[product])+ " "+namelabourfor(product,year)+";";
        if(outputs[product]==0.0)s="";
        return s;
    }
    static String namelabourfor( int product, int year)
    {
        return "labourFor"+tidy(colheads[flow][product])+"_Yr"+year;
    }
    static String namelabourfor(  int year)
    {
        return "labourForYearYr"+ year;
    }
    static String nameoutput(int product, int year)
    {
        return "outputOf"+tidy(colheads[flow][product])+"_Yr"+year;
    }
    static String accumulationconstraint(int product, int input, int year)
    {
        if(matrices[cap][input][product]==0.0) return "";
        String s=namecap(product,input,year)+"\t<=\t";
        s=s+ namecap(product,input,year-1)+" + "+ nameaccumulation(product,input,year-1)+" -\t"+namedep(product,input,year-1);
        return s+";";
    }
    static String nameaccumulation(int product, int input, int year)
    {
        //   System.out.println("acc "+product+ ","+input+","+year);
        return "accumulationFor"+tidy(colheads[flow][product])+"_Of_"+tidy(colheads[flow][input])+"_Yr"+year;
    }
    static String nameaccumulation(int product,   int year)
    {
        return "accumulationOf"+tidy(colheads[flow][product])+"_Yr"+year  ;
    }
    static String nameconsumption(int product,   int year)
    {
        return "finalConsumptionOf"+tidy(colheads[flow][product])+"_Yr"+year  ;
    }
    static String nametarget(int year)
    {
        return "targetFulfillmentForYear"+year;
    }

    static String tidy(String s)   // remove all non alpha
    {
        String ns="";
        char []C= s.toCharArray();
        for (char c:C)
            {
                if ((c>='A') && (c<='Z'))
                    {
                        ns+=c;
                    }
            }
        return ns.trim();
    }
    static String nameproductiveconsumption(int product,   int year)
    {
        return "productiveConsumptionOf_"+tidy(colheads[flow][product])+"_Yr"+year  ;
    }
    static String nameflow(int product, int input, int year)
    {
        return "flowFor_"+tidy(colheads[flow][product])+"_Of_"+tidy(colheads[flow][input])+"_Yr"+year;
    }
    static String nameimport(int product, int input, int year)
    {
        return "importsFor_"+tidy(colheads[flow][product])+"_Of_"+tidy(colheads[flow][input])+"_Yr"+year;
    }
    static String nameimport(int product,  int year)
    {
        return "importsOf_"+tidy(colheads[flow][product]) +"_Yr"+year;
    }
    static String nameexports(int product,  int year)
    {
        return "exportsOf_"+tidy(colheads[flow][product]) +"_Yr"+year;
    }
    static String nameexport (  int year)
    {
        return "exports_Yr"+year;
    }
    static String nameimport(  int year)
    {
        return "imports_Yr"+year;
    }
    static String namedep(int product, int input, int year)
    {
        return "depreciationIn_"+tidy(colheads[flow][product])+"_Production_Of_"+tidy(colheads[flow][input])+"_Yr"+year;
    }
    static String namecap(int product, int input, int year)
    {
        return "capitalstockFor_"+tidy(colheads[flow][product])+"_Made_Up_Of_"+tidy(colheads[flow][input])+"_Yr"+year;
    }

    static int countyears(String[]heads)
    {
        int j=0,i;
        for(i=0; i<heads.length; i++)
            if(heads[i]!=null)
                if(heads[i].startsWith("YEAR"))j++;
        return j;
    }
}
