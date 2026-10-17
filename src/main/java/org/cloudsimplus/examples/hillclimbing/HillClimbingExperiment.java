package org.cloudsimplus.examples.hillclimbing;

import org.cloudsimplus.allocationpolicies.VmAllocationPolicyRoundRobin; // IMPORT BARU
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.hosts.HostSimple;
import org.cloudsimplus.provisioners.PeProvisionerSimple;
import org.cloudsimplus.provisioners.ResourceProvisionerSimple;
import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.resources.PeSimple;
import org.cloudsimplus.schedulers.cloudlet.CloudletSchedulerTimeShared;
import org.cloudsimplus.schedulers.vm.VmSchedulerTimeShared;
import org.cloudsimplus.utilizationmodels.UtilizationModelFull;
import org.cloudsimplus.vms.VmSimple;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class HillClimbingExperiment {

    private static final int NUM_DCS = 3;
    private static final int HOSTS_PER_DC = 3;
    private static final int VMS_PER_HOST = 3;
    private static final int TOTAL_VMS = NUM_DCS * HOSTS_PER_DC * VMS_PER_HOST; // 27

    private static final int NUM_CLOUDLETS = 100;
    private static final long SEED = 42;
    private static final int MAX_ITERATIONS = 500;
    private static final String NEIGHBOURHOOD = "2-exchange";

    public static void main(String[] args) {
        System.out.println("=== Hill-Climbing Cloudlet Scheduling (Multi-DC) ===");
        System.out.println("Data Centers : " + NUM_DCS);
        System.out.println("Host per DC  : " + HOSTS_PER_DC);
        System.out.println("VM per Host  : " + VMS_PER_HOST);
        System.out.println("Total VMs    : " + TOTAL_VMS);
        System.out.println("Cloudlets    : " + NUM_CLOUDLETS);
        System.out.println("Neighbourhood: " + NEIGHBOURHOOD);
        System.out.println();

        CloudSimPlus simulation = new CloudSimPlus();

        List<DatacenterSimple> datacenters = new ArrayList<>();
        for (int i = 0; i < NUM_DCS; i++) {
            datacenters.add(createDatacenter(simulation, "DC-" + (i + 1)));
        }

        DatacenterBrokerSimple broker = new DatacenterBrokerSimple(simulation);

        // Buat variabel untuk melacak index DC terakhir yang dipilih
        final int[] lastDcIndex = { -1 };

        broker.setDatacenterMapper((lastDatacenter, vm) -> {
            // Jika belum ada DC yang dipilih, mulai dari index 0
            if (lastDcIndex[0] == -1) {
                lastDcIndex[0] = 0;
                return datacenters.get(0);
            }

            // Pilih DC berikutnya secara bergiliran (Round-Robin)
            lastDcIndex[0] = (lastDcIndex[0] + 1) % datacenters.size();
            return datacenters.get(lastDcIndex[0]);
        });

        List<VmSimple> vmList = new ArrayList<>();
        for (int i = 0; i < TOTAL_VMS; i++) {
            vmList.add(createVm(i));
        }
        broker.submitVmList(vmList);

        List<CloudletSimple> cloudletList = createCloudlets(NUM_CLOUDLETS, SEED);

        HillClimbingScheduler hc = new HillClimbingScheduler(
                new ArrayList<>(cloudletList),
                new ArrayList<>(vmList),
                SEED,
                MAX_ITERATIONS,
                NEIGHBOURHOOD);
        List<Cloudlet> optimizedOrder = hc.solve();

        for (Cloudlet c : optimizedOrder) {
            broker.submitCloudlet(c);
        }

        simulation.start();

        double makespan = calculateMakespan(cloudletList);
        double totalCost = calculateTotalCost(cloudletList);
        double avgWaitingTime = calculateAvgWaitingTime(cloudletList);

        System.out.println();
        System.out.println("=== HASIL SIMULASI ===");
        System.out.println("Makespan         : " + String.format("%.2f", makespan) + " detik");
        System.out.println("Total Cost       : $" + String.format("%.4f", totalCost));
        System.out.println("Avg Waiting Time : " + String.format("%.2f", avgWaitingTime) + " detik");
    }

    private static DatacenterSimple createDatacenter(CloudSimPlus simulation, String name) {
        List<HostSimple> hostList = new ArrayList<>();

        for (int i = 0; i < HOSTS_PER_DC; i++) {
            List<Pe> peList = new ArrayList<>();
            for (int j = 0; j < 4; j++) {
                peList.add(new PeSimple(2600, new PeProvisionerSimple()));
            }

            // RAM Host = 6144 MB (6 GB) -> Maksimal 3 VM (2 GB per VM)
            HostSimple host = new HostSimple(6144, 1000000, 500000, peList);
            host.setRamProvisioner(new ResourceProvisionerSimple())
                    .setBwProvisioner(new ResourceProvisionerSimple())
                    .setVmScheduler(new VmSchedulerTimeShared());
            hostList.add(host);
        }

        // Gunakan RoundRobin agar VM tersebar merata ke semua Host
        DatacenterSimple dc = new DatacenterSimple(simulation, hostList, new VmAllocationPolicyRoundRobin());
        dc.setName(name);
        return dc;
    }

    private static VmSimple createVm(int id) {
        VmSimple vm = new VmSimple(id, 2600, 1);
        vm.setRam(2048)
                .setBw(1000)
                .setSize(50000)
                .setCloudletScheduler(new CloudletSchedulerTimeShared());
        return vm;
    }

    private static List<CloudletSimple> createCloudlets(int n, long seed) {
        Random rand = new Random(seed);
        List<CloudletSimple> list = new ArrayList<>();
        UtilizationModelFull utilization = new UtilizationModelFull();
        for (int i = 0; i < n; i++) {
            long length = 500 + (long) (rand.nextDouble() * 4500);
            long fileSize = 300 + (long) (rand.nextDouble() * 2700);
            CloudletSimple cloudlet = new CloudletSimple(i, length, 1);
            cloudlet.setFileSize(fileSize)
                    .setOutputSize(fileSize)
                    .setUtilizationModel(utilization);
            list.add(cloudlet);
        }
        return list;
    }

    private static double calculateMakespan(List<? extends Cloudlet> cloudlets) {
        double max = 0;
        for (Cloudlet c : cloudlets) {
            max = Math.max(max, c.getFinishTime());
        }
        return max;
    }

    private static double calculateTotalCost(List<? extends Cloudlet> cloudlets) {
        double costPerSecond = 0.01;
        double totalExec = 0;
        for (Cloudlet c : cloudlets) {
            totalExec += (c.getFinishTime() - c.getStartTime());
        }
        return totalExec * costPerSecond;
    }

    private static double calculateAvgWaitingTime(List<? extends Cloudlet> cloudlets) {
        double totalWait = 0;
        for (Cloudlet c : cloudlets) {
            totalWait += (c.getStartTime() - c.getBrokerArrivalTime());
        }
        return totalWait / cloudlets.size();
    }
}
