# ALGS4 + Jakarta + Rust

Sorting, selection and priority queues (ALGS4 style) in Java, a Rust microservice
that does the same sorting and selection over HTTP, and a Jakarta EE REST layer
that can use either one.

...(Files, Build & Run, Usage, Status, as before)...

## Planned extension: CRUD API

The next step is to extend this into a full CRUD API:

- **Persistence:** Jakarta Persistence (JPA) stores entities such as items with
  names, prices and timestamps.
- **Endpoints:** create, read, update and delete, with proper status codes and
  validation.
- **Sorting and pagination:** handled by the database through indexed queries.
- **Algorithm features:** the existing library supplies computed results on top
  of the stored data, such as percentile statistics through quickselect and
  top-k rankings through the priority queue.
- **Rust service:** stays a separate component reserved for heavy compute, while
  the Jakarta backend handles business logic, persistence and orchestration.

```
Client -> Jakarta (port 8080) -> Middleware -+-> Java (in-process)
                                             +-> Rust service (port 8081)
```

All files are in one folder:

| File | Purpose |
|---|---|
| `ALGS4Middleware.java` | Sorting, selection, priority queues, and the `Engine` switch (JAVA or RUST) |
| `RustBackend.java` | HTTP client used when the engine is RUST |
| `Main.java` | Command-line demo |
| `main.rs`, `Cargo.toml` | Rust service on port 8081 |
| `RestApplication.java`, `ComputeResource.java`, `AlgorithmsResource.java`, `pom.xml` | Jakarta REST app on port 8080 |

## Requirements

- Java 17 or newer (`java -version`)
- Maven (`mvn -v`)
- Rust and Cargo (`cargo --version`). If missing:
  ```bash
  curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y
  source $HOME/.cargo/env
  ```

You need a terminal for each step that keeps running (steps 2 and 4). Open extra terminal tabs as needed.

## Step 1: Run the Java demo

```bash
javac -d out ALGS4Middleware.java RustBackend.java Main.java
java -cp out com.example.Main
```

Expected output:

```
QuickSort ascending: [1, 3, 5, 8, 10, 20]
3rd smallest via QuickSelect: 5
MergeSort descending: [20, 10, 8, 5, 3, 1]
HeapSort ascending: [1, 3, 5, 8, 10, 20]
MinPQ peek: 5
Iterating MinPQ: 5 10 20
```

## Step 2: Start the Rust service

In a terminal:

```bash
cargo run
```

The first build takes a few minutes. When it prints `Running target/debug/rust_service`, leave it running.

## Step 3: Test the Rust service

In a second terminal:

```bash
curl http://localhost:8081/add/7/5
# 12

curl -X POST localhost:8081/sort -H 'Content-Type: application/json' \
  -d '{"values":[10,3,5,1],"algo":"merge","descending":false}'
# [1,3,5,10]

curl -X POST localhost:8081/select -H 'Content-Type: application/json' \
  -d '{"values":[10,3,5,1],"k":2}'
# 5
```

`algo` is `heap`, `merge` or `quick`. `k` starts at 0, so `k=2` is the 3rd smallest.

Then run the Java demo using the Rust engine:

```bash
java -cp out com.example.Main rust
```

It prints the Step 1 output plus:

```
Rust HeapSort ascending: [1, 3, 5, 8, 10, 20]
Rust MergeSort descending: [20, 10, 8, 5, 3, 1]
Rust 3rd smallest: 5
```

## Step 4: Start the Jakarta app

The Rust service from Step 2 must still be running. In a third terminal:

```bash
mvn package && curl -L -o payara-micro.jar https://repo1.maven.org/maven2/fish/payara/extras/payara-micro/6.2025.11/payara-micro-6.2025.11.jar && java -jar payara-micro.jar --deploy mvn-target/jakarta-rust-integration.war
```

This builds the WAR, downloads Payara Micro (a Jakarta EE 10 server) and starts it on port 8080. Wait about 30 seconds until it reports the application is deployed, and leave it running.

You can deploy `mvn-target/jakarta-rust-integration.war` to WildFly 27+, TomEE 10 or Payara 6 instead.

## Step 5: Test the Jakarta endpoints

In a fourth terminal:

```bash
# Calls the Rust service
curl "http://localhost:8080/jakarta-rust-integration/api/compute?a=7&b=5"
# Rust says: 12

# Sorting in Java (default)
curl "http://localhost:8080/jakarta-rust-integration/api/algorithms/sort?values=10,3,5,1,20,8&algo=merge&order=desc"
# [20, 10, 8, 5, 3, 1]

# Sorting in Rust
curl "http://localhost:8080/jakarta-rust-integration/api/algorithms/sort?values=10,3,5,1,20,8&algo=heap&engine=rust"
# [1, 3, 5, 8, 10, 20]

# Selection (k starts at 0)
curl "http://localhost:8080/jakarta-rust-integration/api/algorithms/select?values=10,3,5,1,20,8&k=2&engine=rust"
# 5
```

Parameters for `/api/algorithms/sort`: `values` (comma-separated integers), `algo` = `quick` | `merge` | `heap`, `order` = `asc` | `desc`, `engine` = `java` | `rust`.

## Troubleshooting

- **Cannot reach port 8080:** the Jakarta app is not running. Do Step 4 and wait for the deployed message.
- **`Error: Connection refused` from an endpoint using Rust:** the Rust service is not running. Do Step 2.
- **`cargo: command not found`:** install Rust (see Requirements), then `source $HOME/.cargo/env`.
- **404 from `curl` in Step 4 (download):** check the current Payara Micro 6 version at https://payara.fish/downloads/payara-platform-community-edition and change the version in the command.
- **Different Rust URL:** set the `RUST_SERVICE_URL` environment variable (default `http://localhost:8081`). `ComputeResource` has the URL hard-coded.

## Using the library in your own code

```java
Integer[] arr = {10,3,5,1,20,8};

// Sorting
ALGS4Middleware.Middleware.sort(arr, ALGS4Middleware.Middleware.SortType.QUICK);
ALGS4Middleware.Middleware.sort(arr, Comparator.reverseOrder(), ALGS4Middleware.Middleware.SortType.MERGE);

// Selection (k starts at 0)
Integer kth = ALGS4Middleware.Middleware.select(arr, 2);

// Same calls, running in the Rust service
ALGS4Middleware.Middleware.sort(arr, ALGS4Middleware.Middleware.SortType.HEAP, false,
        ALGS4Middleware.Middleware.Engine.RUST);
Integer third = ALGS4Middleware.Middleware.select(arr, 2, ALGS4Middleware.Middleware.Engine.RUST);

// Priority queues (Java only)
var minPQ = ALGS4Middleware.Middleware.<Integer>minPQ(2);
minPQ.insert(10); minPQ.insert(5); minPQ.insert(20);
System.out.println(minPQ.peek()); // 5
for (int x : minPQ) System.out.print(x + " ");
```

The Rust engine works on `Integer` values in natural order (ascending or descending). `select` shuffles the array you pass in, so copy it first if the order matters.

## License

MIT License
