package Part_4;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Partitioner;

public class MyPartitioner extends Partitioner<WritableComparablePair, IntWritable> {
	@Override
	public int getPartition(WritableComparablePair key, IntWritable value, int numReduceTasks){
		if(numReduceTasks==0)
			return 0;
		return Math.abs(key.left.hashCode()) % numReduceTasks;
	}
}
