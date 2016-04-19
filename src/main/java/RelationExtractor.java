/**
 * Created by wlane on 4/18/16.
 */
import org.cleartk.examples.pos.RunExamplePOSAnnotator;
import org.cleartk.timeml.*;
import org.cleartk.examples.*;

public class RelationExtractor {
    public static void main(String[] args){
        System.out.print("Running TempEval 2013 ClearTK-TimeML system");
        try {
            TimeMlAnnotate.main("src/main/resources/TE3-platinum-test");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
