import org.cleartk.timeml.eval.TempEval2013Evaluation;

public class RelationExtractor {
    public static void main(String[] args){
        System.out.println("Running TempEval 2013 ClearTK-TimeML system");

        //Training and testing data directories
        String timebank_dir = "src/main/resources/train/TBAQ-cleaned/TimeBank"; // Training corpus
        String aquaint_dir = "src/main/resources/train/TBAQ-cleaned/AQUAINT"; // Training corpus
        String devtest_dir = "src/main/resources/train/TBAQ-cleaned/dev-test"; // Test set for development phase
        String platinum_test_dir = "src/main/resources/te3-platinum"; // For final performance evaluation only

        try {
            String[] evalArgs = {"--train-dirs", timebank_dir, aquaint_dir, "--test-dirs", devtest_dir};
            System.out.println("Using: " + devtest_dir + " as dev-test data");
            TempEval2013Evaluation.main(evalArgs);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
