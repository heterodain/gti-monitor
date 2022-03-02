package com.heterodain.gtimonitor.device;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.io.ModbusSerialTransaction;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.net.SerialConnection;
import com.ghgande.j2mod.modbus.util.SerialParameters;
import com.heterodain.gtimonitor.config.DeviceConfig.Gti;

/**
 * グリッドタイインバーターデバイス
 */
@Component
@Slf4j
public class GridTieInverterDevice {
	/** GTIへのシリアル接続 */
	private Map<String, SerialConnection> connections = new ConcurrentHashMap<>();

	/**
	 * GTI接続
	 * 
	 * @param config GTI情報
	 * @throws IOException
	 */
	public void connect(Gti config) throws IOException {
		synchronized (connections) {
			// 接続
			if (!connections.containsKey(config.getComPort())) {
				log.info("GTIに接続します: {}", config);

				var params = new SerialParameters();
				params.setPortName(config.getComPort());
				params.setBaudRate(9600);
				params.setDatabits(8);
				params.setParity("None");
				params.setStopbits(1);
				params.setEncoding("rtu");
				params.setEcho(false);

				var connection = new SerialConnection(params);
				connection.open();
				connections.put(config.getComPort(), connection);
			}
		}
	}

	/**
	 * 現在の発電電力取得
	 * 
	 * @param config GTI情報
	 * @return 発電電力(W)
	 * @throws IOException
	 * @throws ModbusException
	 */
	public Double getCurrentPower(Gti config) throws IOException, ModbusException {
		// 接続取得
		var connection = connections.get(config.getComPort());
		if (connection == null) {
			throw new IOException("GTIに接続されていません。" + config);
		}

		// 読み込み
		synchronized (connection) {
			var req = new ReadMultipleRegistersRequest(86, 1);
			req.setUnitID(config.getUnitId());
			var tr = new ModbusSerialTransaction(connection);
			tr.setRequest(req);
			tr.execute();

			var res = (ReadMultipleRegistersResponse) tr.getResponse();
			return ((double) res.getRegisterValue(0)) / 10D;
		}
	}

	/**
	 * GTI接続解除
	 */
	public void disconnectAll() {
		connections.values().forEach(connection -> {
			connection.close();
		});
	}
}