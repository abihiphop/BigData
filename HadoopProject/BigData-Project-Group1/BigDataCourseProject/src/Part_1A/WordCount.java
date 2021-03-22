package Part_1A;

import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class WordCount {
	public static void main(String[] args) throws Exception {
		Configuration config = new Configuration();
		String[] files = new GenericOptionsParser(config, args)
				.getRemainingArgs();
		Path input = new Path(files[0]);
		Path output = new Path(files[1]);
				
		@SuppressWarnings("deprecation")
		Job myJob = new Job(config, "wordcount");
		myJob.setJarByClass(WordCount.class);
		myJob.setMapperClass(Map.class);
		myJob.setReducerClass(Reduce.class);
		myJob.setOutputKeyClass(Text.class);
		myJob.setOutputValueClass(IntWritable.class);
		FileInputFormat.addInputPath(myJob, input);
		FileOutputFormat.setOutputPath(myJob, output);
		System.exit(myJob.waitForCompletion(true) ? 0 : 1);
	
	}

	public static class Map extends
			Mapper<LongWritable, Text, Text, IntWritable> {
	
		
		public void map(LongWritable key, Text value, Context con)
				throws IOException, InterruptedException {
			String line = value.toString().replaceAll("\"", "").replaceAll("\'", "");
			String[] words = line.split("\\s|\\-");
			for (String word : words) {
				 Matcher m = Pattern.compile("^[a-zA-Z]+[a-zA-Z?.!,;]?$").matcher(word);
			        if (m.find()) {
			        	word = word.replaceAll("[.?!;,]+", "").toLowerCase();
			        	Text outputKey = new Text(word.trim());
						IntWritable outputValue = new IntWritable(1);
						con.write(outputKey, outputValue);
			        }			
			}
		}
		
	
	}
	

	public static class Reduce extends
			Reducer<Text, IntWritable, Text, IntWritable> {
		public void reduce(Text word, Iterable<IntWritable> values, Context con)
				throws IOException, InterruptedException {
			int sum = 0;
			for (IntWritable value : values) {
				sum += value.get();
			}
			con.write(word, new IntWritable(sum));
		}

	}

}