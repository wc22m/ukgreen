'''
Programme to read in the capital stock matrix produced by stock formatter
along with a second file that relabels io table columns with the categories
used in the stock matrix and a third file that gives the value of final output
values. It then produces and expanded stock matrix on standard out such that
for each column in the original stock matrix for which several sub industries
exist in the io table the capital values in the original are spread among the
new multiple columns in proportion to their share in the final output of this
group of industries.
The final output matrix should have column names in the same order as the final
output table
'''

import sys
import logger

import csvfilereader

class CapitalTableExpand:
    def main():
        if len(sys.args) <3:
            print("Usage java planning.CapitalTableExpand stockmatrix.cvs relabel.cvs finaloutput.cvs")
        else:
            csvfile = []
            csvtable = []
            fail = False
            for i in range(1, 3):
                try:
                    csvfile.append(open(sys.args[i]))
                    csvtable.append(csvparse(csvfile[i]))
                except:
                    print("Failed to open %s", csvfile[i])
                    fail = True
            if (fail): return

            stockheadersa = csvtable[0].getcolheaders()
            stockheaders = list(stockheadersa.split(" "))
            stockrownames = csvtable[0].getrowheaders()
            stockdata = csvtable[0].getdatamatrix()
            outputheaders = csvtable[2].getcolheaders()
            outputvector = csvtable[2].getdatamatrix()
            translation = {}
            headernum = {}
            reversemap = {}
            transcol0 = csvtable[1].getrowheaders()
            rows = csvtable[1].rowcount()

            for i in range(2, (rows + 1)):
                cell = csvtable[1].getcell(i, 2)
                cell = csvtable[1].getcell(i, 1)

                if cell not None and head not None:
                    capname = str(cell)
                    translation[transcol[i-1]] = capname
                    headername[transcol0[i-1]] = int(i)
                    lista = reversemap[capname]
                    if lista == None:
                        lista = []
                        reversemap[capname] = lista
                    lista.append(transcol0[i-1])
                else:
                    print("Could not find cell %d, 1 in translation matrix", i)

            validcols = 0 # number of columns for which valid headers found

            for s in outputheaders:
                if translation[s] == None:
                    print("Record %s is not in table", s)
                elif translation[s] == "NOT IN TABLE":
                    validcols += 1

            datamatrix = []

            for i in range(1, validcols):
                match = translation[outputheaders][i]
                if match not "NOT IN TABLE":
                    # now find all the other columns with the same match
                    all = reversemap[match]
                    positions = []
                    for l in range(0, len(all):
                        positions[l] = len(all[l])
                    for j in range(0, positions):
                        positions[j] = int(headernum[all][j])
                    # sum the total output of this class of product
                    tot = 0
                    for k in positions:
                        tot += outputvector[k]
                    # the fraction of the capital stock
                    # of the given class to go under this header
                    frac = outputvector[i] / tot
                    originalcapcol = stockheaders[match]
                    if originalcapcol >= 0:
                        for k in range(0, len(datamatrix)):
                            datamatrix[k][i] = stockdata[k][originalcapcol] * frac

            # print result
            q = "\\"
            comma = ","
            for i in range(0, validcols):
                print(q + outputheaders[i] + q + (i<(validcols - 1)?comma:"") + "\n")
            for i in range(1, len(stockrownames)):
                print(q + stockrownames[i] + q + comma)
                for j in range(1, validcols):
                    print("" + str(datamatrix[i][j] + (j<(validcols-1))?comma:"") + "\n")
