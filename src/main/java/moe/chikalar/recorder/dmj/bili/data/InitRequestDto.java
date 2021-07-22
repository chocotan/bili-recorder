package moe.chikalar.recorder.dmj.bili.data;

import java.io.Serializable;

public class InitRequestDto implements Serializable {

	//用户uid 默认无登陆状态为0 非必选
	private Long uid = 0L;
	//房间号 必选
	private Long roomid;
	//协议版本 目前为2 
	private Integer protover=2;
	//平台 可以为web
	private String platform="web";
	//客户端版本 已知1.11.0 1.8.5 1.5.15 1.13.1 无用
	private String clientver="1.14.0";
	//未知 可以是2
	private Integer type = 2;
	//用户标识  通过接口https://api.live.bilibili.com/room/v1/Danmu/getConf?room_id=房间号&platform=pc&player=web获取 在里面为token
	private String key ;
	
	public InitRequestDto() {
		super();
	}


	public InitRequestDto(Long roomid, String key) {
		super();
		this.roomid = roomid;
		this.key = key;
	}

	public InitRequestDto(Long uid, Long roomid, String key) {
		super();
		this.uid = uid;
		this.roomid = roomid;
		this.key = key;
	}

	public InitRequestDto(Long uid, Long roomid, Integer protover, String platform, String clientver, Integer type,
						  String key) {
		super();
		this.uid = uid;
		this.roomid = roomid;
		this.protover = protover;
		this.platform = platform;
		this.clientver = clientver;
		this.type = type;
		this.key = key;
	}


	public Long getUid() {
		return uid;
	}


	public void setUid(Long uid) {
		this.uid = uid;
	}


	public Long getRoomid() {
		return roomid;
	}


	public void setRoomid(Long roomid) {
		this.roomid = roomid;
	}


	public Integer getProtover() {
		return protover;
	}


	public void setProtover(Integer protover) {
		this.protover = protover;
	}


	public String getPlatform() {
		return platform;
	}


	public void setPlatform(String platform) {
		this.platform = platform;
	}


	public String getClientver() {
		return clientver;
	}


	public void setClientver(String clientver) {
		this.clientver = clientver;
	}


	public Integer getType() {
		return type;
	}


	public void setType(Integer type) {
		this.type = type;
	}


	public String getKey() {
		return key;
	}


	public void setKey(String key) {
		this.key = key;
	}

}
