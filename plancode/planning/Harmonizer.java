package planning;
import java.io.*;
import java.util.*;
/** A class to optimise a set of linear production technologies to meet
a Kantorovich style output target and having a pregiven set of initial resources.<p>
 *
 * It produces an output file of the plan in lp-solve format on standard out<p>
 *  Class to provide optimisation of plans using the algorithm in Towards a New Socialism
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
public class Harmonizer
{
    static final double useweight = 1;
    static   double phase2adjust = 0.4 ;
    static   double capacitytarget=0.98;
    static double slackness = 1- capacitytarget;
    static   double startingtemp=0.23;
    static double meanh=0;
    static boolean phase1rescale=true,phase1average=true;
    static boolean phase2rescale=true;
    static boolean usemeanmethod = false;
    static boolean eqnonfin=false;// apply equalisation to non final goods
    static int iters=100;
    static boolean verbose=false;
    static double [] productHarmony= {};
    static double[] HphaseA= {},HphaseB= {},HphaseC= {},HphaseD= {},HphaseE= {}; // snapshots of harmony in different stages of algorithm
    /** C is a technology complex, fixed resources should be added as nonproduced products<p>
     * planTargets is the target output of each product<p>
     * returns a vector of technology intensities*/
    public static double [] balancePlan(TechnologyComplex C, double[] planTargets, double [] initialresource )throws Exception
    {
        if (verbose)
            {
                writeln("balancePlan");
                writeln(planTargets);
                writeln( initialresource);
            };
        if(planTargets.length != C.productCount())
            throw new InconsistentScale(
                "plan target has length "+planTargets.length+" but the number of products in TechnologyComplex is "+C.techniqueCount()
            );
        Vector< Vector<Integer>> producerIndex=C.buildProducerIndex();
        double [] intensity = new double[C.techniqueCount()];
        initialiseIntensities(intensity,C,initialresource,planTargets);
        if(verbose)
            {
                System.out.print("initialised intensity");
                writeln(intensity);
            }
        double t=startingtemp;
        productHarmony=new double[C.productCount()];
        for(int i=0; i<iters; i++)
            {
                defineNonFinalTargets(intensity,C,initialresource,planTargets);
                double[] gross=computeGrossAvail(C,intensity,initialresource);
                double [] netOutput = computeNetAvailable(C,intensity,initialresource);
                for(int k=0; k<netOutput.length; k++)
                    //   if(!C.nonfinal[k])
                    productHarmony[k]=   ( Harmony.H(planTargets[k],netOutput[k],gross[k]));
                meanh=mean(productHarmony,C);
                double [] productHarmonyDerivatives = computeHarmonyDerivatives(netOutput,planTargets,C,intensity,gross);
                // phase A
                HphaseA=productHarmony;
                adjustIntensities(intensity,
                                  productHarmonyDerivatives,
                                  t,
                                  C,
                                  productHarmony,
                                  producerIndex,
                                  initialresource,planTargets);
                if(verbose)
//if(i==(iters-1))
                    printstate(  intensity,  C,  initialresource,   planTargets);
            }
        return intensity;
    }
    /** compute the derivatives of the harmonies of all products with repect to marginal increase in output in terms of
    actual output units not intensities */
    static  double []  computeHarmonyDerivatives(double[] netOutput,double[] planTargets,TechnologyComplex C,double[] intentsity,double[]gross )
    {
        double []dh=new double[netOutput.length];
        for (int i=0; i<dh.length; i++)
            {
                dh[i]= Harmony.dH(planTargets[i],netOutput[i],gross[i]);
            }
        for(int solve=0; solve<3; solve++)
            for (int i=0; i<dh.length; i++)
                if(C.nonfinal[i]&&!C.nonproduced[i])  // weighted average of derivative due to shortage and due to potential other use
                    {
                        dh[i]= (dh[i]+ useweight* nonfinalHarmonyDerivativeMax(netOutput,i,dh,  C )   )/(useweight+1);
                    }
        return dh;
    }
    static  double []  computeHarmony (double[] netOutput,double[] planTargets,TechnologyComplex C,double[] intentsity,double[]gross )
    {
        double []h=new double[netOutput.length];
        for (int i=0; i<h.length; i++)
            {
                h[i]= Harmony.H(planTargets[i],netOutput[i],gross[i]);
            }
        return h;
    }
    static void printstate(double[]intensity,TechnologyComplex C, double []initial, double [] targets)
    {
        double [] netOutput = computeNetAvailable(C,intensity,initial );
        double[]gross =computeGrossAvail(C,intensity,initial );
        double [] h =computeHarmony(netOutput,targets,C,intensity,gross);
        double [] hd = computeHarmonyDerivatives(netOutput,targets,C,intensity,gross);
        printstateS(netOutput, hd,h,C,intensity);
    }
    static void printstateS(double[] netOutput,double[]productHarmonyDerivatives,double[]productHarmony,TechnologyComplex C,double[]intensity)
    {
        System.out.println(""+C.prodheadings());
        System.out.print("netoutput ,");
        writeln(netOutput);
        System.out.print("h ,");
        writeln(productHarmony);
        System.out.print("dh/dp ,");
        writeln (productHarmonyDerivatives);
        double[] expansionrate=new double[C.techniques.size()];
        double[] gainrate=new double[C.techniques.size()];
        for(int i=0; i<C.techniques.size(); i++)
            {
                Technique t= C.techniques.elementAt(i);
                gainrate[i]=t.rateOfHarmonyGain(productHarmonyDerivatives);
                expansionrate[i] =1+sigmoid(gainrate[i] ) *startingtemp*phase2adjust ;
            }
        System.out.println(""+C.techheadings());
        System.out.print("intensity,");
        writeln(intensity);
        System.out.print("gainrate ,");
        writeln(gainrate);
        System.out.print("expansionrate ,");
        writeln(expansionrate);
    }
    static void resline (String s,double[]v,PrintStream p )
    {
        p.print(s);
        for(int i=0; i<v.length; i++)
            p.print(","+v[i]);
        p.println();
    }
    static void printResults(String prodf,TechnologyComplex C, double [] intensity,double[] initialResource, double [] targets)throws Exception
    {
        double [] netoutput=computeNetAvailable(C,intensity,initialResource);
        double []gross=computeGrossAvail(  C,   intensity, initialResource);
        double []use = computeUsage( C,   intensity, initialResource);
        PrintStream outf = new PrintStream(prodf);
        outf.print("PRODUCTS");
        for(int i=0; i<netoutput.length; i++)
            outf.print(","+C.productIds[i]);
        outf.println();
        resline("\"Targets\"",targets,outf);
        resline("\"Output available for final consumption\"",netoutput,outf);
        resline("\"Intermediate use plus exports\"",use,outf);
        resline("\"Gross Avail product plus stocks\"",gross,outf);
        resline("HphaseA-startofiteration",HphaseA,outf);
        resline("HphaseB-afterequalize",HphaseB,outf);
        resline("HphaseC-afterexpandbygain",HphaseC,outf);
        resline("HphaseD-beforephase1rescale",HphaseD,outf);
        resline("HphaseE-afterphase1rescale",HphaseE,outf);
        double [] h =computeHarmony(netoutput,targets,C,intensity,gross );
        resline("Harmony-atend",h,outf);
        outf.print("\"Target fullfillment\"");
        double [] hd = computeHarmonyDerivatives(netoutput,targets,C,intensity,gross);
        double total=0;
        int count=0;
        for(int i=0; i<netoutput.length; i++)
            {
                outf.print(","+netoutput[i]/targets[i]);
                total+= netoutput[i]/targets[i];
                count++;
            };
        outf.println();
        outf.print("\"HarmonyValueperunit(dh/dp)\"");
        for(int i=0; i<netoutput.length; i++)
            outf.print(","+hd[i]);
        outf.println();
        System.out.print("TECHNIQUES");
        writeln(C.techheadings());
        System.out.print("intensity,");
        writeln(intensity);
        System.out.print("Output of main product");
        for(int i=0; i<intensity.length; i++)
            System.out.print(","+intensity[i]*C.techniques.elementAt(i).getGrossOutput());
        System.out.println();
        /*
        System.out.println("Mean fullfillment ratio, "+ "    useweight ,"+  "phase2adjust ,"+
                           "capacitytarget,"+    "fractionalmove,"+
                           "   phase1rescale,"+ "   phase2rescale,"+ "   simpleequalisation , usesigmoid,iters");
        System.out.println("  "+(total/count)+
                           "     ,"+useweight+
                           "  ,"+ phase2adjust + " ,"+capacitytarget+
                           " ,"+fractionalmove+
                           "  ,"+phase1rescale+
                           "  ,"+phase2rescale+
                           "   ,"+  simpleequalisation+
                           ","+Harmony.useSigmoid+","+iters) ;*/
    }
    static void  writeln(String s)
    {
        System.out.println(s);
    }
    static void  writeln(double []d)
    {
        for(int i=0; i<d.length; i++)System.out.printf("%5.4f,",d[i]);
        writeln("");
    }
    static void  write (double []d)
    {
        for(int i=0; i<d.length; i++)System.out.printf("%5.4f,",d[i]);
    }
    /** for non final goods we make derivatives their harmonies the maximum of the derivatives of the harmonies of their users */
    static double nonfinalHarmonyDerivativeMax(double[] netOutput,int nonfinal,double [] dharmonies,TechnologyComplex C )
    {
        Vector< Vector<Integer>> userIndex;
        userIndex=C.buildUserIndex();
        double max,total;
        max= -1e22;
        total=0;
        int best;
        best=0;
        Vector<Integer> users = userIndex.elementAt(nonfinal);
        for(int i=0; i<users.size(); i++)
            {
                int techno=users.elementAt(i);
                Technique t= C.techniques.elementAt(techno);
                int produces =t.getProductCode();
                double dhp= dharmonies[produces];
                double d= dhp*marginalphysicalproduct(  techno,   nonfinal, C );
                // if it is a joint producing technology it will have harmony contributions from the coproducts
                if (t instanceof JointProductionTechnique)
                    {
                        JointProductionTechnique J=(JointProductionTechnique)t;
                        double[] mpp= J. marginalphysicalcoproducts(nonfinal);
                        int[] codes= J.getCoproductionCodes();
                        for (int j=0; j<mpp.length; j++)
                            d+= dharmonies[codes[j]]*mpp[j];
                    }
                total +=d;
                if((d)>max)
                    {
                        max=d;
                    }
            }
        return total/users.size();
        //return max;
    }
    /** marginal physical product of technology techno with respect to the input */
    static double marginalphysicalproduct(int techno, int input, TechnologyComplex C )
    {
        Technique user=C.techniques.elementAt(techno);
        return user.marginalphysicalproduct(input);
    }
    static double mean(double[] m,TechnologyComplex C )
    {
        double sum = 0;
        int num=0;
        for (int i = 0; i < m.length; i++)
            if(!C.nonproduced[i])
                {
                    sum += m[i];
                    num++;
                }
        return sum / num;
    }
    static double sdev(double[] m, TechnologyComplex C)
    {
        double sum = 0;
        double av= mean(m,C);
        int num=0;
        for (int i = 0; i < m.length; i++) if(!C.nonproduced[i])
                {
                    sum += (m[i]-av)*(m[i]-av) ;
                    num++;
                }
        return Math.sqrt(sum / num);
    }
    static double mean(double[] m  )
    {
        double sum = 0;
        int num=0;
        for (int i = 0; i < m.length; i++)
            {
                sum += m[i];
                num++;
            }
        return sum / num;
    }
    static double sdev(double[] m,double av)
    {
        double sum = 0;
        int num=0;
        for (int i = 0; i < m.length; i++)
            {
                sum += (m[i]-av)*(m[i]-av) ;
                num++;
            }
        return Math.sqrt(sum / num);
    }
    /** define non final targets to be current usages of these products */
    static void defineNonFinalTargets(double[]intense,TechnologyComplex C,  double [] initialresource,double[]targets)
    {
        double [] used = computeUsage(C,intense,initialresource);
        for (int i=0; i<C.nonfinal.length; i++)
            if(C.nonfinal[i])
                {
                    targets[i]=used[i]*slackness;
                }
    }
    /** shrink or expand all industries in order to not exceed target level of use of the critical fixed reource
     * frozen techniques are returned to original level of intensity */
    static void rescaleIntensity(double[]intense,TechnologyComplex C, double [] initialresource,double[]targets)
    {
        double [] netoutput=computeNetAvailable(C,intense,initialresource);
        double [] used = computeUsage(C,intense,initialresource);
        double[] gross=computeGrossAvail(C,intense,initialresource);
        double maxfrac=0,totalfrac=0;
        boolean allpositive = true;
        int count=0;
        HphaseD=computeHarmony(  netoutput,  targets,  C, intense,gross );
        if(phase1rescale)
            {
                for (int i=0; i<netoutput.length; i++) if(C.nonproduced[i])
                        {
                            double resource = initialresource[i] ;
                            double usage =used[i]  ;
                            double fractionaluse = usage /resource;
                            /*  if(verbose  )
                                  System.out.println("non produced ,"+C.productIds[i]+",fracuse, "+fractionaluse+",resource ,"
                                                     +resource+",netoutput,"+netoutput[i]+ ",usage ,"
                                                     +usage);*/
                            if (fractionaluse > maxfrac) maxfrac=fractionaluse;
                            totalfrac+= fractionaluse;
                            count++;
                        }
                double avfrac = totalfrac/count;
                double expansionratio = capacitytarget/maxfrac;
                if(phase1average)expansionratio = capacitytarget/avfrac;
                // expand overall scale of production to balance
                //  if(expansionratio>1)
                {
                    for (int i=0; i<intense.length; i++)
                        if(C.techniques.elementAt(i).frozen)
                            intense[i]=capacitytarget ;
                        else
                            intense[i]*=(expansionratio);
                    if (verbose)System.out.println("scale all output by,"+expansionratio);
                }
            }
        netoutput=computeNetAvailable(C,intense,initialresource);
        if(verbose)
            {
                writeln("after phase 1 rescale intensity");
                printstate(intense,C,initialresource,targets);
            }
        double [] grossAvail =computeGrossAvail(C,intense,initialresource);
        HphaseE=computeHarmony(  netoutput,  targets,  C, intense,grossAvail);
        for(double d:netoutput)allpositive = allpositive && (d>=0);
        if (!allpositive)
            if(phase2rescale)
                {
                    Vector< Vector<Integer> >ui=C. buildUserIndex();
                    double [] shrinkby = new double[C.techniqueCount()];
                    for(int i=0; i<shrinkby.length; i++)shrinkby[i]=1;
                    for(int i=0; i<netoutput.length; i++)
                        if(netoutput[i]<0)
                            {
                                double amountused = grossAvail[i]-netoutput[i];
                                double shortfallratio = capacitytarget*(grossAvail[i] )/amountused;
                                if(verbose) System.out.println(C.productIds[i]+", shortfall ratio, "+shortfallratio+
                                                                   ",netoutput,"+netoutput[i]+
                                                                   ", amount used,"+amountused+
                                                                   ", gross available "+grossAvail[i]
                                                                  );
                                Vector<Integer>users = ui.elementAt(i);
                                double weight=0;
                                // go through all techniques which use product i
                                for(Integer I:users)   // big I is a technique number
                                    {
                                        Technique t= C.techniques.elementAt(I.intValue());
                                        // check that they do not actually make product i as output
                                        if(t.productCode!=i)
                                            {
                                                // reduce its intensity by the shortfall ratio
                                                if (shortfallratio<shrinkby[I])
                                                    shrinkby[I]= shortfallratio;
                                            }
                                    }
                            }
                    for(int i=0; i<shrinkby.length; i++)
                        {
                            if(C.techniques.elementAt(i).frozen)
                                intense[i]=capacitytarget ;
                            else
                                {
                                    intense[i]*=shrinkby[i];
                                    if(verbose)if(shrinkby[i]<1)System.out.println("Shrink "+C.techniques.elementAt(i).getIdentifier()+"by "+shrinkby[i]);
                                }
                        }
                }
        // now make sure no other resource has a negative output
        if(verbose)
            {
                writeln("after phase 2 rescale intensity");
                printstate(intense,C,initialresource,targets);
            }
    }
    static void initialiseIntensities(double[]intensity,TechnologyComplex C, double [] initialresource,double[]targets )
    {
        for (int i=0; i<intensity.length; i++)
            intensity[i]=0.99 ;
      //  rescaleIntensity(intensity,C,initialresource,targets);
    }
    static void equaliseHarmony(double [] intensity,
                                double [] derivativeOfProductHarmony,
                                double []netproduct,
                                double temperature,
                                TechnologyComplex C,
                                double[] h,
                                Vector< Vector<Integer>> index,
                                double [] initialresource,
                                double [] targets,
                                double [] grossOutput
                               )throws IllegalIntensity
    {
        double totalh=0;
        int worst=0,count=0;
        for(int k=0; k<h.length; k++)
            if(!C.nonproduced[k])
                if(!C.nonfinal[k]|| eqnonfin)
                    {
                        count++;
                        totalh+=h[k];
                    }
        // find mean harmony
        double mh=totalh/count;
        double meanf=0;
        int k=0;
        double maxfrac=0;
        for(k=0; k<h.length; k++)
            if(!C.nonproduced[k])
                if(!C.nonfinal[k]|| eqnonfin)
                    {
                        // work out how much to change its output to get it on the mean
                        // we do this using the inverse harmony function
                        double gradient = Harmony.dH(targets[k],netproduct[k],grossOutput[k]);
                        double changeinharmony = mh-h[k];
                        double changeinoutput = changeinharmony/gradient;
                        double fractionalchange =  sigmoid(changeinoutput/(grossOutput[k]-initialresource[k]));
                        if (fractionalchange <0) fractionalchange *= 0.3;
                        else fractionalchange = Math.sqrt(fractionalchange)*0.1; // bias towards expansion since sqrt of fraction >fraction
                        //   if(k>h.length-50)System.out.println("k,"+k+","+C.productIds[k]+",mh,"+mh+",h[k],"+h[k]+",frac,"+fractionalchange);
                        count++;
                        meanf+=fractionalchange;
                        Vector<Integer> productionSet =index.elementAt(k);
                        if(Math.abs(maxfrac)<Math.abs(fractionalchange))
                            {
                                maxfrac = fractionalchange;
                                worst=k;
                            }
                        for(Integer I:productionSet)
                            {
                                intensity[I]*= (1+fractionalchange);
                                if(intensity[I]<0)throw new IllegalIntensity(" intensity "+I+" went negative, fractional change = "+fractionalchange);
                            }
                    }
        meanf=meanf/count;
        for(k=0; k<h.length; k++)
            if(!C.nonproduced[k])
                if(!C.nonfinal[k]|| eqnonfin)
                    {
                        // normalise the shift in intensity to compensate for the mean fractional change
                        // we do this by making a shift in the opposite sense to the mean shift already done
                        Vector<Integer> productionSet =index.elementAt(k);
                        for(Integer I:productionSet)
                            {
                                intensity[I]*= (1-meanf);
                            }
                    }
        if (verbose)System.out.println("Max fractional change in equalise ,"+maxfrac+", "+C.productIds[worst]+
                                           ",meanfractional "+meanf+
                                           ",harmony,"+h[worst]+
                                           ",meanh,"+mh+"\nderivative,"+derivativeOfProductHarmony[worst]+
                                           ",change,"+temperature* (h[worst]-mh)/derivativeOfProductHarmony[worst]
                                          );
    }
    static double sigmoid(double d)
    {
        if (d>0) return d/(1+d);
        if (d==0) return 0;
        d= -d;
        return -(d/(1+d) );
    }
    static void adjustIntensities(double [] intensity,
                                  double [] derivativeOfProductHarmony,
                                  double temperature,
                                  TechnologyComplex C,
                                  double[] h,
                                  Vector< Vector<Integer>> index,
                                  double [] initialresource,
                                  double[] planTargets)throws IllegalIntensity
    {
        double []netOutput;
        netOutput=computeNetAvailable(C,intensity,initialresource);
        if(verbose)
            {
                writeln("preequalisation");
                printstate(  intensity,  C,  initialresource,   planTargets);
            }
        equaliseHarmony(  intensity,
                          derivativeOfProductHarmony,
                          netOutput,
                          temperature,
                          C,
                          h,
                          index,
                          initialresource,
                          planTargets,
                          computeGrossAvail(C,intensity,initialresource) );
        double [] gross = computeGrossAvail(C,intensity,initialresource);
        netOutput=computeNetAvailable(C,intensity,initialresource);
        HphaseB=computeHarmony(  netOutput,  planTargets,  C, intensity,gross );
        derivativeOfProductHarmony=computeHarmonyDerivatives(  netOutput,  planTargets,  C, intensity,gross );
        if(verbose)
            {
                writeln("post equalise prereallocation");
                printstate(  intensity,  C,  initialresource,   planTargets);
            }
        double[] expansionrate=new double[C.techniques.size()];
        for(int i=0; i<C.techniques.size(); i++)
            {
                Technique t= C.techniques.elementAt(i);
                expansionrate[i] = t.rateOfHarmonyGain(derivativeOfProductHarmony);
            //    if(verbose)System.out.println(t.identifier+"\t,gainrate,"+expansionrate[i]);
            }
        double meane = mean(expansionrate);
        for(int i=0; i<C.techniques.size(); i++)
            {
                double adjustedexp=sigmoid( expansionrate[i] )*temperature*phase2adjust  ;
                // absolute limit to shrink rate
                // shrink or expand in proportion to gains
                intensity[i]*=(1+ adjustedexp);
                if(intensity[i]<0)throw new IllegalIntensity(" intensity "+i+" went negative, adjustedexp=" +adjustedexp);
            }
        netOutput=computeNetAvailable(C,intensity,initialresource);
        gross = computeGrossAvail(C,intensity,initialresource);
        derivativeOfProductHarmony=computeHarmonyDerivatives(  netOutput,  planTargets,  C, intensity,gross );
        HphaseC=computeHarmony(  netOutput,  planTargets,  C, intensity,gross );
        if(verbose)
            {
                writeln("postreallocation");
                printstate(  intensity,  C,  initialresource,   planTargets);
            }
        rescaleIntensity(intensity,C,initialresource,planTargets);
    }
    static double[] computeNetAvailable(TechnologyComplex C, double [] intensity,double[]initial)
    {
        double [] output =  computeGrossAvail(  C,  intensity, initial);
        double [] usage = computeUsage( C,  intensity, initial);
        for (int i=0; i<output.length; i++)output[i]-=usage[i];
        return output;
    }
    /** works out total current productive consumption of each product */
    static double[] computeUsage(TechnologyComplex C, double [] intensity,double[]initial)
    {
        double [] usage = new double[initial.length];
        for(int j=0; j<C.techniqueCount(); j++)
            {
                Technique t= C.techniques.elementAt(j);
                for(int k=0; k<t.inputCodes.length; k++)
                    {
                        usage[t.inputCodes[k]]+= intensity[j]*t.inputUsage[k];
                    }
            }
        return usage;
    }
    /** gives the vector of total amount produced or available in initial resource vector - does not deduct productive consumption */
    static double[] computeGrossAvail(TechnologyComplex C, double [] intensity,double[]initial)
    {
        double [] output = new double[C.productCount()];
        for(int i =0; i<output.length; i++)output[i]=initial[i];
        for(int j=0; j<C.techniqueCount(); j++)
            {
                Technique t= C.techniques.elementAt(j);
                output [t.productCode]+= t.grossOutput*intensity[j];
                //if (verbose )
                //    writeln(""+t.productCode+" " +t.grossOutput+" "+intensity[j]);
                if (t instanceof JointProductionTechnique)
                    {
                        JointProductionTechnique J=(JointProductionTechnique)t;
                        int[] codes= J.getCoproductionCodes();
                        double[]Q=J.getCoproductionQuantities();
                        for (int k2=0; k2<codes.length; k2++)
                            {
                                output[codes[k2]]+= intensity[j]*Q[k2];
                            }
                    }
            }
        return output;
    }
    /** we test it using Kantorovich's excavator example */
    public static void main(String[] args)
    {
        int dest =0;
        int src=1;
        int products=6;
        // matrix format of the problem as used in the Pascal Kantorovich solver
        double [][][]ctechniques   =
        {
            {{105,0,0,0,0,0},{0,0,0, 1,0,0}},
            {{107,0,0,0,0,0},{0,0,0, 0,1,0}},
            {{64,0,0,0,0,0},{0,0,0, 0,0,1}},
            {{0,56,0,0,0,0},{0,0,0, 1,0,0}},
            {{0,66,0,0,0,0},{0,0,0, 0,1,0}},
            {{0,38,0,0,0,0},{0,0,0,  0,0,1}},
            {{0,0,56,0,0,0},{0,0,0, 1,0,0}},
            {{0,0,83, 0,0,0},{0,0,0,0,1,0}},
            {{0,0,53, 0,0,0},{0,0,0, 0,0,1}}
        };
        TechnologyComplex C= new TechnologyComplex(products);
        // name the goods
        String[] labels= {"A","B","C","M1","M2","M3"};
        for (int l=0; l<6; l++)C.setProductName(l,labels[l]);
        for (int i=0; i<ctechniques.length; i++)
            {
                int productCode=firstnonzero(ctechniques[i][dest]);
                int srcCode = firstnonzero(ctechniques[i][src]);
                double[] usage= {ctechniques[i][src][srcCode]};
                int[]codes= {srcCode};
                Technique t;
                C.addTechnique( t=new Technique("T"+i,productCode,ctechniques[i][dest][productCode],usage,codes));
            }
        // now add fixed techniques to supply initial resources
        double [] initialresource = {0,0,0,1,1,1};
        for (int j =3; j<6; j++)
            {
                C.nonfinal[j]=true;
                C.nonproduced[j]=true;
            }
        //    for (Technique t0:C.techniques)writeln(""+t0);
        // now set the plan target
        double[]ctarget  = {64,64,64,0.05,0.05,0.05};
        double[] kantorovichsanswer =
        {
            0.671365,      0,      0,
            0.328635,0.789238,      0,
            0,    0.210762,          1
        };
        try
            {
                double[] intensity=balancePlan(  C, ctarget,initialresource);
                double [] netoutput=computeNetAvailable(C,intensity,initialresource);
                writeln("iters "+iters);
                writeln("phase 2 adjust "+phase2adjust +" starting temp "+startingtemp +"capacity target" +capacitytarget+" use weight "+useweight);
                writeln("net outputs");
                writeln( netoutput);
                writeln("our intensities, followed by Kantorovich's ones ");
                writeln(intensity);
                writeln(kantorovichsanswer);
            }
        catch(Exception e)
            {
                System.err.println("fail "+e);
                e.printStackTrace();
            }
    }
    static int firstnonzero(double[]d)
    {
        for(int i=0; i<d.length; i++)
            if(d[i]!=0.0)return i;
        return d.length;
    }
}
class InconsistentScale extends Exception
{
    InconsistentScale(String s)
    {
        super(s);
    }
}
class IllegalIntensity extends Exception
{
    IllegalIntensity(String s)
    {
        super(s);
    }
}
