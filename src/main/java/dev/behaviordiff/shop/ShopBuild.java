package dev.behaviordiff.shop;

import dev.behaviordiff.contract.Journey;
import java.util.Set;

/** A checked-in build. Implementations must not open sockets. */
public interface ShopBuild {
  String id();

  Set<String> observedTables();

  Set<String> declaredSinks();

  Observation run(Journey journey);
}
