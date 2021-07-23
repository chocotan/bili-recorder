package moe.chikalar.recorder.dmj.kugou;

import moe.chikalar.recorder.struct.StructClass;
import moe.chikalar.recorder.struct.StructField;

import java.io.Serializable;

@StructClass
public class KugouMsg implements Serializable {
    @StructField(order = 0)
    private int magic;
    @StructField(order = 1)
    private int version;
    @StructField(order = 2)
    private char type;
    @StructField(order = 3)
    private int cmd;
    @StructField(order = 4)
    private int paylod;
    @StructField(order = 4)
    private int attr;
    @StructField(order = 4)
    private int crc;
    @StructField(order = 4)
    private int skip;

}
