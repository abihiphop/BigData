package Part_2;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.WritableComparable;

public class WritableComparablePair implements
		WritableComparable<WritableComparablePair> {
	public String left;
	public String right;

	public WritableComparablePair() {
	}

	public WritableComparablePair(String u, String v) {
		this.left = u;
		this.right = v;
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeUTF(left);
		out.writeUTF(right);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		left = in.readUTF();
		right = in.readUTF();
	}

	public static WritableComparablePair read(DataInput in) throws IOException {
		WritableComparablePair wcp = new WritableComparablePair();
		wcp.readFields(in);
		return wcp;
	}

	@Override
	public String toString() {
		return "(" + left + "," + right + ")";
	}

	@Override
	public int compareTo(WritableComparablePair secondPair) {
		int withLeft = this.left.compareTo(secondPair.left);
		if (withLeft != 0) {
			return withLeft;
		} else {
			return this.right.compareTo(secondPair.right);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((left == null) ? 0 : left.hashCode());
		result = prime * result + ((right == null) ? 0 : right.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		WritableComparablePair other = (WritableComparablePair) obj;
		if (left == null) {
			if (other.left != null)
				return false;
		} else if (!left.equals(other.left))
			return false;
		if (right == null) {
			if (other.right != null)
				return false;
		} else if (!right.equals(other.right))
			return false;
		return true;
	}

}