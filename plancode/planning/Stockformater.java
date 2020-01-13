/* Programme to read in the capital stock file from the
National office of statistics ( first having been converted to
csv format) and form a matrix whose rows are labeled by the
types of capital stock, columns by the industry in which it
is located, and cells of which containg Net Capital Stocks .*/
package planning;
import java.io.*;
import java.util.*;

class Stockformater {
    static Hashtable<String,Hashtable> Asset2Industry = new Hashtable<String,Hashtable>();
    static Vector<String> Industries = new Vector<String>();
    static Vector<String> Assets = new Vector<String>();
    static void store(String Asset,String Industry, double value) {

        Hashtable t = Asset2Industry.get(Asset);
        if(t==null) { // create an entry for the asset in the vector and the hashtable
            Assets.add(Asset);
            t= new Hashtable();
            Asset2Industry.put(Asset,t);
        }
        if (!Industries.contains(Industry))Industries.add(Industry);
        t.put(Industry, new Double(value));
    }
    static double get(String Asset, String Industry) {
        return ((Double)(Asset2Industry.get(Asset).get(Industry))).doubleValue();
    }
    public static void main(String [] args) {
        if (args.length<1) {
            System.out.println(" Usage java Stockformater inputfile.cvs");
        } else {
            csvfilereader r= new csvfilereader(args[0]);
            pcsv p=r.parsecsvfile();
            if(p==null) {
                System.out.println(" null returned from parse ");
            } else {

                pcsv thisline,nextline;
                boolean netcap = false;
                int i;
                String Asset,	Measure,	Industry;
                Asset="";
                Measure="";
                Industry="";
                double value=0;
                for(nextline =p; nextline!=null;) {
                    value=0;
                    Asset="";
                    Measure="";
                    Industry="";
                    for(thisline = nextline,i=0; thisline!=null; thisline=thisline.right) {
                        if(thisline.tag instanceof linestart) {

                            nextline = ((linestart)(thisline.tag)).down;
                        }
                        else if(thisline.tag instanceof alpha) {
                            i++;

                            if(i==1) Asset = ((alpha)(thisline.tag)).textual;
                            else if(i==2) Measure =( (alpha)(thisline.tag)).textual;
                            else if(i==3) Industry =( (alpha)(thisline.tag)).textual;
                            else if(((alpha)(thisline.tag)).textual=="z") { // the spreadsheet uses this as a zero marker in col 4
                                value =0;
                            }
                        }
                        else if (thisline.tag instanceof numeric) {
                            i++;
                            value = ((numeric)(thisline.tag)).number;
                        }
                        // we have processed one line

                        if(Measure.startsWith("Net capital stocks")) store(Asset,Industry,value);
                    }
                }
                // at this point we should have the entire database in hash tables
                // we print on the standard out a csv matrix
                System.out.print("\"Industry\",");
                for(String s:Industries)System.out.print("\""+s+"\",");
                System.out.println("");
                for(String s:Assets) {
                    System.out.print("\""+s+"\"");
                    for(String I:Industries)
                        System.out.print(","+get(s,I));
                    System.out.println("");
                }
            }
        }
    }

}
