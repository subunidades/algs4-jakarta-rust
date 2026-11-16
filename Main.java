package com.example;

import java.util.Comparator;

public class Main {
    public static void main(String[] args) {
        Integer[] arr = {10,3,5,1,20,8};

        // QuickSort
        ALGS4Middleware.Middleware.sort(arr, ALGS4Middleware.Middleware.SortType.QUICK);
        System.out.println("QuickSort ascending: " + java.util.Arrays.toString(arr));

        // QuickSelect: 3rd smallest (index 2)
        Integer kth = ALGS4Middleware.Middleware.select(arr, 2);
        System.out.println("3rd smallest via QuickSelect: " + kth);

        // MergeSort descending
        ALGS4Middleware.Middleware.sort(arr, Comparator.reverseOrder(), ALGS4Middleware.Middleware.SortType.MERGE);
        System.out.println("MergeSort descending: " + java.util.Arrays.toString(arr));

        // HeapSort ascending
        ALGS4Middleware.Middleware.sort(arr, ALGS4Middleware.Middleware.SortType.HEAP);
        System.out.println("HeapSort ascending: " + java.util.Arrays.toString(arr));

        // PriorityQueue
        var minPQ = ALGS4Middleware.Middleware.<Integer>minPQ(2);
        minPQ.insert(10); minPQ.insert(5); minPQ.insert(20);
        System.out.println("MinPQ peek: " + minPQ.peek());
        System.out.print("Iterating MinPQ: ");
        for (int x : minPQ) System.out.print(x + " ");
        System.out.println();

        // Same API, Rust engine (start the Rust service first, then: java -cp out com.example.Main rust)
        if (args.length > 0 && args[0].equals("rust")) {
            Integer[] r = {10,3,5,1,20,8};
            ALGS4Middleware.Middleware.sort(r, ALGS4Middleware.Middleware.SortType.HEAP, false,
                    ALGS4Middleware.Middleware.Engine.RUST);
            System.out.println("Rust HeapSort ascending: " + java.util.Arrays.toString(r));
            ALGS4Middleware.Middleware.sort(r, ALGS4Middleware.Middleware.SortType.MERGE, true,
                    ALGS4Middleware.Middleware.Engine.RUST);
            System.out.println("Rust MergeSort descending: " + java.util.Arrays.toString(r));
            System.out.println("Rust 3rd smallest: "
                    + ALGS4Middleware.Middleware.select(r, 2, ALGS4Middleware.Middleware.Engine.RUST));
        }
    }
}
