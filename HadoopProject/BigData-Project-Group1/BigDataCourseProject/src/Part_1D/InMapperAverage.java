package Part_1D;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

 


public class InMapperAverage {
	
	public static class WritablePair implements Writable {
		public int sum;
		public int count;
	
		public WritablePair() {;}
		
		public WritablePair(int sum, int count) {
			this.sum = sum;
			this.count = count;
		}

		
		public int getSum() {
			return sum;
		}

		public void setSum(int sum) {
			this.sum = sum;
		}

		public int getCount() {
			return count;
		}

		public void setCount(int count) {
			this.count = count;
		}

		@Override
		public void write(DataOutput out) throws IOException {
			out.writeInt(sum);
			out.writeInt(count);
		}

		@Override
		public void readFields(DataInput in) throws IOException {
			sum = in.readInt();
			count = in.readInt();
		}

		public static WritablePair read(DataInput in) throws IOException {
			WritablePair wp = new WritablePair();
			wp.readFields(in);
			return wp;
		}

		@Override
		public String toString( ) {
			return "(" + sum + "," + count + ")";
		} 
		
	}

	public static void main(String[] args) throws Exception {
		Configuration config = new Configuration();
		String[] files = new GenericOptionsParser(config, args)
				.getRemainingArgs();
		Path input = new Path(files[0]);
		Path output = new Path(files[1]);

		@SuppressWarnings("deprecation")
		Job job = new Job(config, "in-mapper-average");

		job.setJarByClass(InMapperAverage.class);

		// Mapper Output
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(WritablePair.class);

		// Reducer Output
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(DoubleWritable.class);

		job.setMapperClass(Map.class);
		job.setReducerClass(Reduce.class);

		FileInputFormat.addInputPath(job, input);
		FileOutputFormat.setOutputPath(job, output);

		System.exit(job.waitForCompletion(true) ? 0 : 1);

	}

	public static class Map extends
			Mapper<LongWritable, Text, Text, WritablePair> {

		private HashMap<Text, WritablePair> H;

		protected void setup(Context context) throws IOException,
				InterruptedException {
			Configuration conf = context.getConfiguration();
			H = new HashMap<Text, WritablePair>();
		}

		public void map(LongWritable key, Text value, Context con)
				throws IOException, InterruptedException {
			String[] inportantFields = value.toString().split("\\s");
			int length = inportantFields.length;
			Matcher m1 = Pattern.compile("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$")
					.matcher(inportantFields[0]); // Matching IP
			Matcher m2 = Pattern.compile("\\d+").matcher(
					inportantFields[length - 1]); // Matching quantity
			if (m1.find() && m2.find()) {
				Text IP = new Text(inportantFields[0]);
				int quantity = Integer.parseInt(inportantFields[length - 1]);
				if (H.containsKey(IP)) {
					H.put(IP, new WritablePair(H.get(IP).getSum() + quantity, H
							.get(IP).getCount() + 1));
				} else {
					H.put(IP, new WritablePair(quantity, 1));
				}
			}

		}

		@Override
		public void cleanup(Context context) throws IOException,
				InterruptedException {
			for (Text key : H.keySet()) {
				context.write(key, H.get(key)); // Final Mapper Output
			}
		}

	}

	public static class Reduce extends
			Reducer<Text, WritablePair, Text, DoubleWritable> { // Reducer Input
																// , Reducer
																// Output Format
		public void reduce(Text key, Iterable<WritablePair> values,
				Context context) throws IOException, InterruptedException {
			int sum = 0;
			int cnt = 0;

			for (WritablePair val : values) {
				sum += val.getSum();
				cnt += val.getCount();
			}
			double avg = (double)sum / cnt;
			context.write(key, new DoubleWritable(avg));
		}

	}

}
