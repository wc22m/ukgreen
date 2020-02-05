package planning;
import Epl.node.*;
import Epl.lexer.*;
import Epl.parser.*;
import Epl.analysis.*;
import java.io.*;
import java.util.*;
class Eplvars extends Epl.analysis.DepthFirstAdapter {
    Hashtable<String,Eplvar> idtab;
    boolean targeted=false;
    boolean inresource=false;
    boolean inproduct=false;
    boolean intechnique=false;
    private Vector<Technique>techniques=new Vector<Technique>();
    private Vector<AAtom>  targets,inputs,outputs;// used to build up list of atoms for different constructs

    private Vector<AAtom>   resources=new Vector<AAtom>();
    public void inAAtom(AAtom node)
    {
        defaultIn(node);
        String t=node.getIdentifier().toString().trim();
        if(!idtab.containsKey(t)) {
            idtab.put(t, new Eplvar(t));
            //    System.out.println("new "+t);
        }
        if(targeted) {
            targets.add(node);
            idtab.get(t).intargets=true;
        }
        if(inproduct  ) {
            outputs.add(node);
            idtab.get(t).assignmentcount+=1;
            //  System.out.println(t+"\tinproduct "+idtab.get(t).assignmentcount);
        }
        if(!inproduct&& intechnique)inputs.add(node);
        if(  inresource ) {
            resources.add(node);
            idtab.get(t).assignmentcount+=1;
            //     System.out.println(t+"\tinresource "+idtab.get(t).assignmentcount+" num resources "+resources.size());
        }
    }
    Eplvars(Hashtable<String,Eplvar> vars) {
        idtab=vars;
    }
    public void inATechstatStatement(ATechstatStatement node)
    {
        defaultIn(node);
        intechnique=true;
        inputs=new Vector<AAtom>();
        outputs=new Vector<AAtom>();
    }
    /** convert a vector of atoms into an array of indexes into the state vector of the
     * variables of the atoms */
    public  int[] codes(Vector<AAtom > inputs) {
        int incount = inputs.size();
        int []indices = new int [incount];
        int i=0;
        for(AAtom a:inputs) {
            int pos =indexof(a);
            indices[i]=pos;
            i++;
        }
        return indices;
    }
    /** convert a vector of atoms into an array of doubles being the floating constants
    *  of the atoms */
    public  double[] consts(Vector<AAtom > inputs) {
        int incount = inputs.size();
        double[] quantities = new double[incount];
        int i=0;
        for(AAtom a:inputs) {
            int pos =indexof(a);
            quantities[i]=valof(a);
            i++;
        }
        return quantities;
    }
    /** return the indices into the state vector of the predeclared resources */
    public int[]getResCodes() {
        return codes(resources);
    }
    /** return the  magnitudes of the predeclared resources */
    public double[]getResConsts() {
        return consts(resources);
    }
    public Vector<Technique>getTechniques() {
        return techniques;
    }

    /** return the indices into the state vector of the targets */
    public int[]getTargCodes() {
        return codes(targets);
    }
    /** return the  magnitudes of the targets */
    public double[]getTargConsts() {
        return consts(targets);
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
        if(outputs.size()==1) {
            // non joint production

            Technique t= new Technique(name,productCode,flow,quantities,indices);
            techniques.add(t);
        } else { // joint production
            Technique t=
                new JointProductionTechnique (name,productCode,flow,quantities,indices,consts(outputs),codes(outputs));
            techniques.add(t);
        }
    }
    int indexof(AAtom a) {
        return indexof(a.getIdentifier().toString().trim());
    }
    double valof(AAtom a) {
        return (new Double(a.getFloatingConstant().toString().trim())).doubleValue();
    }
    int indexof(String var) {
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
    }

}
class Eplvar {
    static int statevecpos=0;
    String name;
    int posInStateVec;
    int assignmentcount=0;
    boolean intargets=false;

    public Eplvar(String n) {
        name=n;
        posInStateVec=statevecpos;
        statevecpos++;
        //  System.out.println("initialise "+n+" "+posInStateVec);
    }
}
/** this is the main class of the epl compiler */
public class Eplc {
    static double defaulttarget= 1 - Harmonizer.capacitytarget;// we do not want to have any actual 0 targets as this will resultin overflow conditions
    public static void main(String[] arguments) {
        if(arguments.length != 1) {
            System.out.println("usage:");
            System.out.println("java plannin.Eplc sourcefile   ");
            System.exit(1);
        }
        Parser parser;
        try {
            // set up the file input
            FileReader r=    new FileReader(arguments[0]);
            PushbackReader pr =  new PushbackReader(new BufferedReader(r), 1024)  ;
            Lexer lexer = new Lexer(pr);
            parser = new Parser(lexer);
            // parse the input to get an abstract syntax tree
            Node ast = parser.parse();
            Hashtable<String,Eplvar> vars = new Hashtable<String,Eplvar>();
            try {
                Eplvars ev=new Eplvars(vars);
                // walk over the abstract syntax tree to extract information into the Eplvars object
                ast.apply(ev);
                TechnologyComplex tc=new TechnologyComplex(Eplvar.statevecpos);
                // define all the techniques in the technology complex by getting them from the Eplvars object
                for( Technique t : ev.getTechniques()) {
                    tc.addTechnique(t);

                }
                // the hastable vars now contains details of all variables used
                // define the product names and state vector positions
                for (String s:vars.keySet()) {
                    Eplvar epv = vars.get(s);
                    tc.setProductName(epv.posInStateVec,s);
                    if (epv.assignmentcount==0)
                        throw new Exception("Economic variable "+s+" has no resource assigned to it and no technique producing it");
                    if(Harmonizer.verbose)
                        System.out.println(s+"\t"
                                           +vars.get(s).posInStateVec+"\t"
                                           +vars.get(s).intargets+"\t"
                                           +vars.get(s).assignmentcount);
                }
                if(Harmonizer.verbose)
                    for( Technique t : ev.getTechniques()) {

                        System.out.println(" "+t.toString(tc));
                    }
                // set up the state vector initialised by resources
                double[] initialresource = new double[tc.productCount()];
                double[]res = ev.getResConsts();
                //  Harmonizer.writeln(res);
                int[] ind = ev.getResCodes();
                for (int i=0; i<ind.length; i++) {
                    initialresource[ind[i]]=res[i];
                }
                // Harmonizer.writeln(initialresource);
                // set up the map of non finals
                //boolean[] nonfinals=new boolean[tc.productCount()];
                for(int i=0; i<tc.nonfinal.length; i++)tc.nonfinal[i]=true;
                ind = ev.getTargCodes();
                for(int i=0; i<ind.length; i++)tc.nonfinal[ind[i]]=false;
                // set up the map of non produced
                // boolean[] nonproduced=new boolean[tc.productCount()];
                for(int i=0; i<tc.nonproduced.length; i++)tc.nonproduced[i]=true;

                for( Technique t : ev.getTechniques()) {
                    //System.out.println(t.getIdentifier());
                    if (t instanceof JointProductionTechnique) {
                        JointProductionTechnique tj=(JointProductionTechnique)t;
                        for(int i:tj.getCoproductionCodes()) {
                            tc.nonproduced[i]=false;
                            //System.out.println("falsify "+i+","+tc.productIds[i]);
                        }
                    } else
                    {   tc.nonproduced[t.getProductCode()]=false;
                        // System.out.println("falsify "+t.getProductCode()+","+tc.productIds[t.getProductCode()]);
                    }
                }

                // set up the target vector
                double []targetvector = new double[tc.productCount()];
                for(int i=0; i<targetvector.length; i++)targetvector[i]=defaulttarget;
                double []tg=ev.getTargConsts();
                int [] ti =ev.getTargCodes();
                for(int i=0; i<ti.length; i++)targetvector[ti[i]]=tg[i];


                // run the harmonizer
                long start = System.currentTimeMillis();
             //   Harmonizer.verbose=true;
             //   Harmonizer.iters=50;

                if(Harmonizer.verbose)
                    System.out.println("products "+tc.productCount()+ " techniques "+tc.techniqueCount()+"\n"+tc.allheadings());


                double [] intensities= Harmonizer.balancePlan(   tc, targetvector,  initialresource   );
                long stop= System.currentTimeMillis();
                // print the results

                if(Harmonizer.verbose)Harmonizer.printstate(intensities,tc, initialresource, targetvector);
                Harmonizer. printResults(tc,intensities,initialresource,targetvector);
            }
            catch(Exception c) {
                System.err.println("Error "+c);
                //     c.printStackTrace();
                System.exit(-1);
            }

        }
        catch(Exception e) {
            System.out.println(e);
            System.out.println("exit");
            System.exit(-1);
        }

    }
    static void  writeln(String s) {
        System.out.println(s);
    } static void  write (String s) {
        System.out.print (s);
    }
    static String stockname(int row,int col,int year) {
        return "C_"+row+"_"+col+"yr"+year;
    }
    static void  writeln(boolean []d) {
        for(int i=0; i<d.length; i++)System.out.print(" ,"+d[i]);
        writeln("");
    }
    static void  writeln(double []d) {
        for(int i=0; i<d.length; i++)System.out.printf(",%5.4f",d[i]);
        writeln("");
    }
    static void  writeln(String []s) {
        for(int i=0; i<s.length; i++)System.out.printf(",\""+s[i]+"\"");
        writeln("");
    } static void  write(double []d) {
        for(int i=0; i<d.length; i++)System.out.printf(",%5.4f",d[i]);

    }
    static void  write(String []s) {
        for(int i=0; i<s.length; i++)System.out.printf(","+s[i]);

    }

}
