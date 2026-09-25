package org.sopt.buddys.global.common;

public final class NullPairValidator {

  private NullPairValidator() {
  }

  public static boolean isValidPair(Object first, Object second) {
    return (first == null) == (second == null);
  }
}
