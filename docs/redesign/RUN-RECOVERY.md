# Interrupted-run recovery

## Connected (2026-09-25, PR #22)

Items 3 and 4 of the requirements below are now implemented in the service and root UI: periodic
RECORDING writes, the SETTLING boundary before settlement (required — settlement stops if it cannot
be written), a start gate while an unresolved checkpoint exists, Resume/Finish on reopen, automatic
re-save of an interrupted settlement, other-account runs saved only under their original owner, and
clearing the matching checkpoint after completion. The checkpoint (format 2) also keeps the
fake-location flag and the party crew. Native tests: RunCrashRecoveryTest and
RunCheckpointPersistenceTest in the CI interaction suite. Still not run: physical process
termination at every write boundary, reboot, and background GPS behavior on a device.


## Original production behavior (before Room 14)

`WalkSessionService` retains the active state only in memory and returns START_NOT_STICKY.
`stopSession` currently raises the accounted-step preference, calls `RewardRepository.settleSession`
(ledger insert, notification insert and energy preference write), then inserts the walk-session row.
These operations are not one transaction. Replaying this sequence after interruption can duplicate
credit or energy consumption; merely enabling service restart would not solve recovery.

## Room 14 settlement boundary (implemented, native validation pending)

The service now delegates the core local settlement to RunSettlementRepository. One Room transaction
inserts the activity, local ledger entry, notification and a receipt keyed by recordingOwner/startedAt.
Replaying that identity returns the saved result without recalculation or a second insert. A receipt
failure rolls all four tables back together. This receipt does not certify server-confirmed SUP.

Energy remains in the existing preference store. A run receipt ID and debit are applied in one
DataStore edit, then acknowledged in Room. A failed acknowledgement can be replayed without another
debit; older-day receipts do not consume a new day's refill. Pending energy is reconciled before the
next run calculation and before background settlement. Added native failure-injection, concurrent
finish and database/preferences reopening tests. Execution is pending.

Still outside this boundary: accounted-step advancement before settlement, course/faction/party
follow-up operations, background settlement itself, shared account/equipment state, storage-error UI,
startup recovery, receipt retention/compaction and physical process-termination tests. The service
checkpoint is not enabled until these recovery paths can represent their actual status safely.

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

1. Validate the new Room 14 migration and settlement/energy receipt boundary against actual native
   failures and reopening. Extend durable completion to the remaining follow-up effects and the
   accounted-step baseline before claiming the entire finish flow is recoverable.
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

## Save failure presentation (native validation pending)

Stopping now freezes sensor/timer state before settlement and publishes SAVING. Core failures leave
the same active identity paused as FAILED, expose Retry saving, and prevent Resume/manual mutation
of a potentially settled record. A same-process retry uses the Room receipt and does not repeat
already-attempted legacy course/faction follow-ups if upload scheduling fails. This attempt marker
is not durable, and those follow-ups still use their existing best-effort behavior; neither is proof
of complete course/party recovery. Four viewport/theme/font fixtures exercise saving/failed layouts
and disabled/enabled controls, not actual service failure injection. Real service errors/process
termination still require integration evidence.
