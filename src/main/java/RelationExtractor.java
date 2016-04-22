import org.cleartk.timeml.eval.TempEval2013Evaluation;

public class RelationExtractor {
    public static void main(String[] args){
        System.out.print("Running TempEval 2013 ClearTK-TimeML system");
        try {
//            --train-dirs /path/to/TimeBank /path/to/AQUAINT
//            --test-dirs /path/to/te3-platinum
            String[] in_out = {"--train-dirs", "src/main/resources/train/TBAQ-cleaned/TimeBank","src/main/resources/train/TBAQ-cleaned/AQUAINT", "--test-dirs","src/main/resources/train/TBAQ-cleaned/dev-test"};
            TempEval2013Evaluation.main(in_out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
