package com.examagent.scheduler;

/** Ordinal order is priority order (HIGH first) - used directly as the Comparable key in PriorityTask. */
public enum BatchJobPriority {
    HIGH,
    NORMAL,
    LOW
}
