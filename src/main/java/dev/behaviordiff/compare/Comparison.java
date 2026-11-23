package dev.behaviordiff.compare;

import dev.behaviordiff.contract.ContractException;
import dev.behaviordiff.contract.ErrorCategory;
import dev.behaviordiff.contract.Journey;
import dev.behaviordiff.shop.Builds;
import dev.behaviordiff.shop.Observation;
import dev.behaviordiff.shop.RunGate;
import dev.behaviordiff.shop.ShopBuild;
import java.util.List;

/** Gate, run, then compare. Setup failures propagate and do not claim coverage. */
public final class Comparison {
  private Comparison() {}

  public static Report run(Journey journey, String leftId, String rightId, List<String> approvals) {
    return run(journey, Builds.get(leftId), Builds.get(rightId), approvals);
  }

  public static Report run(
      Journey journey, ShopBuild left, ShopBuild right, List<String> approvals) {
    RunGate.check(left);
    RunGate.check(right);
    Observation leftObservation = execute(left, journey);
    Observation rightObservation = execute(right, journey);
    return DiffEngine.compare(journey, leftObservation, rightObservation, approvals);
  }

  private static Observation execute(ShopBuild build, Journey journey) {
    try {
      return build.run(journey);
    } catch (ContractException ex) {
      if (ex.category() == ErrorCategory.INCOMPLETE_EVIDENCE) {
        return Observation.incomplete(build.id(), ex.getMessage());
      }
      throw ex;
    } catch (RuntimeException ex) {
      return Observation.incomplete(build.id(), "adapter failed: " + ex.getMessage());
    }
  }
}
