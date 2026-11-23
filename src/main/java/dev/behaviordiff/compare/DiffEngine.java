package dev.behaviordiff.compare;

import dev.behaviordiff.contract.ContractException;
import dev.behaviordiff.contract.ErrorCategory;
import dev.behaviordiff.contract.Journey;
import dev.behaviordiff.shop.Effect;
import dev.behaviordiff.shop.Observation;
import dev.behaviordiff.shop.StepObservation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Compares unordered row sets and effect multiplicity for one shared journey. */
public final class DiffEngine {
  static final Set<String> VOLATILE = Set.of("requestId");
  private static final List<String> ORDER_FIELDS =
      List.of("amountCents", "destination", "note", "orderId", "status");
  private static final List<String> RESERVATION_FIELDS =
      List.of("orderId", "quantity", "reservationId", "sku");
  private static final Set<String> TABLES = Set.of("orders", "reservations");

  private DiffEngine() {}

  public static Report compare(
      Journey journey, Observation left, Observation right, List<String> approvals) {
    Map<String, Set<String>> approved = parseApprovals(approvals);
    if (!left.complete() || !right.complete()) {
      String message = joinReasons(left, right);
      return Report.incomplete(message);
    }
    if (left.steps().size() != journey.actions().size()
        || right.steps().size() != journey.actions().size()) {
      return Report.incomplete("missing instrumentation: a build did not snapshot every step.");
    }

    List<Map<String, Object>> rowChanges = new ArrayList<>();
    List<Map<String, Object>> effectChanges = new ArrayList<>();
    boolean responseChanged = false;
    String earliest = null;
    List<String> steps = new ArrayList<>();

    for (int i = 0; i < journey.actions().size(); i++) {
      StepObservation l = left.steps().get(i);
      StepObservation r = right.steps().get(i);
      if (l.truncated() || r.truncated()) {
        return Report.incomplete(
            "snapshot truncated at step '" + journey.actions().get(i).id() + "'.");
      }
      if (!l.before().keySet().containsAll(TABLES)
          || !l.after().keySet().containsAll(TABLES)
          || !r.before().keySet().containsAll(TABLES)
          || !r.after().keySet().containsAll(TABLES)) {
        return Report.incomplete(
            "missing instrumentation at step '" + journey.actions().get(i).id() + "'.");
      }
      String actionId = journey.actions().get(i).id();
      steps.add(actionId);
      boolean stepResponse = !sameResponse(l, r);
      if (stepResponse) {
        responseChanged = true;
      }
      List<Map<String, Object>> stepRows = rowDiffs(actionId, l, r, approved);
      List<Map<String, Object>> stepEffects = effectDiffs(l, r);
      if (earliest == null && (stepResponse || !stepRows.isEmpty() || !stepEffects.isEmpty())) {
        earliest = actionId;
      }
      rowChanges.addAll(stepRows);
      effectChanges.addAll(stepEffects);
    }

    rowChanges.sort(
        Comparator.comparing((Map<String, Object> row) -> (String) row.get("actionId"))
            .thenComparing(row -> (String) row.get("table"))
            .thenComparing(row -> (String) row.get("key")));
    effectChanges.sort(
        Comparator.comparing((Map<String, Object> row) -> (String) row.get("actionId"))
            .thenComparing(row -> (String) row.get("sink"))
            .thenComparing(row -> (String) row.get("effectType"))
            .thenComparing(row -> (String) row.get("businessKey")));

    boolean any = responseChanged || !rowChanges.isEmpty() || !effectChanges.isEmpty();
    String status;
    if (!any) {
      status = "identical";
    } else if (fullyApproved(rowChanges, effectChanges, responseChanged)) {
      status = "approved";
    } else {
      status = "changes";
    }
    List<String> approvalList = new ArrayList<>(approvals);
    approvalList.sort(Comparator.naturalOrder());
    return Report.success(
        status,
        journey.name(),
        journey.seed(),
        journey.clock(),
        left.buildId(),
        right.buildId(),
        earliest,
        responseChanged,
        approvalList,
        rowChanges,
        effectChanges,
        steps);
  }

  static Map<String, Set<String>> parseApprovals(List<String> approvals) {
    Map<String, Set<String>> approved = new LinkedHashMap<>();
    for (String raw : approvals) {
      int dot = raw == null ? -1 : raw.indexOf('.');
      if (raw == null || dot <= 0 || dot != raw.lastIndexOf('.') || dot == raw.length() - 1) {
        throw new ContractException(
            ErrorCategory.MALFORMED_INPUT,
            "approval '"
                + raw
                + "' must be table.field, for example orders.destination.");
      }
      String table = raw.substring(0, dot);
      String field = raw.substring(dot + 1);
      List<String> known = "orders".equals(table) ? ORDER_FIELDS : null;
      if ("reservations".equals(table)) {
        known = RESERVATION_FIELDS;
      }
      if (known == null || !known.contains(field)) {
        throw new ContractException(
            ErrorCategory.MALFORMED_INPUT,
            "approval '"
                + raw
                + "' is not a contract field. Row appearance and volatile requestId cannot be"
                + " approved.");
      }
      approved.computeIfAbsent(table, ignored -> new TreeSet<>()).add(field);
    }
    return approved;
  }

  private static boolean fullyApproved(
      List<Map<String, Object>> rows, List<Map<String, Object>> effects, boolean responseChanged) {
    if (responseChanged || !effects.isEmpty()) {
      return false;
    }
    return rows.stream().allMatch(row -> Boolean.TRUE.equals(row.get("approved")));
  }

  private static boolean sameResponse(StepObservation left, StepObservation right) {
    return left.httpStatus() == right.httpStatus()
        && strip(left.responseBody()).equals(strip(right.responseBody()));
  }

  private static List<Map<String, Object>> rowDiffs(
      String actionId, StepObservation left, StepObservation right, Map<String, Set<String>> approved) {
    List<Map<String, Object>> changes = new ArrayList<>();
    changes.addAll(tableDiff("orders", "orderId", ORDER_FIELDS, actionId, left, right, approved));
    changes.addAll(
        tableDiff(
            "reservations", "reservationId", RESERVATION_FIELDS, actionId, left, right, approved));
    return changes;
  }

  private static List<Map<String, Object>> tableDiff(
      String table,
      String primaryKey,
      List<String> fields,
      String actionId,
      StepObservation left,
      StepObservation right,
      Map<String, Set<String>> approved) {
    Map<String, Map<String, Object>> leftBefore = index(left.before().get(table), primaryKey, fields);
    Map<String, Map<String, Object>> leftAfter = index(left.after().get(table), primaryKey, fields);
    Map<String, Map<String, Object>> rightBefore =
        index(right.before().get(table), primaryKey, fields);
    Map<String, Map<String, Object>> rightAfter = index(right.after().get(table), primaryKey, fields);
    TreeSet<String> keys = new TreeSet<>();
    keys.addAll(leftAfter.keySet());
    keys.addAll(rightAfter.keySet());
    List<Map<String, Object>> changes = new ArrayList<>();
    for (String key : keys) {
      Map<String, Object> lAfter = leftAfter.get(key);
      Map<String, Object> rAfter = rightAfter.get(key);
      if (Objects.equals(lAfter, rAfter)) {
        continue;
      }
      List<String> changed = changedFields(fields, lAfter, rAfter);
      Set<String> allowed = approved.getOrDefault(table, Set.of());
      boolean rowApproved = !changed.contains("*") && allowed.containsAll(changed);
      Map<String, Object> change = new LinkedHashMap<>();
      change.put("actionId", actionId);
      change.put("approved", Boolean.valueOf(rowApproved));
      change.put("changedFields", changed);
      change.put("key", key);
      change.put("leftAfter", lAfter);
      change.put("leftBefore", leftBefore.get(key));
      change.put("leftOp", op(leftBefore.get(key), lAfter));
      change.put("rightAfter", rAfter);
      change.put("rightBefore", rightBefore.get(key));
      change.put("rightOp", op(rightBefore.get(key), rAfter));
      change.put("table", table);
      changes.add(change);
    }
    return changes;
  }

  private static List<String> changedFields(
      List<String> fields, Map<String, Object> left, Map<String, Object> right) {
    if (left == null || right == null) {
      return List.of("*");
    }
    List<String> changed = new ArrayList<>();
    for (String field : fields) {
      if (!Objects.equals(left.get(field), right.get(field))) {
        changed.add(field);
      }
    }
    changed.sort(Comparator.naturalOrder());
    return List.copyOf(changed);
  }

  private static String op(Map<String, Object> before, Map<String, Object> after) {
    if (before == null && after == null) {
      return "none";
    }
    if (before == null) {
      return "insert";
    }
    if (after == null) {
      return "delete";
    }
    if (before.equals(after)) {
      return "none";
    }
    return "update";
  }

  private static Map<String, Map<String, Object>> index(
      List<Map<String, Object>> rows, String primaryKey, List<String> fields) {
    Map<String, Map<String, Object>> indexed = new LinkedHashMap<>();
    if (rows == null) {
      return indexed;
    }
    for (Map<String, Object> row : rows) {
      Map<String, Object> stripped = project(row, fields);
      indexed.put(String.valueOf(row.get(primaryKey)), stripped);
    }
    return indexed;
  }

  private static Map<String, Object> project(Map<String, Object> row, List<String> fields) {
    Map<String, Object> projected = new LinkedHashMap<>();
    for (String field : fields) {
      projected.put(field, row.get(field));
    }
    return projected;
  }

  private static List<Map<String, Object>> effectDiffs(StepObservation left, StepObservation right) {
    Map<EffectKey, Group> leftGroups = group(left.effects());
    Map<EffectKey, Group> rightGroups = group(right.effects());
    TreeSet<EffectKey> keys = new TreeSet<>();
    keys.addAll(leftGroups.keySet());
    keys.addAll(rightGroups.keySet());
    List<Map<String, Object>> changes = new ArrayList<>();
    for (EffectKey key : keys) {
      Group l = leftGroups.getOrDefault(key, Group.empty());
      Group r = rightGroups.getOrDefault(key, Group.empty());
      boolean sameKind = samePayloadKind(l, r);
      if (l.sequences.size() == r.sequences.size() && sameKind) {
        continue;
      }
      String cause;
      if (!sameKind && nullVersusAbsent(l.payloads, r.payloads)) {
        cause = "null-vs-absent";
      } else if (!sameKind) {
        cause = "payload";
      } else if (l.sequences.size() > 0 && r.sequences.size() > 0) {
        cause = "duplicate-idempotency-key";
      } else if (r.sequences.isEmpty()) {
        cause = "missing-effect";
      } else {
        cause = "extra-effect";
      }
      Map<String, Object> change = new LinkedHashMap<>();
      change.put("actionId", key.actionId);
      change.put("businessKey", key.businessKey);
      change.put("cause", cause);
      change.put("effectType", key.effectType);
      change.put("leftCount", Integer.valueOf(l.sequences.size()));
      change.put("leftSequences", l.sequences);
      change.put("rightCount", Integer.valueOf(r.sequences.size()));
      change.put("rightSequences", r.sequences);
      change.put("sink", key.sink);
      changes.add(change);
    }
    return changes;
  }

  private static boolean samePayloadKind(Group left, Group right) {
    java.util.HashSet<String> kinds = new java.util.HashSet<>();
    kinds.addAll(left.canonical);
    kinds.addAll(right.canonical);
    return kinds.size() <= 1;
  }

  private static boolean nullVersusAbsent(
      List<Map<String, Object>> left, List<Map<String, Object>> right) {
    if (left.size() != right.size() || left.isEmpty()) {
      return false;
    }
    List<Map<String, Object>> l = new ArrayList<>(left);
    List<Map<String, Object>> r = new ArrayList<>(right);
    l.sort(Comparator.comparing(DiffEngine::payloadCanon));
    r.sort(Comparator.comparing(DiffEngine::payloadCanon));
    boolean saw = false;
    for (int i = 0; i < l.size(); i++) {
      Map<String, Object> a = l.get(i);
      Map<String, Object> b = r.get(i);
      TreeSet<String> fields = new TreeSet<>();
      fields.addAll(a.keySet());
      fields.addAll(b.keySet());
      for (String field : fields) {
        boolean inA = a.containsKey(field);
        boolean inB = b.containsKey(field);
        if (inA && inB) {
          if (!Objects.equals(a.get(field), b.get(field))) {
            return false;
          }
        } else if (inA && a.get(field) == null) {
          saw = true;
        } else if (inB && b.get(field) == null) {
          saw = true;
        } else {
          return false;
        }
      }
    }
    return saw;
  }

  private static Map<EffectKey, Group> group(List<Effect> effects) {
    Map<EffectKey, Group> groups = new TreeMap<>();
    for (Effect effect : effects) {
      EffectKey key =
          new EffectKey(effect.actionId(), effect.sink(), effect.effectType(), effect.businessKey());
      Group group = groups.computeIfAbsent(key, ignored -> new Group());
      Map<String, Object> payload = strip(effect.payload());
      group.sequences.add(Integer.valueOf(effect.sequence()));
      group.payloads.add(payload);
      group.canonical.add(payloadCanon(payload));
    }
    return groups;
  }

  private static String payloadCanon(Map<String, Object> payload) {
    TreeMap<String, Object> sorted = new TreeMap<>(payload);
    StringBuilder out = new StringBuilder();
    for (Map.Entry<String, Object> entry : sorted.entrySet()) {
      out.append(entry.getKey()).append('=');
      if (!payload.containsKey(entry.getKey())) {
        out.append("<absent>");
      } else if (entry.getValue() == null) {
        out.append("<null>");
      } else {
        out.append(entry.getValue());
      }
      out.append(';');
    }
    return out.toString();
  }

  private static Map<String, Object> strip(Map<String, Object> raw) {
    Map<String, Object> copy = new LinkedHashMap<>();
    if (raw == null) {
      return copy;
    }
    for (Map.Entry<String, Object> entry : raw.entrySet()) {
      if (!VOLATILE.contains(entry.getKey())) {
        copy.put(entry.getKey(), entry.getValue());
      }
    }
    return copy;
  }

  private static String joinReasons(Observation left, Observation right) {
    if (!left.complete() && !right.complete()) {
      return left.incompleteReason() + "; " + right.incompleteReason();
    }
    return !left.complete() ? left.incompleteReason() : right.incompleteReason();
  }

  private record EffectKey(String actionId, String sink, String effectType, String businessKey)
      implements Comparable<EffectKey> {
    @Override
    public int compareTo(EffectKey other) {
      return Comparator.comparing(EffectKey::actionId)
          .thenComparing(EffectKey::sink)
          .thenComparing(EffectKey::effectType)
          .thenComparing(EffectKey::businessKey)
          .compare(this, other);
    }
  }

  private static final class Group {
    private final List<Integer> sequences = new ArrayList<>();
    private final List<Map<String, Object>> payloads = new ArrayList<>();
    private final List<String> canonical = new ArrayList<>();

    private static Group empty() {
      return new Group();
    }
  }
}
