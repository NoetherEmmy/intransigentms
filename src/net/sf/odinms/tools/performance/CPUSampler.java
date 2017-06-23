package net.sf.odinms.tools.performance;

import java.io.IOException;
import java.io.Writer;
import java.lang.Thread.State;
import java.util.*;

public class CPUSampler {
    private final List<String> included = new LinkedList<>();
    private static final CPUSampler instance = new CPUSampler();
    private long interval = 5L;
    private SamplerThread sampler;
    private final Map<StackTrace, Integer> recorded = new LinkedHashMap<>();
    private int totalSamples;

    private CPUSampler() {
    }

    public static CPUSampler getInstance() {
        return instance;
    }

    public void setInterval(final long millis) {
        interval = millis;
    }

    public void addIncluded(final String include) {
        for (final String alreadyIncluded : included) {
            if (include.startsWith(alreadyIncluded)) return;
        }
        included.add(include);
    }

    public void reset() {
        recorded.clear();
        totalSamples = 0;
    }

    public void start() {
        if (sampler == null) {
            sampler = new SamplerThread();
            sampler.start();
        }
    }

    public void stop() {
        if (sampler != null) {
            sampler.stop();
            sampler = null;
        }
    }

    public SampledStacktraces getTopConsumers() {
        final List<StacktraceWithCount> ret = new ArrayList<>();
        final Set<Map.Entry<StackTrace, Integer>> entrySet = recorded.entrySet();
        for (final Map.Entry<StackTrace, Integer> entry : entrySet) {
            ret.add(new StacktraceWithCount(entry.getValue(), entry.getKey()));
        }
        Collections.sort(ret);
        return new SampledStacktraces(ret, totalSamples);
    }

    public void save(final Writer writer, final int minInvocations, final int topMethods) throws IOException {
        final SampledStacktraces topConsumers = getTopConsumers();
        final StringBuilder builder = new StringBuilder(); // build our summary :o
        builder.append("Top Methods:\n");
        for (int i = 0; i < topMethods && i < topConsumers.getTopConsumers().size(); ++i) {
            builder.append(topConsumers.getTopConsumers().get(i).toString(topConsumers.getTotalInvocations(), 1));
        }
        builder.append("\nStack Traces:\n");
        writer.write(builder.toString());
        writer.write(topConsumers.toString(minInvocations));
        writer.flush();
    }

    private void consumeStackTraces(final Map<Thread, StackTraceElement[]> traces) {
        for (final Map.Entry<Thread, StackTraceElement[]> trace : traces.entrySet()) {
            final int relevant = findRelevantElement(trace.getValue());
            if (relevant != -1) {
                final StackTrace st = new StackTrace(trace.getValue(), relevant, trace.getKey().getState());
                final Integer i = recorded.get(st);
                totalSamples++;
                if (i == null) {
                    recorded.put(st, 1);
                } else {
                    recorded.put(st, i + 1);
                }
            }
        }
    }

    private int findRelevantElement(final StackTraceElement[] trace) {
        if (trace.length == 0) {
            return -1;
        } else if (included.isEmpty()) {
            return 0;
        }
        int firstIncluded = -1;
        for (final String myIncluded : included) {
            for (int i = 0; i < trace.length; ++i) {
                final StackTraceElement ste = trace[i];
                if (ste.getClassName().startsWith(myIncluded)) {
                    if (i < firstIncluded || firstIncluded == -1) {
                        firstIncluded = i;
                        break;
                    }
                }
            }
        }
        if (firstIncluded >= 0 && trace[firstIncluded].getClassName().equals("net.sf.odinms.tools.performance.CPUSampler$SamplerThread")) { // Don't sample us.
            return -1;
        }
        return firstIncluded;
    }

    private static class StackTrace {
        private final StackTraceElement[] trace;
        private final State state;

        public StackTrace(final StackTraceElement[] trace, final int startAt, final State state) {
            this.state = state;
            if (startAt == 0) {
                this.trace = trace;
            } else {
                this.trace = new StackTraceElement[trace.length - startAt];
                System.arraycopy(trace, startAt, this.trace, 0, this.trace.length);
            }
        }

        @Override
        public boolean equals(final Object obj) {
            if (!(obj instanceof StackTrace)) return false;
            final StackTrace other = (StackTrace) obj;
            if (other.trace.length !=  trace.length) return false;
            if (!(other.state == this.state)) return false;
            for (int i = 0; i < trace.length; ++i) {
                if (!trace[i].equals(other.trace[i])) return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int ret = 13 * trace.length + state.hashCode();
            for (final StackTraceElement ste : trace) {
                ret ^= ste.hashCode();
            }
            return ret;
        }

        public StackTraceElement[] getTrace() {
            return trace;
        }

        @Override
        public String toString() {
            return toString(-1);
        }

        public String toString(final int traceLength) {
            final StringBuilder ret = new StringBuilder("State: ");
            ret.append(state.name());
            if (traceLength > 1) {
                ret.append('\n');
            } else {
                ret.append(' ');
            }
            int i = 0;
            for (final StackTraceElement ste : trace) {
                i++;
                if (i > traceLength) break;
                ret.append(ste.getClassName());
                ret.append('#');
                ret.append(ste.getMethodName());
                ret.append(" (Line: ");
                ret.append(ste.getLineNumber());
                ret.append(")\n");
            }
            return ret.toString();
        }
    }

    private class SamplerThread implements Runnable {
        private boolean running = false, shouldRun = false;
        private Thread rthread;

        public void start() {
            if (!running) {
                shouldRun = true;
                rthread = new Thread(this, "CPU Sampling Thread");
                rthread.start();
                running = true;
            }
        }

        public void stop() {
            this.shouldRun = false;
            rthread.interrupt();
            try {
                rthread.join();
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            while (shouldRun) {
                consumeStackTraces(Thread.getAllStackTraces());
                try {
                    Thread.sleep(interval);
                } catch (final InterruptedException e) {
                    return;
                }
            }
        }
    }

    public static class StacktraceWithCount implements Comparable<StacktraceWithCount> {
        private final int count;
        private final StackTrace trace;

        public StacktraceWithCount(final int count, final StackTrace trace) {
            super();
            this.count = count;
            this.trace = trace;
        }

        public int getCount() {
            return count;
        }

        public StackTraceElement[] getTrace() {
            return trace.getTrace();
        }

        @Override
        public int compareTo(final StacktraceWithCount o) {
            return -Integer.valueOf(count).compareTo(o.count);
        }

        @Override
        public String toString() {
            return count + " Sampled Invocations\n" + trace.toString();
        }

        private double getPercentage(final int total) {
            return Math.round((((double) count) / total) * 10000.0d) / 100.0d;
        }

        public String toString(final int totalInvoations, final int traceLength) {
            return count + "/" + totalInvoations + " Sampled Invocations (" + getPercentage(totalInvoations) + "%) " + trace.toString(traceLength);
        }
    }

    public static class SampledStacktraces {
        final List<StacktraceWithCount> topConsumers;
        final int totalInvocations;

        public SampledStacktraces(final List<StacktraceWithCount> topConsumers, final int totalInvocations) {
            super();
            this.topConsumers = topConsumers;
            this.totalInvocations = totalInvocations;
        }

        public List<StacktraceWithCount> getTopConsumers() {
            return topConsumers;
        }

        public int getTotalInvocations() {
            return totalInvocations;
        }

        @Override
        public String toString() {
            return toString(0);
        }

        public String toString(final int minInvocation) {
            final StringBuilder ret = new StringBuilder();
            for (final StacktraceWithCount swc : topConsumers) {
                if (swc.getCount() >= minInvocation) {
                    ret.append(swc.toString(totalInvocations, Integer.MAX_VALUE));
                    ret.append('\n');
                }
            }
            return ret.toString();
        }
    }
}
