# ALGS4 + Jakarta + Rust

One project combining:

- **ALGS4Middleware**: a self-contained Java library of ALGS4-style sorting, selection and priority queues.
- **Jakarta EE backend**: exposes the library over REST and calls a separate Rust microservice.
- **Rust microservice**: Actix-Web service for lightweight compute.

## Files (all in one folder)

```
Cargo.toml               Rust build (binary is main.rs)
main.rs                  Actix-Web service: /add/{a}/{b}, POST /sort, POST /select
pom.xml                  WAR build: jakarta-rust-integration
ALGS4Middleware.java     sorting, selection, priority queues + Engine switch (JAVA | RUST)
RustBackend.java         HTTP client the Middleware uses to call the Rust service
ComputeResource.java     /api/compute -> calls the Rust service
AlgorithmsResource.java  /api/algorithms/sort, /select -> Middleware (engine=java|rust)
RestApplication.java     mounts JAX-RS at /api
Main.java                command-line demo (add "rust" to also run the Rust engine)
```

## How it fits together

```
REST client -> Jakarta (AlgorithmsResource) -> Middleware.sort/select(..., Engine) -+-> Engine.JAVA: in-process ALGS4 code
                                                                                   +-> Engine.RUST: RustBackend -> HTTP -> Rust service (main.rs)
```

`Middleware` stays the single entry point. Priority queues are in-process Java only (they are
stateful and comparator-driven, so they can't be sent over HTTP). Engine.RUST handles Integer
values in natural order, ascending or descending.

## Build & Run

### Rust service
```bash
cargo run
```
Runs on http://localhost:8081/add/{a}/{b}
```bash
curl http://localhost:8081/add/7/5
# 12
```

### Jakarta backend
```bash
mvn package
```
Deploy `mvn-target/jakarta-rust-integration.war` to Payara, WildFly, or TomEE.
(Maven writes to `mvn-target/` so it doesn't collide with Cargo's `target/`.)

Quickest way to run it: Payara Micro (a single jar, Jakarta EE 10). Keep the Rust service running, then in a new terminal:
```bash
mvn package && curl -L -o payara-micro.jar https://repo1.maven.org/maven2/fish/payara/extras/payara-micro/6.2025.11/payara-micro-6.2025.11.jar && java -jar payara-micro.jar --deploy mvn-target/jakarta-rust-integration.war
```
This builds the WAR, downloads Payara Micro, and starts it on port 8080. The terminal stays busy while the server runs (about 30 seconds until it says the application is deployed). Then test from another terminal with the `curl` commands under "Endpoints" below.

Port 8080 is only available while this server is running. If you "can't reach 8080", it has not been started.

### Endpoints
```bash
# Calls the Rust service
curl "http://localhost:8080/jakarta-rust-integration/api/compute?a=7&b=5"
# Rust says: 12

# Uses ALGS4Middleware (algo = quick | merge | heap, order = asc | desc)
curl "http://localhost:8080/jakarta-rust-integration/api/algorithms/sort?values=10,3,5,1,20,8&algo=merge&order=desc"
# [20, 10, 8, 5, 3, 1]

# Same call, but the sorting runs in the Rust service
curl "http://localhost:8080/jakarta-rust-integration/api/algorithms/sort?values=10,3,5,1,20,8&algo=heap&engine=rust"
# [1, 3, 5, 8, 10, 20]

# k is 0-based: 2 = 3rd smallest (add &engine=rust to run it in Rust)
curl "http://localhost:8080/jakarta-rust-integration/api/algorithms/select?values=10,3,5,1,20,8&k=2"
# 5
```

Rust service directly:
```bash
curl -X POST localhost:8081/sort -H 'Content-Type: application/json' \
  -d '{"values":[10,3,5,1],"algo":"merge","descending":false}'
# [1,3,5,10]
curl -X POST localhost:8081/select -H 'Content-Type: application/json' -d '{"values":[10,3,5,1],"k":2}'
# 5
```

### Library demo (no server needed)
```bash
javac -d out ALGS4Middleware.java RustBackend.java Main.java
java -cp out com.example.Main
```
With the Rust service running (`cargo run`), also exercise the Rust engine:
```bash
java -cp out com.example.Main rust
```
The Rust URL defaults to `http://localhost:8081`; override with the `RUST_SERVICE_URL` environment variable.

## ALGS4Middleware usage

```java
Integer[] arr = {10,3,5,1,20,8};

// QuickSort ascending
ALGS4Middleware.Middleware.sort(arr, ALGS4Middleware.Middleware.SortType.QUICK);

// MergeSort descending
ALGS4Middleware.Middleware.sort(arr, Comparator.reverseOrder(), ALGS4Middleware.Middleware.SortType.MERGE);

// HeapSort ascending
ALGS4Middleware.Middleware.sort(arr, ALGS4Middleware.Middleware.SortType.HEAP);

// QuickSelect: 3rd smallest
Integer kth = ALGS4Middleware.Middleware.select(arr, 2);

// Same API, run it in the Rust service instead
ALGS4Middleware.Middleware.sort(arr, ALGS4Middleware.Middleware.SortType.HEAP, false,
        ALGS4Middleware.Middleware.Engine.RUST);
Integer third = ALGS4Middleware.Middleware.select(arr, 2, ALGS4Middleware.Middleware.Engine.RUST);

// Priority queues
var minPQ = ALGS4Middleware.Middleware.<Integer>minPQ(2);
minPQ.insert(10); minPQ.insert(5); minPQ.insert(20);
System.out.println(minPQ.peek()); // 5
for (int x : minPQ) System.out.print(x + " ");

var maxPQ = ALGS4Middleware.Middleware.<Integer>priorityQueue(2, Comparator.reverseOrder());
```

## Status

Tested and working (in GitHub Codespaces):
- Java library demo: `java -cp out com.example.Main`
- Rust service: `cargo run`, then `curl http://localhost:8081/add/7/5` returns `12`
- Rust sort: `POST /sort` returns the sorted array
- Java calling Rust through `Middleware`: `java -cp out com.example.Main rust`

Not tested yet:
- `mvn package` and deploying the WAR to a Jakarta EE 10 server (Payara 6, WildFly 27+, TomEE 10)
- The `/api/compute` and `/api/algorithms/*` REST endpoints (they need that server running)

## Running in GitHub Codespaces (works from an iPad in Safari)

1. Repo page: **Code -> Codespaces -> Create codespace on main**.
2. Java is preinstalled. Rust may not be; if `cargo: command not found`, install it:
   ```bash
   curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y
   source $HOME/.cargo/env
   ```
3. Terminal 1: `cargo run` (the first build takes a minute or two; leave it running).
4. Terminal 2 (the **+** button): run the `curl` and `java` commands above.
5. If Codespaces asks to forward a port, forward **8081**. You only need this to open the service in a browser; the terminal tests don't need it.
6. For the Jakarta endpoints on port 8080, start Payara Micro with the command in "Jakarta backend" above (third terminal tab), then forward **8080** if asked.

## Changes from the original two repos

- `ALGS4Middleware.java`, `Main.java`: added `package com.example;`.
- `ALGS4Middleware.java`: fixed `partition` (it used the 1-based heap `exch` with 0-based indices, which crashed QuickSort and QuickSelect) by adding a 0-based `swap`.
- `ALGS4Middleware.java`: fixed `heapSort` sorting opposite to the comparator (the heap sink was a min-heap).
- `ALGS4Middleware.java`: added `Engine { JAVA, RUST }` and the overloads `sort(Integer[], SortType, boolean, Engine)` and `select(Integer[], int, Engine)`. Existing methods are unchanged.
- `Main.java`: `minPQ(2)` is now `<Integer>minPQ(2)` (the original did not compile because `var` could not infer the type); added an optional `rust` argument.
- `main.rs`: kept `/add`, added `POST /sort` and `POST /select`.
- `Cargo.toml`: added a `[[bin]]` entry so the binary is the root `main.rs`.
- `pom.xml`: compiles from the repo root and writes to `mvn-target/` so it does not collide with Cargo's `target/`.
- New files: `RustBackend.java`, `AlgorithmsResource.java`, `RestApplication.java`, `.gitignore`.
- Commit `Cargo.lock`; it pins the Rust dependency versions.

## Notes

- Rust runs separately as a service; no WASM inside the JVM.
- Jakarta handles business logic, persistence, and orchestration; Rust handles compute.
- You can swap REST for gRPC if stronger contracts and performance are needed.
- `QuickSort` is randomized. `QuickSelect` is O(n) expected time. Priority queues resize automatically.
- `ALGS4Middleware.select` shuffles the array you pass in (as in the original), so copy it first if order matters.
- The Rust service binds to 127.0.0.1:8081 and `ComputeResource` calls `http://localhost:8081`; change both if you move it.

## License

MIT License
