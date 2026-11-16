use actix_web::{get, post, web, App, HttpResponse, HttpServer, Responder};
use serde::Deserialize;
use std::time::{SystemTime, UNIX_EPOCH};

#[get("/add/{a}/{b}")]
async fn add(path: web::Path<(i32, i32)>) -> impl Responder {
    let (a, b) = path.into_inner();
    format!("{}", a + b)
}

/* ---------------- HTTP layer ---------------- */

#[derive(Deserialize)]
struct SortRequest {
    values: Vec<i64>,
    #[serde(default = "default_algo")]
    algo: String, // "heap" | "merge" | "quick"
    #[serde(default)]
    descending: bool,
}

#[derive(Deserialize)]
struct SelectRequest {
    values: Vec<i64>,
    k: usize, // 0-based: k = 2 is the 3rd smallest
    #[serde(default)]
    descending: bool,
}

fn default_algo() -> String {
    "quick".to_string()
}

#[post("/sort")]
async fn sort_values(req: web::Json<SortRequest>) -> HttpResponse {
    let mut v = req.values.clone();
    let desc = req.descending;
    match req.algo.to_lowercase().as_str() {
        "heap" => heap_sort(&mut v, desc),
        "merge" => merge_sort(&mut v, desc),
        "quick" => quick_sort(&mut v, desc),
        other => return HttpResponse::BadRequest().body(format!("unknown algo: {}", other)),
    }
    HttpResponse::Ok().json(v)
}

#[post("/select")]
async fn select_value(req: web::Json<SelectRequest>) -> HttpResponse {
    let mut v = req.values.clone();
    if req.k >= v.len() {
        return HttpResponse::BadRequest()
            .body(format!("k out of range: {} (length {})", req.k, v.len()));
    }
    let r = quick_select(&mut v, req.k, req.descending);
    HttpResponse::Ok().json(r)
}

/* ---------------- ALGS4-style algorithms ---------------- */

/// "a comes before b" in the requested order
fn less(a: i64, b: i64, desc: bool) -> bool {
    if desc { a > b } else { a < b }
}

// HeapSort (max-heap w.r.t. the requested order)
fn sink(a: &mut [i64], mut k: usize, n: usize, desc: bool) {
    loop {
        let mut j = 2 * k + 1;
        if j >= n {
            break;
        }
        if j + 1 < n && less(a[j], a[j + 1], desc) {
            j += 1;
        }
        if !less(a[k], a[j], desc) {
            break;
        }
        a.swap(k, j);
        k = j;
    }
}

fn heap_sort(a: &mut [i64], desc: bool) {
    let n = a.len();
    if n < 2 {
        return;
    }
    for k in (0..n / 2).rev() {
        sink(a, k, n, desc);
    }
    let mut end = n;
    while end > 1 {
        a.swap(0, end - 1);
        end -= 1;
        sink(a, 0, end, desc);
    }
}

// MergeSort
fn merge_sort(a: &mut [i64], desc: bool) {
    let mut aux = a.to_vec();
    let n = a.len();
    msort(a, &mut aux, 0, n, desc);
}

fn msort(a: &mut [i64], aux: &mut [i64], lo: usize, hi: usize, desc: bool) {
    if hi - lo < 2 {
        return;
    }
    let mid = lo + (hi - lo) / 2;
    msort(a, aux, lo, mid, desc);
    msort(a, aux, mid, hi, desc);
    aux[lo..hi].copy_from_slice(&a[lo..hi]);
    let (mut i, mut j) = (lo, mid);
    for k in lo..hi {
        if i >= mid {
            a[k] = aux[j];
            j += 1;
        } else if j >= hi {
            a[k] = aux[i];
            i += 1;
        } else if less(aux[j], aux[i], desc) {
            a[k] = aux[j];
            j += 1;
        } else {
            a[k] = aux[i];
            i += 1;
        }
    }
}

// QuickSort / QuickSelect (randomized by shuffling first)
struct Rng(u64);

impl Rng {
    fn new() -> Self {
        let t = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .map(|d| d.as_nanos() as u64)
            .unwrap_or(88172645463325252);
        Rng(t | 1)
    }
    fn below(&mut self, n: usize) -> usize {
        let mut x = self.0;
        x ^= x << 13;
        x ^= x >> 7;
        x ^= x << 17;
        self.0 = x;
        (x % (n as u64)) as usize
    }
}

fn shuffle(a: &mut [i64]) {
    let mut r = Rng::new();
    for i in (1..a.len()).rev() {
        let j = r.below(i + 1);
        a.swap(i, j);
    }
}

fn partition(a: &mut [i64], lo: usize, hi: usize, desc: bool) -> usize {
    let pivot = a[lo];
    let mut i = lo + 1;
    let mut j = hi;
    loop {
        while i <= hi && less(a[i], pivot, desc) {
            i += 1;
        }
        while less(pivot, a[j], desc) {
            j -= 1;
        }
        if i >= j {
            break;
        }
        a.swap(i, j);
        i += 1;
        j -= 1;
    }
    a.swap(lo, j);
    j
}

fn qsort(a: &mut [i64], lo: usize, hi: usize, desc: bool) {
    if hi <= lo {
        return;
    }
    let j = partition(a, lo, hi, desc);
    if j > lo {
        qsort(a, lo, j - 1, desc);
    }
    qsort(a, j + 1, hi, desc);
}

fn quick_sort(a: &mut [i64], desc: bool) {
    if a.len() < 2 {
        return;
    }
    shuffle(a);
    let hi = a.len() - 1;
    qsort(a, 0, hi, desc);
}

fn quick_select(a: &mut [i64], k: usize, desc: bool) -> i64 {
    shuffle(a);
    let (mut lo, mut hi) = (0usize, a.len() - 1);
    while hi > lo {
        let j = partition(a, lo, hi, desc);
        if j < k {
            lo = j + 1;
        } else if j > k {
            hi = j - 1;
        } else {
            return a[k];
        }
    }
    a[k]
}

#[actix_web::main]
async fn main() -> std::io::Result<()> {
    HttpServer::new(|| {
        App::new()
            .service(add)
            .service(sort_values)
            .service(select_value)
    })
    .bind(("127.0.0.1", 8081))?
    .run()
    .await
}
