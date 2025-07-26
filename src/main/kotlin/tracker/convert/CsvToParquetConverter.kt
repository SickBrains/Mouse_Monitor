package tracker.convert

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.GenericData
import org.apache.parquet.avro.AvroParquetWriter
import org.apache.parquet.hadoop.ParquetWriter
import org.apache.parquet.hadoop.metadata.CompressionCodecName
import org.apache.parquet.io.OutputFile
import org.apache.parquet.io.PositionOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import org.apache.hadoop.conf.Configuration

object CsvToParquetConverter {

    private class LocalOutputFile(private val file: File) : OutputFile {
        override fun create(blockSizeHint: Long): PositionOutputStream = createStream()
        override fun createOrOverwrite(blockSizeHint: Long): PositionOutputStream = createStream()
        override fun supportsBlockSize(): Boolean = false
        override fun defaultBlockSize(): Long = 0

        private fun createStream(): PositionOutputStream {
            val out: OutputStream = FileOutputStream(file)
            return object : PositionOutputStream() {
                private var position: Long = 0

                override fun write(b: Int) {
                    out.write(b)
                    position++
                }

                override fun write(b: ByteArray, off: Int, len: Int) {
                    out.write(b, off, len)
                    position += len
                }

                override fun getPos(): Long = position
                override fun close() = out.close()
            }
        }
    }

    fun convert(csvFile: File) {
        val lines = csvFile.readLines().drop(1)

        val schema = SchemaBuilder.record("MouseEvent").fields()
            .requiredLong("start")
            .requiredLong("end")
            .requiredInt("x")
            .requiredInt("y")
            .requiredInt("left")
            .requiredInt("right")
            .requiredInt("middle")
//            .requiredInt("x1")
//            .requiredInt("x2")
//            .requiredInt("ctrl")
//            .requiredInt("shift")
//            .requiredInt("alt")
//            .requiredInt("win")
            .requiredString("window")
            .requiredInt("repeats")
            .endRecord()

        val parquetFile = File(csvFile.absolutePath.replace(".csv", ".parquet"))

        val writer: ParquetWriter<GenericData.Record> = AvroParquetWriter.builder<GenericData.Record>(LocalOutputFile(parquetFile))
            .withSchema(schema)
            .withCompressionCodec(CompressionCodecName.SNAPPY)
            .withConf(Configuration())
            .build()

        lines.forEach { line ->
            val parts = line.split(",")
            if (parts.size < 15) return@forEach

            val record = GenericData.Record(schema).apply {
                put("start", parts[0].toLong())
                put("end", parts[1].toLong())
                put("x", parts[2].toInt())
                put("y", parts[3].toInt())
                put("left", parts[4].toInt())
                put("right", parts[5].toInt())
                put("middle", parts[6].toInt())
//                put("x1", parts[7].toInt())
//                put("x2", parts[8].toInt())
//                put("ctrl", parts[9].toInt())
//                put("shift", parts[10].toInt())
//                put("alt", parts[11].toInt())
//                put("win", parts[12].toInt())
                put("window", parts[13].removeSurrounding("\""))
                put("repeats", parts.getOrNull(14)?.toInt() ?: 0)
            }

            writer.write(record)
        }

        writer.close()

        val metadata = MetadataCollector.collect()
        val jsonFile = File(csvFile.absolutePath.replace(".csv", ".meta.json"))
        jacksonObjectMapper()
            .writerWithDefaultPrettyPrinter()
            .writeValue(jsonFile, metadata)
    }
}
