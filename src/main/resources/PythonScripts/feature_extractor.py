#!/opt/python-3.4/bin/python3

"""%prog [--help] <e1> <e1_begin> <e1_end> <e2> <e2_begin> <e2_end> [<"sentence">]"""


import sys
from delphin.interfaces import ace
from delphin.mrs import simplemrs


__author__ = 'Martin J. Horn'


def output_features(features):
    for feat in features:
        print(feat)


def extract_features(e1, e1_b, e1_e, e2, e2_b, e2_e, ace_result):
    features = []
    mrs = simplemrs.loads_one(ace_result)
    ep_1, ep_2 = None, None
    for ep in mrs.eps():
        if ep.cfrom <= e1_b < ep.cto and (ep.pred.pos == 'n' or ep.pred.pos == 'v'):
            ep_1 = ep
            continue
        elif ep.cfrom <= e2_b < ep.cto and (ep.pred.pos == 'n' or ep.pred.pos == 'v'):
            ep_2 = ep
            break
    if ep_1 and ep_2:
        feat_1 = "e1=" + ep_1.pred.lemma
        feat_2 = "e2=" + ep_2.pred.lemma
        features.append(feat_1)
        features.append(feat_2)
        for prop in mrs.properties(ep_1.iv):
            feat = "e1:" + prop + ":" + mrs.properties(ep_1.iv)[prop]
            features.append(feat)
        for prop in mrs.properties(ep_2.iv):
            feat = "e2:" + prop + ":" + mrs.properties(ep_2.iv)[prop]
            features.append(feat)
        args_1 = mrs.args(ep_1.nodeid)
        args_2 = mrs.args(ep_2.nodeid)
        feat = None
        for arg in args_1:
            if ep_2.label == args_1[arg] or args_2['ARG0'] == args_1[arg]:
                feat = "e1:"+ep_1.pred.pos+"["+arg+"->e2:"+ep_2.pred.pos + "]"
                print(feat)
                features.append(feat)
        if not feat:
            for arg in args_2:
                if ep_1.label == args_2[arg] or args_1['ARG0'] == args_2[arg]:
                    feat = "e2:"+ep_2.pred.pos+":"+arg+":e1"+ep_1.pred.pos
                    print(feat)
                    features.append(feat)

        paths = find_paths(mrs, ep_1, ep_2, "")
        if paths:
            shortest_path = paths[0]
            for path in paths:
                if len(path.split(",")) < len(shortest_path.split(",")):
                    shortest_path = path
        else:
            shortest_path = "PATH:NO_PATH"
        features.append("PATH:" + shortest_path)

    return features


seen_ids = set()


def find_paths(mrs, ep_1, ep_2, cur_path):
    # Recursive path search using incoming args of second event
    paths = []
    if len(cur_path) == 0:
        if ep_2.pred.pos:
            cur_path = ep_2.pred.pos
        else:
            cur_path = "NO_POS"
        # Debug line: pred name
        # cur_path = ep_2.pred.string.strip("\"")
    else:
        if ep_2.pred.pos:
            cur_path = ep_2.pred.pos + "," + cur_path
        else:
            cur_path = "NO_POS," + cur_path
        # Debug line: pred name
        # cur_path = ep_2.pred.string.strip("\"") + "," + cur_path
    in_args = mrs.incoming_args(ep_2.nodeid)

    for node_id in in_args:
        global seen_ids
        if node_id not in seen_ids:
            if mrs.ep(node_id).label != ep_1.label and mrs.ep(node_id).iv != ep_1.iv:
                for arg in in_args[node_id]:
                    prefix = arg + ":"
                    break
                seen_ids.add(node_id)
                paths += find_paths(mrs, ep_1, mrs.ep(node_id), prefix+cur_path)
            else:
                for arg in in_args[node_id]:
                    prefix = arg + ":"
                    break
                if mrs.ep(node_id).pred.pos:
                    cur_path = mrs.ep(node_id).pred.pos + "," + prefix + cur_path
                else:
                    cur_path = "NO_POS," + prefix + cur_path
                # Debug line: pred name
                # cur_path = mrs.ep(node_id).pred.string.strip("\"") + "," + prefix + cur_path
                paths.append(cur_path)
        else:
            paths.clear()

    return paths


def run_ace(sentence):
    ace_bin = "/Applications/ace/ace-0.9.22/ace"
    erg_file = "/Applications/ace/ace-0.9.22/erg-1214-osx-0.9.22.dat"
    results = ace.parse(erg_file, sentence, cmdargs=['-n', '1'], executable=ace_bin)['RESULTS']
    if results:
        res1 = ace.parse(erg_file, sentence, cmdargs=['-n', '1'], executable=ace_bin)['RESULTS'][0]['MRS']
    else:
        res1 = None

    return res1


def read_doc(e1, e1_begin, e1_end, e2, e2_begin, e2_end, file_name=None):
    # Ensure e1 occurs first, e2 occurs second
    if int(e1_begin) < int(e2_begin):
        e1_string = e1
        e1_begin_sent = int(e1_begin)
        e1_end_sent = int(e1_end)
        e2_string = e2
        e2_begin_sent = int(e2_begin)
        e2_end_sent = int(e2_end)
    else:
        e1_string = e2
        e1_begin_sent = int(e2_begin)
        e1_end_sent = int(e2_end)
        e2_string = e1
        e2_begin_sent = int(e1_begin)
        e2_end_sent = int(e1_end)

    # Check for specified input file
    if file_name:
        sent = file_name
        # # Read sentence from file
        # with open(file_name, 'r') as doc:
        #     doc_text = doc.read()
        #     sent = doc_text.strip()
    else:
        # Read sentence from stdin
        sent = input()

    return e1_string, e1_begin_sent, e1_end_sent, e2_string, e2_begin_sent, e2_end_sent, sent


if __name__ == "__main__":
    from optparse import OptionParser

    parser = OptionParser(__doc__)
    options, args = parser.parse_args()
    if len(args) == 6:
        e1, e1_b, e1_e, e2, e2_b, e2_e, sent = read_doc(args[0], args[1],
                                                        args[2], args[3],
                                                        args[4], args[5])
        res = run_ace(sent)
        if res:
            feats = extract_features(e1, e1_b, e1_e, e2, e2_b, e2_e, res)
            output_features(feats)
        else:
            print("NO_PARSE")
    if len(args) == 7:
        e1, e1_b, e1_e, e2, e2_b, e2_e, sent = read_doc(args[0], args[1],
                                                        args[2], args[3],
                                                        args[4], args[5],
                                                        file_name=args[6])
        res = run_ace(sent)
        if res:
            feats = extract_features(e1, e1_b, e1_e, e2, e2_b, e2_e, res)
            output_features(feats)
        else:
            print("NO_PARSE")
    else:
        sys.stderr.write("Must specify 6/7 arguments: <e1> <e1_begin> <e1_end> "
                         "<e2> <e2_begin> <e2_end> [<\"sentence\">]\n")
