
package com.orasi;

import com.orasi.data.Randomizer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Factory class for creating named synthetic data generator algorithms
 * 
 */
public class SyntheticFactory {

  private static final SyntheticFactory singleton = new SyntheticFactory();

  private SyntheticFactory() {
    registerDataProvider("Number", () -> {
      return Integer.toString(Randomizer.instance().randomInt(0, Integer.MAX_VALUE));
    });

    registerDataProvider("First Name", () -> {
      return Randomizer.instance().randomFirstName();
    });

    registerDataProvider("Last Name", () -> {
      return Randomizer.instance().randomLastName();
    });

    registerDataProvider("State", () -> {
      return Randomizer.instance().randomState();
    });

    registerDataProvider("City", () -> {
      return Randomizer.instance().randomCityName();
    });

    registerDataProvider("Street", () -> {
      return Randomizer.instance().randomStreetName();
    });

    registerDataProvider("Domain", () -> {
      return Randomizer.instance().randomDomain();
    });

    registerDataProvider("Adjective", () -> {
      return Randomizer.instance().randomAdjective();
    });

    registerDataProvider("Adverb", () -> {
      return Randomizer.instance().randomAdverb();
    });

    registerDataProvider("Verb", () -> {
      return Randomizer.instance().randomVerb();
    });

    registerDataProvider("Noun", () -> {
      return Randomizer.instance().randomNouns();
    });

    registerDataProvider("Email Address", () -> {
      return Randomizer.instance().randomEmailAddress();
    });
  }

  /**
   * A handle to the singleton instance
   * @return
   */
  public static final SyntheticFactory instance() {
    return singleton;
  }

  private final Map<String, SyntheticDataProvider> providerMap = new HashMap<>(10);

  /**
   * Allows the use to register a custom Synthetic algorithm provider
   * @param name
   * @param dataProvider
   */
  public final void registerDataProvider(String name, SyntheticDataProvider dataProvider) {
    providerMap.put(name, dataProvider);
  }

  /**
   * Gets a listing of the names of the registered providers
   */
  public Set<String> getNames() {
    return providerMap.keySet();
  }

  /**
   * Gets a provider algorithm based off of the name
   * @param name The name of the provider
   * @return A provider algorithm
   */
  public SyntheticDataProvider getProvider(String name) {
    return providerMap.get(name);
  }
}
