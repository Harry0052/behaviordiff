package dev.behaviordiff.contract;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Parses a journey file into the version 1 contract. Rejects unknown fields, unknown actions,
 * oversized payloads, and any string that names an external URL.
 */
public final class JourneyValidator {
  public static final int MAX_BYTES = 65_536;
  public static final int MAX_ACTIONS = 32;
  public static final int MAX_STRING = 256;
  public static final String SCHEMA_VERSION = "1";
  public static final String SEED = "seed-cancel-o1";
  public static final String CLOCK = "2026-01-15T12:00:00Z";
  public static final Set<String> ACTIONS = Set.of("cancel_order");

  private static final Pattern NAME = Pattern.compile("^[a-z][a-z0-9-]{0,63}$");
  private static final Pattern ORDER_ID = Pattern.compile("^O-[0-9]{1,6}$");
  private static final Set<String> ROOT_FIELDS =
      Set.of("schemaVersion", "name", "seed", "clock", "actions");
  private static final Set<String> ACTION_FIELDS = Set.of("id", "name", "input");
  private static final Set<String> INPUT_FIELDS = Set.of("orderId");

  private final ObjectMapper mapper = new ObjectMapper();

  public JourneyValidator() {
    mapper.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
  }

  public Journey parse(byte[] utf8) {
    if (utf8 == null) {
      throw new ContractException(ErrorCategory.MALFORMED_INPUT, "fixture bytes are missing.");
    }
    if (utf8.length > MAX_BYTES) {
      throw new ContractException(
          ErrorCategory.OVERSIZED_INPUT,
          "fixture is "
              + utf8.length
              + " bytes; the limit is "
              + MAX_BYTES
              + " bytes. Split the journey or remove unused fields.");
    }
    JsonNode root = readTree(utf8);
    if (!root.isObject()) {
      throw new ContractException(
          ErrorCategory.MALFORMED_INPUT, "fixture root must be a JSON object.");
    }
    requireFields((ObjectNode) root, ROOT_FIELDS, "$");
    String schemaVersion = text(root, "schemaVersion", "$");
    if (!SCHEMA_VERSION.equals(schemaVersion)) {
      throw new ContractException(
          ErrorCategory.MALFORMED_INPUT,
          "schemaVersion '"
              + schemaVersion
              + "' is unsupported. Use schemaVersion \""
              + SCHEMA_VERSION
              + "\".");
    }
    String name = identifier(text(root, "name", "$"), "$.name");
    String seed = text(root, "seed", "$");
    if (!SEED.equals(seed)) {
      throw new ContractException(
          ErrorCategory.UNKNOWN_SEED,
          "seed '" + seed + "' is not available. Use seed \"" + SEED + "\".");
    }
    String clock = text(root, "clock", "$");
    if (!CLOCK.equals(clock)) {
      throw new ContractException(
          ErrorCategory.MALFORMED_INPUT,
          "clock '"
              + clock
              + "' is not the fixed demo clock. Use \""
              + CLOCK
              + "\" so two runs stay comparable.");
    }
    JsonNode actionsNode = root.get("actions");
    if (!actionsNode.isArray()) {
      throw new ContractException(ErrorCategory.MALFORMED_INPUT, "$.actions must be an array.");
    }
    if (actionsNode.isEmpty() || actionsNode.size() > MAX_ACTIONS) {
      throw new ContractException(
          ErrorCategory.MALFORMED_INPUT,
          "$.actions has "
              + actionsNode.size()
              + " entries; allowed range is 1.."
              + MAX_ACTIONS
              + ".");
    }
    List<Journey.Action> actions = new ArrayList<>();
    for (int i = 0; i < actionsNode.size(); i++) {
      actions.add(parseAction(actionsNode.get(i), i));
    }
    long distinct = actions.stream().map(Journey.Action::id).distinct().count();
    if (distinct != actions.size()) {
      throw new ContractException(
          ErrorCategory.MALFORMED_INPUT, "action ids must be unique within the journey.");
    }
    return new Journey(schemaVersion, name, seed, clock, actions);
  }

  private Journey.Action parseAction(JsonNode node, int index) {
    String at = "$.actions[" + index + "]";
    if (!node.isObject()) {
      throw new ContractException(ErrorCategory.MALFORMED_INPUT, at + " must be an object.");
    }
    requireFields((ObjectNode) node, ACTION_FIELDS, at);
    String id = identifier(text(node, "id", at), at + ".id");
    String name = text(node, "name", at);
    if (!ACTIONS.contains(name)) {
      throw new ContractException(
          ErrorCategory.UNKNOWN_ACTION,
          "action '"
              + name
              + "' at "
              + at
              + ".name is not in "
              + ACTIONS
              + ". Remove the step or use one of the allowed names.");
    }
    JsonNode inputNode = node.get("input");
    if (!inputNode.isObject()) {
      throw new ContractException(ErrorCategory.MALFORMED_INPUT, at + ".input must be an object.");
    }
    requireFields((ObjectNode) inputNode, INPUT_FIELDS, at + ".input");
    String orderId = text(inputNode, "orderId", at + ".input");
    if (!ORDER_ID.matcher(orderId).matches()) {
      throw new ContractException(
          ErrorCategory.MALFORMED_INPUT,
          at + ".input.orderId '" + orderId + "' must match O-<digits>, for example O-1.");
    }
    Map<String, String> input = new LinkedHashMap<>();
    input.put("orderId", orderId);
    return new Journey.Action(id, name, input);
  }

  private JsonNode readTree(byte[] utf8) {
    try {
      return mapper.readTree(utf8);
    } catch (Exception ex) {
      String detail = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
      throw new ContractException(
          ErrorCategory.MALFORMED_INPUT,
          "fixture is not valid JSON (" + detail + "). Fix the syntax and retry.",
          ex);
    }
  }

  private void requireFields(ObjectNode node, Set<String> allowed, String at) {
    Iterator<String> names = node.fieldNames();
    while (names.hasNext()) {
      String field = names.next();
      if (!allowed.contains(field)) {
        throw new ContractException(
            ErrorCategory.MALFORMED_INPUT,
            "unknown field '"
                + field
                + "' at "
                + at
                + ". Allowed fields: "
                + allowed
                + ".");
      }
    }
    for (String field : allowed) {
      if (!node.has(field)) {
        throw new ContractException(
            ErrorCategory.MALFORMED_INPUT, "missing field '" + field + "' at " + at + ".");
      }
    }
  }

  private String text(JsonNode parent, String field, String at) {
    JsonNode node = parent.get(field);
    if (node == null || !node.isTextual()) {
      throw new ContractException(
          ErrorCategory.MALFORMED_INPUT, at + "." + field + " must be a JSON string.");
    }
    String value = node.textValue();
    if (value.isBlank() || value.length() > MAX_STRING) {
      throw new ContractException(
          ErrorCategory.MALFORMED_INPUT,
          at + "." + field + " must be 1.." + MAX_STRING + " characters.");
    }
    if (value.contains("://")) {
      throw new ContractException(
          ErrorCategory.EXTERNAL_SINK,
          at
              + "."
              + field
              + " contains a URL. Journeys cannot name an external endpoint or sink.");
    }
    return value;
  }

  private String identifier(String value, String at) {
    if (!NAME.matcher(value).matches()) {
      throw new ContractException(
          ErrorCategory.MALFORMED_INPUT,
          at + " '" + value + "' must match " + NAME.pattern() + ".");
    }
    return value;
  }
}
