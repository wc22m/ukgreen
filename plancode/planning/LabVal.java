/** computes labour value of outputs of industries plus prices of production */
package planning;
import java.io.*;
import java.util.*;
class LabVal
{
    public static void main(String [] args)
    {
        if (args.length<2)
            {
                System.out.println(" Usage java planning.Labvals stockmatrix.cvs   iotable.cvs");
            }
        else
            {
                csvfilereader[] r= new csvfilereader[2];
                pcsv[] csvs=new pcsv[2];
                boolean fail=false;
                for (int i=0; i<2; i++)
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
                String[] stockrownames= r[0].getrowheaders(csvs[0]);
                Vector<String> stockrows=  new Vector<String>();
                for(String v:stockrownames)stockrows.add(v);
                double[][]stockdata =r[0]. getdatamatrix(csvs[0]);
                String[] ioheaders=r[1].getcolheaders(csvs[1]);
                Vector<String> ioheadersv = new Vector<String>();
                for(String v:ioheaders)ioheadersv.add(v);
                double[][] iomatrix= r[1]. getdatamatrix(csvs[1]) ;
                String[]iorownames =r[1].getrowheaders(csvs[1]);
                Vector<String> ionv = new Vector<String>();
                for(String v:iorownames)ionv.add(v);
                int totaldemand=ioheadersv.indexOf("Total Demand for Products at Basic Prices".toUpperCase());
                int exports=ioheadersv.indexOf("Total Exports".toUpperCase());
                int lastcostrow= ionv.indexOf("Activities Of Membership Organisations NPISH".toUpperCase());
                int labrow= ionv.indexOf("Compensation of employees".toUpperCase());
                int imports = labrow-3;
                int finaloutput=ionv.indexOf("Total Output".toUpperCase());
                int totcaprow=stockrows.indexOf("TOTAL");
                int surplusrow=ionv.indexOf("Gross operating surplus".toUpperCase());
                double []pricetoval=new double[exports+1];
                double []pricetopriceofproduction = new double[exports+1];
                double []finaloutputasval=new double[exports+1];
                double []finaloutputaspp = new double[exports+1];
                double epsilon=0.1;
                double totalsurplus = iomatrix[surplusrow][totaldemand];
                double totalcapital = stockdata[totcaprow][totaldemand];
                double profitrate = totalsurplus/totalcapital;
                for(int iter=1 ; iter<12; iter++)
                    {
                        double totprice=0;
                        double totval=0;
                        double totpp=0;
                        for(int col=1; col<=exports; col++)
                            {
                                double coltotal=iomatrix[labrow][col]+iomatrix[imports][col]*pricetoval[exports];
                                double colppt=iomatrix[labrow][col]+iomatrix[imports][col]*pricetopriceofproduction[exports];
                                for(int row=1; row<lastcostrow; row++)
                                    {
                                        coltotal+=(pricetoval[row]*iomatrix[row][col]);
                                        colppt+=(pricetoval[row]*(iomatrix[row][col]+profitrate*stockdata[row][col]));
                                    }
                                pricetoval[col]=coltotal/(iomatrix[finaloutput][col]+epsilon);
                                pricetopriceofproduction[col]=colppt/(iomatrix[finaloutput][col]+epsilon);
                                finaloutputasval[col]=coltotal;
                                finaloutputaspp[col]=colppt;
                                totval+=coltotal;
                                totpp+=colppt;
                                totprice+=iomatrix[finaloutput][col];
                            }
                        if(iter<11)for(int col=1; col<=exports; col++)
                                {
                                    pricetoval[col]*=(totprice/totval);// normalise the price vector
                                    //finaloutputasval[col]*=(totprice/totval);// normalise the total output val vector
                                }
                    }
                printResult(ioheaders,"labour value of final output including imputed labour cost of imports",finaloutputasval);
                System.out.println("\"profit rate\","+profitrate);
                printResult(ioheaders,"production price of final output including imputed pp of imports",finaloutputaspp);
            }
    }
    /** print results on the standard output */
    static void printResult(String[] ioheaders,String iorowname, double[] datavec)
    {
        String q="\"";
        String comma=",";
        for(int i=0; i<ioheaders.length-1; i++)
            System.out.print(q+ioheaders[i]+q+(i<(ioheaders.length-1)?comma:""));
        System.out.println();
        System.out.print(q+iorowname +q+comma);
        for(int j=1 ; j<datavec.length ; j++)
            System.out.print(""+(int)datavec [j]+(j<(datavec.length-1)?comma:""));
        System.out.println();
    }
}

