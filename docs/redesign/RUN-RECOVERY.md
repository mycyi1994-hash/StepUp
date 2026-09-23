# Interrupted-run recovery — implementation in progress

## Existing production behavior

`WalkSessionService` retains the active state only in memory and returns START_NOT_STICKY.
`stopSession` currently raises the accounted-step preference, calls `RewardRepository.settleSession`
(ledger insert, notification insert and energy preference write), then inserts the walk-session row.
These operations are not one transaction. Replaying this sequence after interruption can duplicate
credit or energy consumption; merely enabling service restart would not solve recovery.

## Storage boundary implemented, not connected to service yet

`RunCheckpointStore` uses Android AtomicFile and a versioned binary format. It preserves the captured
owner, start time, party size, steps, exercise duration, exact timestamped GPS points, valid/flagged
segment counts, distance/speed, lap records and selected goal. It does not round or simplify evidence.

- Reopening a RECORDING checkpoint produces a paused state, with GPS fix false. Downtime adds no
  exercise time or distance. Resuming must establish fresh sensor/GPS baselines.
- A SETTLING checkpoint cannot be resumed or changed back into RECORDING. It requires reconciliation
  against durable settlement receipts. There is no automatic financial replay in this store.
- Another run/account cannot overwrite or clear unresolved data. Stale/regressive snapshots fail.
- Unreadable/unsupported data is retained and reported as an error, not erased as an empty run.
- The owner must provide one store instance per file; its mutex serializes operations. This is not
  a cross-process/multi-instance writer API.

Three Android persistence tests cover reopen/owner isolation, interrupted AtomicFile output plus
unsupported-file preservation, and a settlement boundary that survives reopening. They are included
in the required interaction suite. Native execution is pending; no real process-kill success claim.

## Remaining integration requirements

1. Introduce a unique durable settlement receipt tied to the recording identity/start. Commit the
   session row, local ledger/notification changes and receipt together in Room. Make energy application
   idempotent across the separate preference store; reconcile pending receipts before new calculation.
2. Capture equipment/crew/course attribution under the correct account. Current-account reads at
   finish and globally shared reward/equipment stores remain unsafe (see ACCOUNT-DATA-AUDIT.md).
3. Connect serialized checkpoint writes at start, periodic updates, pause and the settlement boundary.
   Gate starting a new run while an unresolved checkpoint exists. Surface storage failure instead of
   silently continuing under a persistence promise.
4. On app reopen, show an interrupted recording paused with Resume/Finish. Show a distinct recovery
   state for interrupted settlement; never replay credits solely because an active file exists.
5. Clear only the matching checkpoint after durable completion. Verify process termination before
   and after every write boundary, repeat Finish, account change, reboot, GPS baseline reset and a
   preserved last route/laps. Physical background GPS behavior requires separate device evidence.

The storage layer is a foundation for the requested full recovery, not a replacement for it.
