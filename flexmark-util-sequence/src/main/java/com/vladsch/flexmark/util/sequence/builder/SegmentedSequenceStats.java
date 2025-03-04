package com.vladsch.flexmark.util.sequence.builder;

import com.vladsch.flexmark.util.misc.MinMaxAvgLong;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SegmentedSequenceStats {
  private static class StatsEntry implements Comparable<StatsEntry> {
    private int segments;
    private int count;
    private final MinMaxAvgLong segStats = new MinMaxAvgLong();
    private final MinMaxAvgLong length = new MinMaxAvgLong();
    private final MinMaxAvgLong overhead = new MinMaxAvgLong();

    StatsEntry(int segments) {
      this.segments = segments;
    }

    void add(int segments, int length, int overhead) {
      count++;
      this.segStats.add(segments);
      this.length.add(length);
      this.overhead.add(overhead);
    }

    void add(StatsEntry other) {
      count += other.count;
      this.segStats.add(other.segStats);
      this.length.add(other.length);
      this.overhead.add(other.overhead);
    }

    @Override
    public int compareTo(SegmentedSequenceStats.StatsEntry o) {
      int segs = Integer.compare(segments, o.segments);
      if (segs != 0) {
        return segs;
      }
      return Integer.compare(count, o.count);
    }

    @Override
    public boolean equals(Object object) {
      if (this == object) {
        return true;
      }
      if (object == null || getClass() != object.getClass()) {
        return false;
      }
      return segments == ((StatsEntry) object).segments;
    }

    @Override
    public int hashCode() {
      return segments;
    }
  }

  private List<SegmentedSequenceStats.StatsEntry> aggregatedStats;
  private final Map<StatsEntry, StatsEntry> stats = new HashMap<>();

  private SegmentedSequenceStats() {}

  public void addStats(int segments, int length, int overhead) {
    StatsEntry entry = new StatsEntry(segments);
    entry = stats.computeIfAbsent(entry, k -> k);
    entry.add(segments, length, overhead);
  }

  private String getStatsText(List<StatsEntry> entries) {
    StringBuilder out = new StringBuilder();
    int iMax = entries.size();

    out.append(
            String.format(
                "%10s,%10s,%10s,%10s,%10s,%10s,%10s,%10s,%10s,%10s,%10s,%10s,%10s,%8s",
                "count", "min-seg", "avg-seg", "max-seg", "min-len", "avg-len", "max-len",
                "min-ovr", "avg-ovr", "max-ovr", "tot-len", "tot-chr", "tot-ovr", "ovr %"))
        .append("\n");

    for (int i = iMax; i-- > 0; ) {
      StatsEntry entry = entries.get(i);
      out.append(
              String.format(
                  "%10d,%10d,%10d,%10d,%10d,%10d,%10d,%10d,%10d,%10d,%10d,%10d,%10d,%8.3f",
                  entry.count,
                  entry.count == 1 ? entry.segments : entry.segStats.getMin(),
                  entry.count == 1 ? entry.segments : entry.segStats.getAvg(entry.count),
                  entry.count == 1 ? entry.segments : entry.segStats.getMax(),
                  entry.length.getMin(),
                  entry.length.getAvg(entry.count),
                  entry.length.getMax(),
                  entry.overhead.getMin(),
                  entry.overhead.getAvg(entry.count),
                  entry.overhead.getMax(),
                  entry.length.getTotal(),
                  entry.length.getTotal() * 2,
                  entry.overhead.getTotal(),
                  entry.length.getTotal() == 0
                      ? 0
                      : 100.0 * entry.overhead.getTotal() / entry.length.getTotal() / 2.0))
          .append("\n");
    }
    return out.toString();
  }

  public String getAggregatedStatsText() {
    return getStatsText(getAggregatedStats());
  }

  private static final List<Integer> AGGR_STEPS = new ArrayList<>();

  static {
    AGGR_STEPS.add(1);
    AGGR_STEPS.add(2);
    AGGR_STEPS.add(3);
    AGGR_STEPS.add(4);
    AGGR_STEPS.add(5);
    AGGR_STEPS.add(6);
    AGGR_STEPS.add(7);
    AGGR_STEPS.add(8);
    AGGR_STEPS.add(15);
    AGGR_STEPS.add(16);
    AGGR_STEPS.add(256);

    int step = 65536;
    int start = 65536;
    int nextStep = 65536 * 16;
    for (int i = start; i < nextStep; i += step) AGGR_STEPS.add(i);
  }

  private static final int MAX_BUCKETS = AGGR_STEPS.size();

  public List<StatsEntry> getAggregatedStats() {
    if (aggregatedStats == null) {
      List<StatsEntry> entries = getStats();
      aggregatedStats = new ArrayList<>(MAX_BUCKETS);

      int currentBucket = MAX_BUCKETS - 1;
      int currentBucketSegments = AGGR_STEPS.get(currentBucket);

      int iMax = entries.size();
      for (int i = 0; i < MAX_BUCKETS; i++) {
        aggregatedStats.add(null);
      }

      for (int i = iMax; i-- > 0; ) {
        StatsEntry entry = entries.get(i);
        if (entry.segments < currentBucketSegments) {
          // find the next bucket to hold this entry
          while (currentBucket > 0) {
            currentBucket--;
            currentBucketSegments = AGGR_STEPS.get(currentBucket);
            if (entry.segments >= currentBucketSegments) {
              break;
            }
          }
        }

        StatsEntry aggrEntry = aggregatedStats.get(currentBucket);

        if (aggrEntry == null) {
          aggrEntry = new StatsEntry(currentBucketSegments);
          aggregatedStats.set(currentBucket, aggrEntry);
        }

        aggrEntry.add(entry);
      }

      aggregatedStats.removeIf(Objects::isNull);
    }

    return aggregatedStats;
  }

  public String getStatsText() {
    List<StatsEntry> entries = getStats();
    return getStatsText(entries);
  }

  public List<StatsEntry> getStats() {
    List<StatsEntry> entries = new ArrayList<>(stats.keySet());

    entries.sort(StatsEntry::compareTo);
    return entries;
  }

  static SegmentedSequenceStats getInstance() {
    return new SegmentedSequenceStats();
  }
}
