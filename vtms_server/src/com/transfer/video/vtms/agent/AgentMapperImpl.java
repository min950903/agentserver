package com.transfer.video.vtms.agent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Properties;

import javax.annotation.Resources;

public class AgentMapperImpl implements AgentMapper {
	private static String PROPERTIES_PATH = null;
	private static Properties AGENT_PROPERTIES = null;

	private CryptogramImpl cryptogram = new CryptogramImpl(AGENT_PROPERTIES.getProperty("key"));

	static {
		try {
			PROPERTIES_PATH = "C:" + File.separator + "agent" + File.separator 
							  + "resources" + File.separator + "agent.properties";
			AGENT_PROPERTIES = new Properties();
			AGENT_PROPERTIES.load(new FileInputStream(new File(PROPERTIES_PATH)));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Device selectDeviceInfo() {

		BufferedReader bufferedReader = null;
		Process process = null;

		Device device = new Device();

		try {
			String cmd = AGENT_PROPERTIES.getProperty("cmd.ipconfig");
			process = Runtime.getRuntime().exec(cmd);

			bufferedReader = new BufferedReader(
					new InputStreamReader(process.getInputStream(), AGENT_PROPERTIES.getProperty("charset")));

			String line = null;
			while ((line = bufferedReader.readLine()) != null) {
				if ((line.contains("호스트 이름"))) {
					String hostName = line.substring((line.lastIndexOf(" ") + 1));
					device.setHostName(hostName);
				}

				if (line.contains("무선 LAN 어댑터 Wi-Fi")) {
					while (true) {
						line = bufferedReader.readLine();

						if (line.contains("미디어 상태")) {
							break;
						} else if (line.contains("IPv4")) {
							device.setIp(line.substring(line.indexOf(":") + 2, line.lastIndexOf("(")));
							break;
						}
					}

					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (process != null) {
					process.destroy();
				}
				if (bufferedReader != null) {
					bufferedReader.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return device;
	}

	@Override
	public void insertAccessKey(String accessKey) {
		try {
			String enCryptAccessKey = cryptogram.encrypt(accessKey);

			AGENT_PROPERTIES.setProperty("accessKey", enCryptAccessKey);
			AGENT_PROPERTIES.store(new FileOutputStream(PROPERTIES_PATH), enCryptAccessKey);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public String selectAccessKey() {
		String accessKey = "";

		try {
			accessKey = cryptogram.decrypt(AGENT_PROPERTIES.getProperty("accessKey"));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return accessKey;
	}

	@Override
	public String selectServerInfo() {
		String serverIP = "";

		try {
			serverIP = cryptogram.decrypt(AGENT_PROPERTIES.getProperty("server.ip"));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return serverIP;
	}

	@Override
	public String getProperties(String key) {
		String value = this.AGENT_PROPERTIES.getProperty(key) != null
				? String.valueOf(this.AGENT_PROPERTIES.getProperty(key))
				: "";

		try {
			if ("accessKey" == key) {
				value = cryptogram.decrypt(AGENT_PROPERTIES.getProperty("accessKey"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return value;
	}
}
