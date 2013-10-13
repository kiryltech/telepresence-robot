package com.epam.telepresence.web;

import android.util.Log;

import com.epam.telepresence.usb.UsbService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class RobotControlServiceClient {

	private static final byte FWD_SHIFT = 0;
	private static final byte BKWD_SHIFT = 1;
	private static final byte LEFT_SHIFT = 2;
	private static final byte RIGHT_SHIFT = 3;
	private static final byte STOP_SHIFT = 4;

	private static final Command DO_NOTHING = new Command() {
		@Override
		public void run(UsbService usbService, RobotControlServiceClient client) {}
	};

	private static final Map<String, Command> COMMANDS = new HashMap<String, Command>() {
		{
			put("<EMPTY>", 	DO_NOTHING);
			put("Forward", 	new SendByteCommand((byte) (1 << FWD_SHIFT)));
			put("Backward", new SendByteCommand((byte) (1 << BKWD_SHIFT)));
			put("Left", 	new SendByteCommand((byte) (1 << LEFT_SHIFT)));
			put("Right", 	new SendByteCommand((byte) (1 << RIGHT_SHIFT)));
			put("Stop/Rst", new SendByteCommand((byte) (1 << STOP_SHIFT)));
		}

		@Override
		public Command get(Object key) {
			Command command = super.get(key);
			return command == null ? DO_NOTHING : command;
		}
	};

	private long lastNonEmptyCommand = 0;

	public void startClient(final UsbService usbService) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (!Thread.currentThread().isInterrupted()) {
					try {
						URL url = new URL("http://207.244.68.115:8080/apps/RobotApp/Commander");
						InputStream in = url.openConnection().getInputStream();
						BufferedReader br = new BufferedReader(new InputStreamReader(in));
						String command = br.readLine();
						Log.i("URL", command);
						in.close();
						handleCommand(usbService, command);
					} catch (MalformedURLException e) {
						Log.e("RobotControlServiceClient", e.getMessage(), e);
					} catch (IOException e) {
						Log.e("RobotControlServiceClient", e.getMessage(), e);
					}
					try {
						Thread.sleep((System.currentTimeMillis() - lastNonEmptyCommand) < 15000 ? 100 : 3000);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
			}
		}).start();
	}

	private void handleCommand(UsbService usbService, String command) {
		COMMANDS.get(command).run(usbService, this);
	}

	private interface Command {
		void run(UsbService usbService, RobotControlServiceClient client);
	}

	private static class SendByteCommand implements Command {

		private byte b;

		private SendByteCommand(byte b) {
			this.b = b;
		}

		@Override
		public void run(UsbService usbService, RobotControlServiceClient client) {
			usbService.sendByte(b);
			client.lastNonEmptyCommand = System.currentTimeMillis();
		}
	}
}