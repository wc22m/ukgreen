package planning;
import java.io.*;
import java.util.*;
/** A programme to construct 5 year or n year socialist plans
 *
 * It produces an output file of the plan in lp-solve format on standard out<p>
 * Usage java planning.nyearplan flowmatrix.csv capitalmatrix.csv depreciationmatrix.csv laboursupplyandtargets.csv
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
public class nyearEPL
{
    static final int flow=0,cap=1,dep=2,targ=3;
    static String [][] rowheads = new String[4][1];
    static String [][]colheads = new String[4][1];
    static double [][][] matrices= new double [4][1][1];
    static double []outputs;
    static double[] labour;
    public static boolean nodepinflow=false;// assume that the flow element of the I/O table is depreciation if true
    static int maxprod,idlen=240;
    static int   consistent(String []shorter,String[]longer)   /* return -1 if the lists are consistent */
    {
        if(longer.length<shorter.length)return 0;
        for(int i= 0 ; i<shorter.length; i++)
            {
                if(shorter[i]==null) return i;
                if(longer[i]==null) return i;
                if (!shorter[i].equals(longer[i]))return i;
            }
        return -1;
    }
    public static void main(String [] args)throws Exception
    {
        if (args.length !=4 )
            {
                System.err.println("Usage java planning.nyearEPL flowmatrix.csv capitalmatrix.csv depreciationmatrix.csv laboursupplyandtargets.csv");
            }
        else
            {
                csvfilereader flowread,capread,depread,labtargread;
                flowread=new csvfilereader(args[flow]);
                pcsv flowtab = flowread.parsecsvfile();
                capread= new csvfilereader(args[cap]);
                pcsv captab = capread.parsecsvfile();
                depread = new csvfilereader(args[dep]);
                pcsv deptab = depread.parsecsvfile();
                labtargread = new csvfilereader(args[targ]);
                pcsv labetctab=labtargread .parsecsvfile();
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
                pcsv[] parsed = {flowtab,captab,deptab,labetctab};
                for (int i=flow ; i<=targ; i++)
                    {
                        rowheads[i]=flowread.getrowheaders(parsed[i]);
                        colheads[i]=flowread.getcolheaders(parsed[i]);
                        matrices[i]=flowread.getdatamatrix(parsed[i]);
                        int consistency=consistent(colheads[flow],colheads[i]);
                        if(consistency>=0) throw new Exception(" flow table col header inconsistent with header of table "+i
                                                                   +"\n"+  colheads[flow][consistency]+" !="+colheads[i][consistency]+" at position "+consistency);
                        if(i!= targ)
                            {
                                consistency=consistent(colheads[i],rowheads[i]);
                                if(consistency>=0) throw new Exception("   col header inconsistent with row header for table  "+i
                                                                           +"\n"+  colheads[i][consistency]+" !="+rowheads[i][consistency]+" at position "+consistency
                                                                           +"\ncolheads="+Arrays.toString(colheads[i])
                                                                           +"\nrowheads="+Arrays.toString(rowheads[i]));
                            }
                    }
                outputs = matrices[flow][outputrowinheaders() ];
                //   System.out.println("labour row is "+labourRow());
                labour = matrices[flow][labourRow()];
                // System.out.println("outputs "+Arrays.toString(outputs));
                // System.out.println("labour "+Arrays.toString(labour));
                // System.out.println("flow matrix "+Arrays.deepToString(matrices[flow]));
                // System.out.println("row headers "+Arrays.deepToString(rowheads));
                int years = countyears(rowheads[targ]);
                maxprod=colheads[flow].length-1;
                int year;
                // System.out.println(maximiser(years));
                System.out.print("Target[");
                for (year=1; year<=years; year++)
                    // set a target fiven by leontief demand for year
                    System.out.print(targeqn(year)+(year<years?" ":"];\n"));
                System.out.println(" ");
                for (year=1; year<=years; year++)
                    // set technique to produce the final outputs for all products in target list
                    System.out.println(targtech(year));
                for (year=1; year<=years; year++)
                    {
                        System.out.println(labourtotal(year));
                        // now print out labour supply constraint
                        System.out.println("Resource "+"\t " +matrices[targ][year][labourRow()]+"\t"+namelabourfor(year)+";");
                        for(int product=1; product<=maxprod; product++)
                            {
                                // iterate through all the things to be produced
                                System.out.print("\nTechnique t"+nameoutput(product,year)+"[");
                                for(int stock =1; stock<=maxprod; stock++)
                                    {
                                        String eq=outputequationfor(product,stock,year);
                                        String eq2 = flowconstraintfor(product,stock,year);
                                        if(!((eq+eq2).equals(""))) System.out.print(eq+" "+eq2+(stock<maxprod?" ":""));
                                    }
                                System.out.println(labourconstraintfor(product,year)+"]-> "+outputs[product]+" "+nameoutput(product,year)+";\n");
                                for(int stock =1; stock<=maxprod; stock++)
                                    {
                                        //  System.out.println(namedep(product,stock,year)+" =\t"+matrices[dep][stock][product]+" "+namecap(product,stock,year)+";");
                                        if (year>1)
                                            {
                                                if(matrices[cap][stock][product]>0)
                                                    {
                                                        if (year == years)
                                                            {
                                                                System.out.println("Technique A"+namecap(product,stock,year)+
                                                                                   "of"+nameoutput(stock,year-1)+"[1 "+
                                                                                   nameoutput(stock,year-1)+"]->1 "+namecap(product,stock,year)+";");
                                                            }
                                                        else
                                                            {
                                                                double [] coproduction= new double[years-year+1];
                                                                for (int k=0; k<coproduction.length; k++)
                                                                    coproduction[k]=Math.pow(1-matrices[dep][stock][product],k);// depreciate added on capital stock in future years
                                                                String s ="Technique A"+namecap(product,stock,year)+
                                                                          "of"+nameoutput(stock,year-1)+"[1 "+
                                                                          nameoutput(stock,year-1)+"]->[";
                                                                for (int k=0; k<coproduction.length; k++)
                                                                    {
                                                                        s+=coproduction[k];
                                                                        s+=" ";
                                                                        s+=namecap(product,stock,k+year)+" ";
                                                                    }
                                                                System.out.println(s+"];");
                                                            }
                                                        /*   //  System.out.println(accumulationconstraint(product,stock,year,years)+"\n");
                                                            String s="Technique "+nameaccumulation(product,input,year)+"[1 "+nameoutput(input,year-1)+"]->\t1 "+nameaccumulation(product,input,year)+";\n";
                                                        s+="Technique "+namecap(product,input,year)+"[1 "+nameaccumulation(product,input,year)+"]->[";
                                                        for (int y=year; y<=years; y++)
                                                        s=s+" "+Math.pow(1-matrices[dep][input][product],y-year)+" "+namecap(product,input,y);
                                                        //     s=s+ namecap(product,input,year-1)+" + "+ nameaccumulation(product,input,year-1)+" -\t"+namedep(product,input,year-1);
                                                         println(s+"];");*/
                                                    }
                                                /*
                                                 * if (year == years-1) {
                                                // penultimate year gets a simple technique
                                                //   writeln("i"+i);writeln("capnumtoflownum[i]"+capnumtoflownum[i]);writeln("flownum(capnumtoflownum[i],year)"+flownum(capnumtoflownum[i],year));
                                                Technique t=new Technique ("A"+C.productIds[mainoutput]+"of"+C.productIds[codes[0]], mainoutput,grossout,   usage, codes);
                                                C.addTechnique(t);
                                                } else {
                                                // other years get a joint production method
                                                double [] coproduction= new double[years-year-1];
                                                int [] cocodes= new int[years-year-1];
                                                for (int k=0; k<cocodes.length; k++) {
                                                    cocodes[k]=capnum(i,year+k+2,caps);
                                                    coproduction[k]=Math.pow(1-deprate(i),k+1);// depreciate added on capital stock in future years
                                                }
                                                Technique t=new JointProductionTechnique("A"+C.productIds[mainoutput]+"of"+C.productIds[codes[0]],mainoutput,grossout,usage,codes,coproduction,cocodes);
                                                C.addTechnique(t);
                                                }
                                                 */
                                            }
                                        else     // set initial capital stocks
                                            {
                                                if(matrices[cap][stock][product]>0)
                                                    for (int y=year; y<=years; y++)
                                                        System.out.println("Resource \t"+
                                                                           (Math.pow( 1-matrices[dep][stock][product],y-1)*matrices[cap][stock][product])+"\t \t"+namecap(product,stock,y)+";");
                                            }
                                    }
                                // System.out.println(labourconstraintfor(product,year));
                                // System.out.println(accumulationtotal(product,year));
                                //  System.out.println(productiveconsumption(product,year));
                                // System.out.println(nameconsumption(product,year)+"\t<=\t"+ nameoutput(product,year) + " - "+nameaccumulation(product,year)+                                       "-"+nameproductiveconsumption(product,year)+";");
                            }
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
    static String targeqn(int year)
    {
        String s= "";
        for (int i=1; i<=maxprod; i++)if(matrices[targ][year][i]>0)s= s +
                        +( matrices[targ][year][i] )+ " "+ ((nameconsumption(i,year)))+(i<maxprod?" ":" ");
        return s ;
    } static String tidy(String s)   // remove all non alpha
    {
        String ns="";
        char []C= s.toCharArray();
        for (char c:C)
            {
                if ((c>='A') && (c<='Z'))
                    {
                        if(ns.length()<idlen) ns+=c;
                    }
            }
        return ns.trim() ;
    }
    static String targtech(int year)
    {
        String s= "";
        //  for (int i=1; i<=maxprod; i++)if(matrices[targ][year][i]>0)s= s +"Technique[1 "
        //                 +( nameoutput(i,year) )+ "]-> \t1 "+nameconsumption(i,year)+(";\n");
        return s ;
    }
    static String productiveconsumption(int product, int year)
    {
        String s=nameproductiveconsumption(product,year)+"\t>=\t"+nameflow(1,product,year);
        for (int i=2; i<=maxprod; i++)s= s +" +\t"  +nameflow(i,product,year);
        return s+";";
    }
    static String accumulationtotal(int product, int year)
    {
        String s=nameaccumulation(product,year)+"\t>=\t"+nameaccumulation(1,product,year);
        for (int i=2; i<=maxprod; i++)s= s +" +\t"  +nameaccumulation(i,product,year);
        return s+";";
    }
    static String labourtotal(  int year)
    {
        String s="";
//       for (int i=1; i<=maxprod; i++)s+= "\nTechnique[ 1 "+namelabourfor( year)+"\t]->\t 1 "+namelabourfor(i, year) +";\n";
        return s;
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
        String s="";
        if(matrices[flow][input][product]!=0.0)
            {
                if (!(nodepinflow &&(matrices[cap][input][product]>0)))
                    s=s+ " \t"+(matrices[flow][input][product])+" \t"+nameflow(product,input,year)+"\n";
            }
        else
            {
                s="";
            }
        return s;
    }
    static String outputequationfor(int product,int stock, int year)
    {
        String s="";
        if(matrices[cap][stock][product]!=0.0)
            {
                s=s+ (matrices[cap][stock][product])+" "+namecap(product,stock,year)+" ";
            }
        else
            {
                s="";
            }
        return s;
    }
    static String labourconstraintfor(int product,  int year)throws Exception
    {
        String s="";
        s=s+(labour[product]);
        s=s+" "+namelabourfor(year)+" ";
        if(labour[product]==0.0)s="";
        return s;
    }
    static String namelabourfor( int product, int year)
    {
        return "labourFor"+colheads[flow][product]+year;
    }
    static String namelabourfor(  int year)
    {
        return "labourForYear"+ year;
    }
    static String nameoutput(int product, int year)
    {
        return /*"outputOf"+*/tidy(colheads[flow][product])+year;
    }
    static String nameaccumulation(int product, int input, int year)
    {
        //   System.out.println("acc "+product+ ","+input+","+year);
        return "AC"+tidy(colheads[flow][product])+"Of"+colheads[flow][input]+year;
    }
    static String nameaccumulation(int product,   int year)
    {
        return "A"+tidy(colheads[flow][product])+year  ;
    }
    static String nameconsumption(int product,   int year)
    {
        return tidy(colheads[flow][product])+year  ;
    }
    static String nametarget(int year)
    {
        return "targetFulfillmentForYear"+year;
    }
    static String nameproductiveconsumption(int product,   int year)
    {
        return "productiveConsumptionOf"+colheads[flow][product]+year  ;
    }
    static String nameflow(int product, int input, int year)
    {
        return nameoutput(input,year);
        //   return "flowFor"+colheads[flow][product]+"Of"+colheads[flow][input]+year;
    }
    static String namedep(int product, int input, int year)
    {
        return "depreciationIn"+colheads[flow][product]+"of"+colheads[flow][input]+year;
    }
    static String namecap(int product, int input, int year)
    {
        return "C_"+input+"_"+product+"yr"+/*colheads[flow][input]+*/year;
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
