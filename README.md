# Hill-Climbing untuk Cloud Task Scheduling

Implementasi algoritma **Hill-Climbing** untuk penjadwalan cloudlet 
di **CloudSim Plus**, berdasarkan paper:

> Hernando, L., Mendiburu, A., & Lozano, J. A. (2022). 
> *Hill-climbing algorithm: let's go for a walk before finding the optimum.*

## 📋 Fitur

- Algoritma Hill-Climbing (deterministic best-improvement)
- Dua jenis neighbourhood: **2-exchange** dan **insert**
- Metrik: Makespan, Total Cost, Average Waiting Time
- Data center: 3 host, 12 VM

## 🔧 Prasyarat

- **JDK 17** (atau lebih baru)
- **Eclipse IDE** (atau IntelliJ IDEA)
- **Maven** (biasanya sudah bundled dengan Eclipse)
- Koneksi internet (untuk unduh dependensi Maven)

## 🚀 Cara Menjalankan

### 1. Import ke Eclipse
1. Buka Eclipse
2. **File → Import → Maven → Existing Maven Projects**
3. Browse ke folder `project-hillclimbing/`
4. Centang `pom.xml` → **Finish**
5. Tunggu Eclipse mengunduh dependensi (5-15 menit)

### 2. Jalankan Simulasi
1. Buka `src/main/java/org/cloudsimplus/examples/hillclimbing/HillClimbingExperiment.java`
2. Klik kanan → **Run As → Java Application**
3. Lihat output di konsol Eclipse

### 3. Ubah Skenario
Edit konstanta di `HillClimbingExperiment.java`:
```java
private static final int NUM_CLOUDLETS = 100;   // 100, 300, 500
private static final String NEIGHBOURHOOD = "2-exchange"; // "2-exchange" / "insert"
