package mastodonc;

import java.io.IOException;
import java.io.BufferedInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.compress.CodecPool;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.apache.hadoop.io.compress.Decompressor;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;


public class BaldrRecordReader extends RecordReader<LongWritable, BytesWritable> {
    private FSDataInputStream fsin;
    private BufferedInputStream in;
    private Decompressor decompressor;

    private final int RECORD_LENGTH_SIZE = 8;

    private LongWritable currentKey = new LongWritable(0);
    private BytesWritable currentValue = new BytesWritable();

    private boolean isFinished = false;
    private int pos = 0;

    @Override
    public void initialize(InputSplit inputSplit, TaskAttemptContext taskAttemptContext)
        throws IOException, InterruptedException {

        FileSplit split = (FileSplit)inputSplit;
        Configuration conf = taskAttemptContext.getConfiguration();
        Path path = split.getPath();
        FileSystem fs = path.getFileSystem(conf);

        // Open the stream
        fsin = fs.open(path);

        CompressionCodec codec = new CompressionCodecFactory(conf).getCodec(path);

        if (codec != null) {
            decompressor = CodecPool.getDecompressor(codec);
            in = new BufferedInputStream(codec.createInputStream(fsin, decompressor));
        } else {
            in = new BufferedInputStream(fsin);
        }

     }

    @Override
    public boolean nextKeyValue() throws IOException {

        byte[] recordLengthBytes = new byte[RECORD_LENGTH_SIZE];

        int bytesRead = in.read(recordLengthBytes, 0, RECORD_LENGTH_SIZE);

        pos = pos + RECORD_LENGTH_SIZE;

        if (bytesRead < 0) {
            isFinished = true;
            return false;
        }

        ByteBuffer recordLengthBuffer = ByteBuffer.wrap(recordLengthBytes);
        recordLengthBuffer.order(ByteOrder.BIG_ENDIAN);
        long recordLengthLong = recordLengthBuffer.getLong();

        if (recordLengthLong > Integer.MAX_VALUE) {
            throw new RuntimeException("record larger than expected length: " + recordLengthLong
                                       + " data looks like: " + new String(recordLengthBytes, "UTF8"));
        }

        if (recordLengthLong < 0) {
            throw new RuntimeException("record is a negative length: " + recordLengthLong
                                       + " data looks like: " + new String(recordLengthBytes, "UTF8"));
        }

        int recordLength = (int)recordLengthLong;
        byte[] recordBytes = new byte[recordLength];
        in.read(recordBytes, 0, recordLength);

        pos = pos + recordLength;
        currentKey.set(currentKey.get() + 1);
        currentValue.set(recordBytes, 0, recordLength);

        return true;
    }

    @Override
    public void close() throws IOException {
        if (fsin != null) {
            fsin.close();
        }
    }

    /**
     * Rather than calculating progress, we just keep it simple
     */
    @Override
    public float getProgress()
        throws IOException, InterruptedException
    {
        return isFinished ? 1f : 0f;
    }

     /**
      * Returns the current key, which is the index of the baldr
      * record in the file being processed.
      */
    @Override
    public LongWritable getCurrentKey() throws IOException, InterruptedException {
        return currentKey;
    }

    /**
     * Returns the current baldr record
     */
    @Override
    public BytesWritable getCurrentValue() throws IOException, InterruptedException {
        return currentValue;
    }

}
