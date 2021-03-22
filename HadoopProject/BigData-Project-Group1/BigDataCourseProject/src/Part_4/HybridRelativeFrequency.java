package Part_4;

import java.io.IOException;

import org.apache.hadoop.fs.Path;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.io.MapWritable;

import Part_1D.InMapperAverage.WritablePair;

;

public class HybridRelativeFrequency {

	public static void main(String[] args) throws Exception {
		Configuration config = new Configuration();
		String[] files = new GenericOptionsParser(config, args)
				.getRemainingArgs();
		Path input = new Path(files[0]);
		Path output = new Path(files[1]);

		@SuppressWarnings("deprecation")
		Job job = new Job(config, "In-mapperAvg");

		job.setJarByClass(HybridRelativeFrequency.class);

		// Mapper Output
		job.setMapOutputKeyClass(WritableComparablePair.class);
		job.setMapOutputValueClass(IntWritable.class);

		// Reducer Output
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(MyMapWritable.class);

		job.setMapperClass(Map.class);
		job.setReducerClass(Reduce.class);
		job.setPartitionerClass(MyPartitioner.class);

		/*
		 * FileSystem fs = FileSystem.get(config); if (fs.exists(output)) {
		 * fs.delete(output, true); }
		 */
		FileInputFormat.addInputPath(job, input);
		FileOutputFormat.setOutputPath(job, output);

		System.exit(job.waitForCompletion(true) ? 0 : 1);

	}

	public static class Map extends
			Mapper<LongWritable, Text, WritableComparablePair, IntWritable> {

		private static IntWritable one = new IntWritable(1);

		private HashMap<WritableComparablePair, Integer> H;

		protected void setup(Context context) throws IOException,
				InterruptedException {
			Configuration conf = context.getConfiguration();
			H = new HashMap<WritableComparablePair, Integer>();
		}

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
					WritableComparablePair pair = new WritableComparablePair(u,
							v);
					if (H.containsKey(pair)) {
						H.put(pair, H.get(pair) + 1);
					} else {
						H.put(pair, 1);
					}

				}
			}

		}

		@Override
		public void cleanup(Context context) throws IOException,
				InterruptedException {
			for (WritableComparablePair key : H.keySet()) {
				context.write(key, new IntWritable(H.get(key))); // Final Mapper
																	// Output
			}
		}

	}

	public static class MyMapWritable extends MapWritable {
		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			Set<Writable> keySet = this.keySet();
			result.append("{");
			for (Object key : keySet) {
				result.append("{" + key.toString() + " => " + this.get(key)
						+ "}");
			}
			result.append("}");
			return result.toString();
		}
	}

	public static class Reduce extends
			Reducer<WritableComparablePair, IntWritable, Text, MyMapWritable> { // Reducer
		// Input
		// , Reducer
		// Output Format

		private String wPrev;
		private HashMap<String, Integer> H;

		@Override
		public void setup(Context context) {
			wPrev = null;
			H = new HashMap<String, Integer>();
		}

		public void reduce(WritableComparablePair key,
				Iterable<IntWritable> values, Context context)
				throws IOException, InterruptedException {

			if (!key.left.equals(wPrev) && wPrev != null) {
				int total = 0;
				for (String k : H.keySet()) {
					total += H.get(k);
				}

				MyMapWritable newH = new MyMapWritable();
				for (String k : H.keySet()) {
					newH.put(new Text(k), new DoubleWritable(H.get(k) * 1.0
							/ total));
				}
				context.write(new Text(wPrev), newH);
				H = new HashMap<String, Integer>();
			}
			int sum = 0;
			for (IntWritable val : values)
				sum += val.get();
			H.put(key.right, sum);
			wPrev = key.left;

		}

		@Override
		public void cleanup(Context context) throws IOException,
				InterruptedException {
			int total = 0;
			for (String k : H.keySet()) {
				total += H.get(k);
			}
			MyMapWritable newH = new MyMapWritable();
			for (String k : H.keySet()) {
				newH.put(new Text(k),
						new DoubleWritable(H.get(k) * 1.0 / total));
			}
			context.write(new Text(wPrev), newH);
		}

	}

}
