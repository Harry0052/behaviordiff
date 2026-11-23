package dev.behaviordiff.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class JourneyValidatorTest {
  private final JourneyValidator validator = new JourneyValidator();

  @Test
  void acceptsTheLabeledValidFixture() throws Exception {
    byte[] bytes = Files.readAllBytes(Path.of("fixtures/journeys/cancel-o1.valid.json"));
    Journey journey = validator.parse(bytes);
    assertEquals("cancel-o1", journey.name());
    assertEquals(JourneyValidator.SEED, journey.seed());
    assertEquals(JourneyValidator.CLOCK, journey.clock());
    assertEquals(1, journey.actions().size());
    assertEquals("cancel-order", journey.actions().get(0).id());
    assertEquals("cancel_order", journey.actions().get(0).name());
    assertEquals("O-1", journey.actions().get(0).input().get("orderId"));
  }

  @Test
  void rejectsUnknownActionWithTheActionName() throws Exception {
    byte[] bytes = Files.readAllBytes(Path.of("fixtures/journeys/unknown-action.invalid.json"));
    ContractException ex = assertThrows(ContractException.class, () -> validator.parse(bytes));
    assertEquals(ErrorCategory.UNKNOWN_ACTION, ex.category());
    assertEquals(true, ex.getMessage().contains("teleport_order"));
    assertEquals(true, ex.getMessage().contains("cancel_order"));
  }

  @Test
  void rejectsDuplicateKeys() {
    String document =
        """
        {"schemaVersion":"1","schemaVersion":"1","name":"cancel-o1",
         "seed":"seed-cancel-o1","clock":"2026-01-15T12:00:00Z","actions":[]}
        """;
    ContractException ex =
        assertThrows(
            ContractException.class,
            () -> validator.parse(document.getBytes(StandardCharsets.UTF_8)));
    assertEquals(ErrorCategory.MALFORMED_INPUT, ex.category());
  }

  @Test
  void rejectsMalformedJson() throws Exception {
    byte[] bytes = Files.readAllBytes(Path.of("fixtures/journeys/malformed.invalid.json"));
    ContractException ex = assertThrows(ContractException.class, () -> validator.parse(bytes));
    assertEquals(ErrorCategory.MALFORMED_INPUT, ex.category());
  }

  @Test
  void rejectsOversizedBytesBeforeTrustingTheDocument() {
    byte[] bytes = new byte[JourneyValidator.MAX_BYTES + 1];
    ContractException ex = assertThrows(ContractException.class, () -> validator.parse(bytes));
    assertEquals(ErrorCategory.OVERSIZED_INPUT, ex.category());
  }

  @Test
  void rejectsAnExternalUrlInsideTheFixture() {
    String document =
        """
        {"schemaVersion":"1","name":"cancel-o1","seed":"seed-cancel-o1",
         "clock":"2026-01-15T12:00:00Z","actions":[{"id":"cancel-order",
         "name":"cancel_order","input":{"orderId":"https://carrier.example/O-1"}}]}
        """;
    ContractException ex =
        assertThrows(
            ContractException.class,
            () -> validator.parse(document.getBytes(StandardCharsets.UTF_8)));
    assertEquals(ErrorCategory.EXTERNAL_SINK, ex.category());
  }
}
