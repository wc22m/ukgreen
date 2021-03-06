package planning;
import Epl.node.*;
import Epl.lexer.*;
import Epl.parser.*;
import Epl.analysis.*;
import java.io.*;
import java.util.*;
class Eplvars extends Epl.analysis.DepthFirstAdapter
{
    boolean semanticerror=false;
    Hashtable<String,Eplvar> idtab;
    boolean targeted=false;
    boolean inresource=false;
    boolean inproduct=false;
    boolean intechnique=false;
    boolean frozen=false;
    private Vector<Technique>techniques=new Vector<Technique>();
    private Vector<AAtom>  targets,inputs,outputs;// used to build up list of atoms for different constructs
    HashSet<String> nonfinals = new HashSet<String>();
    HashSet<String> nonproduced = new HashSet<String>();
    private Vector<AAtom>   resources=new Vector<AAtom>();
    public void inAAtom(AAtom node)
    {
        defaultIn(node);
        String t=node.getIdentifier().toString().trim();
        if(!idtab.containsKey(t))
            {
                idtab.put(t, new Eplvar(t));
                //    System.out.println("new "+t);
            }
        if(targeted)
            {
                targets.add(node);
                idtab.get(t).intargets=true;
            }
        if(inproduct  )
            {
                outputs.add(node);
                idtab.get(t).assignmentcount+=1;
                //  System.out.println(t+"\tinproduct "+idtab.get(t).assignmentcount);
            }
        if(!inproduct&& intechnique)inputs.add(node);
        if(  inresource )
            {
                resources.add(node);
                idtab.get(t).assignmentcount+=1;
                //     System.out.println(t+"\tinresource "+idtab.get(t).assignmentcount+" num resources "+resources.size());
            }
    }
    Eplvars(Hashtable<String,Eplvar> vars)
    {
        idtab=vars;
    }
    public void inATechstatStatement(ATechstatStatement node)
    {
        defaultIn(node);
        intechnique=true;
        inputs=new Vector<AAtom>();
        outputs=new Vector<AAtom>();
    }
    public void inAFtechstatStatement(AFtechstatStatement node)
    {
        defaultIn(node);
        frozen=true;
        intechnique=true;
        inputs=new Vector<AAtom>();
        outputs=new Vector<AAtom>();
    }
    /** convert a vector of atoms into an array of indexes into the state vector of the
     * variables of the atoms */
    public  int[] codes(Vector<AAtom > inputs,int start)
    {
        int incount = inputs.size();
        int []indices = new int [incount-start];
        for( int i=0; i<indices.length; i++)
            {
                AAtom  a=inputs.elementAt(i+start);
                int pos =indexof(a);
                indices[i]=pos;
            }
        return indices;
    } public int[] codes(Vector<AAtom > inputs)
    {
        return codes(inputs,0);
    }
    /** convert a vector of atoms into an array of doubles being the floating constants
    *  of the atoms */
    public  double[] consts(Vector<AAtom > inputs, int start)
    {
        int incount = inputs.size();
        double[] quantities = new double[incount-start];
        for( int i=0; i<quantities.length; i++)
            {
                AAtom a=inputs.elementAt(i+start);
                int pos =indexof(a);
                quantities[i]=valof(a);
            }
        return quantities;
    } public  double[] consts(Vector<AAtom > inputs)
    {
        return consts(inputs,0);
    }
    /** return the indices into the state vector of the predeclared resources */
    public int[]getResCodes()
    {
        return codes(resources);
    }
    /** return the  magnitudes of the predeclared resources */
    public double[]getResConsts()
    {
        return consts(resources);
    }
    public Vector<Technique>getTechniques()
    {
        return techniques;
    }

    /** return the indices into the state vector of the targets */
    public int[]getTargCodes()
    {
        return codes(targets);
    }
    /** return the  magnitudes of the targets */
    public double[]getTargConsts()
    {
        return consts(targets);
    }
    /** record name of any non produced resources */
    public void outANonp(ANonp a)
    {
        nonproduced.add(a.getIdentifier().toString().trim());
    }
    /** record name of each non final product */
    public void outANonf(ANonf a)
    {
        nonfinals.add(a.getIdentifier().toString().trim());
    }
    public void outATechnique(ATechnique node)
    {
        defaultOut(node);
        intechnique=false;
        double[] quantities = consts(inputs);
        int []indices = codes(inputs);
        int productCode=indexof(outputs.elementAt(0));
        double flow = valof(outputs.elementAt(0));
        String name =node.getIdentifier().toString().trim();
        if(outputs.size()==1)
            {
                // non joint production
                Technique t= new Technique(name,productCode,flow,quantities,indices);
                t.frozen=frozen;
                techniques.add(t);
            }
        else     // joint production
            {
                Technique t=
                    new JointProductionTechnique (name,productCode,flow,quantities,indices,consts(outputs,1),codes(outputs,1));
                t.frozen=frozen;
                techniques.add(t);
            }
        frozen=false;
    }
    int indexof(AAtom a)
    {
        return indexof(a.getIdentifier().toString().trim());
    }
    double valof(AAtom a)
    {
        return (new Double(a.getFloatingConstant().toString().trim())).doubleValue();
    }
    int indexof(String var)
    {
        Eplvar v = idtab.get(var);
        //   System.out.println("index of "+var + " "+v.posInStateVec);
        return v.posInStateVec;
    }

    public void inAResstatStatement(AResstatStatement node)
    {
        defaultIn(node);
        inresource=true;
    }

    public void outAResstatStatement(AResstatStatement node)
    {
        defaultOut(node);
        inresource=false;
    }

    public void inAProduct(AProduct node)
    {
        defaultIn(node);
        inproduct=true;
    }

    public void outAProduct(AProduct node)
    {
        defaultOut(node);
        inproduct=false;
    }

    public void inATargstatStatement(ATargstatStatement node)
    {
        defaultIn(node);
        targets=new Vector<AAtom>();
        targeted=true;
    }

    public void outATargstatStatement(ATargstatStatement node)
    {
        defaultOut(node);
        targeted=false;
        HashSet<String>allt=new HashSet<String>();
        for(AAtom a:targets)
            {
                String s=a.getIdentifier().toString().trim();
                if(allt.contains(s))
                    {
                        System.err.println(s+" in targets more than once ");
                        semanticerror=true;
                    }
                allt.add(s);
                if(nonfinals.contains(s))
                    {
                        System.err.println(s+" in targets but declared non final ");
                        semanticerror=true;
                    }
            }
    }

}
class Eplvar
{
    static int statevecpos=0;
    String name;
    int posInStateVec;
    int assignmentcount=0;
    boolean intargets=false;

    public Eplvar(String n)
    {
        name=n;
        posInStateVec=statevecpos;
        statevecpos++;
        //  System.out.println("initialise "+n+" "+posInStateVec);
    }
}
/** this is the main class of the epl compiler */
public class Eplc
{
    static double defaulttarget= 1 - Harmonizer.capacitytarget;// we do not want to have any actual 0 targets as this will resultin overflow conditions
    public static void main(String[] arguments)
    {   Harmonizer.iters=10;
        Parser parser;
        try
            {
                // set up flags
                if( arguments.length>0)
                    {
                        for(String s:arguments)
                            {
                                if (s.equals("-V")) Harmonizer.verbose=true;
                                if (s.startsWith("-I"))Harmonizer.iters=getcount(s);
                                // we expect the other numeric quantities to be in percentages
                                if (s.startsWith("-phaseTwoadjust"))Harmonizer.phase2adjust=getcount(s)*0.01;
                                if (s.startsWith("-capacityTarget"))Harmonizer.capacitytarget=getcount(s)*0.01;
                                if (s.startsWith("-startingtemp"))Harmonizer.startingtemp=getcount(s)*0.01;
                                if (s.startsWith("-nophase1"))Harmonizer.phase1rescale=false;
                                if (s.startsWith("-nophase2"))Harmonizer.phase2rescale=false;
                                if (s.startsWith("-usemeanmethod"))Harmonizer.usemeanmethod=true;
                                if (s.startsWith("-h"))
                                    {
                                        System.out.println("usage:\n"+
                                                           "java planning.Eplc sourcefile productionresultsfile [options]\n "+
                                                           "options\n"+
                                                           "-V \t\t\tverbose\n"+
                                                           "-Ixxx \t\t\txxx number of iterations to run the optimiser for \n"+
                                                           "\t\t\t default "+Harmonizer.iters+"\n"+
                                                           "-phaseTwoadjustxx \twhere xx are two digits specifying the percentage\n\t\t\tadjustment done in phase 2\n"+
                                                           "\t\t\t default "+(int)(Harmonizer.phase2adjust*100)+"\n"+
                                                           "-capacityTargetxx \twhere xx are two digits specifying the percentage\n\t\t\tutilisation of resource aimed at\n"+
                                                           "\t\t\t default "+(int)(Harmonizer.capacitytarget*100)+"\n"+
                                                           "-startingtempxx \twhere xx are two digits specifying the percentage\n\t\t\tmove in intensity each iteration\n"+
                                                           "\t\t\t default "+(int)(Harmonizer.startingtemp *100)+"\n"+
                                                           "-nophaseX \twhere X =1 or 2 switch off phases of rescaling \n"+
                                                           "-usemeanmethod \tbase expansion or contraction on mean input harmony not derivatives\n"+
                                                           "-h \t\t\tprint this text"
                                                          );
                                        return;
                                    }
                            }
                    }
                if(arguments.length < 2)
                    {
                        System.out.println("usage:");
                        System.out.println("java planning.Eplc sourcefile productionresultsfile [options] ");
                        System.exit(1);
                    }
                // set up the file input
                FileReader r=    new FileReader(arguments[0]);
                PushbackReader pr =  new PushbackReader(new BufferedReader(r), 1024)  ;
                Lexer lexer = new Lexer(pr);
                parser = new Parser(lexer);
                // parse the input to get an abstract syntax tree
                Node ast = parser.parse();
                Hashtable<String,Eplvar> vars = new Hashtable<String,Eplvar>();
                try
                    {
                        Eplvars ev=new Eplvars(vars);
                        // walk over the abstract syntax tree to extract information into the Eplvars object
                        ast.apply(ev);
                        if (ev.semanticerror)throw new Exception("Semantic errors detected - not possible to run");
                        TechnologyComplex tc=new TechnologyComplex(Eplvar.statevecpos);
                        // define all the techniques in the technology complex by getting them from the Eplvars object
                        for( Technique t : ev.getTechniques())
                            {
                                tc.addTechnique(t);
                            }
                        // the hastable vars now contains details of all variables used
                        // define the product names and state vector positions
                        for (String s:vars.keySet())
                            {
                                Eplvar epv = vars.get(s);
                                tc.setProductName(epv.posInStateVec,s);
                                if (epv.assignmentcount==0)
                                    throw new Exception("Economic variable "+s+" has no resource assigned to it and no technique producing it");
                                /*
                                 * if(Harmonizer.verbose)
                                             System.out.println(s+"\t"
                                                                +vars.get(s).posInStateVec+"\t"
                                                                +vars.get(s).intargets+"\t"
                                                                +vars.get(s).assignmentcount);*/
                            }
                        if(Harmonizer.verbose)
                            for( Technique t : ev.getTechniques())
                                {
                                    //   System.out.println(" "+t.toString(tc));
                                }
                        // set up the state vector initialised by resources
                        double[] initialresource = new double[tc.productCount()];
                        double[]res = ev.getResConsts();
                        //  Harmonizer.writeln(res);
                        int[] ind = ev.getResCodes();
                        for (int i=0; i<ind.length; i++)
                            {
                                initialresource[ind[i]]=res[i];
                            }
                        // set up the map of non finals
                        for(int i=0; i<tc.nonfinal.length; i++)
                            tc.nonfinal[i]=false;
                        for(String s:ev.nonfinals)
                            tc.nonfinal[vars.get(s).posInStateVec]=true;
                        // set up the map of non produced
                        for(int i=0; i<tc.nonproduced.length; i++)
                            tc.nonproduced[i]=false;
                        for(String s:ev.nonproduced)
                            tc.nonproduced[vars.get(s).posInStateVec]=true;
                        // set up the target vector
                        double []targetvector = new double[tc.productCount()];
                        for(int i=0; i<targetvector.length; i++)targetvector[i]=defaulttarget*(initialresource[i]+1);
                        double []tg=ev.getTargConsts();
                        int [] ti =ev.getTargCodes();
                        for(int i=0; i<ti.length; i++)
                            {
                                targetvector[ti[i]]=tg[i];
                           //     if(Harmonizer.verbose)System.out.println("ti,"+ti[i]+",tg,"+tg[i]);
                            }
                        // run the harmonizer
                        long start = System.currentTimeMillis();
                        if(Harmonizer.verbose)
                            System.out.println("products "+tc.productCount()+ " techniques "+tc.techniqueCount() );
                        double [] intensities= Harmonizer.balancePlan(   tc, targetvector,  initialresource   );
                        long stop= System.currentTimeMillis();
                        // print the results
                        //    if(Harmonizer.verbose)Harmonizer.printstate(intensities,tc, initialresource, targetvector);
                        Harmonizer. printResults(arguments[1],tc,intensities,initialresource,targetvector);
                    }
                catch(Exception c)
                    {
                        System.err.println("Error "+c);
                        c.printStackTrace();
                        System.exit(-1);
                    }
            }
        catch(Exception e)
            {
                System.out.println(e);
                System.out.println("exit");
                System.exit(-1);
            }
    }
    static void  writeln(String s)
    {
        System.out.println(s);
    } static void  write (String s)
    {
        System.out.print (s);
    }
    static String stockname(int row,int col,int year)
    {
        return "C_"+row+"_"+col+"yr"+year;
    }
    static void  writeln(boolean []d)
    {
        for(int i=0; i<d.length; i++)System.out.print(" ,"+d[i]);
        writeln("");
    }
    static void  writeln(double []d)
    {
        for(int i=0; i<d.length; i++)System.out.printf(",%5.4f",d[i]);
        writeln("");
    }
    static int getcount(String s)throws Exception
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
        if (tail.length()==0) throw new Exception ("no number suffix in  "+s);
        return new Integer(tail).intValue();
    }
    static void  writeln(String []s)
    {
        for(int i=0; i<s.length; i++)System.out.printf(",\""+s[i]+"\"");
        writeln("");
    } static void  write(double []d)
    {
        for(int i=0; i<d.length; i++)System.out.printf(",%5.4f",d[i]);
    }
    static void  write(String []s)
    {
        for(int i=0; i<s.length; i++)System.out.printf(","+s[i]);
    }

}
