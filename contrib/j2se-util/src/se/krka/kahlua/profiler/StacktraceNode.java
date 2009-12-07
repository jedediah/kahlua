package se.krka.kahlua.profiler;

import java.util.*;
import java.io.PrintWriter;

public class StacktraceNode {
    private final long time;
    private final String name;
    private final List<StacktraceNode> children;

    public StacktraceNode(String name, List<StacktraceNode> children, long time) {
        this.name = name;
        this.children = children;
        this.time = time;
    }

    public static StacktraceNode createFrom(StacktraceCounter counter,
                                            String name,
                                            int maxDepth,
                                            double minTimeRatio,
                                            int maxChildren) {

        StacktraceNode returnValue = new StacktraceNode(name, new ArrayList<StacktraceNode>(), counter.getTime());

        if (maxDepth > 0) {
            Map<StacktraceElement,StacktraceCounter> map = counter.getChildren();

            List<Map.Entry<StacktraceElement, StacktraceCounter>> childArray = new ArrayList<Map.Entry<StacktraceElement, StacktraceCounter>>(map.entrySet());
            Collections.sort(childArray, new Comparator<Map.Entry<StacktraceElement, StacktraceCounter>>() {
                @Override
                public int compare(Map.Entry<StacktraceElement, StacktraceCounter> o1, Map.Entry<StacktraceElement, StacktraceCounter> o2) {
                    return Long.signum(o2.getValue().getTime() - o1.getValue().getTime());
                }
            });
            for (int i = childArray.size() - 1; i >= maxChildren; i--) {
                childArray.remove(i);
            }

            for (Map.Entry<StacktraceElement, StacktraceCounter> entry : childArray) {
                StacktraceElement element = entry.getKey();
                StacktraceCounter childCounter = entry.getValue();

                if (childCounter.getTime() >= minTimeRatio * counter.getTime()) {
                    StacktraceNode childNode = createFrom(childCounter, element.toString(),
                            maxDepth - 1,
                            minTimeRatio,
                            maxChildren);
                    returnValue.children.add(childNode);
                }
            }
        }

        return returnValue;
    }

    public void output(PrintWriter writer) {
        output(writer, "", time, time);
    }

    public void output(PrintWriter writer, String indent, long parentTime, long rootTime) {
        writer.println(String.format("%-40s   %4d ms   %5.1f%% of parent    %5.1f%% of total",
                indent + name,
                time,
                100.0 * time / parentTime,
                100.0 * time / rootTime));
        String nextIndent = indent + "  ";
        for (StacktraceNode child : children) {
            child.output(writer, nextIndent, time, rootTime);
        }
    }
}
