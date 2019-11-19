package com.transfer.video.vtms.agent;

public interface AgentMapper {
	public Device selectDeviceInfo();
	public String selectAccessKey();	
	public void insertAccessKey(String accessKey);	
	public String selectServerInfo();
	public String getProperties(String key);
}
