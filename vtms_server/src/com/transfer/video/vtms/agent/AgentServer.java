package com.transfer.video.vtms.agent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class AgentServer {
	public static void main(String[] args) {
		AgentMapper agentMapper = new AgentMapperImpl();
		Socket client = null;

		try (ServerSocket server = new ServerSocket(80)) {
			while (true) {
				client = server.accept();
				
				String saveVideoPath = agentMapper.getProperties("path.video");
				Files.createDirectories(Paths.get(saveVideoPath));
				
				String accessKey = agentMapper.getProperties("accessKey");
				String videoName = writeVideoInfo(accessKey, client, saveVideoPath);
				
				if (videoName != null ) {
					String quitCmd = agentMapper.getProperties("cmd.taskkill");
					Process process = Runtime.getRuntime().exec(quitCmd);
					
					Thread.sleep(1000);
					process.destroy();
					
					deleteFile(new File(agentMapper.getProperties("path.video")));

					String playCmd = agentMapper.getProperties("cmd.playvideo") +" " + saveVideoPath + videoName;
					process = Runtime.getRuntime().exec(playCmd);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	
	/**
	 * 수신 정보를 받아와서 접근 키와 확장자를 구분하여 반환한다.
	 * 
	 * @param Receiveinfo 수신 정보를 받는다.
	 * @return
	 */
	private static String[] getReceiveInfo(String Receiveinfo) {
		String[] infoArray = new String[2];
		infoArray[0] = Receiveinfo.substring(0,19);
		infoArray[1] = Receiveinfo.substring(19);
		return infoArray;
	}
	
	
	/**
	 * 영상 파일을 수신 한다.
	 * 
	 * @param client 클라이언트 소켓을 받는다.
	 * @param path 영상 파일이 저장될 경로를 받는다.
	 * @return 영상 파일 명을 반환하는데 영상 파일을 저장 하지 않았을시 null을 반환한다.
	 */
	private static String writeVideoInfo(String accessKey, Socket client, String path) {
		InputStream receiveInfo = null;
		OutputStream videoFile = null;
		
		boolean isCatchAccessKey = false;
		String videoName = null;

		try {
			receiveInfo = client.getInputStream();
			
			byte[] buffer = new byte[1000];
			int readCount = 0;
			while ((readCount = receiveInfo.read(buffer)) != -1) {
				if (!isCatchAccessKey) {
					if(readCount < 23) { break; }
					
					String[] infoArray = getReceiveInfo(new String(Arrays.copyOf(buffer, 23)));
					
					if (!infoArray[0].equals(accessKey)) { break; }
					videoName = System.currentTimeMillis() + infoArray[1];
					videoFile = new FileOutputStream(path + File.separator + videoName);
					
					videoFile.write(buffer, 23, (readCount - 23));

					isCatchAccessKey = true;
				} else { videoFile.write(buffer, 0, readCount); }
			}
			
			if(isCatchAccessKey == true) {
				videoFile.flush();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (videoFile != null) { videoFile.close(); }
				if (receiveInfo != null) { receiveInfo.close(); }
				if (client != null) { client.close(); }
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return videoName;
	}
	
	/**
	 * 디렉토리 내 가장 최근 영상 파일을 제외한 모든 영상 파일을 삭제한다.
	 * 
	 * @param videoFile 영상 파일이 저장된 디렉토리는 받는다.
	 */
	private static void deleteFile(File videoFile) {
		if (videoFile.exists()) {
			File[] files = videoFile.listFiles();
			
			for (int i = 0; i < files.length - 1; i++) {
				files[i].delete();
			}
		}
	}
	
}
