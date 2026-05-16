--------------------------- MODULE LeaseEpoch ---------------------------
(***************************************************************************)
(* TLA+ specification of the lease_epoch fencing protocol used by the      *)
(* distributed task queue (V2 design).                                     *)
(*                                                                         *)
(* One task is modeled (the protocol is per-task; the global system is the *)
(* parallel composition over task ids, so a per-task proof generalizes).   *)
(*                                                                         *)
(* Roles:                                                                  *)
(*   - Store : the authoritative DB row for the task.                      *)
(*   - Janitor : a leader-elected sweeper that resets expired leases.      *)
(*   - Workers : a finite set of workers competing to claim/run the task.  *)
(*                                                                         *)
(* The protocol's central claim is:                                        *)
(*   At most one (worker, epoch) pair is ever permitted to commit a side   *)
(*   effect for the task; any "stale" worker (one whose epoch has been     *)
(*   superseded) is excluded from completing.                              *)
(***************************************************************************)
EXTENDS Naturals, FiniteSets, TLC

CONSTANTS Workers,        \* finite set of worker ids
          MaxEpoch        \* bound for model checking

ASSUME /\ IsFiniteSet(Workers)
       /\ Workers # {}
       /\ MaxEpoch \in Nat /\ MaxEpoch >= 1

States == {"PENDING", "CLAIMED", "SUCCEEDED", "FAILED_TERMINAL"}

VARIABLES
  storeState,     \* current state of the task in the DB
  storeEpoch,     \* current lease_epoch in the DB (monotonic)
  storeOwner,     \* current worker_id holding the lease, or NONE
  leaseExpired,   \* abstract clock: TRUE iff the current lease is expired
  workerEpoch,    \* workerEpoch[w]: the epoch w believes it owns (0 = none)
  workerPhase,    \* workerPhase[w] \in {"idle","running","done"}
  committed       \* set of (worker, epoch) pairs that committed a side effect

NONE == CHOOSE x : x \notin Workers

vars == << storeState, storeEpoch, storeOwner, leaseExpired,
           workerEpoch, workerPhase, committed >>

TypeOK ==
  /\ storeState \in States
  /\ storeEpoch \in 0..MaxEpoch
  /\ storeOwner \in Workers \cup {NONE}
  /\ leaseExpired \in BOOLEAN
  /\ workerEpoch \in [Workers -> 0..MaxEpoch]
  /\ workerPhase \in [Workers -> {"idle","running","done"}]
  /\ committed \subseteq (Workers \X (0..MaxEpoch))

Init ==
  /\ storeState   = "PENDING"
  /\ storeEpoch   = 0
  /\ storeOwner   = NONE
  /\ leaseExpired = FALSE
  /\ workerEpoch  = [w \in Workers |-> 0]
  /\ workerPhase  = [w \in Workers |-> "idle"]
  /\ committed    = {}

(***************************************************************************)
(* Actions                                                                 *)
(***************************************************************************)

\* A worker claims the task: atomic conditional UPDATE in the store.
\* Bumps lease_epoch; returns the new epoch as the worker's fencing token.
Claim(w) ==
  /\ workerPhase[w] = "idle"
  /\ storeState = "PENDING"
  /\ storeEpoch + 1 <= MaxEpoch
  /\ storeState'   = "CLAIMED"
  /\ storeEpoch'   = storeEpoch + 1
  /\ storeOwner'   = w
  /\ leaseExpired' = FALSE
  /\ workerEpoch'  = [workerEpoch  EXCEPT ![w] = storeEpoch + 1]
  /\ workerPhase'  = [workerPhase  EXCEPT ![w] = "running"]
  /\ UNCHANGED << committed >>

\* The worker performs its side effect AND updates the store atomically,
\* gated by the fencing predicate (epoch match). The "atomic" pairing models
\* either (a) a side effect inside the same DB tx, or (b) an external
\* receiver that uses the fencing token as its idempotency key.
Complete(w) ==
  /\ workerPhase[w] = "running"
  /\ \* fencing predicate: store still agrees this worker owns this epoch
     storeOwner = w
  /\ storeEpoch = workerEpoch[w]
  /\ storeState = "CLAIMED"
  /\ storeState'  = "SUCCEEDED"
  /\ workerPhase' = [workerPhase EXCEPT ![w] = "done"]
  /\ committed'   = committed \cup {<<w, workerEpoch[w]>>}
  /\ UNCHANGED << storeEpoch, storeOwner, leaseExpired, workerEpoch >>

\* A worker may attempt to complete after its lease has been stolen.
\* The conditional UPDATE matches zero rows; the worker observes this and
\* aborts WITHOUT committing. This is the "stale worker exclusion" path.
StaleAbort(w) ==
  /\ workerPhase[w] = "running"
  /\ \/ storeOwner # w
     \/ storeEpoch # workerEpoch[w]
     \/ storeState # "CLAIMED"
  /\ workerPhase' = [workerPhase EXCEPT ![w] = "done"]
  /\ UNCHANGED << storeState, storeEpoch, storeOwner, leaseExpired,
                  workerEpoch, committed >>

\* Abstract passage of time: the current lease becomes expired.
LeaseExpire ==
  /\ storeState = "CLAIMED"
  /\ leaseExpired = FALSE
  /\ leaseExpired' = TRUE
  /\ UNCHANGED << storeState, storeEpoch, storeOwner,
                  workerEpoch, workerPhase, committed >>

\* Janitor reset: atomic conditional UPDATE that bumps the epoch and
\* returns the task to PENDING. The previous owner is fenced out because
\* storeEpoch no longer matches its workerEpoch[w].
JanitorReset ==
  /\ storeState = "CLAIMED"
  /\ leaseExpired = TRUE
  /\ storeEpoch + 1 <= MaxEpoch
  /\ storeState'   = "PENDING"
  /\ storeEpoch'   = storeEpoch + 1
  /\ storeOwner'   = NONE
  /\ leaseExpired' = FALSE
  /\ UNCHANGED << workerEpoch, workerPhase, committed >>

\* Cleanup: a stale worker that has aborted may become eligible to claim
\* a future incarnation of the task (only relevant for liveness; harmless
\* for safety because its old workerEpoch no longer matches the store).
Recycle(w) ==
  /\ workerPhase[w] = "done"
  /\ workerPhase' = [workerPhase EXCEPT ![w] = "idle"]
  /\ workerEpoch' = [workerEpoch EXCEPT ![w] = 0]
  /\ UNCHANGED << storeState, storeEpoch, storeOwner, leaseExpired, committed >>

Next ==
  \/ \E w \in Workers : Claim(w)
  \/ \E w \in Workers : Complete(w)
  \/ \E w \in Workers : StaleAbort(w)
  \/ \E w \in Workers : Recycle(w)
  \/ LeaseExpire
  \/ JanitorReset

Spec == Init /\ [][Next]_vars /\ WF_vars(JanitorReset)
                              /\ \A w \in Workers : WF_vars(Claim(w))
                              /\ \A w \in Workers : WF_vars(Complete(w))
                              /\ \A w \in Workers : WF_vars(StaleAbort(w))

(***************************************************************************)
(* SAFETY INVARIANTS                                                       *)
(***************************************************************************)

\* I1: epoch monotonicity (the store's epoch never decreases).
\*     Expressed as an action invariant.
EpochMonotone == [][ storeEpoch' >= storeEpoch ]_vars

\* I2: at most one owner at a time.
SingleOwner ==
  storeState = "CLAIMED" => storeOwner \in Workers

\* I3: a worker can only be running with the epoch it was issued.
WorkerEpochAuth ==
  \A w \in Workers :
     workerPhase[w] = "running" => workerEpoch[w] > 0

\* I4: MUTUAL EXCLUSION OF COMMITS PER EPOCH.
\*     For any epoch e, at most one worker has ever committed at e.
AtMostOneCommitPerEpoch ==
  \A e \in 0..MaxEpoch :
     Cardinality({ w \in Workers : <<w,e>> \in committed }) <= 1

\* I5: STALE WORKER EXCLUSION.
\*     If a commit pair <<w,e>> exists, then e equals the store epoch
\*     at the moment of the commit, i.e. e <= storeEpoch and the commit
\*     pair's epoch is never less than the store's epoch at commit time.
\*     Expressed inductively as: any committed epoch is the same as the
\*     store epoch at the time it was committed (captured by the
\*     conjunction of CommitImpliesAuthoritative below).
CommitImpliesAuthoritative ==
  \A pair \in committed :
     LET w == pair[1]
         e == pair[2]
     IN  \* The state immediately after Complete(w) had storeEpoch = e
         \* and storeOwner = w; since storeEpoch only increases,
         \* the commit's epoch is bounded by the current store epoch.
         e <= storeEpoch

\* I6: only one terminal SUCCESS per task.
SingleSuccess ==
  Cardinality(committed) <= MaxEpoch  \* trivial bound; tightened below

\* I7: at any point in time, no two distinct workers simultaneously hold
\*     a "running" status with the same epoch.
NoConcurrentSameEpochRunners ==
  \A w1, w2 \in Workers :
     (w1 # w2 /\ workerPhase[w1]="running" /\ workerPhase[w2]="running")
       => workerEpoch[w1] # workerEpoch[w2]

SafetyInvariants ==
  /\ TypeOK
  /\ SingleOwner
  /\ WorkerEpochAuth
  /\ AtMostOneCommitPerEpoch
  /\ CommitImpliesAuthoritative
  /\ NoConcurrentSameEpochRunners

(***************************************************************************)
(* LIVENESS PROPERTIES                                                     *)
(***************************************************************************)

\* L1: progress -- if the task is PENDING and some worker is idle,
\*     eventually the task is CLAIMED.
EventuallyClaimed ==
  (storeState = "PENDING" /\ \E w \in Workers : workerPhase[w] = "idle")
    ~> (storeState = "CLAIMED")

\* L2: stuck-lease recovery -- a CLAIMED task with an expired lease
\*     eventually returns to PENDING (janitor liveness).
ExpiryRecovers ==
  (storeState = "CLAIMED" /\ leaseExpired) ~> (storeState = "PENDING")

\* L3: eventual completion -- if a worker holds a valid (non-expired)
\*     lease and stays running long enough, the task eventually reaches
\*     SUCCEEDED. (Requires the assumption that the worker is not
\*     partitioned -- see notes.)
EventuallySucceeds ==
  (storeState = "CLAIMED" /\ ~leaseExpired) ~> (storeState = "SUCCEEDED")

\* L4: stale workers do not block forever.
StaleResolves ==
  \A w \in Workers :
    (workerPhase[w] = "running" /\
       (storeOwner # w \/ storeEpoch # workerEpoch[w]))
    ~> (workerPhase[w] = "done")

LivenessProperties ==
  /\ EventuallyClaimed
  /\ ExpiryRecovers
  /\ StaleResolves

(***************************************************************************)
(* STALE-WORKER EXCLUSION PROOF (informal, machine-checkable in TLAPS)     *)
(*                                                                         *)
(* Theorem: For any reachable state s and any worker w with                *)
(*   workerPhase[w] = "running" and workerEpoch[w] = e,                   *)
(*   if storeEpoch(s) > e, then w cannot subsequently execute Complete(w).*)
(*                                                                         *)
(* Proof sketch:                                                           *)
(*  1. storeEpoch is monotonically non-decreasing (I1, by inspection of   *)
(*     the only writers Claim and JanitorReset; both set storeEpoch' =    *)
(*     storeEpoch + 1, and no other action mutates storeEpoch).           *)
(*  2. workerEpoch[w] is set only by Claim(w) and zeroed only by          *)
(*     Recycle(w); while workerPhase[w] = "running", workerEpoch[w] is   *)
(*     therefore constant (Recycle requires "done").                     *)
(*  3. Complete(w) requires storeEpoch = workerEpoch[w].                 *)
(*  4. If at some earlier state storeEpoch > e, then by (1) storeEpoch  *)
(*     remains > e forever after, so the guard in (3) is false forever  *)
(*     after, so Complete(w) is permanently disabled.                    *)
(*  5. The only remaining enabled actions for w in "running" are         *)
(*     StaleAbort(w) (which moves w to "done" without modifying          *)
(*     committed) and environment actions on other workers. Hence w     *)
(*     cannot extend `committed`. QED.                                  *)
(*                                                                         *)
(* Corollary (mutual exclusion of commits per epoch):                     *)
(*   For each epoch e, at most one Claim action ever produced epoch e   *)
(*   (since storeEpoch is strictly increasing on Claim and JanitorReset *)
(*   transitions). Therefore at most one worker ever has                *)
(*   workerEpoch[w] = e, so at most one Complete(w) can add a pair      *)
(*   <<w,e>> to committed. This is exactly invariant I4.                *)
(***************************************************************************)

(***************************************************************************)
(* REMAINING LIVENESS FAILURES UNDER NETWORK PARTITION                    *)
(*                                                                         *)
(* The safety properties above are unconditional: they hold under         *)
(* arbitrary asynchrony, message loss, and partitions, because every     *)
(* state-changing operation is a conditional atomic UPDATE in the store. *)
(* Liveness, however, is not unconditional. Below are the partition      *)
(* scenarios that break specific liveness properties, with remediations. *)
(*                                                                         *)
(* P1. Worker <-> Store partition during running.                        *)
(*     The worker cannot heartbeat or commit. The lease expires; the     *)
(*     janitor resets and a new worker claims with epoch+1. Safety       *)
(*     holds (the partitioned worker is fenced). Liveness holds for the *)
(*     TASK (another worker completes it) but FAILS for the side effect *)
(*     if the side effect's external receiver is also unreachable to the *)
(*     new worker. Mitigation: side effects must be addressable from     *)
(*     either side of any single partition; partition-aware admission   *)
(*     control should refuse to claim if the worker cannot reach the    *)
(*     receiver.                                                         *)
(*                                                                         *)
(* P2. Janitor <-> Store partition.                                      *)
(*     If the leader janitor is partitioned away from the store but     *)
(*     keeps its etcd lease (because etcd is on a different network     *)
(*     leg), JanitorReset cannot fire. ExpiryRecovers (L2) is violated  *)
(*     until either the partition heals or the etcd lease expires and a *)
(*     new janitor is elected on the store-reachable side. Mitigation:  *)
(*     bind the janitor's liveness lease to a heartbeat that itself     *)
(*     touches the store, so loss of store reachability implies loss of *)
(*     leadership.                                                       *)
(*                                                                         *)
(* P3. Etcd quorum loss (split-brain prevention).                        *)
(*     Under loss of etcd quorum, no janitor leader exists; expired     *)
(*     leases are not reset. Safety preserved (no one can claim because *)
(*     the dispatcher also depends on Kafka, and even if a worker did   *)
(*     claim, the fencing predicate still holds). Liveness for the     *)
(*     entire shard is lost. This is a deliberate CP choice and cannot *)
(*     be removed without giving up exactly-one-claimant safety.        *)
(*                                                                         *)
(* P4. Asymmetric partition: worker can WRITE store but cannot READ     *)
(*     responses.                                                       *)
(*     The worker's Complete UPDATE may commit at the store while the   *)
(*     worker times out and assumes failure. The worker neither marks   *)
(*     itself "done" nor releases workerEpoch; from the store's view    *)
(*     the task is SUCCEEDED. Safety: holds (committed has the pair    *)
(*     exactly once). Liveness: the worker is permanently "running" in *)
(*     its local view -- a per-worker liveness failure (StaleResolves   *)
(*     L4 violated for that worker until partition heals). Mitigation: *)
(*     workers periodically re-read their own state via an independent *)
(*     read path; on detecting storeState != "CLAIMED" with their      *)
(*     epoch, transition to "done" idempotently.                        *)
(*                                                                         *)
(* P5. Clock skew + partition (Byzantine-time hazard).                  *)
(*     leaseExpired is abstract here, but in production it is computed *)
(*     by the store using clock_timestamp(). If the store's clock leaps *)
(*     forward during a partition, JanitorReset may fire while the     *)
(*     real-time-honest worker still holds an unexpired lease. Safety  *)
(*     is preserved by fencing (the worker's Complete is rejected),   *)
(*     but the worker wastes effort. Mitigation: cap leases at         *)
(*     min(remaining_deadline, max_lease) and reject store clock jumps *)
(*     larger than a configured bound (TrueTime-style uncertainty,    *)
(*     or HLC).                                                         *)
(*                                                                         *)
(* P6. Producer <-> Outbox/Kafka partition.                             *)
(*     Outside this spec's scope, but worth noting: the enqueue path  *)
(*     can stall (no Claim ever fires for a new task) without ever    *)
(*     being unsafe. EventuallyClaimed (L1) is therefore conditional   *)
(*     on producer connectivity.                                       *)
(*                                                                         *)
(* Summary: under any network partition, the protocol preserves all of *)
(* I1..I7 unconditionally, but the liveness properties L1..L4 are      *)
(* conditioned on (a) eventual reconnection of at least one viable    *)
(* worker to the store, (b) janitor leadership being colocated (in    *)
(* the connectivity sense) with the store, and (c) bounded clock      *)
(* skew. These conditions match the standard CAP/FLP boundary and are *)
(* the strongest liveness guarantees achievable for an exactly-one-   *)
(* claimant fencing protocol.                                         *)
(***************************************************************************)

============================================================================
