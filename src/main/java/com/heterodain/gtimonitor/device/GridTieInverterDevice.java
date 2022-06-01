package com.heterodain.gtimonitor.device;

import java.io.Closeable;
import java.io.IOException;

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
public class GridTieInverterDevice implements Closeable {

	/** GTIへのシリアル接続 */
	private SerialConnection connection;
	/** RS485のユニットID */
	private Integer unitId;

	/**
	 * GTI接続
	 * 
	 * @param config GTI情報
	 * @throws IOException
	 */
	public void connect(Gti config) throws IOException {
		// 接続
		log.info("GTIに接続します: {}", config);

		var params = new SerialParameters();
		params.setPortName(config.getComPort());
		params.setBaudRate(9600);
		params.setDatabits(8);
		params.setParity("None");
		params.setStopbits(1);
		params.setEncoding("rtu");
		params.setEcho(false);

		connection = new SerialConnection(params);
		connection.open();

		unitId = config.getUnitId();
	}

	/**
	 * 現在の発電電力取得
	 * 
	 * @return 発電電力(W)
	 * @throws IOException
	 * @throws ModbusException
	 */
	public Double getCurrentPower() throws IOException, ModbusException {
		var req = new ReadMultipleRegistersRequest(86, 1);
		req.setUnitID(unitId);
		var tr = new ModbusSerialTransaction(connection);
		tr.setRequest(req);
		tr.execute();

		var res = (ReadMultipleRegistersResponse) tr.getResponse();
		return ((double) res.getRegisterValue(0)) / 10D;
	}

	@Override
	public void close() throws IOException {
		if (connection != null && connection.isOpen()) {
			connection.close();
		}
	}
}