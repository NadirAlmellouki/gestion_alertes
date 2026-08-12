package FST.MST_RSI.PFA.classification.domain.port;

import FST.MST_RSI.PFA.classification.domain.model.AlertClassificationContext;
import FST.MST_RSI.PFA.classification.domain.model.ClassificationResult;
import FST.MST_RSI.PFA.classification.domain.model.SolutionContext;

import java.util.List;

public interface AlertClassifierPort {

    ClassificationResult classify(AlertClassificationContext alertContext, List<SolutionContext> businessContext);
}
