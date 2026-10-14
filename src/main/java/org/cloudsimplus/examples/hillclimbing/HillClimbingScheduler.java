package org.cloudsimplus.examples.hillclimbing;

import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.vms.Vm;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Implementasi algoritma Hill-Climbing (deterministic best-improvement)
 * untuk penjadwalan cloudlet berdasarkan paper:
 * "Hill-climbing algorithm: let's go for a walk before finding the optimum"
 * oleh Hernando, Mendiburu, & Lozano.
 */
public class HillClimbingScheduler {

    private final List<Cloudlet> cloudlets;
    private final List<Vm> vms;
    private final Random random;
    private final int maxIterations;
    private final String neighbourhood; // "2-exchange" atau "insert"

    public HillClimbingScheduler(List<Cloudlet> cloudlets, 
                                  List<Vm> vms,
                                  long seed, 
                                  int maxIterations,
                                  String neighbourhood) {
        this.cloudlets = cloudlets;
        this.vms = vms;
        this.random = new Random(seed);
        this.maxIterations = maxIterations;
        this.neighbourhood = neighbourhood;
    }

    /**
     * Menjalankan algoritma Hill-Climbing (best-improvement).
     * @return Solusi terbaik (permutasi cloudlet)
     */
    public List<Cloudlet> solve() {
        // 1. Inisialisasi solusi awal (random permutation)
        List<Cloudlet> current = new ArrayList<>(cloudlets);
        Collections.shuffle(current, random);

        double currentFitness = evaluate(current);
        int iteration = 0;
        boolean improved = true;

        System.out.println("Fitness awal: " + currentFitness);

        // 2. Loop hingga local optimum atau max iterasi
        while (improved && iteration < maxIterations) {
            improved = false;
            List<Cloudlet> bestNeighbor = null;
            double bestNeighborFitness = currentFitness;

            // 3. Generate neighbours sesuai jenis neighbourhood
            List<List<Cloudlet>> neighbors;
            if ("insert".equalsIgnoreCase(neighbourhood)) {
                neighbors = generateInsertNeighbors(current);
            } else {
                neighbors = generate2ExchangeNeighbors(current);
            }

            // 4. Evaluasi setiap neighbour
            for (List<Cloudlet> neighbor : neighbors) {
                double fitness = evaluate(neighbor);
                if (fitness < bestNeighborFitness) {
                    bestNeighborFitness = fitness;
                    bestNeighbor = neighbor;
                }
            }

            // 5. Pindah ke neighbour terbaik jika ada perbaikan
            if (bestNeighbor != null) {
                current = bestNeighbor;
                currentFitness = bestNeighborFitness;
                improved = true;
            }
            iteration++;
        }

        System.out.println("Fitness akhir: " + currentFitness);
        System.out.println("Jumlah iterasi: " + iteration);
        return current;
    }

    /**
     * Generate neighbours dengan 2-exchange: tukar dua elemen.
     */
    private List<List<Cloudlet>> generate2ExchangeNeighbors(List<Cloudlet> solution) {
        List<List<Cloudlet>> neighbors = new ArrayList<>();
        int n = solution.size();
        for (int i = 0; i < n - 1; i++) {
            for (int j = i + 1; j < n; j++) {
                List<Cloudlet> neighbor = new ArrayList<>(solution);
                Collections.swap(neighbor, i, j);
                neighbors.add(neighbor);
            }
        }
        return neighbors;
    }

    /**
     * Generate neighbours dengan insert: pindahkan elemen ke posisi lain.
     */
    private List<List<Cloudlet>> generateInsertNeighbors(List<Cloudlet> solution) {
        List<List<Cloudlet>> neighbors = new ArrayList<>();
        int n = solution.size();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) continue;
                List<Cloudlet> neighbor = new ArrayList<>(solution);
                Cloudlet c = neighbor.remove(i);
                neighbor.add(j, c);
                neighbors.add(neighbor);
            }
        }
        return neighbors;
    }

    /**
     * Evaluasi fitness: makespan berdasarkan assignment cloudlet ke VM.
     * Aturan: cloudlet ke-i di-assign ke VM (i mod jumlah VM).
     */
    private double evaluate(List<Cloudlet> permutation) {
        int numVms = vms.size();
        double[] vmReadyTime = new double[numVms];

        for (int i = 0; i < permutation.size(); i++) {
            Cloudlet cloudlet = permutation.get(i);
            Vm vm = vms.get(i % numVms);
            double execTime = cloudlet.getLength() / vm.getMips();
            vmReadyTime[i % numVms] += execTime;
        }

        double makespan = 0;
        for (double t : vmReadyTime) {
            makespan = Math.max(makespan, t);
        }
        return makespan;
    }
}