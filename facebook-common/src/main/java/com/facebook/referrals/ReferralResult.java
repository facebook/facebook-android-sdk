package com.facebook.referrals;

import java.util.Collections;
import java.util.List;

/** This class shows the results of a referral operation. */
public class ReferralResult {
  private final List<String> referralCodes;

  /**
   * The constructor.
   *
   * @param referralCodes The referral codes.
   */
  public ReferralResult(List<String> referralCodes) {
    this.referralCodes = referralCodes;
  }

  /**
   * Getter for the referral codes.
   *
   * @return the referral codes.
   */
  public List<String> getReferralCodes() {
    return Collections.unmodifiableList(referralCodes);
  }
}
