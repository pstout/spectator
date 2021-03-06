/**
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.spectator.api.histogram;

import com.netflix.spectator.api.Counter;
import com.netflix.spectator.api.DistributionSummary;
import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Measurement;
import com.netflix.spectator.api.Registry;

import java.util.Collections;

/**
 * Distribution summary that buckets the counts to allow for estimating percentiles.
 */
public class PercentileDistributionSummary implements DistributionSummary {

  /**
   * Creates a distribution summary object that can be used for estimating percentiles.
   *
   * @param registry
   *     Registry to use.
   * @param id
   *     Identifier for the metric being registered.
   * @return
   *     Distribution summary that keeps track of counts by buckets that can be used to estimate
   *     the percentiles for the distribution.
   */
  public static PercentileDistributionSummary get(Registry registry, Id id) {
    return new PercentileDistributionSummary(registry, id);
  }

  private final Id id;
  private final DistributionSummary summary;
  private final Counter[] counters;

  /** Create a new instance. */
  PercentileDistributionSummary(Registry registry, Id id) {
    this.id = id;
    this.summary = registry.distributionSummary(id);
    this.counters = new Counter[PercentileBuckets.length()];
    for (int i = 0; i < counters.length; ++i) {
      Id counterId = id.withTag("percentile", String.format("D%04X", i));
      counters[i] = registry.counter(counterId);
    }
  }

  @Override public Id id() {
    return id;
  }

  @Override public Iterable<Measurement> measure() {
    return Collections.emptyList();
  }

  @Override public boolean hasExpired() {
    return summary.hasExpired();
  }

  @Override public void record(long amount) {
    if (amount >= 0L) {
      summary.record(amount);
      counters[PercentileBuckets.indexOf(amount)].increment();
    }
  }

  /**
   * Computes the specified percentile for this distribution summary.
   *
   * @param p
   *     Percentile to compute, value must be {@code 0.0 <= p <= 100.0}.
   * @return
   *     An approximation of the {@code p}`th percentile in seconds.
   */
  public double percentile(double p) {
    long[] counts = new long[PercentileBuckets.length()];
    for (int i = 0; i < counts.length; ++i) {
      counts[i] = counters[i].count();
    }
    return PercentileBuckets.percentile(counts, p);
  }

  @Override public long count() {
    return summary.count();
  }

  @Override public long totalAmount() {
    return summary.totalAmount();
  }
}
