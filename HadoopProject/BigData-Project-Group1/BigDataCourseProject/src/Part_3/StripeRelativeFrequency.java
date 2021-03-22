package Part_3;



import java.io.IOException;
import java.util.*;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

public class StripeRelativeFrequency {

	public static class Map extends Mapper<LongWritable, Text, Text, MapWritable> {

		private static List<String> windows(int startPosition, String[] events) {
			List<String> res = new ArrayList<>();
			for (int i = startPosition + 1; i < events.length; i++) {
				if (events[i].equals(events[startPosition])) break;
				res.add(events[i]);
			}
			return res;
		}

		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			String line = value.toString();
			String[] events = line.split(" +");
			for (int i = 0; i < events.length - 1; ++i) {
				String u = events[i];
				HashMap<String, Integer> stripe = new HashMap<>();
				for (String v : windows(i, events)) {
					if (stripe.containsKey(v)) {
						stripe.put(v, stripe.get(v) + 1);
					} else {
						stripe.put(v, 1);
					}
				}
				MapWritable sw = new MapWritable();
				for (String k : stripe.keySet()) {
					sw.put(new Text(k), new IntWritable(stripe.get(k)));
				}
				context.write(new Text(u), sw);
			}
		}
	}
	
	public static class MyMapWritable extends MapWritable {

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("{ ");
			for (Writable k : this.keySet()) {
				sb.append(k.toString()).append("=").append(this.get(k).toString()).append(", ");
			}
			String res = sb.toString();
			
			return res.substring(0, res.length() - 2) + " }";
		}
	}

	public static class Reduce extends Reducer<Text, MapWritable, Text, MyMapWritable> {

		public void reduce(Text key, Iterable<MapWritable> values, Context context)
				throws IOException, InterruptedException {

			HashMap<String, Integer> fs = new HashMap<>();
			for(MapWritable mw : values) {
				for(Writable k : mw.keySet()) {
					String v = k.toString();
					int vv = Integer.valueOf(mw.get(k).toString());
					if (fs.containsKey(v)) {
						vv += fs.get(v);
					}
					fs.put(v, vv);
				}
			}
			int sum = 0;
			for (String k : fs.keySet()) {
				sum += fs.get(k);
			}
			
			MyMapWritable fsmw = new MyMapWritable();
			for (String k : fs.keySet()) {
				fsmw.put(new Text(k), new DoubleWritable(fs.get(k) * 1.0 / sum));
			}
			
			context.write(key, fsmw);
		}
	}

	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();

		@SuppressWarnings("deprecation")
		Job job = new Job(conf, "RelativeFreqStripe");
		job.setJarByClass(StripeRelativeFrequency.class);

		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(MapWritable.class);

		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(MyMapWritable.class);

		job.setMapperClass(Map.class);
		job.setReducerClass(Reduce.class);

		job.setInputFormatClass(TextInputFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);

		Path output = new Path(args[1]);
		FileSystem fs = FileSystem.get(conf);
	
		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, output);

		job.waitForCompletion(true);
	}

}
