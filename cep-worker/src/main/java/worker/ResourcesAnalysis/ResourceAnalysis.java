package worker.ResourcesAnalysis;


import javax.management.*;
import java.lang.management.ManagementFactory;

public class ResourceAnalysis {

    private static double getProcessCpuLoad() {
        double percentage = Double.NaN;
        while(percentage == Double.NaN) {

            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectName name = null;
            AttributeList list = null;
            try {
                name = ObjectName.getInstance("java.lang:type=OperatingSystem");
                list = mbs.getAttributes(name, new String[]{"ProcessCpuLoad"});
            } catch (MalformedObjectNameException | InstanceNotFoundException | ReflectionException e) {
                e.printStackTrace();
            }
            Double value = Double.NaN;
            if (!list.isEmpty()) {
                Attribute att = (Attribute) list.get(0);
                value = (Double) att.getValue();

                // usually takes a couple of seconds before we get real values
                if (value != -1.0) {
                    percentage = value;
                }
            }
            // returns a percentage value with 1 decimal point precision
        }
        return ((int) (percentage * 1000) / 10.0);
    }



    private static double getMemoryUsage(){
        double usedPercent=(double)Runtime.getRuntime().totalMemory();
        usedPercent = usedPercent-Runtime.getRuntime().freeMemory();
        usedPercent = usedPercent/Runtime.getRuntime().maxMemory();
        return usedPercent;
    }



    public static boolean Overload(){
        boolean cpuoverload = getProcessCpuLoad() > Double.valueOf(System.getenv("CPU_OVERLOAD"));
        boolean memoryoverload = getMemoryUsage() > Double.valueOf(System.getenv("MEMORY_OVERLOAD"));
        return cpuoverload || memoryoverload;
    }

    public static boolean Underload(){
        boolean cpuUnderload = getProcessCpuLoad() < Double.valueOf(System.getenv("CPU_OVERLOAD"));
        boolean memoryUnderload = getMemoryUsage() < Double.valueOf(System.getenv("MEMORY_OVERLOAD"));
        return cpuUnderload && memoryUnderload;
    }

    public static double getResourceUsage(){
        double cpuload = getProcessCpuLoad();
        double memoryload = getMemoryUsage();
        return memoryload > cpuload ? memoryload : cpuload;
    }
}
