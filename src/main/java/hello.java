/**
 * Created by wlane on 4/18/16.
 */
import org.cleartk.examples.pos.RunExamplePOSAnnotator;
import org.cleartk.timeml.*;
import org.cleartk.examples.*;

public class hello {
    public static void main(String[] args){
        System.out.print("Running TempEval 2013 ClearTK-TimeML system");
        try {
            TimeMlAnnotate.main("/home/wlane/compling/data/tempEval2013");
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
