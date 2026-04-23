---
name: FSH Failure Validation
description: Determine whether a suspicious test run reported by Sieve corresponds
  to a real fail-slow hardware bug by analyzing checker reports, system logs,
  and system design documentation.
---

# FSH Failure Validation

## 1. Purpose

This skill guides an agent to determine whether a suspicious test run reported by Sieve
corresponds to a real fail-slow hardware (FSH) bug.

The goal of this skill is to **reduce manual inspection effort** by systematically analyzing
system logs，checker reports，and system design documentation,
while remaining conservative when evidence is insufficient.

The agent **must not attempt to fix bugs or speculate beyond available evidence**.

---

## 2. What Is an FSH Failure?

An FSH failure is a system failure **triggered by fine-grained fail-slow hardware faults**,
such as slow disks or slow NICs,
where hardware components remain functional but operate with abnormally degraded performance.

Typical FSH failures:
- Are not caused by fail-stop crashes or network partitions
- Often escape coarse-grained fault detectors, such as heartbeats
- May manifest as gray failures, or silent data corruption

---

## 3. FSH Failure Symptoms

The agent must reason based on the following **FSH failure symptoms**.
Each symptom is defined in detail to avoid ambiguity.

### 3.1 Node Service Unavailable

**Definition:**  
A system node remains alive (process running) but is unable to provide expected services.

**Typical indicators:**
- Client requests to the node block indefinitely or timeout
- No explicit crash or exit is observed
- Node is stuck in a specific state without progress

**FSH relevance:**  
Often caused by fail-slow I/O inside synchronized or critical sections,
leading to indefinite blocking while heartbeats or liveness checks still pass.

---

### 3.2 Data Loss

**Definition:**  
Data that should be accessible or retrievable becomes unavailable to clients.

**Typical indicators:**
- Read or write requests fail or hang
- Metadata indicates data exists, but data cannot be served
- No explicit data corruption is observed

**FSH relevance:**  
Fail-slow storage or network interrupts progress without triggering fault-handling logic.

---

### 3.3 Data Inconsistency

**Definition:**  
System state violates consistency guarantees defined by system design.

**Typical indicators:**
- Replicas disagree on metadata or data values
- Index structures point to invalid or inconsistent entries
- Accessing data triggers crashes or deserialization errors

**FSH relevance:**  
Fail-slow I/O reorders execution or overlaps with timeout handlers,
causing partial state updates.

---

### 3.4 Job Failure

**Definition:**  
A system task, operation, or workflow is aborted or fails unexpectedly
without an explicit hardware crash.

**Typical indicators:**
- Distributed jobs terminate prematurely
- Tasks are marked as failed despite partial progress
- Error reports indicate abnormal termination conditions

**FSH relevance:**  
Fail-slow I/O can cause tasks to exceed time bounds or violate implicit assumptions,
leading to unexpected job termination
---

### 3.5 Performance Degradation

**Definition:**  
The system remains functional but exhibits severe performance slowdown
that violates expected service-level objectives.

**Typical indicators:**
- Request latency increases significantly
- Throughput drops under sustained load
- No explicit failure or crash is reported

**FSH relevance:**  
This is the most direct manifestation of fail-slow hardware behavior,
where components remain operational but perform abnormally slowly.

---

### 3.6 Client Stuck

**Definition:**  
Client operations neither complete nor fail within reasonable time bounds.

**Typical indicators:**
- Long-running client requests without progress
- Clients waiting for responses that never arrive

**FSH relevance:**  
Fail-slow hardware delays specific I/O operations while heartbeats or liveness checks still pass.


---

### 3.7 Node Crash

**Definition:**  
A process or node terminates unexpectedly due to runtime errors.

**Typical indicators:**
- JVM crash or fatal error
- Errors such as `MarshallingError` or assertion failures

**FSH relevance:**  
Crashes often result from corrupted or inconsistent in-memory state.

---

## 4. Validation Workflow

The agent must follow this **ordered reasoning workflow**.

### Step 1: Inspect System Logs

- Read all context in logs
- Strictly follow the timeline in logs
- Carefully confirm the configuration in logs
- Identify which node (leader or follower) is affected by the fail-slow hardware.
- Examine logs around the injected fault point
- Identify abnormal delays, blocked operations, or repeated retries
- Check exception stack traces and their consistency across runs

Do **not** assume all logged errors indicate bugs.

---

### Step 2: Analyze Checker Reports

- Read all context in checker reports
- Identify which checker(s) reported the suspicious test run
- Note the execution order of checkers:
  1. Gray failure checker (online)
  2. Data corruption checker (offline, strict oracle)
  3. Log error checker (offline, heuristic)

Data corruption signals are considered strong evidence.

---

### Step 3: Cross-Check with System Design Documentation

- Search for documented fault-tolerance mechanisms related to observed behaviors
- Search for documented exceptions or error conditions that match observed behaviors
- Search for asynchronous options that solve the problem
- Verify whether observed behaviors violate documented system semantics from **cluster-level** and **node-level** perspectives
- Check whether the system is designed to tolerate such behaviors from **cluster-level** and **node-level** perspectives
- Identify whether the failure escapes intended fault-tolerance mechanisms from **cluster-level** and **node-level** perspectives

---

## 5. Criteria for Declaring a Real FSH Bug
There are three-tier criteria for declaring a real FSH bug.
The agent must follow this **ordered declaring workflow**.

### Step 1: A suspicious test run should be labeled as a **TRUE_BUG** only if:

1. The observed failure symptoms match one or more FSH symptoms above, **and**
2. The behavior cannot be explained by:
   - asynchronous options
   - Expected retries
   - Normal timeout handling
   - Designed fault-recovery mechanisms，such as violation of distributed protocols, **or**
3. The symptoms continue to manifest until the fail-slow hardware fault is resolved.
   - Normal timeouts cannot recover the affected node until the fail-slow hardware fault is resolved.

### Step 2: A suspicious test run does not meet these criteria, but it may cause other bugs. It should be labeled as a **UNCERTAIN** if:
1. Functional issues, such as resource leaks.
2. Diagnosability issues, such as poor logging or lack of error reporting.

### Step 3: Otherwise, it should be labeled as a **FALSE_POSITIVE**.

---

## 6. Input Specification

The agent receives the following inputs:

- System logs
- Checker reports
- Relevant system design documentation (URL links)

---

## 7. Output Specification

The agent must return a **structured verdict**:
- TRUE_BUG: A real FSH bug
- FALSE_POSITIVE: Not a real FSH bug
- UNCERTAIN: Insufficient evidence to determine or potential for other issues

```json
{
  "verdict": "TRUE_BUG | FALSE_POSITIVE | UNCERTAIN",
  "justification": "Concise explanation referencing symptoms and evidence"
}