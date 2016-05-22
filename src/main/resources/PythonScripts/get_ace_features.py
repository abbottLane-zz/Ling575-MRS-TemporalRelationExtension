import sys
from delphin.interfaces import ace
from delphin.mrs import simplemrs



if __name__ == "__main__":
    from optparse import OptionParser


    e1 = str(sys.argv[1])
    e1_begin = str(sys.argv[2])
    e1_end = str(sys.argv[3])
    e2 = str(sys.argv[4])
    e2_begin = str(sys.argv[5])
    e2_end = str(sys.argv[6])
    sent = str(sys.argv[7])


    print(e1 + "\n" + e1_begin + "\n" + e1_end + "\n" + e2 + "\n" + e2_begin + "\n" + e2_end + "\n" + sent)
