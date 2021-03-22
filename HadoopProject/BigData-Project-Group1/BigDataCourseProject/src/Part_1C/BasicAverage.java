package Part_1C;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

public class BasicAverage {

	public static class Map extends Mapper<LongWritable, Text, Text, IntWritable> {
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			String line = value.toString();
			String[] words = line.split(" +");
			String first = words[0];
			String last = words[words.length - 1];
			Matcher m1 = Pattern.compile("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$").matcher(first); // Matching IP
			Matcher m2 = Pattern.compile("\\d+").matcher(last); // Matching quantity
			if (m1.find() && m2.find()){
				context.write(new Text(first), new IntWritable(Integer.valueOf(last)));
			}

		}
	}

	public static class Reduce extends Reducer<Text, IntWritable, Text, DoubleWritable> {

		public void reduce(Text key, Iterable<IntWritable> values, Context context)
				throws IOException, InterruptedException {
			int sum = 0;
			int count = 0;
			for (IntWritable val : values) {
				sum += val.get();
				count += 1;
			}
			context.write(key, new DoubleWritable(sum * 1.0 / count));
		}
	}

	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();

		@SuppressWarnings("deprecation")
		Job job = new Job(conf, "ApacheLogAvg");
		job.setJarByClass(BasicAverage.class);


		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(IntWritable.class);
		
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(DoubleWritable.class);

		job.setMapperClass(Map.class);
		job.setReducerClass(Reduce.class);

		job.setInputFormatClass(TextInputFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		
		
		Path output = new Path(args[1]);
		FileSystem fs = FileSystem.get(conf);		
		if (fs.exists(output)) {
			fs.delete(output, true);
		}

		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, output);

		job.waitForCompletion(true);
	}

}