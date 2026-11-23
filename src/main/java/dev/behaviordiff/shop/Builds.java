package dev.behaviordiff.shop;

import dev.behaviordiff.contract.ContractException;
import dev.behaviordiff.contract.ErrorCategory;
import dev.behaviordiff.contract.Journey;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Checked-in demo builds. Names are the public CLI identifiers. */
public final class Builds {
  private Builds() {}

  public static List<String> ids() {
    return List.of(
        "good",
        "duplicate-cancel",
        "retain-reservation",
        "drop-reservation",
        "reroute",
        "reprice",
        "email-cancel",
        "silent-cancel",
        "null-template",
        "absent-template",
        "external-sink",
        "partial-tables",
        "broken-adapter");
  }

  public static ShopBuild get(String id) {
    return switch (id) {
      case "good" -> new Good();
      case "duplicate-cancel" -> new Duplicate();
      case "retain-reservation" -> new Retain();
      case "drop-reservation" -> new Drop();
      case "reroute" -> new Reroute();
      case "reprice" -> new Reprice();
      case "email-cancel" -> new EmailCancel();
      case "silent-cancel" -> new Silent();
      case "null-template" -> new NullTemplate();
      case "absent-template" -> new AbsentTemplate();
      case "external-sink" -> new External();
      case "partial-tables" -> new Partial();
      case "broken-adapter" -> new Broken();
      default ->
          throw new ContractException(
              ErrorCategory.UNKNOWN_BUILD,
              "build '"
                  + id
                  + "' is not checked in. Known builds: "
                  + new TreeSet<>(ids())
                  + ".");
    };
  }

  private abstract static class CancelShop extends SqliteShop {
    private CancelShop(String id) {
      super(id);
    }

    @Override
    protected final StepWork apply(Connection connection, Journey.Action action, DeterministicIds ids)
        throws SQLException {
      requireCancel(action);
      String orderId = orderId(action);
      String requestId = ids.next(action.id());
      cancelOrder(connection, orderId, requestId, destinationOverride(), amountOverride());
      if (deletesReservation()) {
        deleteReservations(connection, orderId);
      }
      return new StepWork(200, canceledBody(orderId), effects(orderId, action.id(), ids));
    }

    protected String destinationOverride() {
      return null;
    }

    protected Integer amountOverride() {
      return null;
    }

    protected boolean deletesReservation() {
      return true;
    }

    protected abstract List<Effect> effects(String orderId, String actionId, DeterministicIds ids);
  }

  private static final class Good extends CancelShop {
    private Good() {
      super("good");
    }

    @Override
    protected List<Effect> effects(String orderId, String actionId, DeterministicIds ids) {
      return List.of(shipping(orderId, actionId, 1, ids.next(actionId)));
    }
  }

  private static final class Duplicate extends CancelShop {
    private Duplicate() {
      super("duplicate-cancel");
    }

    @Override
    protected boolean deletesReservation() {
      return false;
    }

    @Override
    protected List<Effect> effects(String orderId, String actionId, DeterministicIds ids) {
      return List.of(
          shipping(orderId, actionId, 1, ids.next(actionId)),
          shipping(orderId, actionId, 2, ids.next(actionId)));
    }
  }

  private static final class Retain extends CancelShop {
    private Retain() {
      super("retain-reservation");
    }

    @Override
    protected boolean deletesReservation() {
      return false;
    }

    @Override
    protected List<Effect> effects(String orderId, String actionId, DeterministicIds ids) {
      return List.of(email(orderId, actionId, 1, ids.next(actionId), "canceled", true));
    }
  }

  private static final class Drop extends CancelShop {
    private Drop() {
      super("drop-reservation");
    }

    @Override
    protected List<Effect> effects(String orderId, String actionId, DeterministicIds ids) {
      return List.of(email(orderId, actionId, 1, ids.next(actionId), "canceled", true));
    }
  }

  private static final class Reroute extends CancelShop {
    private Reroute() {
      super("reroute");
    }

    @Override
    protected String destinationOverride() {
      return "dock-b";
    }

    @Override
    protected List<Effect> effects(String orderId, String actionId, DeterministicIds ids) {
      return List.of(shipping(orderId, actionId, 1, ids.next(actionId)));
    }
  }

  private static final class Reprice extends CancelShop {
    private Reprice() {
      super("reprice");
    }

    @Override
    protected Integer amountOverride() {
      return Integer.valueOf(1999);
    }

    @Override
    protected List<Effect> effects(String orderId, String actionId, DeterministicIds ids) {
      return List.of(shipping(orderId, actionId, 1, ids.next(actionId)));
    }
  }

  private static final class EmailCancel extends CancelShop {
    private EmailCancel() {
      super("email-cancel");
    }

    @Override
    protected List<Effect> effects(String orderId, String actionId, DeterministicIds ids) {
      return List.of(email(orderId, actionId, 1, ids.next(actionId), "canceled", true));
    }
  }

  private static final class Silent extends CancelShop {
    private Silent() {
      super("silent-cancel");
    }

    @Override
    protected List<Effect> effects(String orderId, String actionId, DeterministicIds ids) {
      return List.of();
    }
  }

  private static final class NullTemplate extends CancelShop {
    private NullTemplate() {
      super("null-template");
    }

    @Override
    protected List<Effect> effects(String orderId, String actionId, DeterministicIds ids) {
      return List.of(email(orderId, actionId, 1, ids.next(actionId), null, true));
    }
  }

  private static final class AbsentTemplate extends CancelShop {
    private AbsentTemplate() {
      super("absent-template");
    }

    @Override
    protected List<Effect> effects(String orderId, String actionId, DeterministicIds ids) {
      return List.of(email(orderId, actionId, 1, ids.next(actionId), null, false));
    }
  }

  /** Declares a remote sink. {@link RunGate} must reject it before {@link #run}. */
  public static final class External implements ShopBuild {
    private int runs;

    @Override
    public String id() {
      return "external-sink";
    }

    @Override
    public Set<String> observedTables() {
      return RunGate.REQUIRED_TABLES;
    }

    @Override
    public Set<String> declaredSinks() {
      return Set.of("https://carrier.example/cancel");
    }

    @Override
    public Observation run(Journey journey) {
      runs++;
      throw new IllegalStateException("external sink was started");
    }

    public int runs() {
      return runs;
    }
  }

  /** Omits reservations so setup must fail. */
  public static final class Partial implements ShopBuild {
    private int runs;

    @Override
    public String id() {
      return "partial-tables";
    }

    @Override
    public Set<String> observedTables() {
      return Set.of("orders");
    }

    @Override
    public Set<String> declaredSinks() {
      return RunGate.IN_PROCESS_SINKS;
    }

    @Override
    public Observation run(Journey journey) {
      runs++;
      throw new IllegalStateException("partial build was started");
    }

    public int runs() {
      return runs;
    }
  }

  /** Adapter failure after the gate has accepted the sinks and tables. */
  public static final class Broken implements ShopBuild {
    @Override
    public String id() {
      return "broken-adapter";
    }

    @Override
    public Set<String> observedTables() {
      return RunGate.REQUIRED_TABLES;
    }

    @Override
    public Set<String> declaredSinks() {
      return RunGate.IN_PROCESS_SINKS;
    }

    @Override
    public Observation run(Journey journey) {
      throw new IllegalStateException("adapter exploded");
    }
  }
}
