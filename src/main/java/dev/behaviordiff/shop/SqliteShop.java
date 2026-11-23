package dev.behaviordiff.shop;

import dev.behaviordiff.contract.ContractException;
import dev.behaviordiff.contract.ErrorCategory;
import dev.behaviordiff.contract.Journey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** In-memory SQLite shop. Subclasses only decide the cancellation mutation. */
public abstract class SqliteShop implements ShopBuild {
  static {
    try {
      Class.forName("org.sqlite.JDBC");
    } catch (ClassNotFoundException ex) {
      throw new ExceptionInInitializerError(ex);
    }
  }

  private final String id;

  protected SqliteShop(String id) {
    this.id = id;
  }

  @Override
  public final String id() {
    return id;
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
  public final Observation run(Journey journey) {
    try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
      exec(connection, loadSeed());
      List<StepObservation> steps = new ArrayList<>();
      DeterministicIds ids = new DeterministicIds(id);
      for (Journey.Action action : journey.actions()) {
        Map<String, List<Map<String, Object>>> before = snapshot(connection);
        StepWork work = apply(connection, action, ids);
        Map<String, List<Map<String, Object>>> after = snapshot(connection);
        steps.add(
            new StepObservation(
                action.id(),
                work.httpStatus(),
                work.responseBody(),
                before,
                after,
                work.effects(),
                false));
      }
      return Observation.ok(id, steps);
    } catch (ContractException ex) {
      throw ex;
    } catch (SQLException | IOException ex) {
      throw new ContractException(
          ErrorCategory.INCOMPLETE_EVIDENCE,
          "sqlite adapter for build '" + id + "' failed: " + ex.getMessage(),
          ex);
    }
  }

  protected abstract StepWork apply(Connection connection, Journey.Action action, DeterministicIds ids)
      throws SQLException;

  protected static void requireCancel(Journey.Action action) {
    if (!"cancel_order".equals(action.name())) {
      throw new ContractException(
          ErrorCategory.INCOMPLETE_EVIDENCE,
          "unknown mapping for action '" + action.name() + "'. No coverage is claimed.");
    }
  }

  protected static String orderId(Journey.Action action) {
    return action.input().get("orderId");
  }

  protected static void cancelOrder(
      Connection connection, String orderId, String requestId, String destination, Integer amount)
      throws SQLException {
    StringBuilder sql =
        new StringBuilder("UPDATE orders SET status = 'canceled', request_id = ?");
    if (destination != null) {
      sql.append(", destination = ?");
    }
    if (amount != null) {
      sql.append(", amount_cents = ?");
    }
    sql.append(" WHERE order_id = ?");
    try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
      int index = 1;
      statement.setString(index++, requestId);
      if (destination != null) {
        statement.setString(index++, destination);
      }
      if (amount != null) {
        statement.setInt(index++, amount.intValue());
      }
      statement.setString(index, orderId);
      int updated = statement.executeUpdate();
      if (updated != 1) {
        throw new ContractException(
            ErrorCategory.INCOMPLETE_EVIDENCE,
            "order '" + orderId + "' is not in seed " + dev.behaviordiff.contract.JourneyValidator.SEED
                + ". No coverage is claimed.");
      }
    }
  }

  protected static void deleteReservations(Connection connection, String orderId) throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement("DELETE FROM reservations WHERE order_id = ?")) {
      statement.setString(1, orderId);
      statement.executeUpdate();
    }
  }

  protected static Effect shipping(String orderId, String actionId, int sequence, String requestId) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("orderId", orderId);
    payload.put("requestId", requestId);
    return new Effect(
        "shipping", "cancel_shipment", "ship-cancel:" + orderId, actionId, sequence, payload);
  }

  protected static Effect email(
      String orderId, String actionId, int sequence, String requestId, Object template, boolean includeTemplate) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("orderId", orderId);
    if (includeTemplate) {
      payload.put("template", template);
    }
    payload.put("requestId", requestId);
    return new Effect(
        "email", "order_canceled", "email:" + orderId + ":canceled", actionId, sequence, payload);
  }

  protected static Map<String, Object> canceledBody(String orderId) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("orderId", orderId);
    body.put("status", "canceled");
    return body;
  }

  private static void exec(Connection connection, String sql) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      for (String part : sql.split(";")) {
        String trimmed = part.trim();
        if (!trimmed.isEmpty()) {
          statement.execute(trimmed);
        }
      }
    }
  }

  private static String loadSeed() throws IOException {
    try (var in = SqliteShop.class.getResourceAsStream("/seed/seed-cancel-o1.sql")) {
      if (in == null) {
        throw new IOException("classpath seed /seed/seed-cancel-o1.sql is missing");
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private static Map<String, List<Map<String, Object>>> snapshot(Connection connection)
      throws SQLException {
    Map<String, List<Map<String, Object>>> tables = new LinkedHashMap<>();
    tables.put("orders", readOrders(connection));
    tables.put("reservations", readReservations(connection));
    return tables;
  }

  private static List<Map<String, Object>> readOrders(Connection connection) throws SQLException {
    List<Map<String, Object>> rows = new ArrayList<>();
    try (PreparedStatement statement =
            connection.prepareStatement(
                "SELECT order_id, status, amount_cents, destination, request_id, note"
                    + " FROM orders ORDER BY order_id");
        ResultSet rs = statement.executeQuery()) {
      while (rs.next()) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("orderId", rs.getString("order_id"));
        row.put("status", rs.getString("status"));
        row.put("amountCents", Integer.valueOf(rs.getInt("amount_cents")));
        row.put("destination", rs.getString("destination"));
        row.put("requestId", rs.getString("request_id"));
        row.put("note", rs.getString("note"));
        rows.add(row);
      }
    }
    return rows;
  }

  private static List<Map<String, Object>> readReservations(Connection connection)
      throws SQLException {
    List<Map<String, Object>> rows = new ArrayList<>();
    try (PreparedStatement statement =
            connection.prepareStatement(
                "SELECT reservation_id, order_id, sku, quantity FROM reservations"
                    + " ORDER BY reservation_id");
        ResultSet rs = statement.executeQuery()) {
      while (rs.next()) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("reservationId", rs.getString("reservation_id"));
        row.put("orderId", rs.getString("order_id"));
        row.put("sku", rs.getString("sku"));
        row.put("quantity", Integer.valueOf(rs.getInt("quantity")));
        rows.add(row);
      }
    }
    return rows;
  }

  public record StepWork(int httpStatus, Map<String, Object> responseBody, List<Effect> effects) {}

  public static final class DeterministicIds {
    private final String buildId;
    private int n;

    DeterministicIds(String buildId) {
      this.buildId = buildId;
    }

    public String next(String stepId) {
      n++;
      return "req-" + buildId + "-" + stepId + "-" + n;
    }
  }
}
