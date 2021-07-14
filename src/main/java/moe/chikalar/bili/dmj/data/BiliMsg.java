package moe.chikalar.bili.dmj.data;

import struct.StructClass;
import struct.StructField;

import java.io.Serializable;

/**
 * @author BanqiJane
 */
@StructClass
public class BiliMsg implements Serializable {
	private static final long serialVersionUID = -8552333200751773861L;
	// 数据包长度 为int
	@StructField(order = 0)
	private int packageLength;
	// 数据包头部长度 为char 为16
	@StructField(order = 1)
	private char packageHeadLength;
	// 数据包协议版本char 0未压缩的json格式数据 1客户端心跳通常为人气值 4字节整数 2为带zlib压缩过的json格式数据 数据包协议版本 为char 有0，1，2
	@StructField(order = 2)
	private char packageVersion;
	// 数据包协议类型 int 目前已知有2，3，5，7，8
	@StructField(order = 3)
	private int packageType;
	//序列号 int 目前已知有0，1
	@StructField(order = 4)
	private int packageOther;
	private BiliMsg() {}
	public static BiliMsg getBarrageHeadHandle() {
		return new BiliMsg();
	}
	public static BiliMsg getBarrageHeadHandle(int packageLength, char packageHeadLength,
                                                                char packageVersion, int packageType,
                                                                int packageOther) {
		BiliMsg biliMsg =new BiliMsg();
		biliMsg.setPackageHeadLength(packageHeadLength);
		biliMsg.setPackageLength(packageLength);
		biliMsg.setPackageOther(packageOther);
		biliMsg.setPackageType(packageType);
		biliMsg.setPackageVersion(packageVersion);
		return biliMsg;
	}
	public int getPackageLength() {
		return packageLength;
	}
	public void setPackageLength(int packageLength) {
		this.packageLength = packageLength;
	}
	public char getPackageHeadLength() {
		return packageHeadLength;
	}
	public void setPackageHeadLength(char packageHeadLength) {
		this.packageHeadLength = packageHeadLength;
	}
	public char getPackageVersion() {
		return packageVersion;
	}
	public void setPackageVersion(char packageVersion) {
		this.packageVersion = packageVersion;
	}
	public int getPackageType() {
		return packageType;
	}
	public void setPackageType(int packageType) {
		this.packageType = packageType;
	}
	public int getPackageOther() {
		return packageOther;
	}
	public void setPackageOther(int packageOther) {
		this.packageOther = packageOther;
	}
	
}
