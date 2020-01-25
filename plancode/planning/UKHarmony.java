package planning;
import java.io.*;
import java.util.*;
/** Another programme to construct 5 year or n year socialist plans this time
it is modified to use the UK input output data as source info, ie, it handles imports and exports
 *<p>
 * It uses the Harmony algorithm to solve the plan<p>
 * Usage java planning.nyearplan flowmatrix.csv capitalmatrix.csv depreciationmatrix.csv laboursupplyandtargets.csv
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
public class UKHarmony extends nyearHarmony {
    static final int flow=0,cap=1,dep=2,targ=3, imports=4;
    ;
    static int exports=0;// will calculate appropriate number
    static final short consistencyrange = 129;// only check the row and col headers up to this
    static final short lastimportrow =105; // disregard imports below this
    static final int labourcolintargets=129;
    static final int forexchangecol=130;
    static final int targetvectorlength=forexchangecol+1;
    static final int lastexportablerow = 111;
    public static void main(String [] args)throws Exception {
        rowheads = new String[5][1];
        colheads = new String[5][1];
        matrices= new double [5][1][1];

        double [][] uktarg;
        if (args.length !=5 ) {
            System.err.println("Usage java planning.UKHarmony flowmatrix.csv capitalmatrix.csv depreciationmatrix.csv laboursupplyandtargets.csv  imports.csv");
        }  else {
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

            if (importctab == null) {
                throw new Exception(" Error opening or parsing "+args[imports]);
            }
            if (flowtab == null) {
                throw new Exception(" Error opening or parsing "+args[flow]);
            }
            if (captab == null) {
                throw new Exception(" Error opening or parsing "+args[cap]);
            }
            if (deptab == null) {
                throw new Exception(" Error opening or parsing "+args[dep]);
            }
            if (labetctab == null) {
                throw new Exception(" Error opening or parsing "+args[targ]);
            }
            pcsv[] parsed = {flowtab,captab,deptab,labetctab,importctab};
            int targlab=-1,targdeficit=-1;
            for (int i=flow ; i<=imports; i++) {
                rowheads[i]=flowread.getrowheaders(parsed[i]);
                colheads[i]=flowread.getcolheaders(parsed[i]);
                matrices[i]=flowread.getdatamatrix(parsed[i]);

                int consistency=consistent(colheads[flow],colheads[i]);
                if(consistency>=0) if(i!= targ)throw new Exception(" flow table col header inconsistent with header of table "+args[i]
                                +"\n"+  colheads[flow][consistency]+" !="+colheads[i][consistency]+" at position "+consistency);
                if(i!= targ) {
                    consistency=consistent(colheads[i],rowheads[i],(i==imports?lastimportrow:consistencyrange));

                    if(consistency>0) throw new Exception("   col header inconsistent with row header for table  "+i
                                                              +"\n'"+  colheads[i][consistency]+"' !='"+rowheads[i][consistency]+"' at position "+consistency
                                                              +"\ncolheads="+Arrays.toString(colheads[i])
                                                              +"\nrowheads="+Arrays.toString(rowheads[i]));
                } else {
                    // the format of the target vector is more resticted for the UK io table since
                    // it assumes we just want to go on maximising the sum of current public and private
                    // sector production. The target spreadsheet for the UK case just supplies labour and trade deficit
                    // so we have to construct an old style target matrix whilst saving the details we have just read in
                    // from the labtargs file
                    uktarg=matrices[i];// save what we read
                    /** the new matrix has as many years as specified in the targets file, but is fully expanded to
                     * have as many rows as there are columns in the standard flow matrix, the last row, (lastimportrow-1)
                     * will be now the labour row */
                    matrices[i]=new double[uktarg.length][targetvectorlength];
                    targlab = findinheaders("LABOUR",colheads[targ]);
                    targdeficit =findinheaders("TRADEDEFICIT",colheads[targ]);


                    /** fill in now the labour targets from the original file read in */
                    for (int j=0; j<uktarg.length; j++) {
                        matrices[i][j][labourcolintargets]=uktarg[j][targlab];
                        matrices[i][j][forexchangecol]=uktarg[j][targdeficit]*0.01;
                        // aim to be within this of the deficit given the allocated cash
                    }

                    // create the new colheads for the targets
                    colheads[targ]=new String[targetvectorlength];
                    colheads[targ][labourcolintargets]="LABOUR";
                    colheads[targ][forexchangecol]="FOREX";
                    for(int j=0; j<consistencyrange-1; j++)colheads[targ][j]=tidy(colheads[flow][j]);
                    writeln(colheads[targ]);
                    /** now find the public and private consumption cols of the flow matrix */
                    int privatecons = findinheaders("HOUSEHOLDS", colheads[flow]);
                    if(privatecons<0)throw new Exception("could not find indes of HOUSEHOLDS");
                    int publiccons = findinheaders("PUBLICSECTOR",colheads[flow]);
                    if(publiccons<0)throw new Exception("could not find indes of PUBLICSECTOR");
                    //  writeln(colheads[targ]);
                    /** set each year to have the same target */
                    for (int y=1; y<uktarg.length; y++)
                    {
                        for(int p=0; p<consistencyrange-1; p++)
                            matrices[targ][y][p]=(matrices[flow][p][privatecons]+matrices[flow][p][publiccons]) ;
                        //    writeln(matrices[i][y]);
                    }
                }
            }
            // go through the targets matrix and make sure no targets are actually zero - make them very small positive amounts
            for(int i=0; i<matrices[targ].length; i++)
                for(int j=0; j<matrices[targ][i].length; j++)
                    if(matrices[targ][i][j]==0)matrices[targ][i][j]=1 - Harmonizer.capacitytarget;
            outputs = matrices[flow][outputrowinheaders() ];
            labour = matrices[flow][labourRow()];
            years = countyears(rowheads[targ]);

            // System.out.println("run for "+years+" years");
            maxprod=129;// this is specific to the UK domestic table that I am using
            writeln("maxprod "+maxprod +" forexchangecol "+forexchangecol);
            //  System.out.println(maximiser(years));
            yearXproductIntensityIndex=new int[years+1][maxprod+2];
            int year;
            caps= countnonzero(matrices[cap]);

            System.out.println("caps "+caps);
            // work out how many products the harmonizer will have to solve for
            // assume that we have N columns in our table and y years then
            // we have Ny year product combinations
            // in addition we have y labour variables
            // plys y foreign exchange variables
            // and caps.y capital stocks
            // so the total is y(caps+N+1)

            C =new TechnologyComplex ((forexchangecol+caps)*years );
            writeln("productnum "+C.productCount());
            // Assign identifiers to the outputs

            for(int i=1; i<=forexchangecol; i++)
                for (  year=1; year<=years; year++) {

                    C.setProductName( flownum(i,year), productName(i,year,flownum(i,year)) );

                }

            for(int i=1; i<=maxprod ; i++)// name capital stocks
                for(int j=1; j<=maxprod; j++)
                    for (  year=1; year<=years; year++)
                        if (matrices[cap][i][j] >0) {
                            C.setProductName(capnum(relativecapnum[i][j],year,caps), "C["+i+"]["+j+"]"+year );
                        }



            for (year=1; year<=years; year++) {
                // add an export technique for each exportable product
                for(int i=1; i<=lastexportablerow; i++)
                {   double [] usage = {1};
                    int  [] codes = {flownum(i,year)};
                    Technique t= new Technique(

                        // add a production technology for each definite product
                        for(int i=1; i<=maxprod; i++)
                {   double [] usage = new double[countinputsTo(i)];
                        int  [] codes = new int[countinputsTo(i)];
                        int j=0;
                        int index;
                        for(int k=1; k<=maxprod+1; k++) {
                            if (matrices[flow][k][i]>0) {
                                double flows=matrices[flow][k][i];
                                usage[j]= flows;
                                codes[j]=flownum(k,year);
                                j++;
                            }
                            if( k<=maxprod)// no labour row for the capital matrix so we miss last row
                                if (matrices[cap][k][i]>0) {
                                    usage[j]= matrices[cap][k ][i ];
                                    codes[j]=capnum(relativecapnum[k][i],year,caps);
                                    j++;
                                }
                        }
                        Technique t=new Technique (  productName(i,year,flownum(i,year)%maxprod), index=flownum(i,year), outputs[i],   usage, codes);
                        C.addTechnique(t);
                        yearXproductIntensityIndex[year][i]=C.techniqueCount()-1;
                    }
                    // now add a joint production technique for each type of capital accumulation except for the last year
                    if(year<years) {
                    for(int i=0; i<caps ; i++) {
                            double [] usage = {1};// one unit of an input produces one unit of acc in next year
                            int  [] codes =  {flownum(capnumtoflownum[i],year)};// always uses current product for this year
                            int mainoutput=capnum(i,year+1,caps);
                            double grossout = 1;
                            if (year == years-1) {
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
                        }
                    }

                }
                for (Technique t:C.techniques) {
                    //    writeln(t.toString());
                }

                // now set up the initial resource vector
                double []initialResource = new double[C.productCount()];
                // put in each years labour

                //   writeln("labour row "+lc);
                for(int y=1; y<=years; y++) {
                    int ir=flownum(labourcolintargets,y);
                    //		System.out.println("lc ="+lc+" ir="+ir);
                    initialResource [ir]=matrices[targ][y][labourcolintargets];
                    C.nonproduced[ir]=true;
                    C.nonfinal[ir]=true;
                }

                // put in each years initial capital stock allowing for depreciation
                for(int i=1; i<matrices[cap].length; i++) {

                    for(int j=1; j<matrices[cap][i].length; j++) {
                        if(matrices[cap][i][j]>0)
                            for(int y=1; y<=years; y++) {
                                int cn=capnum(relativecapnum[i][j],y,caps);
                                if (Harmonizer.verbose)System.out.println(" "+i+","+j+","+y+","+cn);
                                if(y==1)C.nonproduced[cn]=true;
                                C.nonfinal[cn]=true;
                                initialResource [cn]=matrices[cap][ i][j]* Math.pow(1-matrices[dep][i][j],y-1);
                            }
                    }
                }


                // now set up the target vector
                double []targets = new double[C.productCount()];
                // initialise to very small numbers to prevent divide by zero

                for(int i=0; i<targets.length; i++)targets[i]=0.03;
                for(int y=1; y<=years; y++) {
                    for(int j=1; j<labourcolintargets; j++)  // do not include the labour col of the targets
                        targets[flownum(j,y)]=matrices[targ][y][j];
                    //   writeln("matrix targs , year "+y);
                    //   writeln(matrices[targ][y]);
                }

                //     writeln("targets");
                //     writeln(targets);
                Harmonizer.verbose=true;
                Harmonizer.iters=1;
                if( Harmonizer.verbose) { //writeln("allheadings");
                    writeln(C.allheadings());
                }
                long start = System.currentTimeMillis();
                double [] intensities= Harmonizer.balancePlan(   C, targets,  initialResource   );
                long stop= System.currentTimeMillis();

                writeln("took "+((stop-start)*0.001)+" sec");
                printResults(C, intensities, initialResource);
            }
        }
        static int   consistent(String []shorter,String[]longer,int range) { /* return -1 if the lists are consistent */
            int shortone=shorter.length;
            if(longer.length<shorter.length)shortone=longer.length;
            if (shortone > range) shortone= range;
            for(int i= 0 ; i<shortone; i++) {
                if(shorter[i]==null) return i;
                if(longer[i]==null) return i;
                if (!tidy(shorter[i]).equals(tidy(longer[i])))return i;
            }
            return -1;
        }
        static int findinheaders(String key,String[] headers) {
            for(int i=0; i<headers.length; i++)if(headers[i].equals(key))return i;
            return -1;
        }
        static int   consistent(String []shorter,String[]longer ) {
            return consistent(shorter,longer,consistencyrange);
        }

        static String productName(int prod, int year,int internalcode) {
            return colheads[targ][prod]+year;//+"Y"+year+"{"+internalcode+"}";
        }
        static int flownum(int prod, int year) {
            return  (prod-1)+(year-1)*(forexchangecol);
        }
        static int capnum(int prod, int year,int maxcap) {
            return (prod)+(year-1)*(maxcap)+years*(forexchangecol);
        }
        static String tidy(String s)// remove all non alpha
        {   String ns="";
            char []C= s.toCharArray();
            for (char c:C) {
                if ((c>='A') && (c<='Z')) {
                    ns+=c;
                }
            }
            return ns;
        }
    }
