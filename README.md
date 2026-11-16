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

## Notes

- Rust runs separately as a service; no WASM inside the JVM.
- Jakarta handles business logic, persistence, and orchestration; Rust handles compute.
- You can swap REST for gRPC if stronger contracts and performance are needed.
- `QuickSort` is randomized. `QuickSelect` is O(n) expected time. Priority queues resize automatically.
- `ALGS4Middleware.select` shuffles the array you pass in (as in the original), so copy it first if order matters.
- The Rust service binds to 127.0.0.1:8081 and `ComputeResource` calls `http://localhost:8081`; change both if you move it.

## License

MIT License
