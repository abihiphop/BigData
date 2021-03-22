package Part_2;

import java.io.IOException;

import org.apache.hadoop.fs.Path;

import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class PairsRelativeFrequency {

	public static void main(String[] args) throws Exception {
		Configuration config = new Configuration();
		String[] files = new GenericOptionsParser(config, args)
				.getRemainingArgs();
		Path input = new Path(files[0]);
		Path output = new Path(files[1]);

		@SuppressWarnings("deprecation")
		Job job = new Job(config, "In-mapperAvg");

		job.setJarByClass(PairsRelativeFrequency.class);

		// Mapper Output
		job.setMapOutputKeyClass(WritableComparablePair.class);
		job.setMapOutputValueClass(IntWritable.class);

		// Reducer Output
		job.setOutputKeyClass(WritableComparablePair.class);
		job.setOutputValueClass(DoubleWritable.class);

		job.setMapperClass(Map.class);
		job.setReducerClass(Reduce.class);
		job.setPartitionerClass(MyPartitioner.class);

		FileSystem fs = FileSystem.get(config);
		if (fs.exists(output)) {
			fs.delete(output, true);
		}

		FileInputFormat.addInputPath(job, input);
		FileOutputFormat.setOutputPath(job, output);

		System.exit(job.waitForCompletion(true) ? 0 : 1);

	}

	public static class Map extends
			Mapper<LongWritable, Text, WritableComparablePair, IntWritable> {

		private static IntWritable one = new IntWritable(1);

		private static List<String> windows(int startingPosition,
				String[] events) {
			List<String> windowResult = new ArrayList<>();
			for (int i = startingPosition + 1; i < events.length; i++) {
				if (events[i].equals(events[startingPosition]))
					break;
				windowResult.add(events[i]);
			}
			return windowResult;
		}

		public void map(LongWritable key, Text value, Context context)
				throws IOException, InterruptedException {
			String line = value.toString();
			String[] events = line.split("\\s+");
			for (int i = 0; i < events.length - 1; i++) {
				String u = events[i];
				List<String> window = windows(i, events);
				for (String v : window) {
					context.write(new WritableComparablePair(u, v), one);
					context.write(new WritableComparablePair(u, "*"), one);
				}
			}

		}

	}

	public static class Reduce
			extends
			Reducer<WritableComparablePair, IntWritable, WritableComparablePair, DoubleWritable> { // Reducer
																									// Input
		// , Reducer
		// Output Format

		private int sumStar;

		@Override
		public void setup(Context context) {
			sumStar = 0;
		}

		public void reduce(WritableComparablePair key,
				Iterable<IntWritable> values, Context context)
				throws IOException, InterruptedException {
			int sumPair = 0;

			for (IntWritable val : values) {
				sumPair += val.get();
			}

			if (key.right.equals("*")) {  
				sumStar = sumPair;
			} else {
				context.write(key, new DoubleWritable((double) sumPair
						/ sumStar));
			}

		}

	}

}
